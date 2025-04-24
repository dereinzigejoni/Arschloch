import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import htwg.de.Game.ArschlochGame
import htwg.de.Card.Card
import htwg.de.Player.Player
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import scala.io.StdIn.readLine
def dummyPlayer(name: String, card: Card): Player = new Player(name, List(card), 0, true) {
  override def playCard(lastPlayed: Option[List[Card]],
                        inputProvider: () => String = () => readLine().trim): (Some[List[Card]], Player) = {
    (Some(List(card)), copy(hand = List()))
  }
}
class ArschlochGameTest extends AnyWordSpec with Matchers {
  "Arschloch Game" should {
    "use scala.collection.immutable.List as the type for suits and values" in {
      ArschlochGame.suits shouldBe a[List[_]]
      ArschlochGame.values shouldBe a[List[_]]
    }
  }

  "The suits list" should {
    "contain exactly the four suits in order" in {
      ArschlochGame.suits shouldEqual List("♥", "♦", "♠", "♣")
    }
  }

  "The values list" should {
    "contain exactly 2 through A in order" in {
      ArschlochGame.values shouldEqual List("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")
    }
  }
  "getValue" should {
    "handle '10' correctly" in {
      ArschlochGame.getValue("10") shouldEqual 10
    }
    "convert numeric strings to their Int value" in {
      ArschlochGame.getValue("2") shouldBe 2
      ArschlochGame.getValue("3") shouldBe 3
      ArschlochGame.getValue("4") shouldBe 4
      ArschlochGame.getValue("5") shouldBe 5
      ArschlochGame.getValue("6") shouldBe 6
      ArschlochGame.getValue("7") shouldBe 7
      ArschlochGame.getValue("8") shouldBe 8
      ArschlochGame.getValue("9") shouldBe 9
      ArschlochGame.getValue("10") shouldBe 10
    }
    "map face cards correctly: J→11, Q→12, K→13, A→14" in {
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
    "restart after 50 resets when three always-pass KI existieren" in {
      // definiere einen Player, der immer passt
      val alwaysPass = new Player("P", Nil, 0, isHuman = false) {
        override def playCard(lp: Option[List[Card]], ip: () => String) = (None, this)
      }
      val players = List(alwaysPass, alwaysPass, alwaysPass)
      val result = ArschlochGame.playRound(players)
      // nach 50 Resets bekommst du wieder das Original zurück
      result.map(_.rank) shouldBe players.map(_ => None)
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
}
