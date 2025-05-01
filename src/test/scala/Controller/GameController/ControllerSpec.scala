package Controller.GameController
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import de.htwg.Controler.GameControler.GameController
import de.htwg.Model.Card.*
import de.htwg.Model.Player.*
import de.htwg.View.UI.UI
import org.scalatest.PrivateMethodTester

class ControllerSpec extends AnyWordSpec with Matchers with PrivateMethodTester {
  // Dummy-UI für nicht-interaktive Tests
  val dummyUI: UI = new UI {
    def printLine(msg: String): Unit = ()
    def readLine(prompt: String): String = "n"
  }
  class StubUI(inputs: List[String]) extends UI {
    private var messages: List[String] = Nil
    private var inputQueue = inputs

    def printLine(msg: String): Unit = messages = messages :+ msg

    def readLine(prompt: String = ""): String = {
      val next = inputQueue.headOption.getOrElse("")
      inputQueue = inputQueue.drop(1)
      next
    }

    def getPrintedLines: List[String] = messages
  }
  "The GameController" should {
    "deal cards evenly among players" in {
      val gc = new GameController(dummyUI)
      val method = PrivateMethod[List[List[Card]]](Symbol("deal"))
      val hands = gc invokePrivate method(Card.all, 4)
      hands.size shouldBe 4
      hands.map(_.size).distinct.size shouldBe 1 // gleiche Kartenzahl
      hands.flatten.distinct.size shouldBe 52
    }

    "AIs are dealt cards in playRound" in {
      val gc = new GameController(dummyUI)
      val players = List("A", "B", "C").map(n => Player(n, Nil, isHuman = false)) // nur KIs
      val playRound = PrivateMethod[List[Player]](Symbol("playRound"))

      val result = gc invokePrivate playRound(players)

      result.size shouldBe 3
      result.foreach(_.hand.size should be <= 52 / players.size)
    }


    "AIs should remove played cards from hand" in {
      val j1 = Card("J", "♥")
      val j2 = Card("J", "♦")
      val p = Player("AI", List(Card("10", "♠"), j1, j2), isHuman = false)
      val gc = new GameController(dummyUI)
      val aiTurn = PrivateMethod[(Option[List[Card]], Player)](Symbol("aiTurn"))

      val (playedOpt, updated) = gc invokePrivate aiTurn(p, Some(List(Card("10", "♥"))))

      playedOpt.isDefined shouldBe true
      playedOpt.get should contain oneOf(j1, j2)
      updated.hand.count(_.value == "J") shouldBe 1

    }


    "let AI pass if no valid group exists" in {
      val p = Player("AI", List(Card("5", "♣"), Card("7", "♥"), Card("9", "♠")), isHuman = false)
      val gc = new GameController(dummyUI)
      val method = PrivateMethod[(Option[List[Card]], Player)](Symbol("aiTurn"))
      val (opt, updated) = gc invokePrivate method(p, Some(List(Card("K", "♦"), Card("K", "♣"))))
      opt shouldBe None
      updated shouldBe p
    }

  }
  "GameController.askForPlayers" should {
    "create players from stubbed input" in {
      val ui = new StubUI(List("4", "2", "Alice", "", ""))
      val gc = new GameController(ui)

      val method = PrivateMethod[List[Player]](Symbol("askForPlayers"))
      val players = gc invokePrivate method()

      players.size shouldBe 4
      players.count(_.isHuman) shouldBe 2
      players.map(_.name) should contain allOf("Alice", "Spieler2", "KI-1", "KI-2")
    }

    "fallback to defaults on invalid input" in {
      val ui = new StubUI(List("foo", "bar", "", ""))
      val gc = new GameController(ui)

      val method = PrivateMethod[List[Player]](Symbol("askForPlayers"))
      val players = gc invokePrivate method()

      players.size shouldBe 4
      players.count(_.isHuman) shouldBe 2
    }
  }

  "GameController.humanTurn" should {
    "handle passing correctly" in {
      val cards = List(Card("4", "♣"), Card("7", "♥"))
      val player = Player("Tester", cards)
      val ui = new StubUI(List("0"))
      val gc = new GameController(ui)

      val method = PrivateMethod[(Option[List[Card]], Player)](Symbol("humanTurn"))
      val (result, updated) = gc invokePrivate method(player, None)

      result shouldBe None
      updated shouldBe player
    }

    "remove selected cards from hand" in {
      val c1 = Card("8", "♠")
      val c2 = Card("9", "♠")
      val player = Player("Tester", List(c1, c2))
      val ui = new StubUI(List("1 2"))
      val gc = new GameController(ui)

      val method = PrivateMethod[(Option[List[Card]], Player)](Symbol("humanTurn"))
      val (result, updated) = gc invokePrivate method(player, None)

      result.get should contain allOf(c1, c2)
      updated.hand shouldBe empty
    }
  }

}
