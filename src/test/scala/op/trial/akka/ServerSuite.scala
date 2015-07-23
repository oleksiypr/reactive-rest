package op.trial.akka

import java.net.{InetSocketAddress, Socket}
import akka.actor.{Props, ActorSystem}
import akka.routing.{Listen, Listeners}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike, BeforeAndAfterAll}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class ServerSuite extends TestKit(ActorSystem("ServerSuite"))
                     with FunSuiteLike
                     with BeforeAndAfterAll
                     with BeforeAndAfterEach {
  import ServerActor._
  import dispatch._, Defaults._
  import ServerSuite._

  val port = 9090
  val app = "hello-app"

  override def afterAll() = system.shutdown()

  test("server should listen a port") {
    assert(!isListening(port))
    val server = system.actorOf(Props(new ServerActor(app, port)), "server-listen-port")

    Thread.sleep(500)
    assert(isListening(port))

    withClue("server should shutdown and release the port when receive Stop message") {
      server ! Stop
      Thread.sleep(500)
      assert(!isListening(port))
    }
  }

  test("server should receive HTTP requests") {
    val server = system.actorOf(Props(
      new ServerActor(app, port,
        mappings = Map(
          "/foo" -> Props(WorkerMock),
          "/error" -> Props(ErrorWorker)
        )
      ) with Listeners {
        override val receive: Receive = listenerManagement orElse {
          case msg => super.receive(msg); gossip(msg)
        }
      }
    ), "server-receive-http")
    server ! Listen(testActor)

    withClue("success") {
      val req = url(s"http://localhost:$port/$app/foo")
      val resp = Http(req)
      assert(Await.result(resp, 1 second).getStatusCode == 200)
      assert(Await.result(resp, 0 second).getResponseBody == "well done")
      assert (expectMsgPF() {
        case Service(wp ,ex) => true
        case _ => false
      })
    }
    withClue("error response status in case of internal error") {
      val req = url(s"http://localhost:$port/$app/error")
      val resp = Http(req)
      assert(Await.result(resp, 1 second).getStatusCode == 500)
      assert(Await.result(resp, 0 second).getResponseBody == "error message")
    }
    withClue("not found") {
      val req = url(s"http://localhost:$port/$app/not_found")
      val resp = Http(req)
      assert(Await.result(resp, 1 second).getStatusCode == 404)
    }

    system stop server
  }
}

object ServerSuite {
  object WorkerMock   extends RequestWorker((_: Unit) => "well done", ())
  object ErrorWorker  extends RequestWorker((_: Unit) => throw new Error("error message"), ())

  def isListening(port: Int): Boolean = Try {
    val socket = new Socket()
    socket.connect(new InetSocketAddress("localhost", port), 500)
    socket.close()
  } match {
    case Success(_) => true
    case Failure(_) => false
  }
}


