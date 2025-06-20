package de.htwg.blackjack.command

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.{GameState, Deck, Hand, Card, Rank, Suits}
import de.htwg.blackjack.state.GamePhases.{PlayerTurn, DealerTurn, DealerBustPhase, FinishedPhase, PlayerBustPhase}
import scala.util.Try

class StandCommandSpec extends AnyFunSuite {

  // Hilfsmethoden zum Erzeugen eines Decks, einer Hand und einer Karte
  private def deckOf(cards: List[Card]): Deck =
    Deck(cards)

  private def handOf(cards: Card*): Hand =
    cards.foldLeft(Hand.empty)(_ add _)

  private def card(rank: Rank, suit: Suits): Card =
    Card(rank, suit)

  test("stand bewegt activeHand auf die nächste Hand, wenn noch weitere Hände existieren") {
    val hand1 = handOf(card(Rank.Five, Suits.Clubs))
    val hand2 = handOf(card(Rank.Six, Suits.Heart))
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(hand1, hand2),
      dealer      = Hand.empty,
      bets        = List(10.0, 10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val updated = StandCommand.execute(gs).get

    // activeHand wird um 1 erhöht, Phase bleibt PlayerTurn :contentReference[oaicite:0]{index=0}
    assert(updated.activeHand == 1)
    assert(updated.phase == PlayerTurn)
  }

  test("stand wechselt in DealerTurn, wenn es die letzte Hand war") {
    val hand = handOf(card(Rank.Five, Suits.Clubs))
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(hand),
      dealer      = Hand.empty,
      bets        = List(20.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 50.0,
      currentBet  = 0.0
    )

    val updated = StandCommand.execute(gs).get

    // da keine weitere Hand existiert, Phase → DealerTurn :contentReference[oaicite:1]{index=1}
    assert(updated.phase == DealerTurn)
    assert(updated.activeHand == 0)
  }

  test("stand in DealerTurn zieht keine Karte, wenn Dealer-Wert ≥ 17, und wechselt zu FinishedPhase") {
    val dealerHand = handOf(card(Rank.Ten, Suits.Spades), card(Rank.Seven, Suits.Diamonds)) // Wert = 17
    val deckCards  = List(card(Rank.Two, Suits.Clubs))
    val gs = GameState(
      deck        = deckOf(deckCards),
      playerHands = List(handOf(card(Rank.Five, Suits.Heart))),
      dealer      = dealerHand,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = DealerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val updated = StandCommand.execute(gs).get

    // keine Karte gezogen, Deck unverändert, Phase → FinishedPhase :contentReference[oaicite:2]{index=2}
    assert(updated.dealer    == dealerHand)
    assert(updated.deck.cards == deckCards)
    assert(updated.phase      == FinishedPhase)
  }

  test("stand in DealerTurn zieht bis zum Bust und wechselt zu DealerBustPhase") {
    val dealerHand = handOf(card(Rank.Ten, Suits.Spades), card(Rank.Five, Suits.Diamonds)) // Wert = 15
    val draw1      = card(Rank.Seven, Suits.Clubs)   // 15 + 7 = 22 → Bust
    val draw2      = card(Rank.Three, Suits.Heart)  // bleibt im Deck
    val gs = GameState(
      deck        = deckOf(List(draw1, draw2)),
      playerHands = List(handOf(card(Rank.Five, Suits.Heart))),
      dealer      = dealerHand,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = DealerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val updated = StandCommand.execute(gs).get

    // eine Karte gezogen, Hand bustet, Deck enthält nur noch die zweite Karte
    assert(updated.dealer.cards == (dealerHand.cards :+ draw1))
    assert(updated.deck.cards  == List(draw2))
    assert(updated.phase       == DealerBustPhase)
  }

  test("stand tut nichts, wenn Phase nicht PlayerTurn oder DealerTurn ist") {
    val hand = handOf(card(Rank.Seven, Suits.Spades))
    val gs = GameState(
      deck        = deckOf(List(card(Rank.Two, Suits.Heart))),
      playerHands = List(hand),
      dealer      = Hand.empty,
      bets        = List(5.0),
      activeHand  = 0,
      phase       = PlayerBustPhase,
      budget      = 50.0,
      currentBet  = 0.0
    )

    val result = StandCommand.execute(gs).get

    // in PlayerBustPhase bleibt der Zustand unverändert :contentReference[oaicite:3]{index=3}
    assert(result == gs)
  }
}
