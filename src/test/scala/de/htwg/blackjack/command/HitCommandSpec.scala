package de.htwg.blackjack.command

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.{GameState, Deck, Hand, Card, Rank, Suits}
import de.htwg.blackjack.state.GamePhases.{PlayerTurn, DealerTurn}
import scala.util.Try

class HitCommandSpec extends AnyFunSuite {

  // Hilfsmethoden zum Erzeugen von Deck, Hand und Karte
  private def deckOf(cards: List[Card]): Deck =
    Deck(cards) // setzt voraus, dass Deck.apply(List[Card]) existiert

  private def handOf(cards: Card*): Hand =
    cards.foldLeft(Hand.empty)(_ add _)

  private def card(rank: Rank, suit: Suits): Card =
    Card(rank, suit)

  test("hit fügt Karte hinzu und bleibt in PlayerTurn, wenn Hand nicht bustet") {
    // Hand-Wert vorher = 5 + 5 = 10, nach Ziehen einer 6 = 16 -> nicht bust
    val drawCard = card(Rank.Six, Suits.Spades)
    val initialHand = handOf(card(Rank.Five, Suits.Heart), card(Rank.Five, Suits.Diamonds))
    val gs = GameState(
      deck        = deckOf(List(drawCard)),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val updated = HitCommand.execute(gs).get

    // Deck wurde um eine Karte verkleinert
    assert(updated.deck.cards.isEmpty)
    // In Hand ist jetzt die gezogene Karte
    assert(updated.playerHands.head.cards.contains(drawCard))
    // Phase bleibt PlayerTurn, weil nicht bust
    assert(updated.phase == PlayerTurn)
    // activeHand bleibt unverändert
    assert(updated.activeHand == 0)
  }

  test("hit fügt Karte hinzu und wechselt in DealerTurn, wenn Hand bustet") {
    // Hand-Wert vorher = 10 + 10 = 20, nach Ziehen einer 6 = 26 -> bust
    val drawCard = card(Rank.Six, Suits.Clubs)
    val initialHand = handOf(card(Rank.Ten, Suits.Spades), card(Rank.Ten, Suits.Heart))
    val gs = GameState(
      deck        = deckOf(List(drawCard)),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(20.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val updated = HitCommand.execute(gs).get

    // Karte gezogen und in Hand eingefügt
    assert(updated.playerHands.head.cards.contains(drawCard))
    // Phase wechselt zu DealerTurn, weil bust
    assert(updated.phase == DealerTurn)
    // activeHand bleibt bei 0
    assert(updated.activeHand == 0)
  }

  test("hit tut nichts, wenn Phase nicht PlayerTurn ist (z.B. DealerTurn)") {
    val drawCard = card(Rank.Three, Suits.Diamonds)
    val initialHand = handOf(card(Rank.Five, Suits.Clubs))
    val gs = GameState(
      deck        = deckOf(List(drawCard)),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(5.0),
      activeHand  = 0,
      phase       = DealerTurn, // andere Phase
      budget      = 50.0,
      currentBet  = 0.0
    )

    val result = HitCommand.execute(gs).get
    // Zustand unverändert
    assert(result == gs)
  }
}
