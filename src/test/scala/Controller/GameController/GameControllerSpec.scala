package Controller.GameController

import de.htwg.Controler.GameControler.GameController
import org.scalatest.funsuite.AnyFunSuite
import de.htwg.View.UI.UI
import de.htwg.Model.Card.Card
import de.htwg.Model.Player.Player
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers.shouldBe

/** Eine Fake-UI für tests von askForPlayers */
class FakeUi(var inputs: List[String]) extends UI {
  val printed = scala.collection.mutable.ListBuffer.empty[String]
  override def printLine(msg: String): Unit = printed += msg
  override def readLine(prompt: String = ""): String = {
    // Wir speichern auch den Prompt, um zu prüfen, dass er korrekt ausgegeben wurde
    printed += prompt
    inputs match {
      case h :: t =>
        inputs = t
        h
      case Nil =>
        throw new NoSuchElementException("No more inputs")
    }
  }
}

/** Ein No-Op UI, wenn readLine/printLine gar nicht aufgerufen werden sollen */
object DummyUi extends UI {
  override def printLine(msg: String): Unit = ()
  override def readLine(prompt: String): String = ""
}

class GameControllerSpec extends AnyWordSpec {





  "playRound" should {
    "deal the deck evenly among players" in {
      // Erstelle 5 Dummy-Spieler
      val players = (1 to 5).map(i => Player(s"P$i", Nil, isHuman = false)).toList
      val hands = new GameController(DummyUi).playRound(players)

      // Jede Hand hat deck.size / 5 Karten
      val deckSize = Card.all.size
      val expect = deckSize / 5
      hands.foreach { p => p.hand.size shouldBe expect }

      // Insgesamt verteilt: expect*5 Karten
      hands.map(_.hand.size).sum shouldBe expect * 5
    }
  }
}
