package op.trial.akka

import akka.actor._
import akka.cluster.{Member, ClusterEvent}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.language.postfixOps

class ScalabilitySuite extends TestKit(ActorSystem("ScalabilitySuite"))
                          with WordSpecLike
                          with BeforeAndAfterAll {
  import ScalabilitySuite._

  override protected def afterAll() = shutdown()

  val singleNodeClusterProps = Props(new SingleNodeCluster(testActor, Props(new TellStartFakeServer(testActor))))
  val manyNodesClusterProps = Props(new ManyNodesCluster(testActor, Props(new FakeServer)))

  "Server cluster" must {
    "init itself: joint self to a single node cluster and start server actor" in {
      val serverCluster = system.actorOf(singleNodeClusterProps, "test-cluster-1")
      expectMsg(ServerActorStarted)
      expectMsgPF() {
        case state: ClusterEvent.CurrentClusterState => assert(state.members.size == 1)
      }
      system stop serverCluster
    }
    "accept new member" in {
      val serverCluster = system.actorOf(manyNodesClusterProps,  "test-cluster-2")
      serverCluster ! GetAddress
      val address = expectMsgPF() { case adr: Address => adr }
      val workerNode = ActorSystem("ScalabilitySuite").actorOf(Props(new WorkerNode(address)))

      expectMsgPF() { case state: ClusterEvent.CurrentClusterState => () }
      expectMemberUp(m => assert(m.address == address))
      expectMemberUp(m => {})

      serverCluster ! GetMembers
      expectMsg(Members(count = 2))

      system stop workerNode
      system stop serverCluster

      def expectMemberUp(onMemberUp : Member => Unit) = expectMsgPF() { case ClusterEvent.MemberUp(m) => onMemberUp }
    }
  }
}

object ScalabilitySuite {
  case object ServerActorStarted
  case object GetAddress
  case object GetMembers
  case class Members(count: Int)

  class ManyNodesCluster(probe: ActorRef, serverProps: Props) extends SingleNodeCluster(probe, serverProps) {
    override def receive: Receive = {
      case GetAddress => probe ! cluster.selfAddress
      case GetMembers => probe ! Members(count = cluster.state.members.size)
      case msg => super.receive(msg)
    }
  }
  class SingleNodeCluster(probe: ActorRef, serverProps: Props) extends ServerCluster(serverProps) {
    override def receive: Receive = { case msg => probe ! msg; super.receive(msg) }
  }
  class TellStartFakeServer(probe: ActorRef) extends FakeServer {
    probe ! ServerActorStarted
  }
  class FakeServer extends Actor {
    val receive: Actor.Receive = { case _ => () }
  }
}

