package metricinsight.es

import org.elasticsearch.rest.RestRequest
import java.util
import java.lang.Iterable
import java.util.Map.Entry
import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.rest.RestRequest.Method
import unfiltered.request.HttpRequest
import org.elasticsearch.common.io.Streams
import javax.servlet.http.HttpServletRequest
import org.elasticsearch.rest.support.RestUtils

class EsRestRequest(req: HttpRequest[HttpServletRequest]) extends RestRequest {

  val servletRequest = req.underlying
  val cont = Streams.copyToByteArray(req.inputStream)
  val paramerers = new util.HashMap[String, String]()

  if (servletRequest.getQueryString != null) RestUtils.decodeQueryString(servletRequest.getQueryString, 0, paramerers)

  def method(): Method = Method.valueOf(req.method)

  def uri(): String = {
    val queryString = servletRequest.getQueryString
    if (queryString != null && !queryString.trim().isEmpty) {
      servletRequest.getRequestURI.substring(
        servletRequest.getContextPath.length() + servletRequest.getServletPath.length()) + "?" + queryString
    } else {
      servletRequest.getRequestURI.substring(
        servletRequest.getContextPath.length() + servletRequest.getServletPath.length())
    }
  }

  def rawPath(): String = {
    val p = servletRequest.getRequestURI.substring(3
      //  servletRequest.getContextPath.length() + servletRequest.getServletPath.length()
    )
    println(s"rawPath: $p")
    p
  }

  def hasContent: Boolean = cont.length > 0

  def contentUnsafe(): Boolean = false

  def content(): BytesReference = new BytesArray(cont)

  def header(name: String): String = servletRequest.getHeader(name)

  import scala.collection.JavaConversions._

  def headers(): Iterable[Entry[String, String]] = //new util.ArrayList[Entry[String, String]]()
    req.headerNames.map(n => n -> header(n)).toMap[String, String].entrySet()

  def hasParam(key: String): Boolean = paramerers.containsKey(key)

  def param(key: String): String = paramerers.get(key)

  def params(): util.Map[String, String] = paramerers

  def param(key: String, defaultValue: String): String = Option(paramerers.get(key)).getOrElse(defaultValue)

}
