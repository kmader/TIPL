package tipl.deepzoom

import java.io.{FileInputStream, BufferedInputStream, File}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.server.{Request, Server}
import org.eclipse.jetty.server.handler.AbstractHandler

/**
 * A simple webserver for hosting and allowing interaction with deepzoom objects backed by RDDs
 * Created by mader on 10/31/14.
 */

object JDZServer {
  def main(args: Array[String]) {
    val server: Server = new Server(8080)
    server.setHandler(new JDZServer)
    server.start
    server.join
  }
}

class JDZServer extends AbstractHandler {

  case class HandleObj(target: String, baseRequest: Request, request: HttpServletRequest,
                       response: HttpServletResponse) extends Serializable

  def handle(target: String, baseRequest: Request, request: HttpServletRequest,
             response: HttpServletResponse) {
    val ho = HandleObj(target,baseRequest,request,response)

    target match {
      case "" | "/" | "/index.html" | "/index.htm" => baseUrl(ho)
      case _ => missingUrl(ho)
    }
  }
  def baseUrl(ho: HandleObj) = {
    ho.response.setContentType("text/html;charset=utf-8")
    ho.response.setStatus(HttpServletResponse.SC_OK)
    ho.baseRequest.setHandled(true)
    ho.response.getWriter.println("<h1>Hello World</h1><h2>"+ho.target+"</h2><h2>PM: "+ho
      .baseRequest.getParameterMap().toString+"</h2><h2>QS: "+ho.request.getQueryString()+"</h2>")

  }

  def missingUrl(ho: HandleObj) = {
    ho.response.setContentType("text/html;charset=utf-8")
    ho.response.setStatus(HttpServletResponse.SC_OK)
    ho.baseRequest.setHandled(true)
    ho.response.getWriter.println("<h1>404 The path you are looking can't be a found</h1>"+ho
      .baseRequest.toString)
  }

  def sendImage(ho: HandleObj,image: File, format: String = "jpeg") = {
    ho.response.setContentType("image/"+format)
    ho.response.setStatus(HttpServletResponse.SC_OK)
    ho.baseRequest.setHandled(true)
    val bis = new BufferedInputStream(new FileInputStream(image))
    while (bis.available() != 0) {
      ho.response.getWriter.write(bis.read())
    }

  }


}