package de.htwg.blackjack.command

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.{GameState, Deck, Hand, Card, Rank, Suits}
import de.htwg.blackjack.state.GamePhases.{PlayerTurn, DealerTurn}
import scala.util.Try

class SplitCommandSpec extends AnyFunSuite {

  // Hilfsmethoden zum Erzeugen von Deck, Hand und Karte
  private def deckOf(cards: List[Card]): Deck =
    Deck(cards) // setzt voraus, dass Deck.apply(List[Card]) existiert

  private def handOf(cards: Card*): Hand =
    cards.foldLeft(Hand.empty)(_ add _)

  private def card(rank: Rank, suit: Suits): Card =
    Card(rank, suit)

  test("split tut nichts, wenn Hand nicht genau 2 Karten hat") {
    val initialHand = handOf(card(Rank.Three, Suits.Clubs)) // nur 1 Karte
    val gs = GameState(
      deck        = deckOf(List(card(Rank.Ace, Suits.Spades), card(Rank.King, Suits.Heart))),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val result = SplitCommand.execute(gs).get
    assert(result == gs)
  }

  test("split tut nichts, wenn Kartenwerte unterschiedlich sind") {
    val initialHand = handOf(card(Rank.Four, Suits.Heart), card(Rank.Five, Suits.Diamonds))
    val gs = GameState(
      deck        = deckOf(List(card(Rank.Two, Suits.Spades), card(Rank.Three, Suits.Clubs))),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(20.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val result = SplitCommand.execute(gs).get
    assert(result == gs)
  }

  test("split tut nichts, wenn Budget < Einsatz") {
    val initialHand = handOf(card(Rank.Six, Suits.Spades), card(Rank.Six, Suits.Heart))
    val gs = GameState(
      deck        = deckOf(List(card(Rank.Four, Suits.Clubs), card(Rank.Five, Suits.Diamonds))),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(50.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 30.0,   // Budget kleiner als Einsatz
      currentBet  = 0.0
    )

    val result = SplitCommand.execute(gs).get
    assert(result == gs)
  }

  test("split tut nichts, wenn Deck weniger als 2 Karten enthält") {
    val initialHand = handOf(card(Rank.Seven, Suits.Clubs), card(Rank.Seven, Suits.Diamonds))
    val gs = GameState(
      deck        = deckOf(List(card(Rank.Ten, Suits.Spades))), // nur 1 Karte
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val result = SplitCommand.execute(gs).get
    assert(result == gs)
  }

  test("split teilt Hand, verdoppelt Einsatz, zieht 2 Karten und passt Budget an") {
    val c1 = card(Rank.Eight, Suits.Heart)
    val c2 = card(Rank.Eight, Suits.Spades)
    val drawA = card(Rank.Ten, Suits.Clubs)
    val drawB = card(Rank.King, Suits.Diamonds)
    val initialHand = handOf(c1, c2)
    val gs = GameState(
      deck        = deckOf(List(drawA, drawB)),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(15.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val updated = SplitCommand.execute(gs).get

    // Aus zwei Hälften werden zwei neue Hände mit jeweilser ursprünglicher Karte + gezogener Karte
    val newHands = updated.playerHands
    assert(newHands.size == 2)
    assert(newHands.head.cards == List(c1, drawA))
    assert(newHands(1).cards == List(c2, drawB))

    // Einsätze-Liste wurde gepatcht: [bet, bet]
    assert(updated.bets == List(15.0, 15.0))

    // Budget um einen Einsatz verringert
    assert(updated.budget == 85.0)

    // Deck wurde um genau 2 Karten verkleinert
    assert(updated.deck.cards.isEmpty)

    // activeHand und phase bleiben unverändert
    assert(updated.activeHand == 0)
    assert(updated.phase == PlayerTurn)
  }

  test("split tut nichts, wenn Phase nicht PlayerTurn ist (z.B. DealerTurn)") {
    val initialHand = handOf(card(Rank.Nine, Suits.Heart), card(Rank.Nine, Suits.Clubs))
    val gs = GameState(
      deck        = deckOf(List(card(Rank.Two, Suits.Diamonds), card(Rank.Three, Suits.Spades))),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(20.0),
      activeHand  = 0,
      phase       = DealerTurn, // falsche Phase
      budget      = 100.0,
      currentBet  = 0.0
    )

    val result = SplitCommand.execute(gs).get
    assert(result == gs)
  }
}
