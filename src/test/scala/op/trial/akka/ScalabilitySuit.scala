package op.trial.akka

import akka.actor.{Actor, Props, ActorSystem}
import akka.testkit.TestKit
import com.sun.net.httpserver.HttpExchange
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._
import scala.language.postfixOps
import ServerActor._
import ScalabilitySuit._

object ScalabilitySuit {
  class FakeWorker() extends Actor() {
    val receive: Receive = { case _ => () }
  }
  class FakeServer extends ServerActor with FakeServerAware {
    def initWorker(workerProps: Props, exchange: HttpExchange) {}
    def respond(status: Int, body: Array[Byte]) {}
    def receive: Actor.Receive = { case _ => () }
  }
  trait FakeServerAware extends ServerAware {
    def startUp() {}
    def shutDown() {}
  }
}

class ScalabilitySuit extends TestKit(ActorSystem("ScalabilitySuit"))
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
    }
  }
}

