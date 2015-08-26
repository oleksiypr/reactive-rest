package op.trial.akka

import akka.actor.{Props, Actor}
import com.sun.net.httpserver.HttpExchange
import scala.util.{Failure, Success}
import ServerActor._

abstract class ServerActor extends Actor with LifeCicleAware {
  override def preStart() = startUp()
  override def postStop() = shutDown()

  def initWorker(workerProps: Props, exchange: HttpExchange)
  def respond(status: Int, body: Array[Byte])

  val service: Receive = {
    case Service(workerProps, exchange) => initWorker(workerProps, exchange)
    case Success(res) => respond(200, res.toString.getBytes)
    case Failure(cause) => respond(500, cause.getMessage.getBytes)
    case Stop => context stop self
  }
}

object ServerActor {
  case object Stop
  case class Service(workerProps: Props, exchange: HttpExchange)
}
