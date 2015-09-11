package op.trial.akka

import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster

package object util {
  class FakeClusterWorker(probe: ActorRef) extends Actor {
    probe ! Cluster(context.system).selfAddress
    context stop self
    def receive: Actor.Receive = { case _ => ()}
  }

  class ProbeFakeWorker(probe: ActorRef, messageToProbe: Any, messageToParent: Any) extends FakeWorker(messageToParent) {
    override def work() {
      probe ! messageToProbe
      super.work()
    }
  }
  class FakeWorker(messageToParent: Any) extends RequestWorker((_: Unit) => {}, ()) {
    override def work() {
      context.parent ! messageToParent
      context stop self
    }
  }
  trait FakeLifeCicleAware extends LifeCicleAware {
    def startUp() {}
    def shutDown() {}
  }
}
