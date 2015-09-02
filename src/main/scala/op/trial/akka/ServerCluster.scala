package op.trial.akka

import akka.actor.Actor
import akka.cluster.{ClusterEvent, Cluster}

class ServerCluster extends Actor {
  val cluster  = Cluster(context.system)
  cluster join cluster.selfAddress

  def receive: Receive = { case _ => () }
}
