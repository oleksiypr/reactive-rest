package op.trial.akka.web

import akka.actor.Props
import com.sun.net.httpserver.HttpExchange
import op.trial.akka.ServerActor
import op.trial.akka.ServerActor.{Job, Service}
import op.trial.akka.web.HttpReactiveServer._

class HttpReactiveServer(val app: String, val port: Int, val mappings: Map[String, Props] = Map.empty) extends ServerActor
                                                                                                          with HttpServerAware {
  override def handleGet(path: String, exchange: HttpExchange) {
    val job = new HttpJob(exchange)
    mappings get path match {
      case Some(workerProps) => self ! Service(workerProps, job)
      case None => job.notFound()
    }
  }

  def localDeploy(workerProps: Props) = context.actorOf(workerProps)
  val receive = service(localDeploy)
}

object HttpReactiveServer {
  class HttpJob(exchange: HttpExchange) extends Job {
    def success(res: Any)         = writeResponse(status = 200, res.asInstanceOf[String].getBytes)
    def failure(cause: Throwable) = writeResponse(status = 500, cause.getMessage.getBytes)
    def notFound()                = writeResponse(status = 404, Array.empty[Byte])

    private[this] def writeResponse(status: Int, body: Array[Byte]) {
      exchange.sendResponseHeaders(status, 0L)
      exchange.getResponseBody.write(body)
      exchange.getResponseBody.close()
    }
  }
}


