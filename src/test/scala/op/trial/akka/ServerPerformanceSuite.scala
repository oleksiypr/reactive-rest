package op.trial.akka

import akka.actor.{Actor, Props, ActorSystem}
import akka.testkit.TestKit
import com.ning.http.client.Response
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success
import java.util.concurrent.TimeoutException

class ServerPerformanceSuite extends TestKit(ActorSystem("ServerPerformanceSuite"))
                                with WordSpecLike
                                with BeforeAndAfterAll {
  import ServerPerformanceSuite._
  import dispatch._, Defaults._

  val port = 9090
  val app = "hello-app"
  val server = system.actorOf(Props(
    new ServerActor(app, port,
      mappings = Map(
        "/foo" -> Props(new RespondOneMB),
        "/bar" -> Props(new CpuAndIOLoad)
      )
    )
  ), "performance-test-server")


  override protected def afterAll() {
    shutdown()
  }

  "Server" must {
    "perform multiple IO operations in parallel" in {
      val n = 100
      var responses = Vector.empty[dispatch.Future[Response]]
      var i = 0
      val t0 = System.currentTimeMillis()
      while (i < n) {
        val req = url(s"http://localhost:$port/$app/foo")
        val resp = Http(req)
        responses :+= resp
        i += 1
      }
      val res = concurrent.Future.sequence(responses)
      Await.ready(res, 2.5 seconds)
      println(s"Done in ${System.currentTimeMillis() - t0} millis")
    }

    "proceed with CPU load while IO in progress " in {
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
      */
    }
  }
}

object ServerPerformanceSuite {
  class RespondOneMB extends RequestWorker((_: Unit) => new String(new Array[Byte](1024*1024)), ())
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
}
