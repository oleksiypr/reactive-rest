package op.trial.akka.cluster

import akka.actor.{Actor, Props}
import akka.cluster.{Cluster, ClusterEvent}

class ServerCluster(serverProps: Props) extends Actor {
  val cluster  = Cluster(context.system)
  cluster join cluster.selfAddress
  cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
  val server = context.actorOf(serverProps)

  def receive: Receive = {case _ => ()}
}
