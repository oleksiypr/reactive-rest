package op.trial.akka

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import java.net.{URI, InetSocketAddress}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit, ThreadPoolExecutor}

trait HttpServerAware extends ServerAware {
  val port: Int
  val app: String
  def handleGet(path: String, exchange: HttpExchange)

  private[this] val server = HttpServer.create(new InetSocketAddress(port), 0)
  private[this] val executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue)
  private[this] val httpHandler = new HttpHandler {
    def handle(exchange: HttpExchange) {
      val path = "/" + new URI(s"/$app").relativize(exchange.getRequestURI).getPath
      handleGet(path, exchange)
    }
  }

  def writeResponse(status: Int, body: Array[Byte], exchange: HttpExchange) {
    exchange.sendResponseHeaders(status, 0L)
    exchange.getResponseBody.write(body)
    exchange.getResponseBody.close()
  }

  def startUp() {
    server.setExecutor(executor)
    server.createContext(s"/$app", httpHandler)
    server.start()
  }

  def shutDown() {
    server.stop(1)
    executor.shutdown()
  }
}
