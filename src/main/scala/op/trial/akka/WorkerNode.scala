package op.trial.akka

import akka.actor.{Address, Actor}
import akka.cluster.Cluster

class WorkerNode(cluster: Address) extends Actor {
  val node  = Cluster(context.system)
  node join cluster

  def receive: Receive = { case _ => () }
}
