package op.trial.akka

import java.net.{InetSocketAddress, Socket}

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuiteLike}
import op.trial.akka.util._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class HttpServerSuite extends TestKit(ActorSystem("HttpServerSuite"))
                         with FunSuiteLike
                         with BeforeAndAfterAll
                         with BeforeAndAfterEach {
  import dispatch._
  import Defaults._
  import HttpServerSuite._

  val port = 9090
  val app = "hello-app"

  override def afterAll() {
    system.shutdown()
    Http.shutdown()
  }

  test("server should listen a port") {
    assert(!isListening(port))
    val server = system.actorOf(Props(new HttpReactiveServer(app, port)), "server-listen-port")

    Thread.sleep(500)
    assert(isListening(port))

    withClue("server should shutdown and release the port when stops") {
      system stop server
      Thread.sleep(500)
      assert(!isListening(port))
    }
  }

  test("server should receive HTTP requests") {
    val server = testServer(
      mappings = Map(
        "/foo" -> Props(new FakeWorker(messageToParent = Success("well done"))),
        "/error" -> Props(new FakeWorker(messageToParent = Failure(new Error("error message"))))
      ),"server-receive-http")

    withClue("success") {
      val resp = Http(url(s"http://localhost:$port/$app/foo"))
      val result = Await.result(resp, 2 second)
      assert(result.getStatusCode == 200)
      assert(result.getResponseBody == "well done")
    }
    withClue("error response status in case of internal error") {
      val resp = Http(url(s"http://localhost:$port/$app/error"))
      val result = Await.result(resp, 2 second)
      assert(result.getStatusCode == 500)
      assert(result.getResponseBody == "error message")
    }
    withClue("not found") {
      val resp = Http(url(s"http://localhost:$port/$app/not_found"))
      assert(Await.result(resp, 2 second).getStatusCode == 404)
    }

    system stop server
  }

  private def testServer(mappings: Map[String, Props], name: String) = system.actorOf(Props(
      new HttpReactiveServer(app, port, mappings)), name)
}

object HttpServerSuite {
  def isListening(port: Int): Boolean = Try {
    val socket = new Socket()
    socket.connect(new InetSocketAddress("localhost", port), 500)
    socket.close()
  } match {
    case Success(_) => true
    case Failure(_) => false
  }
}


