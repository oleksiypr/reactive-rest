package op.trial.akka

import akka.actor._
import akka.cluster.ClusterEvent
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.language.postfixOps

class ScalabilitySuite extends TestKit(ActorSystem("ScalabilitySuite"))
                          with WordSpecLike
                          with BeforeAndAfterAll {

  import ScalabilitySuite._

  override protected def afterAll() = shutdown()
  def serverProps1 = Props(new FakeServer1(testActor))
  def serverProps2 = Props(new FakeServer2)

  "Server cluster" must {
    "init itself: joint self to a single node cluster and start server actor" in {
      val serverCluster = system.actorOf(Props(new TestCluster1(testActor, serverProps1)), "test-cluster-1")
      expectMsg(ServerActorStarted)
      expectMsgPF() {
        case state: ClusterEvent.CurrentClusterState => assert(state.members.size == 1)
      }
      system stop serverCluster
    }
    "accept new member" in {
      val serverCluster = system.actorOf(Props(new TestCluster2(testActor, serverProps2)),  "test-cluster-2")
      serverCluster ! GetAddress
      val address = expectMsgPF() { case adr: Address => adr }
      val workerNode = ActorSystem("ScalabilitySuite").actorOf(Props(new WorkerNode(address)))

      expectMsgPF() {
        case state: ClusterEvent.CurrentClusterState => ()
      }
      expectMsgPF() {
        case ClusterEvent.MemberUp(m) => assert(m.address == address)
      }
      expectMsgPF() {
        case ClusterEvent.MemberUp(m) => ()
      }

      serverCluster ! GetMembers
      expectMsg(Members(count = 2))

      system stop workerNode
      system stop serverCluster
    }
  }
}

object ScalabilitySuite {
  case object ServerActorStarted
  case object GetAddress
  case object GetMembers
  case class Members(count: Int)

  class TestCluster1(probe: ActorRef, serverProps: Props) extends ServerCluster(serverProps) {
    override def receive: Receive = { case msg => probe ! msg; super.receive(msg) }
  }
  class TestCluster2(probe: ActorRef, serverProps: Props) extends ServerCluster(serverProps) {
    override def receive: Receive = {
      case GetAddress => probe ! cluster.selfAddress
      case GetMembers => probe ! Members(count = cluster.state.members.size)
      case msg => probe ! msg; super.receive(msg)
    }
  }
  class FakeServer1(probe: ActorRef) extends Actor {
    probe ! ServerActorStarted
    val receive: Actor.Receive = { case _ => () }
  }
  class FakeServer2 extends Actor {
    val receive: Actor.Receive = { case _ => () }
  }
}

