package LinguaView

import java.io._
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.apache.commons.io.IOUtils
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.ReferencedSVGGraphics2D
import syntax.LFGStructRenderer

/**
  * Created by draplater on 16-7-20.
  */
object LinguaViewServer {

  implicit def toHttpHandler(f: HttpExchange => Unit): HttpHandler =
    new HttpHandler {
      override def handle(httpExchange: HttpExchange): Unit = f(httpExchange)
    }

  /**
    * treebanks: Map[Filename -> LFGStructRenderer]
    */
  val treebanks: Map[String, LFGStructRenderer] =
    initTreebanks(new File("./test-provider/"))

  /**
    * Load all XML files from dir
    *
    * @param dir which dir to load
    * @return Map[Filename -> LFGStructRenderer]
    */
  def initTreebanks(dir: File): Map[String, LFGStructRenderer] = {
    dir.listFiles().filter(f => {
      f.getName.endsWith(".xml")
    }).map(f => {
      val lfgRenderer = new LFGStructRenderer()
      lfgRenderer.loadTreebank(LinguaData.importFromFile(f.getCanonicalPath).LFGStructbank)
      lfgRenderer.init()
      (f.getName, lfgRenderer)
    }).toMap
  }

  def main(args: Array[String]): Unit = {
    // start http server
    val server: HttpServer = HttpServer.create(new InetSocketAddress(8000), 0)
    server.createContext("/api/lfg", {t:HttpExchange => lfgHandler(t)} ) // handle lfg render
    server.createContext("/api/lfg_from_xml",
      {t:HttpExchange => lfgFromXMLHandler(t)} ) // handle lfg from xml
    server.createContext("/", new FileHandler("/web/")) // handle web files
    server.setExecutor(null)
    server.start()
  }

  /**
    * HTTP GET: get SVG of sentence in local treebank
    *
    * @param t HTTP GET param must contains treebank
    */
  def lfgHandler(t: HttpExchange) {
    try {
      val query = t.getRequestURI.getQuery
      val (status_code, response) = if(query == null) {
        (400, "400 Bad Request".getBytes) // no query
      } else {
        val params = queryToMap(query)
        val treebank = params.get("treebank")
        val sent_id = params.get("sent_id")
        if(treebank == null || sent_id == null) {
          (400, "400 Bad Request".getBytes) // no treebank or sent_id
        } else {
          val bank = treebanks(treebank)
          bank.goToSentence(Integer.parseInt(sent_id) - 1)
          bank.init()
          (200, renderSVG(bank).getBytes) // use SVG XML String as Response
        }
      }
      // write response
      t.sendResponseHeaders(status_code, response.length)
      val os: OutputStream = t.getResponseBody
      os.write(response)
      os.close()
    } catch {
        case e: Exception => e.printStackTrace()
    }
  }

  /**
    * HTTP POST: get SVG of sentence from post body
    *
    * @param t HTTP GET param must contains LinguaView XML
    */
  def lfgFromXMLHandler(t: HttpExchange): Unit = {
    try {
      val xml: Array[Byte] = IOUtils.toByteArray(t.getRequestBody)
      val testXMLString: String = new String(xml)
      val re_charset = "encoding=\"([^\"]+)\"".r
      val charset =re_charset.findFirstMatchIn(testXMLString).map(_.group(1)).get
      val xmlString = new String(xml, Charset.forName(charset))
      val lfgRenderer = new LFGStructRenderer()
      lfgRenderer.loadTreebank(LinguaData.importFromString(xmlString).LFGStructbank)
      lfgRenderer.init()
      val response = renderSVG(lfgRenderer).getBytes
      t.sendResponseHeaders(200, response.length)
      val os: OutputStream = t.getResponseBody
      os.write(response)
      os.close()
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  /**
    * Render SVG from LFGStructRenderer
    *
    * @param lfgRenderer LFGStructRenderer
    * @return SVG XML String
    */
  def renderSVG(lfgRenderer: LFGStructRenderer): String = {
    val domImpl = GenericDOMImplementation.getDOMImplementation

    // Create an instance of org.w3c.dom.Document.
    val svgNS = "http://www.w3.org/2000/svg"
    val document = domImpl.createDocument(svgNS, "svg", null)

    // Create an instance of the SVG Generator.
    val g = new ReferencedSVGGraphics2D(document)
    lfgRenderer.render(g)
    val out = new StringWriter()
    g.stream(out, true) // UseCSS=true
    val ret = out.toString
    out.close()
    ret
  }

  /**
    * Convert HTTP GET query to String
    * (This code is converted from Java)
    *
    * @param query HTTP GET query like: a=1&b=2
    * @return util.Map
    */
  def queryToMap(query: String): util.Map[String, String] = {
    val result: util.Map[String, String] = new util.HashMap[String, String]
    for (param <- query.split("&")) {
      val pair = param.split("=")
      if (pair.length > 1) {
        result.put(pair(0), pair(1))
      }
      else {
        result.put(pair(0), "")
      }
    }
    result
  }
}
