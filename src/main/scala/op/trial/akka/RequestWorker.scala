package op.trial.akka

import akka.actor.Actor
import scala.util.Try

class RequestWorker[U, T](function: U => T, arg: U) extends Actor {
  work()

  def receive =  noAction
  def work() {
    context.parent ! Try(function(arg))
    context stop self
  }
  val noAction: Receive = { case _ => () }
}


