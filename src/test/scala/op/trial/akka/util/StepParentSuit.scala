package op.trial.akka.util

import akka.actor.{Actor, ActorSystem, Props, Terminated}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class StepParentSuit extends TestKit(ActorSystem("StepParentSuit"))
                        with WordSpecLike
                        with BeforeAndAfterAll
                        with ImplicitSender {
  override protected def afterAll() = system.shutdown()

  "StepParent" must {
    "forward messages from its child" in {
      val msg = "check me"
      class Child extends Actor {
        context.parent ! msg
        val receive: Actor.Receive = { case _ => () }
      }
      val  stepParent = system.actorOf(Props(new StepParent(Props(new Child), testActor)))
      expectMsg(msg)
      system.stop(stepParent)
    }

    "stop itself when child terminates" in {
      class TerminatingChild extends Actor {
        context stop self
        val receive: Actor.Receive = { case _ => () }
      }
      val  stepParent = watch(system.actorOf(Props(new StepParent(Props(new TerminatingChild), testActor))))
      assert(expectMsgPF() {
        case Terminated(_) => lastSender == stepParent
        case _ => false
      })
    }
  }
}
