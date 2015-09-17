package op.trial.akka

import akka.actor.{ActorRef, Props, Actor}
import scala.concurrent.Future
import scala.util.{Try, Failure, Success}
import ServerActor._

abstract class ServerActor extends Actor with LifeCicleAware {
  private[this] var jobs = Map.empty[ActorRef, Job]
  protected[this] def load = jobs.size

  override def preStart() = startUp()
  override def postStop() = shutDown()

  def service(deploy: WorkerDeploy): Receive = {
    case Service(workerProps, job) => jobs += deploy(workerProps) -> job
    case result: Try[_] =>  handleResult(result)
  }

  private def handleResult(result: Try[Any]) {
    import context.dispatcher
    val worker = sender()
    jobs get worker foreach { job =>
      jobs -= worker
      Future(respond(result, job))
    }
  }
  private def respond(result: Try[Any], job: Job) = result match {
    case Success(res) => job.success(res)
    case Failure(cause) => job.failure(cause)
  }
}

object ServerActor {
  type WorkerDeploy = Props => ActorRef
  trait Job {
    def success(res: Any): Unit
    def failure(cause: Throwable): Unit
  }
  case class Service(workerProps: Props, job: Job)
}