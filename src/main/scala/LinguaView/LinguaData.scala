package LinguaView

import java.io._
import java.util
import javax.xml.parsers.DocumentBuilderFactory

import syntax._
import org.w3c.dom._
import org.xml.sax.InputSource

/**
  * Created by draplater on 16-8-23.
  */
class LinguaData private(_metadata: MetadataManager,
                         _constTreebank: util.ArrayList[ConstTree],
                         _depTreebank: util.ArrayList[DepTree],
                         _deepdepTreebank: util.ArrayList[DepTree],
                         _CCGTreebank: util.ArrayList[CCGNode],
                         _LFGStructbank: util.ArrayList[Element]
                        ) {
  val metadata: MetadataManager = _metadata
  val constTreebank = _constTreebank
  val depTreebank = _depTreebank
  val deepdepTreebank = _deepdepTreebank
  val CCGTreebank = _CCGTreebank
  val LFGStructbank = _LFGStructbank
}

object LinguaData {
  def importFromReader(reader: Reader): LinguaData = {
    val factory = DocumentBuilderFactory.newInstance
    val builder = factory.newDocumentBuilder
    val doc: Document = builder.parse(
      new InputSource(
        new BufferedReader(reader)))

    val docRoot: Element = doc.getDocumentElement // node "viewer"
    var children: NodeList = docRoot.getChildNodes
    // "Success" indicates whether the whole import operation has been successfully implemented
    val sentences = new util.ArrayList[Element]
    var viewerNode: Node = null
    var i: Int = 0
    while (i < children.getLength) {
      val child: Node = children.item(i)
      if (child.getNodeName == "sentence" && child.isInstanceOf[Element]) {
        sentences.add(child.asInstanceOf[Element])
      }
      if (child.getNodeName == "viewer" && child.isInstanceOf[Element]) {
        viewerNode = child
      }
      i += 1
    }
    val constTreebank: util.ArrayList[ConstTree] = new util.ArrayList[ConstTree]
    val depTreebank: util.ArrayList[DepTree] = new util.ArrayList[DepTree]
    val deepdepTreebank: util.ArrayList[DepTree] = new util.ArrayList[DepTree]
    val CCGTreebank: util.ArrayList[CCGNode] = new util.ArrayList[CCGNode]
    val LFGStructbank: util.ArrayList[Element] = new util.ArrayList[Element]
    var Success: Boolean = true
    var sentNum: Int = 0
    sentNum = 0
    //iterate through all the sentences
    while (sentNum < sentences.size) {
      // indicate if each grammar tree is loaded for this sentence
      // if not, use a default tree to fill in
      val sent: Element = sentences.get(sentNum)
      var constTreeAdded: Boolean = false
      var depTreeAdded: Boolean = false
      var deepdepTreeAdded: Boolean = false
      var CCGTreeAdded: Boolean = false
      var LFGStructAdded: Boolean = false

      // get the i-th sentence
      if (sent.isInstanceOf[Element]) {
        val sentChildren: NodeList = sent.getChildNodes
        var tokenls: Element = null
        var j: Int = 0
        // iterate through all the elements in a sentence
        while (j < sentChildren.getLength) {
          val sentElement: Node = sentChildren.item(j)

          // extract element for wordlist
          // but it is only loaded when <deptree> tag is encountered
          if (sentElement.isInstanceOf[Element] && (sentElement.getNodeName eq "wordlist")) {
            tokenls = sentElement.asInstanceOf[Element]
          }
          // extract and load constituent tree
          else if (sentElement.isInstanceOf[Element] && (sentElement.getNodeName eq "constree")) {
            children = sentElement.getChildNodes
            var constStr: String = new String
            var k: Int = 0
            while (k < children.getLength) {
              val constTextNode: Node = children.item(k)
              if (constTextNode.isInstanceOf[Text]) {
                constStr = (constTextNode.asInstanceOf[Text]).getTextContent
              }
              k += 1
            }
            if (!constStr.trim.isEmpty) {
              val constTree: ConstTree = ConstTree.ConstTreeIO.ReadConstTree(constStr)
              constTreebank.add(constTree)
            } else {
              constTreebank.add(null)
            }
            constTreeAdded = true
          }
          //extract and load dependency tree & wordlist
          else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "deptree")) {
            children = sentElement.getChildNodes
            var depStr: String = new String
            var k: Int = 0
            while (k < children.getLength) {
              val depTextNode: Node = children.item(k)
              if (depTextNode.isInstanceOf[Text]) {
                depStr = (depTextNode.asInstanceOf[Text]).getTextContent
              }
              k += 1
            }
            if (!depStr.trim.isEmpty) {
              val depTree: DepTree = new DepTree
              depTree.loadTokens(tokenls)
              depTree.loadEdges(depStr.trim)
              depTreebank.add(depTree)
            }
            else {
              depTreebank.add(null)
            }
            depTreeAdded = true
          }
          //extract and load deep dependency graph & wordlist
          else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "deepdep")) {
            children = sentElement.getChildNodes
            var depStr: String = new String
            var k: Int = 0
            while (k < children.getLength) {
              val depTextNode: Node = children.item(k)
              if (depTextNode.isInstanceOf[Text]) {
                depStr = (depTextNode.asInstanceOf[Text]).getTextContent
              }
              k += 1
            }
            if (!depStr.trim.isEmpty) {
              val depTree: DepTree = new DepTree
              depTree.loadTokens(tokenls)
              depTree.loadEdges(depStr.trim)
              deepdepTreebank.add(depTree)
            }
            else {
              deepdepTreebank.add(null)
            }
            deepdepTreeAdded = true
          }
          // extract LFG structure
          // it is loaded in LFGcomponent.init()
          else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "lfg")) {
            LFGStructbank.add(sentElement.asInstanceOf[Element])
            LFGStructAdded = true
          }
          // extract and load CCG
          else if ((sentElement.isInstanceOf[Element]) && (sentElement.getNodeName eq "ccg")) {
            children = sentElement.getChildNodes
            var CCGStr: String = new String
            var k: Int = 0
            while (k < children.getLength) {
              val CCGTextNode: Node = children.item(k)
              if (CCGTextNode.isInstanceOf[Text]) {
                CCGStr = (CCGTextNode.asInstanceOf[Text]).getTextContent
              }
              k += 1;
            }
            if (CCGStr.trim.matches("^\\( *\\)$")) {
              val CCGTree: CCGNode = new CCGTerminalNode("<L _ _ _ _ _>", 0)
              CCGTreebank.add(CCGTree)
            }
            else if (!CCGStr.trim.isEmpty) {
              CCGStr = CCGStr.replace('{', '<')
              CCGStr = CCGStr.replace('}', '>')
              val CCGTree: CCGNode = CCGNode.getCCGNodeFromString(CCGStr.trim)
              CCGTreebank.add(CCGTree)
            }
            else {
              CCGTreebank.add(null)
            }
            CCGTreeAdded = true
          }
          j += 1;
        }
      }
      sentNum += 1
    }
    new LinguaData(
      if(viewerNode == null) {
        null
      } else {
        new MetadataManager(viewerNode)
      },
      constTreebank, depTreebank, deepdepTreebank, CCGTreebank, LFGStructbank)
  }

  def importFromFile(filename: String): LinguaData = {
    val factory = DocumentBuilderFactory.newInstance
    val builder = factory.newDocumentBuilder
    val doc0: Document = builder.parse(new InputSource(new FileReader(filename)))
    val encoding = doc0.getXmlEncoding

    LinguaData.importFromReader(
      new BufferedReader(
        new InputStreamReader(
          new FileInputStream(filename), encoding)))
  }

  def importFromString(input: String): LinguaData = {
    LinguaData.importFromReader(new StringReader(input))
  }
}