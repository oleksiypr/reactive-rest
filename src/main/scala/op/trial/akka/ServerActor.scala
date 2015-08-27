package op.trial.akka

import akka.actor.{ Props, Actor}
import com.sun.net.httpserver.HttpExchange
import scala.util.{Try, Failure, Success}
import ServerActor._

abstract class ServerActor extends Actor with LifeCicleAware {
  override def preStart() = startUp()
  override def postStop() = shutDown()

  def initWorker(workerProps: Props, exchange: HttpExchange)
  def success(res: Any): Unit
  def failure(cause: Throwable): Unit

  def respond(result: Try[Any]) = result match {
    case Success(res) => success(res)
    case Failure(cause) => failure(cause)
  }

  val service: Receive = {
    case Service(workerProps, exchange) => initWorker(workerProps, exchange)
    case result: Try[_] => respond(result)
  }
}

object ServerActor {
  case class Service(workerProps: Props, exchange: HttpExchange)
}
