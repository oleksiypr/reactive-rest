package op.trial.akka

import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import akka.cluster.ClusterEvent
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.language.postfixOps

class ScalabilitySuite extends TestKit(ActorSystem("ScalabilitySuite"))
                          with WordSpecLike
                          with BeforeAndAfterAll {

  import ScalabilitySuite._

  val serverCluster = system.actorOf(Props(new TestCluster(testActor)), "server-cluster")
  override protected def afterAll() =  {
    system stop serverCluster
    system.shutdown()
  }

  "Server cluster" must {
    "joint self to a single node cluster" in {
      expectMsgPF() {
        case state: ClusterEvent.CurrentClusterState => assert(state.members.size == 1)
      }
      expectMsgPF() {
        case ClusterEvent.MemberUp(member) => ()
      }
    }
  }
}

object ScalabilitySuite {
  class TestCluster(probe: ActorRef) extends ServerCluster {
    cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
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

