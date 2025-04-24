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
  "getValue" should {
    "den richtigen numerischen Wert für Zahlen und Bildkarten zurückgeben" in {
      ArschlochGame.getValue("J") shouldEqual 11
      ArschlochGame.getValue("Q") shouldEqual 12
      ArschlochGame.getValue("K") shouldEqual 13
      ArschlochGame.getValue("A") shouldEqual 14
      ArschlochGame.getValue("5") shouldEqual 5
    }
    "handle '10' correctly" in {
      ArschlochGame.getValue("10") shouldEqual 10
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
  }
  "exchangeCards" should {
    "die Karten korrekt zwischen Präsident und Arschloch tauschen" in {
      val card2H = Card("2", "♥")  // Wert: 2
      val card3D = Card("3", "♦")  // Wert: 3
      val cardKC = Card("K", "♣")  // Wert: 13
      val cardAS = Card("A", "♠")  // Wert: 14

      // Präsident: Hand unsortiert, aber beim Sortieren (ascending) wird [2H, 3D, KC] generiert,
      // sodass die schlechtesten zwei Karten [2H, 3D] sind.
      val president = Player("President", List(card2H, card3D, cardKC), 0, true)
      // Arschloch: Beim Sortieren (descending) wird aus [KC, A♠, 3D] die Reihenfolge [A♠, KC, 3D] erzeugt,
      // sodass die besten zwei Karten [A♠, KC] sind.
      val arschloch = Player("Arschloch", List(cardKC, cardAS, card3D), 0, false)

      val (newPresident, newArschloch) = ArschlochGame.exchangeCards(president, arschloch)

      // Präsident erhält die Ursprungs­karten plus [A♠, KC]
      newPresident.hand should contain allElementsOf (List(card2H, card3D, cardKC, cardAS, cardKC))
      // Arschloch: aus ursprünglicher Hand [KC, A♠, 3D] wird [A♠, KC] entfernt und [2H, 3D] hinzugefügt.
      newArschloch.hand should contain allElementsOf (List(card3D, card2H, card3D))
    }
    "append the cards correctly to president and arschloch" in {
      val c1 = Card("2", "♥")
      val c2 = Card("3", "♣")
      val c3 = Card("K", "♠")
      val c4 = Card("A", "♦")
      val pres = Player("Pres", List(c1, c2), 0, true)
      val arsch = Player("Arsch", List(c3, c4), 0, false)

      val (newPres, newArsch) = ArschlochGame.exchangeCards(pres, arsch)

      newPres.hand should contain allElementsOf List(c1, c2, c3, c4)
      newArsch.hand.exists(_.value == "2") shouldBe true
    }
  }
  "playRound" should {
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
  }
  "mainGameLoop" should {
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
  }
  "exchangeCards" should {
    "swap correctly without removing from president" in {
      val pres = Player("Pres", List(Card("2", "♥"), Card("3", "♦")), 0, true)
      val ar = Player("Ar", List(Card("K", "♠"), Card("A", "♣")), 0, false)
      val (np, na) = ArschlochGame.exchangeCards(pres, ar)
      np.hand should contain allElementsOf List(Card("A", "♣"), Card("K", "♠"))
      na.hand should contain allElementsOf List(Card("2", "♥"), Card("3", "♦"))
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
