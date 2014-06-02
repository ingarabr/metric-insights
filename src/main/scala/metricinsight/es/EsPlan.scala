package metricinsight.es

import org.elasticsearch.rest.RestController
import unfiltered.filter.Plan
import unfiltered.request._
import unfiltered.response.{HttpResponse, Responder, Pass}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

case class EsPlan(restController: RestController) extends Plan {
  override def intent: Plan.Intent = {
    case req@GET(Path(Seg("es" :: tail))) => ElasticSearchResponse(req, restController)
    case req@POST(Path(Seg("es" :: tail))) => ElasticSearchResponse(req, restController)
    case _ => Pass
  }
}

case class ElasticSearchResponse(req: HttpRequest[HttpServletRequest],
                                 restController: RestController) extends Responder[HttpServletResponse] {
  def respond(res: HttpResponse[HttpServletResponse]): Unit = {
    val request: EsRestRequest = new EsRestRequest(req)
    val channel: EsRestChannel = EsRestChannel(request, res)
    restController.dispatchRequest(request, channel)
    channel.waitForResponseFromElasticSearch()
  }

}
