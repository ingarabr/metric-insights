package metricinsight

import org.scalatest._
import scala.concurrent.duration._
import akka.testkit._
import akka.actor.{ActorSystem, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import dispatch.{as, Req}
import scala.concurrent.Future


class MetricFetcherTest extends TestKit(ActorSystem("MetricFetcherSpec"))
with DefaultTimeout with ImplicitSender with FlatSpecLike with Matchers with BeforeAndAfterAll {


  override def afterAll() {
    shutdown()
  }

  it should "modify metrics" in {
    within(3000 millis) {
      val writer = new TestProbe(system)
      val cfg = FetcherConfig("localhost", 1000, Map())
      val props: Props = Props(new  MetricFetcher(writer.ref, cfg) with RestStub )
      val fetcher = system.actorOf(props)

      fetcher ! Messages.GetMetrics

      val result = writer.expectMsgClass(classOf[Messages.Metric])
      result.jsonMetric should startWith( "{\"@timestamp\" : ")
    }
  }

  trait RestStub extends HttpRequest {

    override def request(req: Req): Future[String] = {
      dispatch.Future { """{"metric":"spec"}"""}
    }
  }
}
