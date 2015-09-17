package op.trial.akka.web

import java.net.{InetSocketAddress, URI}
import java.util.concurrent.{LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import op.trial.akka.LifeCicleAware

trait HttpServerAware extends LifeCicleAware {
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
