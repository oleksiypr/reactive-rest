package op.trial.akka

import akka.actor.Actor
import scala.util.Try

class RequestWorker[U, T](function: U => T, arg: U) extends Actor {
  context.parent ! Try(function(arg))
  context stop self

  def receive: Receive = {
    case _ => ()
  }
}


