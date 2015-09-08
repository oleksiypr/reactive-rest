package op.trial.akka

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.TestKit
import op.trial.akka.ServerActor._
import op.trial.akka.util._
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FunSuiteLike}
import scala.util.Success

class ServerActorSuite extends TestKit(ActorSystem("ServerActorSuite"))
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
      messageToParent = Success("OK"))), new FakeJob(testActor))

    withClue("server actor should create new worker on 'Service' message") {
      expectMsg(ServerLoad(1))
      expectMsg(WorkerCreated)
      expectMsg("OK")

      server ! GetServerLoad
      expectMsg(ServerLoad(0))
    }

    system stop server
  }
}

object ServerActorSuite {
  case object WorkerCreated
  case object GetServerLoad
  case class ServerLoad(load: Int)
  case class ServerRespond(result: Any, workersCount: Int)

  class FakeJob(probe: ActorRef) extends Job {
    def success(res: Any) = probe ! res
    def failure(cause: Throwable) {}
  }

  class TestServerActor(probe: ActorRef) extends ServerActor with FakeLifeCicleAware {
    probe ! ServerLoad(load)

    def receive = {
      case s: Service => service(s); probe ! ServerLoad(load)
      case GetServerLoad => probe ! ServerLoad(load)
      case msg => service(msg)
    }
  }
}


