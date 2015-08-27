package op.trial.akka

import akka.actor.{ActorRef, Props}
import com.sun.net.httpserver.HttpExchange
import op.trial.akka.ServerActor._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class HttpReactiveServer(val app: String, val port: Int, val mappings: Map[String, Props] = Map.empty) extends ServerActor with HttpServerAware {
  private[this] var exchanges = Map.empty[ActorRef, HttpExchange]

  override def handleGet(path: String, exchange: HttpExchange) {
    mappings get path match {
      case Some(workerProps) => self ! ServerActor.Service(workerProps, exchange)
      case None => writeResponse(404, Array.empty[Byte], exchange)
    }
  }

  def receive = service
  def initWorker(workerProps: Props, exchange: HttpExchange) = exchanges += context.actorOf(workerProps) -> exchange
  def success(res: Any) = respond(200, res.asInstanceOf[String].getBytes)
  def failure(cause: Throwable) = respond(500, cause.getMessage.getBytes)
  def respond(status: Int, body: Array[Byte]) {
    import context.dispatcher
    val worker = sender()
    exchanges get worker foreach { exchange =>
      exchanges -= worker
      Future(writeResponse(status, body, exchange))
    }
  }
}


