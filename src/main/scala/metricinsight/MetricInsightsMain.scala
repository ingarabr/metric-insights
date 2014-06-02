package metricinsight

import java.io.File
import org.elasticsearch.node.NodeBuilder
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.rest.RestController
import metricinsight.es.EsPlan
import org.elasticsearch.node.internal.InternalNode


object MetricInsightsMain extends App {

  override def main(args: Array[String]): Unit = {
    super.main(args)
    println("Starting app!")

    println("Starting ElasticSearch")
    val node = NodeBuilder.nodeBuilder()
      .settings(ImmutableSettings.builder()
      .put("http.enabled", false) // get from config!
    ).build
    node.start()

    val restController = node.asInstanceOf[InternalNode].injector.getInstance(classOf[RestController])

    println("Starting actorSystem")
    val actorSystem = new ActorSystemSetup(node.client(), Configuration.fetchersConfig())

    println("Starting webserver")
    initWebServer(Configuration.serverConfig(), restController)

    println("Shutting down...")

    actorSystem.stop()
    node.stop()
    println("Done!")
  }

  def initWebServer(cfg: ServerConfig, restController: RestController) {
    val webapp = new File("src/main/webapp/").toURI.toURL
    unfiltered.jetty.Http(cfg.port)
      .plan(EsPlan(restController))
      .resources(webapp)
      .run()
  }

}
