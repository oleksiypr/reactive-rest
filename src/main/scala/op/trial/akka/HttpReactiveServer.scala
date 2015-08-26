package op.trial.akka

import akka.actor.{ActorRef, Props}
import com.sun.net.httpserver.HttpExchange

import scala.concurrent.Future

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
  def respond(status: Int, body: Array[Byte]) {
    import context.dispatcher
    val worker = sender()
    exchanges get worker foreach { exchange =>
      exchanges -= worker
      Future(writeResponse(status, body, exchange))
    }
  }
}


