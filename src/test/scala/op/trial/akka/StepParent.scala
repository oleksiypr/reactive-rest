package op.trial.akka

import akka.actor._

class StepParent(child: Props, probe: ActorRef) extends Actor {
  context.watch(context.actorOf(child))
  val receive: Receive = {
    case Terminated(_) => context stop self
    case msg => probe forward msg
  }
}