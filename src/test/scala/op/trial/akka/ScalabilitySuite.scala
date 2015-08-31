package op.trial.akka

import akka.actor.{Actor, Props, ActorSystem}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._
import scala.language.postfixOps
import ServerActor._
import ScalabilitySuite._

class ScalabilitySuite extends TestKit(ActorSystem("ScalabilitySuit"))
                          with WordSpecLike
                          with BeforeAndAfterAll {

  override protected def afterAll() =  system.shutdown()

  val server = system.actorOf(Props(new FakeServer))
  case object WorkerCreated

  "Server" must {
    "process no more then 8 requests at once" in {
      var workers = 0
      for (i <- 1 to 10) {
        server ! Service(Props[FakeWorker], null)
      }
      receiveWhile(1 second) {
        case WorkerCreated => workers += 1
      }
      fail("to be failed, test not completed!")
    }
  }
}

object ScalabilitySuite {
  import op.trial.akka.util.FakeLifeCicleAware
  class FakeWorker() extends WorkerActor {
    def work() {}
  }
  class FakeServer extends ServerActor with FakeLifeCicleAware {
    def success(res: Any, exchange: Unit) {}
    def failure(cause: Throwable, exchange: Unit) {}
    def receive: Actor.Receive = { case _ => () }
  }
}
