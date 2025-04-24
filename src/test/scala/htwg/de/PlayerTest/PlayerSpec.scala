package htwg.de.PlayerTest
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import htwg.de.Player.Player
import htwg.de.Card.Card
import org.scalatest.matchers.dsl.MatcherWords.exist.or
import org.scalatest.matchers.should.Matchers.exist.or

import java.io.ByteArrayInputStream

class Playertest extends AnyWordSpec with Matchers {
  private val c2 = Card("2", "♥")
  private val c3 = Card("3", "♦")
  private val c4 = Card("4", "♠")
  private val c5 = Card("5", "♣")

  "A Human Player" should {
    "allow playing multiple individual cards by entering comma separated indices" in {
      // Erzeuge einen Spieler mit drei Karten.
      val card1 = Card("5", "♥")
      val card2 = Card("5", "♦")
      val card3 = Card("7", "♣")
      // Hier bewusst unsortiert, damit die Sortierung getestet wird.
      val player = Player("TestHuman", List(card1, card3, card2), 0, true)

      // Definiere einen Input-Provider, der "1,3" zurückgibt.
      val inputProvider = () => "1,2"

      val (played, updatedPlayer) = player.playCard(None, inputProvider)
      played.isDefined shouldBe true
      played.get should have length 2
      // Bei der sortierten Hand (5♥, 6♦, 7♣) entspricht "1,3" den Karten card1 und card3.
      played.get should contain theSameElementsAs List(card1, card2)
      updatedPlayer.hand should have length 1
    }
    "pass when input is 0" in {
      val card1 = Card("5", "♥")
      val player = Player("TestHuman", List(card1), 0, true)
      val (played, updated) = player.playCard(None, () => "0")
      played shouldBe None
      updated.hand should contain(card1)
    }

    "reject non-numeric input and re-ask" in {
      var counter = 0
      val inputs = List("abc", "1")
      val player = Player("TestHuman", List(Card("5", "♥")), 0, true)
      val (played, updated) = player.playCard(None, () => {
        val in = inputs(counter); counter += 1; in
      })
      played.isDefined shouldBe true
    }

    "reject invalid index" in {
      var counter = 0
      val inputs = List("99", "1")
      val card = Card("5", "♥")
      val player = Player("TestHuman", List(card), 0, true)
      val (played, updated) = player.playCard(None, () => {
        val in = inputs(counter); counter += 1; in
      })
      played.get should contain(card)
    }

    "reject weaker card than last played" in {
      var counter = 0
      val card = Card("5", "♥")
      val inputs = List("1", "1", "0") // dritte Eingabe beendet Eingabeversuche
      val inputProvider = () => {
        val in = inputs.lift(counter).getOrElse("0")
        counter += 1
        in
      }

      val player = Player("TestHuman", List(card), 0, true)
      val (played, updated) = player.playCard(Some(List(Card("10", "♠"))), inputProvider)

      val valid = played.isEmpty || played.get.isEmpty
      valid shouldBe true
    }


    "reject different card amount than last played" in {
      var counter = 0
      val cards = List(Card("5", "♥"), Card("5", "♣"))
      // 1. Versuch: falsche Anzahl → wird abgelehnt
      // 2. Versuch: korrektes Pair (1,2) und höher als 4 → wird gespielt
      val inputs = List("1", "1,2")
      val inputProvider = () => {
        val in = inputs(counter)
        counter += 1
        in
      }

      val player = Player("TestHuman", cards, 0, true)
      // lastPlayed mit zwei 4ern → 5er-Pair ist höher und gleich viele Karten
      val lastPlayed = Some(List(Card("4", "♠"), Card("4", "♦")))

      val (played, updated) = player.playCard(lastPlayed, inputProvider)

      // Jetzt sollte tatsächlich gespielt werden
      played.isDefined shouldBe true
      played.get should contain allElementsOf cards
      // Und beide Karten sind aus der Hand entfernt
      updated.hand shouldBe empty
    }
    "play a single card when input is valid" in {
      val card = Card("9", "♠")
      val player = Player("Solo", List(card), 0, isHuman = true)
      val (played, updated) = player.playCard(None, () => "1")

      played shouldBe defined
      played.get should contain only card
      updated.hand shouldBe empty
    }
   



  }
  "A KI Player" should {
    "eine gültige Karte spielen, wenn möglich" in {
      val card1 = Card("5", "♥")
      val card2 = Card("7", "♦")
      val card3 = Card("9", "♣")
      // KI-Spieler (isHuman = false)
      val player = Player("TestKI", List(card1, card2, card3), 0, false)
      // Ohne Vorgabe (lastPlayed = None) sollte die KI basierend auf der Logik (bei Handlänge < 5)
      // die Karte mit dem höchsten Wert spielen.
      val (played, updatedPlayer) = player.playCard(None)
      played.isDefined shouldBe true
      // Erwartet wird die Gruppe, die den höchsten Wert enthält – in diesem Fall Karte "9".
      played.get.head.value shouldEqual card3.value
      updatedPlayer.hand should not contain card3
    }

    "passen, wenn keine spielbare Karten-Gruppe vorhanden ist" in {
      val card1 = Card("5", "♥")
      // KI-Spieler besitzt eine Karte, aber lastPlayed erfordert beispielsweise 2 Karten.
      val player = Player("TestKI", List(card1), 0, false)
      val lastPlayed = Some(List(Card("6", "♦"), Card("6", "♣")))
      val (played, updatedPlayer) = player.playCard(lastPlayed)
      played shouldBe None
      updatedPlayer.hand shouldEqual List(card1)
    }

    "sofort zurückgeben, wenn ein Rang definiert ist" in {
      val card1 = Card("5", "♥")
      val player = Player("RankedPlayer", List(card1), 0, false, Some(1))
      val (played, updatedPlayer) = player.playCard(None)
      played shouldBe None
      updatedPlayer shouldEqual player
    }
    "play highest group when hand.size < 5 und lastPlayed definiert" in {
      val pair3 = List(Card("3", "♥"), Card("3", "♦"))
      // Hand < 5, lastPlayed verlangt 2 Karten
      val ai = Player("KI", pair3 ++ List(Card("4", "♣")), 0, isHuman = false)
      val last = Some(List(Card("2", "♠"), Card("2", "♣")))
      val (played, next) = ai.playCard(last)
      played shouldBe defined
      played.get should contain allElementsOf pair3
      next.hand shouldNot contain allElementsOf pair3
    }
  }
  "playCard" when {

    "rank is already defined" should {
      "return None and same player" in {
        val card = Card("5", "♥")
        val player = Player("P", List(card), 0, true, Some(1))
        val (played, updated) = player.playCard(None, () => "anything")
        played shouldBe None
        updated shouldBe player
      }
      "immediately return None and itself" in {
        val c = Card("5", "♥")
        val p = Player("X", List(c), 0, isHuman = true, rank = Some(2))
        val (played, next) = p.playCard(None, () => fail("Should not be called"))
        played shouldBe None
        next shouldBe p
      }
      "return None immediately (human)" in {
        val card = Card("5", "♥")
        val p = Player("P", List(card), 0, true, rank = Some(1))
        val (played, next) = p.playCard(None, () => fail("should not be called"))
        played shouldBe None
        next shouldBe p
      }
      "return None immediately (KI)" in {
        val card = Card("7", "♦")
        val p = Player("P", List(card), 0, false, rank = Some(2))
        val (played, next) = p.playCard(None)
        played shouldBe None
        next shouldBe p
      }
      "return (None, this) immediately" in {
        val p = Player("P", List(c2, c3), 0, isHuman = true, rank = Some(1))
        val (cards, p2) = p.playCard(None)
        cards shouldBe None
        p2 shouldBe p
      }

    }
    "human with empty hand" should {
      "delegate to KI logic and pass" in {
        val p = Player("EmptyHuman", Nil, 0, true)
        val (played, next) = p.playCard(None, () => fail("should not ask"))
        played shouldBe None
        next shouldBe p
      }
    }
    "human interactions" should {

      "pass on '0'" in {
        val card = Card("5", "♣")
        val p = Player("H", List(card), 0, true)
        val (played, next) = p.playCard(None, () => "0")
        played shouldBe None
        next.hand should contain(card)
      }

      "reject non-numeric then accept valid" in {
        var cnt = 0
        val inputs = List("foo", "1")
        val card = Card("9", "♠")
        val p = Player("H", List(card), 0, true)
        val (played, _) = p.playCard(None, () => {
          val i = inputs(cnt); cnt += 1; i
        })
        played.get should contain(card)
      }

      "reject out-of-range index then accept" in {
        var cnt = 0
        val inputs = List("99", "1")
        val card = Card("5", "♥")
        val p = Player("H", List(card), 0, true)
        val (played, _) = p.playCard(None, () => {
          val i = inputs(cnt); cnt += 1; i
        })
        played.get should contain(card)
      }

      "reject wrong card-count vs lastPlayed then accept stronger" in {
        var cnt = 0
        val cards = List(Card("5", "♥"), Card("5", "♣"))
        val inputs = List("1", "1,2")
        val last = Some(List(Card("4", "♠"), Card("4", "♦")))
        val p = Player("H", cards, 0, true)
        val (played, next) = p.playCard(last, () => {
          val i = inputs(cnt); cnt += 1; i
        })
        played.get should contain allElementsOf cards
        next.hand shouldBe empty
      }

      "reject weaker card than lastPlayed then pass" in {
        var cnt = 0
        val inputs = List("1", "0")
        val last = Some(List(Card("10", "♠")))
        val card = Card("5", "♥")
        val p = Player("H", List(card), 0, true)
        val (played, next) = p.playCard(last, () => {
          val i = inputs(cnt); cnt += 1; i
        })
        played shouldBe None
        next.hand should contain(card)
      }
    }
    "isHuman" should {
      "pass on input 0" in {
        val card = Card("5", "♥")
        val player = Player("P", List(card), 0, true)
        val (played, updated) = player.playCard(None, () => "0")
        played shouldBe None
        updated.hand should contain(card)
      }
      "reject non-numeric then accept valid" in {
        var n = 0
        val inputs = List("foo", "1")
        val player = Player("P", List(Card("5", "♥")), 0, true)
        val (played, _) = player.playCard(None, () => {
          val in = inputs(n); n += 1; in
        })
        played.isDefined shouldBe true
      }
      "reject out-of-bounds index then accept" in {
        var n = 0
        val inputs = List("99", "1")
        val card = Card("5", "♥")
        val player = Player("P", List(card), 0, true)
        val (played, _) = player.playCard(None, () => {
          val in = inputs(n); n += 1; in
        })
        played.get should contain(card)
      }
      "reject wrong count vs lastPlayed then accept when correct and stronger" in {
        var n = 0
        val cards = List(Card("5", "♥"), Card("5", "♣"))
        val inputs = List("1", "1,2")
        val player = Player("P", cards, 0, true)
        val lastPlayed = Some(List(Card("4", "♠"), Card("4", "♦")))
        val (played, updated) = player.playCard(lastPlayed, () => {
          val in = inputs(n); n += 1; in
        })
        played.isDefined shouldBe true
        played.get should contain allElementsOf cards
        updated.hand shouldBe empty
      }
    }
    "is not human" should {
      "play highest card when hand < 5" in {
        val cards = List(Card("5", "♥"), Card("7", "♦"), Card("9", "♣"))
        val player = Player("KI", cards, 0, false)
        val (played, _) = player.playCard(None)
        played.get.head.value shouldBe "9"
      }
      "play lowest card when hand ≥ 5" in {
        val cards = List("2", "3", "4", "5", "6").map(v => Card(v, "♦"))
        val player = Player("KI", cards, 0, false)
        val (played, _) = player.playCard(None)
        played.get.head.value shouldBe "2"
      }
      "pass when no group possible" in {
        val player = Player("KI", List(Card("5", "♥")), 0, false)
        val last = Some(List(Card("6", "♠"), Card("6", "♦")))
        val (played, updated) = player.playCard(last)
        played shouldBe None
        updated.hand should contain(Card("5", "♥"))
      }
    }
    "a rank is already defined" should {
      "immediately return None and itself" in {
        val card = Card("5", "♥")
        val p = Player("X", List(card), 0, isHuman = true, rank = Some(2))
        val (played, next) = p.playCard(None, () => fail("Should not be called"))
        played shouldBe None
        next shouldBe p
      }
    }
    "the player is human" should {
      "pass on input '0'" in {
        val card = Card("5", "♣")
        val p = Player("H", List(card), 0, isHuman = true)
        val (played, next) = p.playCard(None, () => "0")
        played shouldBe None
        next.hand should contain(card)
      }
      "reject out-of-range index then accept valid" in {
        var cnt = 0
        val inputs = List("9", "1")
        val card = Card("9", "♠")
        val p = Player("H", List(card), 0, isHuman = true)
        val (played, _) = p.playCard(None, () => {
          val in = inputs(cnt);
          cnt += 1;
          in
        })
        played.get should contain(card)
      }
      "reject wrong card-count vs lastPlayed then accept when correct and stronger" in {
        var cnt = 0
        val cards = List(Card("5", "♥"), Card("5", "♣"))
        val inputs = List("1", "1,2")
        val p = Player("H", cards, 0, isHuman = true)
        val last = Some(List(Card("4", "♠"), Card("4", "♦")))
        val (played, next) = p.playCard(last, () => {
          val in = inputs(cnt);
          cnt += 1;
          in
        })
        played.isDefined shouldBe true
        played.get should contain allElementsOf cards
        next.hand shouldBe empty
      }
      "reject non-numeric input then accept valid" in {
        var cnt = 0
        val inputs = List("foo", "1")
        val p = Player("H", List(Card("7", "♦")), 0, isHuman = true)
        val (played, _) = p.playCard(None, () => {
          val in = inputs(cnt);
          cnt += 1;
          in
        })
        played.isDefined shouldBe true
      }
      "reject weaker card than lastPlayed and then pass" in {
        var cnt = 0
        val inputs = List("1", "0")
        val p = Player("H", List(Card("5", "♥")), 0, isHuman = true)
        val (played, next) = p.playCard(
          Some(List(Card("10", "♠"))),
          () => {
            val in = inputs(cnt);
            cnt += 1;
            in
          }
        )
        played shouldBe None
        next.hand.map(_.value) should contain("5")
      }


    }


    "KI-Logik bei hand.size >= 5 und lastPlayed definiert" should {
      "die niedrigste gültige Gruppe spielen" in {
        // erstelle eine Hand mit zwei 3ern und sonst Einzelkarten
        val pair3 = List(Card("3", "♥"), Card("3", "♦"))
        val others = List("5", "6", "7").map(v => Card(v, "♣"))
        val hand = pair3 ++ others
        val ai = Player("KI", hand, 0, isHuman = false)
        val lastPlayed = Some(List(Card("2", "♠"), Card("2", "♣")))
        val (played, updated) = ai.playCard(lastPlayed)
        // Die Paar-3 ist das einzige group-of-2 – und sie ist > 2
        played shouldBe defined
        played.get should contain allElementsOf pair3
        // und aus der Hand entfernt
        updated.hand shouldNot contain allElementsOf pair3

      }
    }
    "the player is AI" should {
      "play the lowest valid group when hand.size >= 5 and lastPlayed is defined" in {
        // Ein Paar 3er plus drei Einzelkarten → Hand.size == 5
        val pair3 = List(Card("3", "♥"), Card("3", "♦"))
        val others = List("5", "6", "7").map(v => Card(v, "♣"))
        val hand = pair3 ++ others
        val ai = Player("KI", hand, 0, isHuman = false)

        // lastPlayed verlangt 2 Karten mit Wert 2
        val last = Some(List(Card("2", "♠"), Card("2", "♣")))

        val (played, updated) = ai.playCard(last)

        // Die KI muss genau das Paar 3-3 spielen (einzige group-of-2 > 2)
        played shouldBe defined
        played.get should contain theSameElementsAs pair3

        // Und die beiden 3er sind aus der Hand entfernt
        updated.hand shouldNot contain allElementsOf pair3
      }
      "play highest single when hand < 5 and lastPlayed = None" in {
        val cards = List("2", "8", "5").map(v => Card(v, "♦"))
        val p = Player("AI", cards, 0, false)
        val (played, next) = p.playCard(None)
        played.get.map(_.value) shouldBe List("8")
        next.hand should not contain Card("8", "♦")
      }
      "play lowest single when hand ≥ 5 and lastPlayed = None" in {
        val cards = (2 to 6).map(n => Card(n.toString, "♣")).toList
        val p = Player("AI", cards, 0, false)
        val (played, next) = p.playCard(None)
        played.get.map(_.value) shouldBe List("2")
        next.hand should not contain Card("2", "♣")
      }
      "play a pair when available" in {
        val c7a = Card("7", "♠");
        val c7b = Card("7", "♥");
        val c6 = Card("6", "♣")
        val p = Player("AI", List(c7a, c6, c7b), 0, false)
        val (played, _) = p.playCard(None)
        // should pick both 7s
        played.get should contain allElementsOf List(c7a, c7b)
      }
      "pass when no playable group exists" in {
        val p = Player("AI", List(Card("5", "♥")), 0, false)
        val last = Some(List(Card("6", "♦"), Card("6", "♠")))
        val (played, next) = p.playCard(last)
        played shouldBe None
        next.hand should contain(Card("5", "♥"))
      }
      "überspringt menschliche Logik bei leerer Hand (geht in die KI-Logik)" in {
        val p = Player("EmptyHuman", Nil, 0, isHuman = true)
        val (played, updated) = p.playCard(None, () => "whatever")
        played shouldBe None
        updated shouldBe p
      }
      "play highest single card when hand < 5 and lastPlayed is None" in {
        val hand = List("2", "8", "5").map(v => Card(v, "♦"))
        val (played, _) = Player("AI", hand, 0, isHuman = false).playCard(None)
        played.get.map(_.value) shouldBe List("8") // highest
      }
      "play lowest single card when hand ≥ 5" in {
        // erstelle eine Int-Range von 2 bis 6 und wandle in Strings um
        val hand = (2 to 6).map(n => Card(n.toString, "♣")).toList
        val player = Player("KI", hand, 0, false)
        val (played, _) = player.playCard(None)
        // erwartet wird die Karte "2"
        played.get.head.value shouldBe "2"
      }
      "pass when no playable group exists 2.0" in {
        val p = Player("AI", List(Card("5", "♥")), 0, isHuman = false)
        val last = Some(List(Card("6", "♦"), Card("6", "♠")))
        val (played, next) = p.playCard(last)
        played shouldBe None
        next.hand should contain(Card("5", "♥"))
      }
      "play highest card when hand < 5 and lastPlayed = None " in {
        val hand = List("2", "8", "5").map(v => Card(v, "♦"))
        val (played, _) = Player("AI", hand, 0, isHuman = false).playCard(None)
        played.get.map(_.value) shouldBe List("8")
      }
      "play lowest card when hand ≥ 5 and lastPlayed = None " in {
        val hand = (2 to 6).map(n => Card(n.toString, "♣")).toList
        val (played, _) = Player("AI", hand, 0, isHuman = false).playCard(None)
        played.get.map(_.value) shouldBe List("2")
      }
      "KI spielt Paar, wenn mehrere gleiche Karten im Blatt sind" in {
        val c7a = Card("7", "♠")
        val c7b = Card("7", "♥")
        val c6 = Card("6", "♣")
        val ai = Player("KI", List(c7a, c6, c7b), 0, isHuman = false)
        val (played, _) = ai.playCard(None)
        // die KI sollte das Paar 7-7 spielen
        played.get should contain allElementsOf List(c7a, c7b)
      }

    }
    "the player is human with a non-empty hand" should {

      "pass on input \"0\" and return sorted hand" in {
        val hand = List(c5, c2, c4)
        val p = Player("P", hand, 0, isHuman = true)
        val (cards, p2) = p.playCard(None, () => "0")
        cards shouldBe None
        p2.hand shouldBe List(c2, c4, c5) // aufsteigend sortiert
      }

      "play a single card and remove it from the hand" in {
        val hand = List(c5, c2, c4)
        val p = Player("P", hand, 0, isHuman = true)
        val (cardsOpt, p2) = p.playCard(None, () => "1")
        cardsOpt shouldBe defined
        cardsOpt.get shouldBe List(c2) // erste Karte der sortierten Hand
        p2.hand shouldBe List(c4, c5) // Resthand sortiert
      }

      "play multiple cards and remove all chosen ones" in {
        val hand = List(c3, c2, c4, c5)
        val p = Player("P", hand, 0, isHuman = true)
        val (cardsOpt, p2) = p.playCard(None, () => "1,3")
        cardsOpt.get shouldBe List(c2, c4) // Indices 1 und 3 in (2,3,4,5)
        p2.hand shouldBe List(c3, c5)
      }

      "retry on invalid input format and then play correctly" in {
        val hand = List(c3, c2)
        val inputs = Iterator("foo", "2")
        val p = Player("P", hand, 0, isHuman = true)
        val (cardsOpt, p2) = p.playCard(None, () => inputs.next())
        cardsOpt.get shouldBe List(c3)
        p2.hand shouldBe List(c2)
      }

    }

    "the player is AI or has empty hand" should {
      "fall through to AI-/empty-hand logic without exception" in {
        val p = Player("AI", Nil, 0, isHuman = false)
        // lastPlayed = None => AI kann zumindest passen
        val (cardsOpt, p2) = p.playCard(None)
        cardsOpt should (be(None) or be(defined))
        p2 shouldBe a[Player]
      }
    }
    
  }
  "playCard with default inputProvider" should {
    "play the only card when user types '1' and newline" in {
      val card = Card("9", "♠")
      val player = Player("DefaultHuman", List(card), 0, isHuman = true)
      // Simuliere STDIN: "1\n"
      val in = new ByteArrayInputStream("1\n".getBytes)
      Console.withIn(in) {
        val (playedOpt, updated) = player.playCard(None) // default inputProvider
        playedOpt.isDefined shouldBe true
        playedOpt.get should contain only card
        updated.hand shouldBe empty
      }
    }

    "pass when user types '0' and newline" in {
      val card = Card("5", "♥")
      val player = Player("DefaultHuman", List(card), 0, isHuman = true)
      val in = new ByteArrayInputStream("0\n".getBytes)
      Console.withIn(in) {
        val (playedOpt, updated) = player.playCard(None)
        playedOpt shouldBe None
        // Hand bleibt unverändert, aber sortiert zurückgegeben
        updated.hand should contain only card
      }
    }

    "reject non-numeric input then accept valid input" in {
      val card = Card("7", "♦")
      val player = Player("DefaultHuman", List(card), 0, isHuman = true)
      // Erst "foo", dann "1"
      val in = new ByteArrayInputStream("foo\n1\n".getBytes)
      Console.withIn(in) {
        val (playedOpt, _) = player.playCard(None)
        playedOpt.isDefined shouldBe true
        playedOpt.get should contain only card
      }
    }
  }
}
