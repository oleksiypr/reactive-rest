package op.trial.akka

import akka.actor.Actor

abstract class WorkerActor extends Actor {
  work()
  def work()
  def receive: Receive =  default
  val default: Receive = { case _ => () }
}
