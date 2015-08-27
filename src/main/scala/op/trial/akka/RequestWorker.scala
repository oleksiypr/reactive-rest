package op.trial.akka

import scala.util.Try

class RequestWorker[U, T](function: U => T, arg: U) extends WorkerActor {
  def work() {
    context.parent ! Try(function(arg))
    context stop self
  }
}


