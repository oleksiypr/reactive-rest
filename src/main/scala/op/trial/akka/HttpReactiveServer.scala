package op.trial.akka

import akka.actor.{ActorRef, Props}
import com.sun.net.httpserver.HttpExchange

class HttpReactiveServer(val app: String, val port: Int, val mappings: Map[String, Props] = Map.empty) extends ServerActor[HttpExchange]
                                                                                                          with HttpServerAware {
  override def handleGet(path: String, exchange: HttpExchange) {
    mappings get path match {
      case Some(workerProps) => self ! ServerActor.Service(workerProps, exchange)
      case None => writeResponse(404, Array.empty[Byte], exchange)
    }
  }

  def receive = service
  def success(res: Any, exchange: HttpExchange) = writeResponse(status = 200, res.asInstanceOf[String].getBytes, exchange)
  def failure(cause: Throwable, exchange: HttpExchange) = writeResponse(status = 500, cause.getMessage.getBytes, exchange)
}


