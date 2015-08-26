package op.trial.akka

trait LifeCicleAware {
  def startUp()
  def shutDown()
}
