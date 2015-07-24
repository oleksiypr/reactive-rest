package op.trial.akka


import akka.actor.{Terminated, Props, ActorSystem}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.util.{Failure, Success}

class RequestWorkerSuit extends TestKit(ActorSystem("RequestWorkerSuit")) with WordSpecLike
                                                                          with BeforeAndAfterAll {
  override protected def afterAll() = system.shutdown()

  "RequestWorker" must {
    "perform operation and stop itself" in {
      val  stepParent = watch(system.actorOf(Props(new StepParent(Props(
        new RequestWorker(
          (str: String) => s"Worker done: $str",
          "Yes, I am done")),
        testActor)), "rightResponse"))

      val result = expectMsgPF() {
        case Success(str) => str
      }
      assert(result == "Worker done: Yes, I am done")
      assert(expectMsgPF() {
        case Terminated(_) => lastSender == stepParent
        case _ => false
      })
    }

    "send error massages when fail and stop itself" in {
      val err = new Error
      val  stepParent = watch(system.actorOf(Props(new StepParent(Props(
        new RequestWorker(
          (_: Unit) => throw err,
          ())),
        testActor)), "errorResponse"))
      expectMsg(Failure(err))
      assert(expectMsgPF() {
        case Terminated(_) => lastSender == stepParent
        case _ => false
      })
    }
  }
}
