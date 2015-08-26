package op.trial.akka

trait ServerAware {
  def startUp()
  def shutDown()
}
