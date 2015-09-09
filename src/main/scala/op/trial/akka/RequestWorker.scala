package op.trial.akka

import akka.actor.Actor
import scala.util.Try

class RequestWorker[U, T](function: U => T, arg: U) extends Actor {
  work()

  protected[this] def work() {
    val resultTry = Try(function(arg))
    context.parent ! resultTry
    context stop self
  }
  def receive: Receive =  default
  val default: Receive = { case _ => () }
}


