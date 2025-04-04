package htwg.de.Game

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import htwg.de.Card.Card
import htwg.de.Player.Player
import htwg.de.Game.ArschlochGame

class ArschlochGameTest extends AnyWordSpec with Matchers {

  // Hilfsfunktion zum Erzeugen einer Karte mit dem Symbol ♠
  private def card(value: String): Card = Card(value, "♠")

  "getValue" should {
    "return correct numeric values" in {
      ArschlochGame.getValue("2") shouldEqual 2
      ArschlochGame.getValue("10") shouldEqual 10
      ArschlochGame.getValue("J") shouldEqual 11
      ArschlochGame.getValue("Q") shouldEqual 12
      ArschlochGame.getValue("K") shouldEqual 13
      ArschlochGame.getValue("A") shouldEqual 14
    }
  }

  "createDeck" should {
    "generate 52 unique cards" in {
      val deck = ArschlochGame.createDeck()
      deck.size shouldEqual 52
      deck.toSet.size shouldEqual 52
    }
  }

  "shuffleAndDeal" should {
    "deal correct number of cards to each player" in {
      val players = List(
        Player("Player1", List(), 0, isHuman = false),
        Player("Player2", List(), 0, isHuman = false),
        Player("Player3", List(), 0, isHuman = false),
        Player("Player4", List(), 0, isHuman = false)
      )
      val dealtPlayers = ArschlochGame.shuffleAndDeal(players)
      // Bei 4 Spielern sollten 52 / 4 = 13 Karten pro Spieler verteilt werden.
      dealtPlayers.foreach(p => p.hand.size shouldEqual 13)
    }
  }

  "exchangeCards" should {
    "swap 2 best for 2 worst cards" in {
      // Ausgangshände:
      // Arsch: A♠, K♠, 10♠, 9♠ (beste Karten: A♠, K♠)
      // Präsi: 2♠, 3♠, 4♠, 5♠ (schlechteste Karten: 2♠, 3♠)
      val arsch = Player(
        name = "Arsch",
        hand = List(card("A"), card("K"), card("10"), card("9")),
        points = 0,
        isHuman = false
      )
      val praesi = Player(
        name = "Präsi",
        hand = List(card("2"), card("3"), card("4"), card("5")),
        points = 0,
        isHuman = false
      )

      val (newPraesi, newArsch) = ArschlochGame.exchangeCards(praesi, arsch)

      // Erwartet wird, dass Präsi seine ursprünglichen Karten behält und die 2 besten Karten von Arsch erhält.
      val expectedPraesiCards = List("2", "3", "4", "5", "A", "K").map(card)
      // Arsch verliert seine 2 besten Karten und erhält dafür die 2 schlechtesten Karten von Präsi.
      val expectedArschCards = List("10", "9", "2", "3").map(card)

      newPraesi.hand.toSet shouldEqual expectedPraesiCards.toSet
      newArsch.hand.toSet shouldEqual expectedArschCards.toSet
    }
  }

  // TestPlayer zur Simulation deterministischer Züge.
  // Da Player ein case class ist, definieren wir TestPlayer als normale Klasse,
  // die intern den aktuellen Handbestand in einer privaten Variable hält.
  class TestPlayer(
                    name: String,
                    initialHand: List[Card],
                    points: Int,
                    isHuman: Boolean,
                    rank: Option[Int],
                    moves: Iterator[Option[List[Card]]]
                  ) extends Player(name, initialHand, points, isHuman, rank) {
    // Lokale, mutable Variable, um den aktuellen Handbestand zu simulieren.
    private var currentHand: List[Card] = initialHand

    override def playCard(lastPlayed: Option[List[Card]]): (Option[List[Card]], Player) = {
      if (currentHand.isEmpty) (None, this)
      else if (moves.hasNext) {
        moves.next() match {
          case Some(cards) =>
            // Aktualisiere den Handbestand
            currentHand = currentHand.filterNot(c => cards.contains(c))
            // Erzeuge eine neue Instanz über copy, die den aktualisierten Handbestand enthält.
            val updatedPlayer = this.copy(hand = currentHand)
            (Some(cards), updatedPlayer)
          case None =>
            (None, this)
        }
      } else {
        (None, this)
      }
    }
  }

  // AlwaysPassPlayer, die IMMER passt.
  class AlwaysPassPlayer(
                          name: String,
                          initialHand: List[Card],
                          points: Int,
                          isHuman: Boolean,
                          rank: Option[Int]
                        ) extends Player(name, initialHand, points, isHuman, rank) {
    override def playCard(lastPlayed: Option[List[Card]]): (Option[List[Card]], Player) = {
      (None, this)
    }
  }

  "playRound" should {
    "finish and return a valid ranking with 4 players" in {
      // Simuliere, dass jeder Spieler sukzessive eine Karte spielt, bis er leer ist.
      def movesFor(hand: List[Card]): Iterator[Option[List[Card]]] =
        hand.map(c => Some(List(c))).iterator

      val p1 = new TestPlayer("P1", List(card("2"), card("3")), 0, isHuman = false, None, movesFor(List(card("2"), card("3"))))
      val p2 = new TestPlayer("P2", List(card("4"), card("5")), 0, isHuman = false, None, movesFor(List(card("4"), card("5"))))
      val p3 = new TestPlayer("P3", List(card("6"), card("7")), 0, isHuman = false, None, movesFor(List(card("6"), card("7"))))
      val p4 = new TestPlayer("P4", List(card("8"), card("9")), 0, isHuman = false, None, movesFor(List(card("8"), card("9"))))

      val players: List[Player] = List(p1, p2, p3, p4)
      val ranking = ArschlochGame.playRound(players)

      // Überprüfe, dass alle Spieler im Ranking sind und jeder einen gesetzten Rang hat.
      ranking.size shouldEqual 4
      ranking.foreach(p => p.rank.isDefined shouldEqual true)
    }

    "finish correctly with 6 players" in {
      def movesFor(hand: List[Card]): Iterator[Option[List[Card]]] =
        hand.map(c => Some(List(c))).iterator

      val p1 = new TestPlayer("P1", List(card("2"), card("3")), 0, isHuman = false, None, movesFor(List(card("2"), card("3"))))
      val p2 = new TestPlayer("P2", List(card("4"), card("5")), 0, isHuman = false, None, movesFor(List(card("4"), card("5"))))
      val p3 = new TestPlayer("P3", List(card("6"), card("7")), 0, isHuman = false, None, movesFor(List(card("6"), card("7"))))
      val p4 = new TestPlayer("P4", List(card("8"), card("9")), 0, isHuman = false, None, movesFor(List(card("8"), card("9"))))
      val p5 = new TestPlayer("P5", List(card("10"), card("J")), 0, isHuman = false, None, movesFor(List(card("10"), card("J"))))
      val p6 = new TestPlayer("P6", List(card("Q"), card("K")), 0, isHuman = false, None, movesFor(List(card("Q"), card("K"))))

      val players: List[Player] = List(p1, p2, p3, p4, p5, p6)
      val ranking = ArschlochGame.playRound(players)

      // Alle 6 Spieler sollten im Ranking enthalten sein.
      ranking.size shouldEqual 6
      ranking.foreach(p => p.rank.isDefined shouldEqual true)
    }

    "reset the pile when all players always pass" in {
      val p1 = new AlwaysPassPlayer("P1", List(card("2"), card("3")), 0, isHuman = false, None)
      val p2 = new AlwaysPassPlayer("P2", List(card("4"), card("5")), 0, isHuman = false, None)
      val p3 = new AlwaysPassPlayer("P3", List(card("6"), card("7")), 0, isHuman = false, None)
      val p4 = new AlwaysPassPlayer("P4", List(card("8"), card("9")), 0, isHuman = false, None)

      val players: List[Player] = List(p1, p2, p3, p4)
      val ranking = ArschlochGame.playRound(players)

      // Da niemand eine Karte spielt, wird der resetCounter irgendwann >= 50 erreicht und playTurn gibt ein leeres Ranking zurück.
      ranking shouldBe empty
    }
  }
}
