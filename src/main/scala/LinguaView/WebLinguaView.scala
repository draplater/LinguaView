package LinguaView

import java.awt.{Color, Graphics}
import java.io._
import java.util
import java.util.Scanner
import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}

import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import syntax.LFGStructRenderer
import org.eclipse.swt.SWT
import org.eclipse.swt.browser.{Browser, BrowserFunction}
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.{Display, Shell}
import org.w3c.dom.{Document, Element}

/**
  * Created by draplater on 16-7-20.
  */
object WebLinguaView {

  def main(args: Array[String]): Unit = {
    val display = new Display
    val shell = new Shell(display)
    shell.setLayout(new FillLayout)
    val browser = new Browser(shell, SWT.NONE)
    bindFunctionToBrowser(browser, "getSVGFromJava", (args) => getLFG)
    browser.setUrl(WebLinguaView.getClass.getResource("/web/svg.html").getFile)
    shell.open()
    while (!shell.isDisposed) {
      if (!display.readAndDispatch())
        display.sleep()
    }
    display.dispose()
  }

  def getLFG: String = {
    // default LFG structure initialization
    val LFGStructbank: util.ArrayList[Element] = new util.ArrayList[Element]
    // Read LFG default config from default config file
    var defaultLFGStr: String = ""
    val scanner: Scanner = new Scanner(getClass.getResourceAsStream("/raw/default_lfg.xml"))
    try {
      // Read all content from file.
      defaultLFGStr = scanner.useDelimiter("\\A").next()
    } finally {
      if (scanner != null) scanner.close()
    }
    // }
    val stream: InputStream = new ByteArrayInputStream(defaultLFGStr.getBytes("UTF-8"))
    val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    val builder: DocumentBuilder = factory.newDocumentBuilder
    val doc: Document = builder.parse(stream)
    val defaultLFGStructbank = doc.getDocumentElement
    LFGStructbank.add(defaultLFGStructbank)
    val domImpl = GenericDOMImplementation.getDOMImplementation

    // Create an instance of org.w3c.dom.Document.
    val svgNS = "http://www.w3.org/2000/svg"
    val document = domImpl.createDocument(svgNS, "svg", null)

    // Create an instance of the SVG Generator.
    val g = new SVGGraphics2D(document)
    val lfgRenderer = new LFGStructRenderer(g)
    lfgRenderer.loadTreebank(LFGStructbank)
    lfgRenderer.init()
    lfgRenderer.render()
    val out = new StringWriter()
    g.stream(out, true) // UseCSS=true
    val ret = out.toString
    out.close()
    ret
  }

  /**
    * A wrapper for SWT BrowserFunction
 *
    * @param browser browser object to bind
    * @param name function name in JavaScript
    * @param func function to bind
    * @return anything to return to JavaScript
    */
  def bindFunctionToBrowser(browser: Browser,
                            name: String,
                            func: (Array[Object]) => Object): BrowserFunction = {
    new BrowserFunction(browser, name) {
      override def function(args: Array[Object]): Object = func(args)
    }
  }
}
