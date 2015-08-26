package op.trial.akka

import akka.actor.{Props, ActorSystem}
import akka.routing.{Listen, Listeners}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike, BeforeAndAfterAll}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import java.net.{InetSocketAddress, Socket}

class ServerSuite extends TestKit(ActorSystem("ServerSuite"))
                     with FunSuiteLike
                     with BeforeAndAfterAll
                     with BeforeAndAfterEach {
  import ServerActor._
  import dispatch._, Defaults._
  import ServerSuite._

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

    withClue("server should shutdown and release the port when receive Stop message") {
      server ! Stop
      Thread.sleep(500)
      assert(!isListening(port))
    }
  }

  test("server should receive HTTP requests") {
    val server = testServer(
      mappings = Map(
        "/foo" -> Props(new WorkerMock),
        "/bar" -> Props(new WorkerMock),
        "/error" -> Props(new ErrorWorker)
      ),"server-receive-http")

    withClue("server should receive 'Service' and 'Success' messages") {
      val n = 1
      var serviceMessages = 0
      var successMessages = 0
      for (i <- 1 to n) Http(url(s"http://localhost:$port/$app/bar"))

      receiveWhile(1 second) {
        case Service(wp, ex) => serviceMessages += 1
        case Success(resp) => successMessages += 1
      }
      assert(serviceMessages == n)
      assert(successMessages == n)
    }

    withClue("success") {
      val resp = Http(url(s"http://localhost:$port/$app/foo"))
      val result = Await.result(resp, 2 second)
      assert(result.getStatusCode == 200)
      assert(result.getResponseBody == "well done")
      assert (expectMsgPF() {
        case Service(wp ,ex) => true
        case _ => false
      })
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

  private def testServer(mappings: Map[String, Props], name: String) = {
    val server = system.actorOf(Props(
      new HttpReactiveServer(app, port, mappings) with Listeners {
        override val receive: Receive = listenerManagement orElse {
          case msg => super.receive(msg); gossip(msg)
        }
      }
    ), name)
    server ! Listen(testActor)
    server
  }
}

object ServerSuite {
  class WorkerMock   extends RequestWorker((_: Unit) => "well done", ())
  class ErrorWorker  extends RequestWorker((_: Unit) => throw new Error("error message"), ())

  def isListening(port: Int): Boolean = Try {
    val socket = new Socket()
    socket.connect(new InetSocketAddress("localhost", port), 500)
    socket.close()
  } match {
    case Success(_) => true
    case Failure(_) => false
  }
}


