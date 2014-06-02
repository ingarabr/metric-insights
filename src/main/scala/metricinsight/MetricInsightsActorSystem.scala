package metricinsight

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import org.elasticsearch.client.Client
import scala._
import dispatch.{url, Req, Http, as}
import scala.concurrent.duration._
import metricinsight.Messages.{Metric, GetMetrics}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.parsing.json.{JSONObject, JSON}

class ActorSystemSetup(esClient: Client, fetchersCfg: List[FetcherConfig]) {
  val system = ActorSystem.create("MetricInsights")
  val writer = system.actorOf(Props(new ElasticSearchWriter(esClient)))
  val fetchers = fetchersCfg.map(cfg => {
    val actor = system.actorOf(Props(new MetricFetcher(writer, cfg)))
    system.scheduler.schedule(100 milliseconds, cfg.interval seconds, actor, GetMetrics)
  })

  def stop() {
    system.shutdown()
  }
}

class ElasticSearchWriter(esClient: Client) extends Actor {
  def receive: Actor.Receive = {
    case Metric(jsonStr) => esClient.prepareIndex("metrics", "metric").setSource(jsonStr).get()
    case msg => unhandled(msg)
  }
}

class MetricFetcher(esWriter: ActorRef, cfg: FetcherConfig) extends Actor with HttpRequest {
  val metricsRequest: Req = url(cfg.host)

  def receive: Actor.Receive = {
    case GetMetrics => {
      val time = System.currentTimeMillis()
      val http: Future[String] = request(metricsRequest)
      for (content <- http) {
        esWriter ! Metric(modifyMetric(content, time))
      }
    }
    case msg => unhandled(msg)
  }

  def modifyMetric(content: String, time: Long): String = {
    val m = Map(
      "@timestamp" -> System.currentTimeMillis(),
      "tags" -> JSONObject(cfg.tags),
      "metrics" -> JSON.parseRaw(content).get
    )
    JSONObject.apply(m).toString()
  }

}

trait HttpRequest {
  def request(req: Req): Future[String] = {
    Http(req OK as.String)
  }
}

object Messages {

  object GetMetrics

  case class Metric(jsonMetric: String)

}
