package op.trial.akka

import akka.actor.{Actor, ActorRef, Props, ActorSystem}
import akka.testkit.TestKit
import com.sun.net.httpserver.HttpExchange
import op.trial.akka.ScalabilitySuit.FakeLifeCicleAware
import op.trial.akka.ServerActor._
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FunSuiteLike}
import scala.util.Success

class ServerActorSuit extends TestKit(ActorSystem("ServerActorSuit"))
                         with FunSuiteLike
                         with BeforeAndAfterAll
                         with BeforeAndAfterEach {
  import ServerActorSuit._

  override def afterAll() {
    system.shutdown()
  }

  test("server should init worker on 'Service' message received") {
    val server = system.actorOf(Props(new TestServerActor(testActor)))
    server ! Service(Props(new FakeWorker(testActor)), null)
    expectMsg(WorkerCreated)

    withClue("server should respond with result on Success") {
      server ! Success("OK")
      expectMsg(ServerRespond(200, "OK"))
    }

    system stop server
  }
}

object ServerActorSuit {
  case object WorkerCreated
  case class ServerRespond(code: Int, result: Any)

  class FakeWorker(testActor: ActorRef) extends Actor {
    testActor ! WorkerCreated
    context stop self
    val receive: Receive = { case _ => () }
  }
  class TestServerActor(probe: ActorRef) extends ServerActor with FakeLifeCicleAware {
    def initWorker(workerProps: Props, exchange: HttpExchange) = {
      context.actorOf(workerProps)
    }
    def respond(status: Int, body: Array[Byte]) = probe ! ServerRespond(status, new String(body))
    def receive = service
  }
}


