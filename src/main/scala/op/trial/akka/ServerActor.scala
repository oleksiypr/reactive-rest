package op.trial.akka

import akka.actor.{ActorRef, Props, Actor}
import com.sun.net.httpserver.HttpExchange
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ServerActor(val app: String, val port: Int, val mappings: Map[String, Props] = Map.empty) extends Actor with ServerAware {
  import ServerActor._
  private[this] var exchanges = Map.empty[ActorRef, HttpExchange]

  override def preStart() = startUp()
  override def postStop() = shutDown()
  override def handlePath(path: String, exchange: HttpExchange) {
    mappings get path match {
      case Some(workerProps) => self ! Service(workerProps, exchange)
      case None => writeResponse(404, Array.empty[Byte], exchange)
    }
  }

  def receive = service

  private[this] val service: Receive = {
    case Service(workerProps, exchange) => exchanges += context.actorOf(workerProps) -> exchange
    case Success(res) => respond(200, res.toString.getBytes)
    case Failure(cause) => respond(500, cause.getMessage.getBytes)
    case Stop => context stop self
  }

  private def respond(status: Int, body: Array[Byte]) {
    import context.dispatcher
    val worker = sender()
    exchanges get worker foreach { exchange =>
      exchanges -= worker
      Future(writeResponse(status, body, exchange))
    }
  }
}

object ServerActor {
  case object Stop
  case class Service(workerProps: Props, exchange: HttpExchange)
}

