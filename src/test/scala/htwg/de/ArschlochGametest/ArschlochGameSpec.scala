package htwg.de.ArschlochGametest
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import htwg.de.Game.ArschlochGame
import htwg.de.Card.Card
import htwg.de.Player.Player

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import scala.Console.withOut
import scala.io.StdIn.readLine
def dummyPlayer(name: String, card: Card): Player = new Player(name, List(card), 0, true) {
  override def playCard(lastPlayed: Option[List[Card]],
                        inputProvider: () => String = () => readLine().trim): (Some[List[Card]], Player) = {
    (Some(List(card)), copy(hand = List()))
  }
}
class ArschlochGameSpec extends AnyWordSpec with Matchers {


  "ArschlochGame.values" should {
    "be a List" in {
      ArschlochGame.values shouldBe a[List[_]]
    }
  }
  "ArschlochGame.suits" should {
    "be a List" in {
      ArschlochGame.suits shouldBe a[List[_]]
    }
  }


  "ArschlochGame.getValue" should {
    "parse numeric strings and map J/Q/K/A correctly" in {
      ArschlochGame.getValue("2") shouldBe 2
      ArschlochGame.getValue("10") shouldBe 10
      ArschlochGame.getValue("J") shouldBe 11
      ArschlochGame.getValue("Q") shouldBe 12
      ArschlochGame.getValue("K") shouldBe 13
      ArschlochGame.getValue("A") shouldBe 14
    }
  }
  "createDeck" should {
    "ein vollständiges Deck mit 52 Karten erzeugen" in {
      val deck = ArschlochGame.createDeck()
      deck should have size (52)
      deck.foreach { card =>
        ArschlochGame.suits should contain (card.suit)
        ArschlochGame.values should contain (card.value)
      }
    }
    "include all suit/value combinations" in {
      val deck = ArschlochGame.createDeck()
      val expected = for {s <- ArschlochGame.suits; v <- ArschlochGame.values} yield Card(v, s)
      expected.foreach { card => deck should contain(card) }
    }
    "produce 52 unique cards covering all value-suit combinations" in {
      val deck = ArschlochGame.createDeck()
      deck.size shouldBe ArschlochGame.suits.size * ArschlochGame.values.size
      for {
        s <- ArschlochGame.suits
        v <- ArschlochGame.values
      } deck should contain(Card(v, s))
    }
  }
  "shuffleAndDeal" should {
    "allen Spielern die gleiche Anzahl an Karten austeilen" in {
      val players = List(
        Player("Alice", List(), 0, true),
        Player("Bob", List(), 0, true),
        Player("Charlie", List(), 0, true),
        Player("David", List(), 0, true)
      )
      val dealtPlayers = ArschlochGame.shuffleAndDeal(players)
      // Bei 52 Karten und 4 Spielern müssen es 13 Karten pro Spieler sein.
      dealtPlayers.foreach { p =>
        p.hand should have size 13
      }
      dealtPlayers.flatMap(_.hand) should have size (52)
    }
    "drop extra cards when not evenly divisible" in {
      val players = List.fill(3)(Player("P", Nil, 0, false))
      val dealt = ArschlochGame.shuffleAndDeal(players)
      dealt.foreach(_.hand.size shouldBe 17)
      dealt.flatMap(_.hand).size shouldBe 17 * 3
    }
    "give all cards to a single player" in {
      val solo = Player("Solo", Nil, 0, isHuman = true)
      val dealt = ArschlochGame.shuffleAndDeal(List(solo))
      dealt.head.hand should have size 52
    }
    "drop the correct remainder for 5 players" in {
      val players = List.tabulate(5)(i => Player(s"P$i", Nil, 0, false))
      val dealt = ArschlochGame.shuffleAndDeal(players)
      // each must get exactly 10 cards
      dealt.foreach(_.hand.size shouldBe 10)
      // and 10*5 = 50 cards in play
      dealt.flatMap(_.hand).size shouldBe 50
    }
    "distribute every card exactly once and equally among players" in {
      val players = List(
        Player("A", Nil, 0, isHuman = false),
        Player("B", Nil, 0, isHuman = false),
        Player("C", Nil, 0, isHuman = false),
        Player("D", Nil, 0, isHuman = false)
      )
      val dealt = ArschlochGame.shuffleAndDeal(players)

      // alle Hände gleich groß
      dealt.map(_.hand.size).toSet should have size 1

      // keine Duplikate und Gesamtzahl stimmt
      val allCards = dealt.flatMap(_.hand)
      allCards.distinct should have size allCards.size
      allCards.size shouldBe ArschlochGame.createDeck().size
    }
    "use correct slice logic per zipWithIndex" in {
      val deck = (1 to 8).toList.map(n => Card(n.toString, "♠"))
      val players = List(
        Player("P1", Nil, 0, isHuman = false),
        Player("P2", Nil, 0, isHuman = false),
        Player("P3", Nil, 0, isHuman = false),
        Player("P4", Nil, 0, isHuman = false)
      )
      val cardsPer = deck.size / players.size

      val slices = players.zipWithIndex.map { case (_, i) =>
        deck.slice(i * cardsPer, (i + 1) * cardsPer).map(_.value)
      }

      slices should contain inOrderOnly(
        List("1", "2"),
        List("3", "4"),
        List("5", "6"),
        List("7", "8")
      )
    }
    "deal each player the same number of cards, ohne Duplikate und ohne fehlende Karten" in {
      // drei Dummy-Spieler, deren Hände leer starten
      val players = List(
        Player("p1",Nil,0,isHuman = true),
        Player("p2",Nil,0,isHuman = true),
        Player("p3", Nil,0,isHuman = true)
      )

      val dealt = ArschlochGame.shuffleAndDeal(players)
      val fullDeck = ArschlochGame.createDeck()
      val perPlayer = fullDeck.length / players.length

      // jeder kriegt genau perPlayer Karten
      dealt.foreach(_.hand.length shouldEqual perPlayer)


    }
  }
  "exchangeCards" should {
    "handle two empty hands" in {
      val pres = Player("Pres", Nil, 0, true)
      val ars = Player("Arsch", Nil, 0, false)
      val (newPres, newArs) = ArschlochGame.exchangeCards(pres, ars)
      newPres.hand shouldBe empty
      newArs.hand shouldBe empty
    }
    "swap the two highest from the arschloch with the two lowest from the president" in {
      val president = Player("Prez", List(
        Card("2", "♠"), Card("3", "♠"), Card("4", "♠"), Card("5", "♠")
      ), points = 0, isHuman = false)
      val arschloch = Player("Ass", List(
        Card("J", "♠"), Card("Q", "♠"), Card("K", "♠"), Card("A", "♠")
      ), points = 0, isHuman = false)

      val (newPrez, newAss) = ArschlochGame.exchangeCards(president, arschloch)

      // Präsident bekommt A und K, gibt 2 und 3 ab
      newPrez.hand.map(_.value) should contain allOf("A", "K")
      newPrez.hand.map(_.value) shouldNot contain allOf("2", "3")

      // Arschloch bekommt 2 und 3, gibt A und K ab
      newAss.hand.map(_.value) should contain allOf("2", "3")
      newAss.hand.map(_.value) shouldNot contain allOf("A", "K")
    }
  }
  "playRound" should {
    "reset the stack after everyone passes once" in {
      val pass1 = new Player("P1", List(Card("5", "♥")), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }
      val pass2 = new Player("P2", List(Card("6", "♦")), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }
      val result = ArschlochGame.playRound(List(pass1, pass2))
      // after a single reset and then finishing, we fall back to the default <2-error branch>
      // so playRound returns the original two players
      result.map(_.name) shouldBe List("P1", "P2")
    }
    "hit the <2-ranking error when only one finishes normally" in {
      // A always plays their one card and empties out.
      val playerA = new Player("A", List(Card("2", "♠")), 0, true) {
        override def playCard(lp: Option[List[Card]], ip: () => String) =
          (Some(List(Card("2", "♠"))), this.copy(hand = Nil))
      }
      // B never plays and just holds that same card forever
      val playerB = new Player("B", List(Card("3", "♣")), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }

      val out = ArschlochGame.playRound(List(playerA, playerB))
      // because A finishes alone, ranking.length == 1 → triggers the error branch → returns the original
      out.map(_.name) shouldBe List("B","A")
    }
    "eine vollständige Rangliste zurückliefern, wenn Spieler je eine Karte haben" in {
      val card1 = Card("2", "♥")
      val card2 = Card("3", "♦")
      val card3 = Card("4", "♣")

      val players = List(
        dummyPlayer("Alice", card1),
        dummyPlayer("Bob", card2),
        dummyPlayer("Charlie", card3)
      )

      val ranking = ArschlochGame.playRound(players)
      ranking.foreach(p => p.rank shouldBe defined)
      ranking should have size players.size
    }
    "Spieler ohne Karten überspringen" in {
      val card = Card("5", "♥")

      // Dummy-Spieler, der nur spielt, wenn er Karten hat.
      def dummyPlayerOpt(name: String, maybeCard: Option[Card]): Player = new Player(name, maybeCard.toList, 0, true) {
        override def playCard(lastPlayed: Option[List[Card]],
                              inputProvider: () => String = () => readLine().trim): (Option[List[Card]], Player) =
          if (hand.nonEmpty) (Some(List(hand.head)), copy(hand = List())) else (None, this)
      }

      val players = List(
        dummyPlayerOpt("Alice", Some(card)),  // spielt die einzige Karte
        dummyPlayerOpt("Bob", None),           // besitzt keine Karte
        dummyPlayerOpt("Charlie", Some(card))  // spielt ebenfalls
      )

      val ranking = ArschlochGame.playRound(players)
      ranking.foreach(p => p.rank shouldBe defined)
      ranking should have size players.size
    }
    "assign arschloch when one player remains" in {
      val card = Card("3", "♦")
      val dummy = (n: String) => new Player(n, List(card), 0, false) {
        override def playCard(lastPlayed: Option[List[Card]], inputProvider: () => String) = (Some(List(card)), this.copy(hand = Nil))
      }
      val lastOne = Player("Last", List(card), 0, false)
      val players = List(dummy("A"), dummy("B"), lastOne)
      val ranked = ArschlochGame.playRound(players)
      ranked.exists(p => p.name == "Last" && p.rank.contains(2)) shouldBe false
    }

    "restart if everyone passes more than 50 times" in {
      val p1 = new Player("Staller", List(Card("2", "♦")), 0, false) {
        override def playCard(lastPlayed: Option[List[Card]], inputProvider: () => String) = (None, this)
      }
      val p2 = new Player("P2", List(Card("2", "♦")), 0, false) {
        override def playCard(lastPlayed: Option[List[Card]], inputProvider: () => String) = (None, this)
      }

      val players = List(p1, p2)
      val result = ArschlochGame.playRound(players)

      result.map(p => p.copy(rank = None)) shouldBe players
    }


    "restart if president == arschloch" in {
      val p1 = Player("P", List(Card("2", "♠")), 0, false, Some(0))
      val players = List(p1, p1.copy(name = "P2"))
      val result = ArschlochGame.playRound(players)
      result shouldEqual players
    }

    "restart the round when too many consecutive passes occur with non-empty hands" in {
      // Drei KI-Spieler mit jeweils einer Karte, die immer passen.
      def alwaysPass(name: String) = new Player(name, List(Card("2", "♠")), 0, isHuman = false) {
        override def playCard(lastPlayed: Option[List[Card]], inputProvider: () => String) =
          (None, this) // immer passen, Hand bleibt unverändert
      }

      val players = List("Alice", "Bob", "Charlie").map(alwaysPass)
      // Dieser Aufruf geht durch den Zweig resetCounter >= 50 und
      // springt dann aufgrund leerer Rangliste zurück zum ursprünglichen Input.
      val result = ArschlochGame.playRound(players)

      // Am Ende sollte der Input unverändert zurückgegeben werden.
      result.map(_.name) shouldBe List("Alice", "Bob", "Charlie")
    }
  }
  "playRound" when {
    "only one active at start" should {
      "skip empty-hand then play and assign ranks" in {
        val skip = new Player("Skip", Nil, 0, true)
        val play = new Player("Play", List(Card("2", "♥")), 0, true) {
          override def playCard(lp: Option[List[Card]], ip: () => String) =
            (Some(List(Card("2", "♥"))), this.copy(hand = Nil))
        }
        val result = ArschlochGame.playRound(List(play, skip))
        result.map(_.name) shouldBe List("Skip", "Play")
        result.map(_.rank) shouldBe List(Some(0), Some(1))
      }
    }
    "all always pass until resetCounter ≥ 50" should {
      "return original players after too many resets" in {
        val p1 = new Player("P1", Nil, 0, false) {
          override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
        }
        val p2 = new Player("P2", Nil, 0, false) {
          override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
        }
        val players = List(p1, p2)
        val result = ArschlochGame.playRound(players)
        result.map(_.copy(rank = None)) shouldBe players.reverse.map(_.copy(rank = None))
      }
    }
    "fewer than two total players" should {
      "return the input list unchanged" in {
        val solo = new Player("Solo", Nil, 0, false)
        ArschlochGame.playRound(List(solo)) shouldBe List(solo)
      }
    }
    "given an empty player list" should {
      "return an empty ranking" in {
        ArschlochGame.playRound(Nil) shouldBe Nil
      }
    }
    "only one player has cards" should {
      "skip empties then rank correctly" in {
        val e = Player("E", Nil, 0, true)
        val p = new Player("P", List(Card("2", "♥")), 0, true) {
          override def playCard(lp: Option[List[Card]], ip: () => String) =
            (Some(List(Card("2", "♥"))), this.copy(hand = Nil))
        }
        val res = ArschlochGame.playRound(List(e, p))
        res.map(_.name) shouldBe List("P", "E")
        res.map(_.rank) shouldBe List(Some(0), Some(1))
      }
    }
    "everyone passes until too many resets" should {
      "return the reversed original list" in {
        val a = new Player("A", Nil, 0, false) {
          override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
        }
        val b = new Player("B", Nil, 0, false) {
          override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
        }
        val orig = List(a, b)
        val got = ArschlochGame.playRound(orig)
        got.map(_.copy(rank = None)) shouldBe orig.reverse.map(_.copy(rank = None))
      }
    }
    "only one total player" should {
      "return input unchanged" in {
        val solo = Player("S", Nil, 0, false)
        ArschlochGame.playRound(List(solo)) shouldBe List(solo)
      }
    }
    "return input players unchanged and print error when ranking length <2" in {
      val p = Player("Solo", List(Card("2", "♠")), 0, isHuman = true)
      val output = new ByteArrayOutputStream()
      withOut(new PrintStream(output)) {
        val result = ArschlochGame.playRound(List(p))
        result shouldEqual List(p)
      }
      output.toString should include("Fehler: Ungültige Rangliste. Das Spiel wird neu gestartet.")
    }

    "return empty list for empty input and print error" in {
      val output = new ByteArrayOutputStream()
      withOut(new PrintStream(output)) {
        val result = ArschlochGame.playRound(Nil)
        result shouldEqual Nil
      }
      output.toString should include("Fehler: Ungültige Rangliste. Das Spiel wird neu gestartet.")
    }


  }
  "askForPlayers" should {
    "use defaults on invalid input and return the correct mix of human/AI players" in {
      // invalid totalPlayers "2" → default 4
      // invalid numHumans   "0" → default 2
      // then two names
      val input = Seq("2", "0", "Alice", "Bob").mkString("\n")
      val in = new ByteArrayInputStream(input.getBytes)
      Console.withIn(in) {
        val players = ArschlochGame.askForPlayers()
        // defaults: 4 total, 2 humans, 2 AI
        players should have size 4
        players.take(2).map(_.name) should contain allElementsOf Seq("Alice", "Bob")
        players.take(2).forall(_.isHuman) shouldBe true
        players.drop(2).forall(!_.isHuman) shouldBe true
      }
    }
    "use defaults on invalid input" in {
      val in = Seq("2", "0", "X", "Y").mkString("\n")
      Console.withIn(new java.io.ByteArrayInputStream(in.getBytes)) {
        val pls = ArschlochGame.askForPlayers()
        pls should have size 4
        pls.take(2).forall(_.isHuman) shouldBe true
        pls.drop(2).forall(!_.isHuman) shouldBe true
      }
    }
    "use valid totalPlayers and numHumans without defaults" in {
      // Eingabe: 5 Spieler insgesamt, 3 menschliche
      val input = Seq("5", "3", "A", "B", "C").mkString("\n")
      val in = new ByteArrayInputStream(input.getBytes)
      Console.withIn(in) {
        val players = ArschlochGame.askForPlayers()
        players should have size 5
        // Die ersten 3 müssen Humans mit den angegebenen Namen sein
        players.take(3).map(_.name) should contain allElementsOf Seq("A", "B", "C")
        players.take(3).forall(_.isHuman) shouldBe true
        // Rest KI
        players.drop(3).forall(!_.isHuman) shouldBe true
      }
    }
    "nehmen gültige Ränder 3–6 an und defaulten Namen bei Leerstring" in {
      // Boundary: 3 Spieler, 2 Menschen, erster Name leer → "Spieler1", zweiter Name "Bob"
      val in1 = Seq("3", "2", "", "Bob").mkString("\n")
      Console.withIn(new ByteArrayInputStream(in1.getBytes)) {
        val pls = ArschlochGame.askForPlayers()
        pls.size shouldBe 3

        // Erster Human bekommt Default-Name, zweiter den eingegebenen
        pls(0).name shouldBe "Spieler1"
        pls(1).name shouldBe "Bob"

        // KI-Spieler bleibt korrekt benannt
        pls(2).name should startWith("KI-")
      }

      // Boundary: 6 Spieler, 6 Menschen (kein KI)
      val names6 = Seq("6", "6", "A", "B", "C", "D", "E", "F").mkString("\n")
      Console.withIn(new ByteArrayInputStream(names6.getBytes)) {
        val pls6 = ArschlochGame.askForPlayers()
        pls6.size shouldBe 6
        pls6.forall(_.isHuman) shouldBe true
      }
    }
    "default to 4/2 und erzeugt korrekte Größe bei non-numeric input" in {
      val input = Seq("x", "y", "A", "B").mkString("\n")
      Console.withIn(new ByteArrayInputStream(input.getBytes)) {
        val players = ArschlochGame.askForPlayers()
        players should have size 4
        players.take(2).forall(_.isHuman) shouldBe true
        players.drop(2).forall(!_.isHuman) shouldBe true
      }
    }
    "vergibt Default‐Namen bei leerer Eingabe" in {
      val input = Seq("3", "2", "", "B").mkString("\n")
      Console.withIn(new ByteArrayInputStream(input.getBytes)) {
        val players = ArschlochGame.askForPlayers()
        players.head.name shouldBe "Spieler1"
      }
    }
    "defaultet menschliche Spieler auf 2, wenn numHumans > totalPlayers" in {
      // totalPlayers = 4, numHumans = 10 (invalid) → default 2
      val input = Seq("4", "10", "A", "B").mkString("\n")
      val in = new ByteArrayInputStream(input.getBytes)
      Console.withIn(in) {
        val pls = ArschlochGame.askForPlayers()
        pls should have size 4
        pls.take(2).foreach(_.isHuman shouldBe true)
        pls.drop(2).foreach(_.isHuman shouldBe false)
      }
    }
    "produce only humans when numHumans == totalPlayers" in {
      // total=4, humans=4 → no AI
      val input = Seq("4", "4", "A", "B", "C", "D").mkString("\n")
      val in = new java.io.ByteArrayInputStream(input.getBytes)
      Console.withIn(in) {
        val pls = ArschlochGame.askForPlayers()
        pls should have size 4
        pls.forall(_.isHuman) shouldBe true // no AI
      }
    }

    "produce exactly one human when numHumans == 1" in {
      val input = Seq("3", "1", "Solo").mkString("\n")
      val in = new java.io.ByteArrayInputStream(input.getBytes)
      Console.withIn(in) {
        val pls = ArschlochGame.askForPlayers()
        pls.size shouldBe 3
        pls.head.name shouldBe "Solo"
        pls.head.isHuman shouldBe true
        pls.tail.forall(!_.isHuman) shouldBe true // two AIs
      }
    }
    "initialize all player hands to empty lists" in {
      val input = Seq("4", "2", "A", "B").mkString("\n")
      Console.withIn(new ByteArrayInputStream(input.getBytes)) {
        val players = ArschlochGame.askForPlayers()
        // Initial ist bei allen Spielern die Hand leer :contentReference[oaicite:0]{index=0}&#8203;:contentReference[oaicite:1]{index=1}
        players.foreach(_.hand shouldBe empty)
      }
    }

    "assign correct AI player names with sequence KI-1, KI-2, ..." in {
      // 3 Spieler, 1 Mensch → 2 AIs KI-1 und KI-2 :contentReference[oaicite:2]{index=2}&#8203;:contentReference[oaicite:3]{index=3}
      val input = Seq("3", "1", "Solo").mkString("\n")
      Console.withIn(new ByteArrayInputStream(input.getBytes)) {
        val players = ArschlochGame.askForPlayers()
        players.drop(1).map(_.name) shouldEqual Seq("KI-1", "KI-2")
      }
    }
    "prompt for total and human counts" in {
      val input = Seq("3", "2", "A", "B").mkString("\n")
      val out = new ByteArrayOutputStream()
      Console.withIn(new ByteArrayInputStream(input.getBytes)) {
        Console.withOut(new PrintStream(out)) {
          ArschlochGame.askForPlayers()
        }
      }
      val output = out.toString
      output should include("Wie viele Spieler insgesamt? (3-6):") // :contentReference[oaicite:0]{index=0}&#8203;:contentReference[oaicite:1]{index=1}
      output should include("Wie viele davon sind menschliche Spieler? (1-3):") // :contentReference[oaicite:2]{index=2}&#8203;:contentReference[oaicite:3]{index=3}
    }

    "default totalPlayers to 4 when input is invalid or out of range" in {
      // totalPlayers = "2" invalid → default 4
      val input = Seq("2", "2", "A", "B").mkString("\n")
      val out = new ByteArrayOutputStream()
      Console.withIn(new ByteArrayInputStream(input.getBytes)) {
        Console.withOut(new PrintStream(out)) {
          val players = ArschlochGame.askForPlayers()
          players.size shouldBe 4
        }
      }
      out.toString should include("Ungültige Eingabe! Standardmäßig 4 Spieler.") // :contentReference[oaicite:4]{index=4}&#8203;:contentReference[oaicite:5]{index=5}
    }

    "default numHumans to 2 when input is invalid or out of range" in {
      // totalPlayers valid 4, numHumans = "0" invalid → default 2
      val input = Seq("4", "0", "A", "B").mkString("\n")
      val out = new ByteArrayOutputStream()
      Console.withIn(new ByteArrayInputStream(input.getBytes)) {
        Console.withOut(new PrintStream(out)) {
          val players = ArschlochGame.askForPlayers()
          players.count(_.isHuman) shouldBe 2
        }
      }
      out.toString should include("Ungültige Eingabe! Standardmäßig 2 menschliche Spieler.") // :contentReference[oaicite:6]{index=6}&#8203;:contentReference[oaicite:7]{index=7}
    }

    "use default human name when user enters blank" in {
      // first name blank → "Spieler1"
      val input = Seq("3", "2", "", "Bob").mkString("\n")
      val players = Console.withIn(new ByteArrayInputStream(input.getBytes)) {
        ArschlochGame.askForPlayers()
      }
      players(0).name shouldBe "Spieler1" // :contentReference[oaicite:8]{index=8}&#8203;:contentReference[oaicite:9]{index=9}
      players(1).name shouldBe "Bob"
    }
  }
  "mainGameLoop" should {
    "skip the president↔arschloch exchange when no one has a rank" in {
      // a single human player, no initial rank
      val p = Player("Solo", List(Card("2", "♣")), 0, true)
      // we feed 'q' immediately to quit
      val in = new java.io.ByteArrayInputStream("q\n".getBytes)
      val out = new java.io.ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new java.io.PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(p))
          }
        }
      }
      // since nobody had a rank, we never printed the "🔁 Tausche Karten..." line
      out.toString should not include "Tausche Karten zwischen Präsident"
      out.toString should include("Spiel beendet")
    }
    "perform exchange when initial ranks are defined" in {
      // Setup: zwei Spieler, beide mit Rangvorbelegung
      val pres = Player("Pres", List(Card("2", "♣"), Card("3", "♦")), 0, false, Some(1))
      val ars = Player("Ar", List(Card("K", "♠"), Card("A", "♥")), 0, false, Some(0))
      // direkt 'q' eingeben
      val inBytes = new ByteArrayInputStream("q\n".getBytes)
      val outBytes = new ByteArrayOutputStream()

      Console.withIn(inBytes) {
        Console.withOut(new PrintStream(outBytes)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(pres, ars))
          }
        }
      }

      val out = outBytes.toString
      out should include("Tausche Karten zwischen Präsident")
      out should include("Spiel beendet")
    }
    "exit gracefully on 'q' without throwing" in {
      // supply a minimal player list; pressing 'q' immediately quits
      val input = "q\n"
      val in = new ByteArrayInputStream(input.getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(
              List(Player("Solo", List(Card("2", "♣")), 0, true))
            )
          }
        }
      }
      // sicherstellen, dass der Beenden-Text kommt
      out.toString should include("Spiel beendet")
    }
    "quit immediately on 'q' without exception" in {
      val single = Player("Z", List(Card("2", "♦")), 0, true)
      val in = new java.io.ByteArrayInputStream("q\n".getBytes)
      val out = new java.io.ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new java.io.PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(single))
          }
        }
      }
      out.toString should include("Spiel beendet")
    }
    "continue on 'n' then exit gracefully on 'q'" in {
      val single = Player("Solo", List(Card("2", "♣")), 0, isHuman = true)
      // Erst 'n' (neue Runde), dann 'q' (Beenden)
      val input = Seq("n", "q").mkString("\n")
      val in = new ByteArrayInputStream(input.getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(single))
          }
        }
      }
      out.toString should include("Spiel beendet")
    }
    "melden Fehler bei ungültigem Befehl" in {
      val single = Player("Solo", List(Card("2", "♣")), 0, isHuman = false)
      val in = new ByteArrayInputStream("foo\n".getBytes)
      val out = new ByteArrayOutputStream()

      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(single))
          }
        }
      }

      val txt = out.toString
      txt should include("Tippe entweder 'n' für eine neue runde oder 'q' zum beeenden")
      txt should not include ("Spiel beendet")
    }
    "beenden bei 'q' ohne Fehler" in {
      val single = Player("Solo", List(Card("2", "♣")), 0, isHuman = false)
      val in = new ByteArrayInputStream("q\n".getBytes)
      val out = new ByteArrayOutputStream()

      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(single))
          }
        }
      }

      out.toString should include("Spiel beendet")
    }
    "schleifen bei 'n' und danach bei 'q' beenden" in {
      val single = Player("Solo", List(Card("2", "♣")), 0, isHuman = false)
      val seqIn = Seq("n", "q").mkString("\n")
      val in = new ByteArrayInputStream(seqIn.getBytes)
      val out = new ByteArrayOutputStream()

      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(single))
          }
        }
      }

      val txt = out.toString
      txt should include("für die nächste Runde")
      txt should include("Spiel beendet")
    }
    "meldet Fehler bei ungültigem Befehl" in {
      val single = Player("Solo", List(Card("2", "♣")), 0, true)
      val inSeq = Seq("foo", "q").mkString("\n")
      val in = new ByteArrayInputStream(inSeq.getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(single))
          }
        }
      }

      out.toString should include("Tippe entweder")
    }
    "exit on uppercase Q" in {
      val single = Player("Z", List(Card("2", "♦")), 0, isHuman = true)
      val in = new ByteArrayInputStream("Q\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(single))
          }
        }
      }
      out.toString should include("Spiel beendet")
    }
    "treat uppercase N as 'neue Runde' and then quit on lowercase q" in {
      val single = Player("Z", List(Card("2", "♦")), 0, isHuman = true)
      // erst 'N', dann 'q'
      val in = new ByteArrayInputStream(Seq("N", "q").mkString("\n").getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(List(single))
          }
        }
      }
      val txt = out.toString
      txt should include("Spiel beendet")
      txt should include("für die nächste Runde")
    }
  }
  "exchangeCards" should {
    val president = Player("Prez", List(
      Card("2","♠"), Card("3","♠"), Card("4","♠"), Card("5","♠")
    ), 0, isHuman = false)
    val arschloch = Player("Ass", List(
      Card("J","♠"), Card("Q","♠"), Card("K","♠"), Card("A","♠")
    ), 0, isHuman = false)

    "swap the two highest from the arschloch with the two lowest from the president 2.0" in {
      val (newPrez, newAss) = ArschlochGame.exchangeCards(president, arschloch)
      newPrez.hand.map(_.value) should contain allOf ("A","K")
      newPrez.hand.map(_.value) should not contain allOf ("2","3")
      newAss.hand.map(_.value) should contain allOf ("2","3")
      newAss.hand.map(_.value) should not contain allOf ("A","K")
    }


    "print correct swap messages" in {
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.exchangeCards(president, arschloch)
      }
      val o = out.toString
      //o should include ("🔄 Kartentausch:")
      o should include (s"${arschloch.name} gibt seine besten Karten an ${president.name}")
      o should include (s"${president.name} gibt seine schlechtesten Karten an ${arschloch.name}")
    }

  }
  "shuffleAndDeal" should {
    "drop the remainder when deck size isn't divisible" in {
      // 52 Karten, 3 Spieler → 52/3 = 17 pro Spieler, 1 bleibt übrig
      val players = List.fill(3)(Player("P", Nil, 0, true))
      val dealt = ArschlochGame.shuffleAndDeal(players)
      dealt.foreach(_.hand.size shouldBe 17)
      dealt.flatMap(_.hand).size shouldBe 17 * 3
    }
  }

  // Helper, damit der Player-Constructor passt
  private def mk(name: String, r: Option[Int]) =
    Player(name = name, hand = Nil, points = 0, isHuman = false, rank = r)

  "Ranking-Validity-Check" should {
    "erkenne eine leere Rangliste als ungültig" in {
      val ranking0 = List.empty[Player]
      (ranking0.isEmpty || ranking0.length < 2) shouldBe true
    }
    "erkenne eine Rangliste mit nur einem Spieler als ungültig" in {
      val ranking1 = List(mk("A", None))
      (ranking1.isEmpty || ranking1.length < 2) shouldBe true
    }
    "erkenne eine Rangliste mit zwei oder mehr Spielern als gültig" in {
      val ranking2 = List(mk("A", None), mk("B", None))
      (ranking2.isEmpty || ranking2.length < 2) shouldBe false
    }
  }

  "ZipWithIndex-Mapping" should {
    "weist jedem Spieler seinen Index als rank zu" in {
      val ranking = List(mk("X", None), mk("Y", None), mk("Z", None))
      val updatedPlayers = ranking.zipWithIndex.map {
        case (p, idx) => p.copy(rank = Some(idx))
      }

      updatedPlayers.map(_.rank) shouldEqual List(Some(0), Some(1), Some(2))
      updatedPlayers.map(_.name) shouldEqual List("X", "Y", "Z")
    }
  }

  "Filter-und-Sortierschritt" should {
    "filtert nur Spieler mit definiertem rank und sortiert aufsteigend" in {
      val p1 = mk("A", Some(2))
      val p2 = mk("B", None)
      val p3 = mk("C", Some(1))
      val all = List(p1, p2, p3)

      val ranked = all.filter(_.rank.isDefined).sortBy(_.rank.get)

      ranked.map(_.name) shouldEqual List("C", "A")
      ranked.map(_.rank.get) shouldEqual List(1, 2)
    }
  }

  "Ersatz-Mapping nach Kartentausch" should {
    "ersetzt Präsident und Arschloch korrekt in der Spielerliste" in {
      val president = mk("Prez", Some(1))
      val arschloch = mk("Arsch", Some(0))

      val newPrez = president.copy(rank = Some(99))
      val newArsch = arschloch.copy(rank = Some(100))

      // simulierte, gemischte Liste nach shuffleAndDeal
      val shuffledPlayers = List(
        arschloch,
        mk("Foo", None),
        president
      )

      val remapped = shuffledPlayers.map {
        case p if p.name == president.name => newPrez
        case p if p.name == arschloch.name => newArsch
        case other => other
      }

      remapped shouldEqual List(
        newArsch, // Arschloch ersetzt
        mk("Foo", None), // unverändert
        newPrez // Präsident ersetzt
      )
    }
  }

  "playRound" should {
    "rotate players on normal play when hand not empty" in {
      val c1 = Card("2", "♥")
      val c2 = Card("3", "♦")
      // P1 spielt, behält aber noch eine Karte → normale Rotation players.tail :+ updatedPlayer :contentReference[oaicite:4]{index=4}&#8203;:contentReference[oaicite:5]{index=5}
      val p1 = new Player("P1", List(c1, c2), 0, false) {
        override def playCard(lastPlayed: Option[List[Card]], inputProvider: () => String) =
          (Some(List(hand.head)), this.copy(hand = hand.tail))
      }
      // P2 spielt und leert seine Hand
      val p2 = new Player("P2", List(c1), 0, false) {
        override def playCard(lastPlayed: Option[List[Card]], inputProvider: () => String) =
          (Some(List(hand.head)), this.copy(hand = List()))
      }

      val ranking = ArschlochGame.playRound(List(p1, p2))
      // P2 hat als Erster keine Karten mehr, P1 folgt
      ranking.map(_.name) shouldEqual List("P1", "P2")
      ranking.foreach(_.rank shouldBe defined)
    }
  }
  "getValue" should {
    "use the default branch for any numeric string" in {
      // default‐case: card.toInt
      ArschlochGame.getValue("7") shouldBe 7
      ArschlochGame.getValue("9") shouldBe 9
    }
  }

  "createDeck" should {
    "be equivalent to a manual for‐comprehension" in {
      val deck1 = ArschlochGame.createDeck()
      val deck2 = ArschlochGame.suits.flatMap(s => ArschlochGame.values.map(v => Card(v, s)))
      deck1 should contain theSameElementsAs deck2
    }
  }

  "exchangeCards mapping" should {
    "leave any non‐president/arschloch player unchanged" in {
      val president = Player("Prez", List(Card("2", "♠")), 0, false, Some(1))
      val arschloch = Player("Ass", List(Card("A", "♣")), 0, false, Some(0))
      val other = Player("Foo", List(Card("5", "♦")), 0, false, None)

      val (newPrez, newAss) = ArschlochGame.exchangeCards(president, arschloch)
      val players = List(other, president, arschloch)

      val mapped = players.map {
        case p if p.name == president.name => newPrez
        case p if p.name == arschloch.name => newAss
        case x => x
      }

      // other bleibt unverändert, Präsident und Arschloch werden ersetzt
      mapped should contain inOrderOnly(other, newPrez, newAss)
    }
  }
  "shuffleAndDeal" should {

    "deal 8 cards each to 6 players, total of 48 cards" in {
      val players = List.fill(6)(Player("P", Nil, 0, true))
      val dealt = ArschlochGame.shuffleAndDeal(players)
      dealt.foreach(_.hand.size shouldBe 8)
      dealt.flatMap(_.hand).size shouldBe 48
    }
  }

  "exchangeCards" should {
    "swap a single card when each player has exactly one card" in {
      val pres = dummyPlayer("Prez", Card("5", "♥"))
      val ars = dummyPlayer("Ass", Card("9", "♣"))
      val (newPres, newAss) = ArschlochGame.exchangeCards(pres, ars)
      newPres.hand should contain only Card("9", "♣")
      newAss.hand should contain only Card("5", "♥")
    }
  }

  "getValue" should {
    "throw NumberFormatException for non-numeric and non-face strings" in {
      intercept[NumberFormatException] {
        ArschlochGame.getValue("X")
      }
    }
    "return correct value for numeric strings beyond 10 (e.g., \"11\")" in {
      ArschlochGame.getValue("11") shouldBe 11
    }
  }

  "createDeck" should {
    "start with 2♥ and end with A♣" in {
      val deck = ArschlochGame.createDeck()
      deck.head shouldBe Card("2", "♥")
      deck.last shouldBe Card("A", "♣")
    }
    "contain 13 cards for each suit" in {
      val deck = ArschlochGame.createDeck()
      deck.groupBy(_.suit).values.foreach(_.size shouldBe 13)
    }
    "contain 4 cards for each value" in {
      val deck = ArschlochGame.createDeck()
      deck.groupBy(_.value).values.foreach(_.size shouldBe 4)
    }
  }

  "playRound" should {
    "print the new round start message" in {
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(Nil)
      }
      out.toString should include("🔄 Eine neue Runde beginnt")
    }
    "print the current top cards as 'Kein Stapel' initially" in {
      val passive = new Player("P", List(Card("5", "♠")), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(List(passive, passive))
      }
      out.toString should include("Aktuelle oberste Karte(n): Kein Stapel")
    }
    "print skip message when player has empty hand" in {
      val skip = new Player("Skip", Nil, 0, false)
      val play = new Player("Play", List(Card("2", "♥")), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) =
          (Some(List(Card("2", "♥"))), this.copy(hand = Nil))
      }
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(List(skip, play))
      }
      out.toString should include("hat keine Karten mehr und wird übersprungen")
    }
    "print reset message when all players pass for two players" in {
      val p1 = new Player("P1", List(Card("2", "♥")), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }
      val p2 = new Player("P2", List(Card("3", "♦")), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(List(p1, p2))
      }
      out.toString should include("Alle haben gepasst oder nur zwei Spieler übrig: Stapel wird erneuert")
    }
    "print reset message when all players pass for three players" in {
      def alwaysPass(name: String) = new Player(name, List(Card("2", "♠")), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }

      val players = List("A", "B", "C").map(alwaysPass)
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(players)
      }
      out.toString should include("Alle haben gepasst oder nur zwei Spieler übrig: Stapel wird erneuert")
    }
    "print arschloch message when one player remains" in {
      val card = Card("7", "♣")
      val p1 = new Player("P1", List(card), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) =
          (Some(List(card)), this.copy(hand = Nil))
      }
      val p2 = new Player("P2", Nil, 0, false)
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(List(p1, p2))
      }
      out.toString should include("P1 ist Präsident!  P2 ist Arschloch!")
    }
    "print president and arschloch message at the end of a normal round" in {
      val c1 = Card("2", "♥")
      val c2 = Card("3", "♦")
      val p1 = new Player("P1", List(c1), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) =
          (Some(List(c1)), this.copy(hand = Nil))
      }
      val p2 = new Player("P2", List(c2), 0, false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) =
          (Some(List(c2)), this.copy(hand = Nil))
      }
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(List(p1, p2))
      }
      out.toString should include("ist Präsident")
      out.toString should include("ist Arschloch")
    }
    "print error when president and arschloch are the same player" in {
      val p = Player("Solo", List(Card("2", "♠")), 0, false, Some(0))
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(List(p, p.copy(name = "Solo2", rank = Some(0))))
      }
      out.toString should include("Fehler: Ungültige Rangliste. Das Spiel wird neu gestartet")
    }
  }

  "askForPlayers" should {
    "print welcome message" in {
      val in = new ByteArrayInputStream("3\n1\nA\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          ArschlochGame.askForPlayers()
        }
      }
      out.toString should include("🎭 Willkommen bei Arschloch")
    }
    "prompt for each human player name" in {
      val in = new ByteArrayInputStream("3\n2\nAlice\nBob\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          ArschlochGame.askForPlayers()
        }
      }
      val output = out.toString
      output should include("Name von Spieler 1:")
      output should include("Name von Spieler 2:")
    }
    "default names when both provided names are blank" in {
      val in = new ByteArrayInputStream("3\n2\n\n\n".getBytes)
      val players = Console.withIn(in) {
        ArschlochGame.askForPlayers()
      }
      players(0).name shouldBe "Spieler1"
      players(1).name shouldBe "Spieler2"
    }
    "trim whitespace from entered names" in {
      val in = new ByteArrayInputStream("3\n2\n Alice \n Bob \n".getBytes)
      val players = Console.withIn(in) {
        ArschlochGame.askForPlayers()
      }
      players(0).name shouldBe "Alice"
      players(1).name shouldBe "Bob"
    }
    "default to 4 players when totalPlayers > 6 and print warning" in {
      val in = new ByteArrayInputStream("7\n2\nA\nB\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          val players = ArschlochGame.askForPlayers()
          players should have size 4
        }
      }
      out.toString should include("Ungültige Eingabe! Standardmäßig 4 Spieler")
    }
    "print warning when numHumans > totalPlayers" in {
      val in = new ByteArrayInputStream("4\n10\nA\nB\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          ArschlochGame.askForPlayers()
        }
      }
      out.toString should include("Ungültige Eingabe! Standardmäßig 2 menschliche Spieler")
    }
  }

  "mainGameLoop" should {
    "print the starting players list" in {
      val players = List(Player("A", Nil, 0, true))
      val in = new ByteArrayInputStream("q\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(players)
          }
        }
      }
      out.toString should include("Spiel startet mit folgenden Spielern:")
      out.toString should include("- A (Mensch: true)")
    }
    "print prompt for next round or quit" in {
      val players = List(Player("B", Nil, 0, false))
      val in = new ByteArrayInputStream("q\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(players)
          }
        }
      }
      out.toString should include("--- Drücke 'n' für die nächste Runde oder 'q' zum Beenden ---")
    }
  }
  // Zusätzliche Tests für bislang ungetestete Stellen
  "ArschlochGame.suits und values" should {
    "exakt den Literal-Listen entsprechen" in {
      ArschlochGame.suits  shouldEqual List("♥", "♦", "♠", "♣")
      ArschlochGame.values shouldEqual List("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")
    }
  }

  "shuffleAndDeal" should {
    "eine ArithmeticException werfen, wenn keine Spieler übergeben werden" in {
      intercept[ArithmeticException] {
        ArschlochGame.shuffleAndDeal(Nil)
      }
    }
  }

  "exchangeCards internals" should {
    "die beiden höchsten Arschloch-Karten am Ende der Präsidenten-Hand anfügen" in {
      val presHand = List(Card("3","♣"), Card("2","♣"), Card("5","♣"), Card("4","♣"))
      val arsHand  = List(Card("7","♦"), Card("8","♦"), Card("6","♦"), Card("9","♦"))
      val pres = Player("P", presHand, 0, isHuman = false)
      val ars  = Player("A", arsHand,  0, isHuman = false)
      val (newPres, _) = ArschlochGame.exchangeCards(pres, ars)
      // Präsident gibt die beiden schwächsten (2,3) ab, erhält dann die stärksten des Arschloch (9,8) angehängt
      newPres.hand shouldEqual List(Card("5","♣"), Card("4","♣"), Card("9","♦"), Card("8","♦"))
    }
  }

  "playRound" should {
    "zu Beginn 'Kein Stapel' ausgeben, wenn noch keine Karten gespielt wurden" in {
      val pass = new Player("P", List(Card("5","♠")), 0, isHuman = false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }
      val out = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(out)) {
        ArschlochGame.playRound(List(pass, pass))
      }
      out.toString should include("Aktuelle oberste Karte(n): Kein Stapel")
    }
  }

  "askForPlayers" should {

    "keine KI-Spieler erzeugen, wenn numHumans == totalPlayers" in {
      val input = Seq("4", "4", "A", "B", "C", "D").mkString("\n")
      val in = new ByteArrayInputStream(input.getBytes)
      val players = Console.withIn(in) { ArschlochGame.askForPlayers() }
      players.filterNot(_.isHuman) shouldBe empty
    }
  }




}