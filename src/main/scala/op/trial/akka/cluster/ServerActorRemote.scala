package op.trial.akka.cluster

import akka.actor.{Address, Deploy, Props}
import akka.cluster.{Cluster, ClusterEvent}
import akka.remote.RemoteScope
import op.trial.akka.ServerActor
import op.trial.akka.ServerActor.Service

abstract class ServerActorRemote extends ServerActor {
  val cluster  = Cluster(context.system)
  cluster.subscribe(self, classOf[ClusterEvent.MemberUp])

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
