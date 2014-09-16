package metricinsight.es

import org.elasticsearch.rest.{RestResponse, RestChannel}
import unfiltered.response.HttpResponse
import javax.servlet.http.HttpServletResponse
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import org.slf4j.LoggerFactory

case class EsRestChannel(req: EsRestRequest, resWrapper: HttpResponse[HttpServletResponse]) extends RestChannel {

  val logger = LoggerFactory.getLogger(classOf[EsRestChannel])
  val res = resWrapper.underlying
  val lock = new CountDownLatch(1)

  var fault: Option[Exception] = None

  def sendResponse(response: RestResponse) = failable {
    res.setStatus(response.status().getStatus)
    res.setContentType(response.contentType())
    val opaque = Option(req.header("X-Opaque-Id"))
    if (opaque.isDefined) res.addHeader("X-Opaque-Id", opaque.get)
    res.setContentLength(Option(response.suffixContentLength()).getOrElse(0) +
      Option(response.prefixContentLength()).getOrElse(0) +
      Option(response.contentLength()).getOrElse(0))

    val stream: OutputStream = resWrapper.outputStream
    try {
      if (response.prefixContentLength() > 0) stream.write(response.prefixContent(), 0, response.prefixContentLength())
      stream.write(response.content(), 0, response.contentLength())
      if (response.suffixContentLength() > 0) stream.write(response.suffixContent(), 0, response.suffixContentLength())
    } finally {
      stream.close()
      lock.countDown()
    }
  }

  def failable(canFail: => Any): Unit = {
    try {
      canFail
    } catch {
      case e: Exception => {
        logger.warn("Failed to respond", e)
        e.printStackTrace(System.err)
        fault = Some(e)
      }
    }
  }

  def waitForResponseFromElasticSearch() = {
    lock.await()
  }

}
