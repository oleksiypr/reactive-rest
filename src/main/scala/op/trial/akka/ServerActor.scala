package op.trial.akka

import akka.actor.{ActorRef, Props, Actor}
import scala.concurrent.Future
import scala.util.{Try, Failure, Success}
import ServerActor._

abstract class ServerActor[E] extends Actor with LifeCicleAware {
  private[this] var exchanges = Map.empty[ActorRef, E]
  protected[this] def load: Int = exchanges.size

  override def preStart() = startUp()
  override def postStop() = shutDown()

  def success(res: Any, exchange: E): Unit
  def failure(cause: Throwable, exchange: E): Unit

  val service: Receive = {
    case Service(workerProps, exchange: E) => initWorker(workerProps, exchange)
    case result: Try[_] =>  handleResult(result)
  }

  private def initWorker(workerProps: Props, exchange: E) = exchanges += context.actorOf(workerProps) -> exchange
  private def handleResult(result: Try[Any]) {
    import context.dispatcher
    val worker = sender()
    exchanges get worker foreach { exchange =>
      exchanges -= worker
      Future(respond(result, exchange))
    }
  }
  private def respond(result: Try[Any], exchange: E) = result match {
    case Success(res) => success(res, exchange)
    case Failure(cause) => failure(cause, exchange)
  }
}

object ServerActor {
  case class Service[E](workerProps: Props, exchange: E)
}
