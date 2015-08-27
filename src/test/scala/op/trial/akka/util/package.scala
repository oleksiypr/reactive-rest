package op.trial.akka

import akka.actor.ActorRef

package object util {
  class FakeWorker(messageToParent: Any) extends WorkerActor {
    def work() {
      context.parent ! messageToParent
      context stop self
    }
  }
  class ProbeFakeWorker(probe: ActorRef, messageToProbe: Any, messageToParent: Any) extends FakeWorker(messageToParent) {
    override def work() {
      probe ! messageToProbe
      super.work()
    }
  }
}
