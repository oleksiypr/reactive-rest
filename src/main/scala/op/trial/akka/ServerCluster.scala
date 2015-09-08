package op.trial.akka

import akka.actor.{Props, Actor}
import akka.cluster.{ClusterEvent, Cluster}

class ServerCluster(serverProps: Props) extends Actor {
  val cluster  = Cluster(context.system)
  cluster.join(cluster.selfAddress)
  context.actorOf(serverProps)

  def receive: Receive = {case _ => ()}
}
