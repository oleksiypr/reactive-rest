package op.trial.akka

import java.net.{URI, InetSocketAddress}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit, ThreadPoolExecutor}
import akka.actor.{ActorRef, Props, Actor}
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import ServerActor._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ServerActor(val app: String, val port: Int, val mappings: Map[String, Props] = Map.empty) extends Actor {
  private val server = HttpServer.create(new InetSocketAddress(port), 0)
  private val executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue)

  private[this] var exchanges = Map.empty[ActorRef, HttpExchange]

  override def preStart() {
    server.setExecutor(executor)
    server.createContext(s"/$app", httpHandler)
    server.start()
  }

  override def postStop() {
    server.stop(1)
    executor.shutdown()
  }

  def receive = service

  private[this] val service: Receive = {
    case Service(workerProps, exchange) => exchanges += context.actorOf(workerProps) -> exchange
    case Success(res) => respond(200, res.toString.getBytes)
    case Failure(cause) => respond(500, cause.getMessage.getBytes)
    case Stop => context stop self
  }

  private[this] val httpHandler = new HttpHandler {
    def handle(exchange: HttpExchange) {
      val path = "/" + new URI(s"/$app").relativize(exchange.getRequestURI).getPath
      mappings get path match {
        case Some(workerProps) => self ! Service(workerProps, exchange)
        case None => writeResponse(404, Array.empty[Byte], exchange)
      }
    }
  }

  private def respond(status: Int, body: Array[Byte]) {
    import context.dispatcher
    val worker = sender()
    exchanges get worker foreach { exchange =>
      exchanges -= worker
      Future(writeResponse(status, body, exchange))
    }
  }

  private def writeResponse(status: Int, body: Array[Byte], exchange: HttpExchange) {
    exchange.sendResponseHeaders(status, 0L)
    exchange.getResponseBody.write(body)
    exchange.getResponseBody.close()
  }
}

object ServerActor {
  case object Stop
  case class Service(workerProps: Props, exchange: HttpExchange)
}

