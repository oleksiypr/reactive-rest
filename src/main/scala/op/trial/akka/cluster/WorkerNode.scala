package op.trial.akka.cluster

import akka.actor.{Actor, Address}
import akka.cluster.Cluster

class WorkerNode(cluster: Address) extends Actor {
  val node  = Cluster(context.system)
  node join cluster

  def receive: Receive = { case _ => () }
}
