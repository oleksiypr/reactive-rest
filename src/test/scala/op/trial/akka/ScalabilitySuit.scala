package op.trial.akka

import akka.actor.{Props, ActorSystem}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class ScalabilitySuit extends TestKit(ActorSystem("ScalabilitySuit"))
                        with WordSpecLike
                        with BeforeAndAfterAll {
  class FakeServer extends ServerActor(app = "fake-server", port = -1, mappings = Map.empty[String, Props]) with FakeServerAware {

  }

  trait FakeServerAware extends ServerAware

}
