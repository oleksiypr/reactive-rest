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

  def serverProps = Props(new FakeServer(testActor))
  val serverCluster = system.actorOf(Props(new TestCluster(testActor, serverProps)), "server-cluster")

  override protected def afterAll() =  {
    system stop serverCluster
    shutdown()
  }

  "Server cluster" must {
    "joint self to a single node cluster and start server actor" in {
      expectMsg(ServerActorStarted)
      expectMsgPF() {
        case state: ClusterEvent.CurrentClusterState => assert(state.members.size == 1)
      }
    }
  }
}

object ScalabilitySuite {
  case object ServerActorStarted

  class TestCluster(probe: ActorRef, serverProps: Props) extends ServerCluster(serverProps) {
    cluster.subscribe(self, classOf[Any])
    override def receive: Receive = { case msg => probe ! msg; super.receive(msg) }
  }
  class FakeWorker() extends WorkerActor {
    def work() {}
  }
  class FakeServer(probe: ActorRef) extends Actor {
    probe ! ServerActorStarted
    val receive: Actor.Receive = { case _ => () }
  }
}

