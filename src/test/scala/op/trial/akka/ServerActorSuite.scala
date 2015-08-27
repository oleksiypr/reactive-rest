package op.trial.akka

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.TestKit
import op.trial.akka.ScalabilitySuit.FakeLifeCicleAware
import op.trial.akka.ServerActor._
import op.trial.akka.util._
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FunSuiteLike}
import scala.util.Success

class ServerActorSuite extends TestKit(ActorSystem("ServerActorSuit"))
                         with FunSuiteLike
                         with BeforeAndAfterAll
                         with BeforeAndAfterEach {
  import ServerActorSuite._

  override def afterAll() {
    system.shutdown()
  }

  test("server actor should interact with worker") {
    val server = system.actorOf(Props(new TestServerActor(testActor)))
    expectMsg(ServerLoad(0))

    server ! Service(Props(new ProbeFakeWorker(
      probe = testActor,
      messageToProbe = WorkerCreated,
      messageToParent = Success("OK"))), ())

    withClue("server actor should create new worker on 'Service' message") {
      expectMsg(ServerLoad(1))
      expectMsg(WorkerCreated)
      expectMsg(ServerRespond("OK", workersCount = 0))
    }

    system stop server
  }
}

object ServerActorSuite {
  case object WorkerCreated
  case class ServerLoad(load: Int)
  case class ServerRespond(result: Any, workersCount: Int)

  class TestServerActor(probe: ActorRef) extends ServerActor[Unit] with FakeLifeCicleAware {
    probe ! ServerLoad(load)

    def success(res: Any, exchange: Unit) = probe ! ServerRespond(res, workersCount = load)
    def failure(cause: Throwable, exchange: Unit) {}
    def receive = {
      case s@Service(wp, e) => service(s); probe ! ServerLoad(load)
      case msg => service(msg)
    }
  }
}


