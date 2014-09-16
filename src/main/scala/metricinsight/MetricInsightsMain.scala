package metricinsight

import java.io.File
import org.elasticsearch.node.NodeBuilder
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.rest.RestController
import metricinsight.es.EsPlan
import org.elasticsearch.node.internal.InternalNode
import org.slf4j.LoggerFactory

class MetricInsightsMain() {
  val logger = LoggerFactory.getLogger(classOf[MetricInsightsMain])
  logger.info("Starting app!")

  val webapp = new File("src/main/webapp/").toURI.toURL

  val node = NodeBuilder.nodeBuilder()
    .settings(ImmutableSettings.builder()
    .put("http.enabled", false) // get from config!
  ).build

  val actorSystem = new ActorSystemSetup(node.client(), Configuration.fetchersConfig())
  val restController = node.asInstanceOf[InternalNode].injector.getInstance(classOf[RestController])

  node.start()

  unfiltered.jetty.Http(Configuration.serverConfig().port)
    .plan(EsPlan(restController))
    .resources(webapp)
    .run()

  actorSystem.stop()
  node.stop()
  logger.info("Stopping app!")

}

object MetricInsightsMain extends App {

  override def main(args: Array[String]): Unit = {
    new MetricInsightsMain()
  }

}
