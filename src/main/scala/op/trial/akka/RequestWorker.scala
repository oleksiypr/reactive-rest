package op.trial.akka

import akka.actor.Actor
import scala.util.Try

class RequestWorker[U, T](function: U => T, arg: U) extends Actor {
  work()
  def work() {
    context.parent ! Try(function(arg))
    context stop self
  }
  def receive =  noAction
  val noAction: Receive = { case _ => () }
}


