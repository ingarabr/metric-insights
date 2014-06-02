package metricinsight

import scala.collection.JavaConverters._
import com.typesafe.config.{ConfigFactory, Config}
import java.io.File
import scala.collection.mutable
import scala.util.Try

object Configuration {
  val conf = ConfigFactory.parseFile(new File("./app.conf")).withFallback(
    ConfigFactory.parseResources("appDefault.conf"))

  def serverConfig(): ServerConfig = {
    ServerConfig(conf.getInt("server.port"))
  }

  def fetchersConfig(): List[FetcherConfig] = {
    if (conf.hasPath("fetchers")) {
      val list: mutable.Buffer[_ <: Config] = conf.getConfigList("fetchers").asScala
      list.map(FetcherConfig(_)).toList
    } else {
      println("Unable to find fetcher")
      List()
    }

  }

}

case class ServerConfig(port: Int)

case class FetcherConfig(host: String, interval: Int, tags: Map[String, String])

object FetcherConfig {

  def apply(cfg: Config): FetcherConfig = {
    val orElse: Try[Int] = Try(cfg.getInt("interval"))
      .orElse(Try(Configuration.conf.getInt("fetchersDefault.interval")))
    cfg.getConfigList("tags").asScala.map(c => c.entrySet().asScala.map(cf => cf.getKey)) //todo tags
    FetcherConfig(cfg.getString("host"), orElse.get, Map())
  }
}

object EsConfig {
  val esIndexConfig = Map(
    "template" -> "metrics*",
    "mappings" -> Map(
      "_default_" -> Map(
        "dynamic_templates" -> List(Map(
          "string_template" -> Map(
            "match" -> "*",
            "match_mapping_type" -> "string",
            "mapping" -> Map(
              "type" -> "string",
              "index" -> "not_analyzed"
            )
          )
        ))
      )
    )
  )

}
