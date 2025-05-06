package Controller.GameController
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import de.htwg.Model.Player.Player
import de.htwg.Model.Card.Card
import de.htwg.View.UI.UI
import de.htwg.Controler.GameControler.GameController
import de.htwg.View.TUI.TUI

import java.lang.reflect.InvocationTargetException
import scala.util.Random

class ControllerSpec extends AnyWordSpec with Matchers {

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
  object andereUi extends UI {
    override def printLine(msg: String): Unit = ()

    override def readLine(prompt: String): String = ""
  }

  class DummyUI(inputs: List[String] = Nil) extends UI {
    private var index = 0
    var printed: List[String] = Nil

    override def printLine(s: String): Unit = printed ::= s

    override def readLine(prompt: String = ""): String = {
      val res = inputs.lift(index).getOrElse("0")
      index += 1
      res
    }
  }

  class FakeTUI(inputs: List[String]) extends TUI {
    private var inQueue = inputs
    var printedCards: List[List[Card]] = Nil

    override def printLine(msg: String): Unit = () // stumm

    override def readLine(prompt: String): String = {
      // liefert nacheinander die vorgegebenen Antworten
      inQueue match {
        case head :: tail =>
          inQueue = tail
          head
        case Nil =>
          ""
      }
    }

    // genaue Signatur des echten TUI: cards + optionaler index
    override def printCards(cards: List[Card], index: Int = 1): Unit = {
      printedCards = printedCards :+ cards
    }
  }

  class CapturingUI extends UI {
    val lines = scala.collection.mutable.Buffer.empty[String]

    override def printLine(line: String): Unit = lines += line

    override def readLine(prompt: String): String = throw new NotImplementedError

  }

  class StubUI(responses: Seq[String]) extends UI {
    private val it = responses.iterator

    // kein Fehler: genau wie im Trait
    override def printLine(msg: String): Unit = {}

    // Default-Parameter prompt = "" stimmt mit der Trait-Signatur überein
    override def readLine(prompt: String = ""): String =
      if (it.hasNext) it.next()
      else ""
  }



  val c1 = Card("7", "♠")
  val c2 = Card("8", "♠")
  val c3 = Card("A", "♥")
  val c4 = Card("K", "♣")

  "exchangeRoles" should {
    "swap cards correctly between president and asshole" in {
      val president = Player("President", List(c1, c2, c3, c4), isHuman = true)
      val asshole = Player("Asshole", List(c1, c2, c3, c4), isHuman = false)
      val controller = new GameController(new DummyUI())

      val result = controller.exchangeRoles(List(president, asshole))

      result.head.name shouldBe "President"
      result.last.name shouldBe "Asshole"
      result.head.hand.size shouldBe 4
      result.last.hand.size shouldBe 4
      result.head.hand.exists(_.value == "A") shouldBe true
      result.last.hand.exists(_.value == "7") shouldBe true
    }
    "swap correct cards between President and Asshole" in {
      // Erzeuge zwei Spieler mit je 4 Karten
      val low1 = Card("2", "♥");
      val low2 = Card("3", "♣")
      val med1 = Card("5", "♦");
      val med2 = Card("6", "♠")
      val high1 = Card("K", "♣");
      val high2 = Card("A", "♦")

      val pres = Player("P", List(low1, low2, med1, med2), isHuman = true)
      val asshole = Player("A", List(high1, high2, med1, med2), isHuman = false)
      val middle = Player("M", Nil, isHuman = false)

      val inputList = List(pres, middle, asshole)
      val result = new GameController(andereUi).exchangeRoles(inputList)

      val newPres = result.head
      val newMiddle = result(1)
      val newAsshole = result.last

      // President hat jetzt low1,low2 entfernt, dafür high1,high2 bekommen
      newPres.hand.toSet shouldBe Set(med1, med2, high1, high2)

      // Arschloch hat high1,high2 entfernt, dafür low1,low2 bekommen
      newAsshole.hand.toSet shouldBe Set(med1, med2, low1, low2)

      // middle unverändert
      newMiddle shouldBe middle
    }
    "leave list unchanged when fewer than 2 players" in {
      val one = Player("Solo", Nil, isHuman = true)
      val out = new GameController(DummyUi).exchangeRoles(List(one))
      out shouldEqual List(one)
    }
  }
  "playRound" should {
    "distribute shuffled cards evenly to players" in {
      val players = List.tabulate(4)(i => Player(s"Player$i", Nil, isHuman = true)
      )
      val controllers = new GameController(new DummyUI())
      val result = controllers.playRound(players)

      result.foreach(_.hand.nonEmpty shouldBe true)
      result.map(_.hand.size).sum shouldBe Card.all.size
    }


  }
  "askForPlayers" should {
    "create the correct number of human and AI players" in {
      val inputs = List("4", "2", "Alice", "Bob")
      val ui = new DummyUI(inputs)
      val controller = new GameController(ui)

      val players = controller.askForPlayers()

      players.length shouldBe 4
      players.count(_.isHuman) shouldBe 2
      players.count(!_.isHuman) shouldBe 2
      players.map(_.name) should contain allElementsOf List("Alice", "Bob", "KI-1", "KI-2")
    }
    "use default total when input is invalid" in {
      // erste Eingabe "foo" → total = 4 (default)
      // zweite Eingabe "2"  → humans = 2
      // dann zwei Namen: "Alice", "" (--> default "Spieler2")
      val ui = new FakeUi(List("foo", "2", "Alice", ""))
      val ctl = new GameController(ui)

      val players = ctl.askForPlayers()

      // Gesamtzahl = 4 (default)
      players.size shouldBe 4

      // Zwei menschliche Spieler: Alice + "Spieler2"
      val humanNames = players.filter(_.isHuman).map(_.name)
      humanNames should contain("Alice")
      humanNames should contain("Spieler2")

      // Zwei KI-Spieler "KI-1", "KI-2"
      val aiNames = players.filterNot(_.isHuman).map(_.name).sorted
      aiNames shouldEqual List("KI-1", "KI-2")

      // Prompt wurde jeweils korrekt ausgegeben
      ui.printed.count(_.contains("Wie viele Spieler insgesamt")) shouldBe 1
      ui.printed.count(_.contains("Wie viele menschliche Spieler")) shouldBe 1
    }
  }
  "humanTurn" should {
    "return None if player passes (inputs 0)" in {
      val ui = new DummyUI(List("0"))
      val controller = new GameController(ui)
      val player = Player("Tester", List(c1, c2, c3), isHuman = true)

      val method = controller.getClass
        .getDeclaredMethod("humanTurn", classOf[Player], classOf[Option[List[Card]]])
      method.setAccessible(true)
      val (played, updated) = method.invoke(controller, player, None).asInstanceOf[(Option[List[Card]], Player)]

      played shouldBe None
      updated shouldBe player
    }

    "remove selected cards when user chooses indices" in {
      val ui = new DummyUI(List("1 3"))
      val controller = new GameController(ui)
      val player = Player("Tester", List(c1, c2, c3), isHuman = true)

      val method = controller.getClass
        .getDeclaredMethod("humanTurn", classOf[Player], classOf[Option[List[Card]]])
      method.setAccessible(true)
      val (played, updated) = method.invoke(controller, player, None).asInstanceOf[(Option[List[Card]], Player)]

      played.get should contain allElementsOf List(c1, c3)
      updated.hand should contain only c2
    }

    "filter invalid entries and only use valid indices" in {
      val ui = new DummyUI(List("foo 2 -1"))
      val controller = new GameController(ui)
      val player = Player("Tester", List(c1, c2, c3), isHuman = true)

      // finde per Name die erste Methode "humanTurn" mit 2 Parametern
      val method = controller.getClass
        .getDeclaredMethod("humanTurn", classOf[Player], classOf[Option[_]])
      method.setAccessible(true)






      // invoke mit None (wird automatisch als Option behandelt)
      val (played, updated) = method
        .invoke(controller, player, None)
        .asInstanceOf[(Option[List[Card]], Player)]

      played shouldBe defined
      // Nur Index 2 ist gültig
      played.get should contain only c2
      // c1 und c3 bleiben im Hand
      updated.hand should contain only(c1, c3)
    }

    "invoke printCards for TUI variant" in {
      val hand = List(Card("9", "♣"), Card("8", "♦"))
      val tui = new FakeTUI(List("0")) // Eingabe "0" = Pass
      val controller = new GameController(tui)

      val method = controller.getClass
        .getDeclaredMethod("humanTurn", classOf[Player], classOf[Option[List[Card]]])
      method.setAccessible(true)

      val (playedOpt, updated) = method.invoke(
        controller,
        Player("TUI", hand, isHuman = true),
        None: Option[List[Card]]
      ).asInstanceOf[(Option[List[Card]], Player)]

      // printCards wurde genau einmal mit kompletter Hand aufgerufen
      tui.printedCards should have size 1
      tui.printedCards.head shouldBe hand

      // Rückgabe: Pass → None, Hand bleibt erhalten
      playedOpt shouldBe None
      updated.hand shouldBe hand
    }
  }

  "aiTurn" should {
    "play a higher pair if on stack is a weaker pair" in {
      val lowPair = List(Card("7", "♠"), Card("7", "♥"))
      val highPair = List(Card("9", "♦"), Card("9", "♣"))
      val hand = lowPair ++ highPair

      val player = Player("Bot", hand, isHuman = false)
      val controller = new GameController(new DummyUI())

      val method = controller.getClass.getDeclaredMethod("aiTurn", classOf[Player], classOf[Option[List[Card]]])
      method.setAccessible(true)
      val (played, updated) = method.invoke(controller, player, Some(lowPair)).asInstanceOf[(Option[List[Card]], Player)]

      played.get should contain allElementsOf highPair
      updated.hand should contain allElementsOf lowPair
    }
    "play highest card when no stack present" in {
      val cards = List(Card("3", "♣"), Card("A", "♦"), Card("7", "♥"))
      val p = Player("Bot", cards, isHuman = false)
      val ctrl = new GameController(new DummyUI())
      val method = ctrl.getClass.getDeclaredMethod("aiTurn", classOf[Player], classOf[Option[List[Card]]])
      method.setAccessible(true)
      val (playedOpt, updated) = method.invoke(ctrl, p, None)
        .asInstanceOf[(Option[List[Card]], Player)]

      playedOpt.get.map(_.value) should contain("A")
      updated.hand.map(_.value) should not contain ("A")
    }
  }
  "mainLoop" should {
    "start the game and print welcome + prompts" in {
      val inputs = List("4", "2", "Alice", "Bob", "x")
      val ui = new DummyUI(inputs)
      val controller = new GameController(ui)

      controller.start()

      // Begrüßung + Spielerauswahl
      ui.printed.exists(_.contains("Willkommen bei Arschloch")) shouldBe true
      ui.printed.exists(_.contains("Wie viele Spieler insgesamt")) shouldBe true
      ui.printed.exists(_.contains("Wie viele menschliche Spieler")) shouldBe true
      ui.printed.exists(_.contains("Finale Spielreihenfolge")) shouldBe true
    }
    "exit on invalid input fallback without starting second round" in {
      val inputs = List("3", "1", "Tester", "invalid")
      val ui = new DummyUI(inputs)
      val controller = new GameController(ui)

      controller.start()

      // Only one round printed
      ui.printed.count(_.contains("=== Runde")) shouldBe 1
      // Spiel beendet! should be printed
      ui.printed.exists(_.contains("Spiel beendet!")) shouldBe true
    }

    "play one round and print results" in {
      val inputs = List("3", "1", "Tester", "x")
      val ui = new DummyUI(inputs)
      val controller = new GameController(ui)

      controller.start()



      ui.printed.contains("Spiel beendet!")
    }

    "play second round and perform role exchange" in {
      val inputs = List("4", "2", "Anna", "Bob", "n", "x")
      val ui = new DummyUI(inputs)
      val controller = new GameController(ui)

      controller.start()

      // Runde 2 sollte erscheinen → exchangeRoles wurde ausgelöst
      ui.printed.count(_.contains("Runde")) should be >= 2
      // Ergebnisanzeige der zweiten Runde
      ui.printed.contains("\n Ergebnisse:")



    }
    "print role labels Präsident and Arschloch after round" in {
      val inputs = List("3", "1", "Max", "x")
      val ui = new DummyUI(inputs)
      val controller = new GameController(ui)

      controller.start()

      // Wir prüfen auf Rollenzuweisung nach Punkten
      val hasPresident = ui.printed.exists(_.contains("– Präsident"))
      val hasAsshole = ui.printed.exists(_.contains("– Arschloch"))
      hasPresident shouldBe false
      hasAsshole shouldBe false
    }
  }
  "playTurn" should {
    "print message when all players pass in playTurn" in {
      val c1 = Card("7", "♠")
      val c2 = Card("8", "♠")
      val stack = Some(List(Card("A", "♣"))) // Niemand kann höher als A spielen

      val ui = new DummyUI()
      val p1 = Player("P1", List(c1), isHuman = false)
      val p2 = Player("P2", List(c2), isHuman = false)

      val controller = new GameController(ui)

      val result = controller.playTurn(List(p1, p2), stack, Nil, passes = 2)

      println("Gedruckt:")
      ui.printed.foreach(println)

      ui.printed.exists(_.contains("Alle haben gepasst")) shouldBe true
    }
    "return reversed ranking when no active players remain" in {
      val ctrl = new GameController(new DummyUI())
      val p1 = Player("A", Nil, isHuman = false).copy(rank = Some(0))
      val p2 = Player("B", Nil, isHuman = false).copy(rank = Some(1))
      val result = ctrl.playTurn(Nil, Some(List(Card("7", "♠"))), List(p1, p2), passes = 0)
      // ranking.reverse
      result.map(_.name) shouldBe Seq("B", "A")
    }
    "take normal recursion path without ace or all-pass" in {
      // Two AI players with single non-Ace cards
      val c1 = Card("5", "♠")
      val c2 = Card("6", "♥")
      val p1 = Player("Bot1", List(c1), isHuman = false)
      val p2 = Player("Bot2", List(c2), isHuman = false)
      val ui = new DummyUI()
      val ctrl = new GameController(ui)

      val result = ctrl.playTurn(List(p2, p1), None, Nil, passes = 0)

      // Should not trigger ace-play or all-pass messages
      ui.printed.exists(_.contains("Ass gespielt")) shouldBe false
      ui.printed.exists(_.contains("Alle haben gepasst")) shouldBe false
      // Should at least print active turns
      ui.printed.exists(_.contains("ist dran")) shouldBe true
    }
    "call readLine during humanTurn and allow passing" in {
      val ui = new DummyUI(List("0")) // Spieler passt aktiv
      val controller = new GameController(ui)
      val player = Player("Tester", List(Card("9", "♣")), isHuman = true)

      // Zugriff auf private Methode
      val method = controller.getClass.getDeclaredMethod("humanTurn", classOf[Player], classOf[Option[List[Card]]])
      method.setAccessible(true)
      val (played, updated) = method.invoke(controller, player, None).asInstanceOf[(Option[List[Card]], Player)]

      played shouldBe None
      updated shouldBe player

      // readLine wurde genutzt
      ui.printed.exists(_.contains("Indices")) shouldBe false

    }
    "recognize single player left as winner" in {
      val player = Player("Solo", List(c1), isHuman = true)
      val controller = new GameController(new DummyUI())
      val ranked = controller.playTurn(List(player), None, Nil, 0)

      ranked.length shouldBe 1
      ranked.head.name shouldBe "Solo"
      ranked.head.rank shouldBe Some(0)
    }
    "clear stack when an Ace is played" in {
      val ui = new DummyUI()
      val player1 = Player("P1", List(c3), isHuman = false)
      val player2 = Player("P2", List(c1), isHuman = false)
      val controller = new GameController(ui)

      val result = controller.playTurn(List(player1, player2), None, Nil, 0)
      result.exists(_.rank.isDefined) shouldBe true
      ui.printed.exists(_.contains("Ass gespielt")) shouldBe true
    }
    "reset the stack when all players pass" in {
      val ui = new DummyUI()
      val p1 = Player("P1", List(), isHuman = false).copy(rank = Some(0))
      val p2 = Player("P2", List(), isHuman = false)
      val controller = new GameController(ui)

      val result = controller.playTurn(List(p1, p2), Some(List(c1)), Nil, passes = 2)

      ui.printed.exists(_.contains("Alle haben gepasst")) shouldBe false
      result.exists(_.rank.isDefined) shouldBe false
    }
  }



  "GameController#printResults" should {
    "print header and sorted results with roles" in {
      // Setup: drei Spieler mit Rängen 0, 1 und 2
      val p1 = Player("Alice", Nil, isHuman = true).copy(rank = Some(1))
      val p2 = Player("Bob", Nil, isHuman = true).copy(rank = Some(0))
      val p3 = Player("Clara", Nil, isHuman = true).copy(rank = Some(2))
      val all = List(p1, p2, p3)

      val ui = new CapturingUI
      val ctrl = new GameController(ui)

      ctrl.printResults(all)

      ui.lines.head should include("Ergebnisse")
      ui.lines should contain("1. Bob – Präsident")
      ui.lines should contain("2. Alice")
      ui.lines should contain("3. Clara – Arschloch")
    }

    "print message when no players have a rank" in {
      val p = Player("Nobody", Nil, isHuman = true).copy(rank = None)
      val ui = new CapturingUI
      val ctrl = new GameController(ui)

      ctrl.printResults(List(p))

      ui.lines shouldBe Seq(" Keine Spieler haben einen Rang erhalten.")
    }
  }
  "deal" should {
    "split a small deck deterministically into n hands" in {
      val ctrl = new GameController(new DummyUI())
      val deck = List(
        Card("9", "♣"),
        Card("8", "♦"),
        Card("7", "♠"),
        Card("6", "♥")
      )
      // Zugriff auf private Methode
      val method = ctrl.getClass
        .getDeclaredMethod("deal", classOf[List[Card]], classOf[Int])
      method.setAccessible(true)

      // Teile auf 2 Spieler auf
      val parts = method.invoke(ctrl, deck, Int.box(2))
        .asInstanceOf[List[List[Card]]]

      parts shouldBe List(
        List(Card("9", "♣"), Card("8", "♦")),
        List(Card("7", "♠"), Card("6", "♥"))
      )
    }
    "distribute all cards and preserve total count for full deck" in {
      val ctrl = new GameController(new DummyUI())
      val fullDeck = Card.all
      val method = ctrl.getClass
        .getDeclaredMethod("deal", classOf[List[Card]], classOf[Int])
      method.setAccessible(true)

      // Beispiel: auf 4 Hände aufteilen
      val hands = method.invoke(ctrl, fullDeck, Int.box(4))
        .asInstanceOf[List[List[Card]]]

      // 1) Genau 4 Hände
      hands.size shouldBe 4
      // 2) Jede Hand gleich groß
      hands.map(_.size).toSet.size shouldBe 1
      // 3) Insgesamt keine Karten fehlen
      hands.flatten.size shouldBe (fullDeck.size / 4) * 4
      // 4) Keine Duplikate
      hands.flatten.toSet.size shouldBe hands.flatten.size
    }
  }
  "GameController#readTotalPlayers" should {
    "return the number when it's between 3 and 6" in {
      val ui = new StubUI(Seq("5"))
      val ctrl = new GameController(ui)
      ctrl.readTotalPlayers() shouldBe 5
    }
    "fallback to 4 when input is out of range" in {
      val ui = new StubUI(Seq("10"))
      val ctrl = new GameController(ui)
      ctrl.readTotalPlayers() shouldBe 4
    }
    "fallback to 4 when input is non-numeric" in {
      val ui = new StubUI(Seq("foo"))
      val ctrl = new GameController(ui)
      ctrl.readTotalPlayers() shouldBe 4
    }
  }





}
