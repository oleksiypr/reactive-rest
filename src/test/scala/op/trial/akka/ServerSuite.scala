package op.trial.akka

import java.net.{InetSocketAddress, Socket}
import java.util.concurrent.TimeoutException
import akka.actor.{Actor, Props, ActorSystem}
import akka.routing.{Listen, Listeners}
import akka.testkit.TestKit
import com.ning.http.client.Response
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike, BeforeAndAfterAll}
import scala.concurrent.{Future, Await}
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
      ) with Listeners {
        override val receive: Receive = listenerManagement orElse {
          case msg => super.receive(msg); gossip(msg)
        }
      }
    ))
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

  test("performance - server should handle multiple requests") {
    val server = system.actorOf(Props(
      new ServerActor(app, port,
        mappings = Map(
          "/foo" -> Props(new RespondOneMB),
          "/bar" -> Props(new CpuAndIOLoad)
        )
      )
    ))

    withClue("latency due to IO operations") {
      repeat(times = 1) {
        val t0 = System.currentTimeMillis()
        val n = 100
        var i = 0
        var responses = Vector.empty[dispatch.Future[Response]]
        while (i < n) {
          val req = url(s"http://localhost:$port/$app/foo")
          val resp = Http(req)
          responses :+= resp
          i += 1
        }
        val res = concurrent.Future.sequence(responses)
        Await.ready(res, 2 seconds)
        println(s"Done in ${System.currentTimeMillis() - t0} millis")
      }
    }

    //TODO uncomment when ready
    /*
    withClue("and proceed with CPU load while IO in progress") {
      val n = 12
      var i = 0
      var responses = List.empty[dispatch.Future[Response]]
      val t0 = System.currentTimeMillis()
      while (i < n) {
        val req = url(s"http://localhost:$port/$app/bar")
        val resp = Http(req)
        responses ::= resp
        i += 1
      }
      import scala.concurrent.Future
      val res = Future.sequence(responses)
      Await.result(res, 5 second)
      println(s"Done in ${System.currentTimeMillis() - t0} millis")
    }

    system stop server
    */
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
  class CpuAndIOLoad extends Actor {
    import context.dispatcher
    try Await.ready(Future{ while (true) () }, 1 second) catch {
      case ex : TimeoutException =>
      case th: Throwable => throw th
    }
    context.parent ! Success(new String(Array.emptyByteArray))
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


