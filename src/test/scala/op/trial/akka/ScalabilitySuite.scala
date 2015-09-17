package op.trial.akka

import akka.actor._
import akka.cluster.{Cluster, ClusterEvent}
import akka.remote.RemoteScope
import akka.testkit.TestKit
import op.trial.akka.ServerActor.{Service, Job}
import op.trial.akka.util._
import org.scalatest.{FunSuiteLike, BeforeAndAfterAll}
import scala.language.postfixOps

class ScalabilitySuite extends TestKit(ActorSystem("ScalabilitySuite"))
                          with FunSuiteLike
                          with BeforeAndAfterAll {
  import ScalabilitySuite._

  override protected def afterAll() {
    workerSystem.shutdown()
    shutdown()
  }

  val serverClusterProps = Props(new TestCluster(testActor, Props(new FakeServer(testActor))))
  val workerSystem = ActorSystem("ScalabilitySuite")

  test("cluster: start itself, add worker node, deploy worker") {
    val serverCluster = system.actorOf(serverClusterProps, "test-cluster-2")
    expectMsg(ServerActorStarted)
    expectMsgPF() { case state: ClusterEvent.CurrentClusterState => assert(state.members.size == 1) }

    serverCluster ! GetAddress
    val clusterAddress = expectMsgPF() { case adr: Address => adr }
    println("cluster address: " + clusterAddress)


    val workerNode = workerSystem.actorOf(Props(new TestWorkerNode(testActor, clusterAddress)))
    workerNode ! GetAddress
    val workerAddress = expectMsgPF() { case adr: Address => adr }
    println("worker node address: " + workerAddress)

    expectMsgPF() { case ClusterEvent.MemberUp(m) => assert(m.address == clusterAddress) }
    expectMsgPF() { case ClusterEvent.MemberUp(m) => assert(m.address == workerAddress) }

    serverCluster ! GetMembers
    expectMsg(Members(count = 2))

    serverCluster ! GetServer
    val server = expectMsgPF() { case s: ActorRef => s }

    server ! Service(FakeServer.workerProps(testActor), new FakeJob)
    expectMsg(workerAddress)
  }
}

object ScalabilitySuite {
  case object ServerActorStarted
  case object GetAddress
  case object GetMembers
  case object GetServer
  case class TestRemoteRequest(node: Address)
  case class Members(count: Int)

  class FakeJob extends Job {
    def success(res: Any) {}
    def failure(cause: Throwable) {}
  }

  class TestWorkerNode(probe:  ActorRef, cluster: Address) extends WorkerNode(cluster) {
    override def receive: Receive = {
      case GetAddress => probe ! node.selfAddress
      case msg => super.receive(msg)
    }
  }

  class TestCluster(probe: ActorRef, serverProps: Props) extends ServerCluster(serverProps) {
    override def receive: Receive = {
      case GetMembers => probe ! Members(count = cluster.state.members.size)
      case GetServer  => probe ! server
      case GetAddress => probe ! cluster.selfAddress
      case msg        => probe ! msg; super.receive(msg)
    }
  }
  class FakeServer(probe: ActorRef) extends ServerActor with FakeLifeCicleAware {
    val cluster  = Cluster(context.system)
    cluster.subscribe(self, classOf[ClusterEvent.MemberUp])

    probe ! ServerActorStarted

    def remoteDeploy(address: Address)(workerProps: Props) = context.actorOf(workerProps.withDeploy(Deploy(scope = RemoteScope(address))))
    def receive: Receive = awaiting

    val awaiting: Receive = {
      case Service(_, job) => job.failure(new IllegalStateException("Cluster is not ready."))
      case state: ClusterEvent.CurrentClusterState =>
        val notMe = state.members.filterNot(_.address == cluster.selfAddress)
        if (notMe.nonEmpty) context become active(workerNode = notMe.head.address)
      case ClusterEvent.MemberUp(m) if m.address != cluster.selfAddress => context become active(workerNode = m.address)
    }
    def active(workerNode: Address): Receive = super.service(remoteDeploy(workerNode))
  }
  object FakeServer {
    def workerProps(probe: ActorRef) = Props(new FakeClusterWorker(probe))
  }
}

