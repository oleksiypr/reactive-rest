package op.trial.akka

import java.net.{InetSocketAddress, Socket}

import akka.actor.{Actor, Props, ActorSystem}
import akka.testkit.TestKit
import com.ning.http.client.Response
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
    val server = system.actorOf(Props(new ServerActor(app, port)))

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
          "/foo" -> Props(new WorkerMock),
          "/error" -> Props(new ErrorWorker)
        )
      )
    ))
    withClue("GET request for /foo service") {
      val req = url(s"http://localhost:$port/$app/foo")
      val resp = Http(req)
      assert(Await.result(resp, 1 second).getStatusCode == 200)
      assert(Await.result(resp, 0 second).getResponseBody == "well done")
    }
    withClue("error response status in case of internal error") {
      val req = url(s"http://localhost:$port/$app/error")
      val resp = Http(req)
      assert(Await.result(resp, 1 second).getStatusCode == 500)
      assert(Await.result(resp, 0 second).getResponseBody == "error message")
    }

    system stop server
  }

  test("performance - server should handle multiple requests") {
    val server = system.actorOf(Props(
      new ServerActor(app, port,
        mappings = Map(
          "/foo" -> Props(new RespondOneMB))
      )
    ))
    repeat(5) {
      val n = 100
      var i = 0
      var responses = List.empty[dispatch.Future[Response]]
      while (i < n) {
        val req = url(s"http://localhost:$port/$app/foo")
        val resp = Http(req)
        responses ::= resp
        i += 1
      }
      assert(Await.result(responses.head, 1 second).getStatusCode == 200)
    }

    withClue("and proceed with CPU load while IO in progress") {
      //TODO
    }

    system stop server
  }

  private def repeat(times: Int)(body: => Unit) {
    var i = 0
    while (i < times) { body; i += 1 }
  }
}

object ServerSuite {
  class WorkerMock extends Actor {
    context.parent ! Success("well done")
    context stop self
    val receive: Receive = { case _ => () }
  }
  class ErrorWorker extends Actor {
    context.parent ! Failure(new Error("error message"))
    context stop self
    val receive: Receive = { case _ => () }
  }
  class RespondOneMB extends Actor {
    context.parent ! Success(new String(new Array[Byte](1024*1024)))
    context stop self
    val receive: Receive = { case _ => () }
  }

  def isListening(port: Int): Boolean = Try {
    val socket = new Socket()
    socket.connect(new InetSocketAddress("localhost", port), 500)
    socket.close()
  } match {
    case Success(_) => true
    case Failure(_) => false
  }
}


