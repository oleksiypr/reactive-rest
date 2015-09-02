package op.trial.akka

import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import akka.cluster.ClusterEvent
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.language.postfixOps
import ScalabilitySuite._

class ScalabilitySuite extends TestKit(ActorSystem("ScalabilitySuite"))
                          with WordSpecLike
                          with BeforeAndAfterAll {

  override protected def afterAll() =  system.shutdown()

  val server = system.actorOf(Props(new FakeServer))
  case object WorkerCreated

  "Server cluster" must {
    "joint self to a single node cluster" in {
      val serverCluster = system.actorOf(Props(new TestCluster(testActor)), "server-cluster")
      expectMsgPF() {
        case state: ClusterEvent.CurrentClusterState => ()
      }
    }
  }
}

object ScalabilitySuite {
  class TestCluster(probe: ActorRef) extends ServerCluster {
    override def receive: Receive = { case msg => probe ! msg; super.receive(msg) }
  }

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

