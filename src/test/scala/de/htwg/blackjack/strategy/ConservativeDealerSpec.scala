package de.htwg.blackjack.strategy

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.model.{Deck, Hand, Card, Rank, Suits, DealerBust, Finished}

class ConservativeDealerSpec extends AnyFunSuite {

  // Helper to build cards, hands, and decks
  private def card(rank: Rank, suit: Suits): Card = Card(rank, suit)
  private def handOf(cards: Card*): Hand = cards.foldLeft(Hand.empty)(_ add _)
  private def deckOf(cards: List[Card]): Deck = Deck(cards)

  private val dealer = new ConservativeDealer

  test("play returns same deck and hand, status Finished, when hand value ≥ 17 and not bust") {
    val initialHand = handOf(card(Rank.Ace, Suits.Spades), card(Rank.Six, Suits.Heart)) // 11 + 6 = 17
    val initialDeck = deckOf(List(card(Rank.Two, Suits.Clubs)))
    val (outDeck, outHand, status) = dealer.play(initialDeck, initialHand)

    // No drawing occurred
    assert(outDeck eq initialDeck)
    assert(outHand eq initialHand)
    assert(status == Finished)
  }

  test("play draws cards until reaching at least 17, returns remaining deck and status Finished") {
    val initialHand = handOf(card(Rank.Ten, Suits.Clubs)) // 10
    val c1 = card(Rank.Three, Suits.Heart)  // 10 + 3 = 13
    val c2 = card(Rank.Four, Suits.Spades)   // 13 + 4 = 17
    val rest = card(Rank.Five, Suits.Diamonds)
    val initialDeck = deckOf(List(c1, c2, rest))

    val (outDeck, outHand, status) = dealer.play(initialDeck, initialHand)

    // After drawing c1 and c2, rest should remain
    assert(outDeck.cards == List(rest))
    // Hand should include the two drawn cards
    assert(outHand.cards == List(card(Rank.Ten, Suits.Clubs), c1, c2))
    assert(status == Finished)
  }

  test("play draws until bust and returns status DealerBust") {
    val initialHand = handOf(card(Rank.King, Suits.Diamonds), card(Rank.Five, Suits.Clubs)) // 10 + 5 = 15
    val bustCard = card(Rank.Queen, Suits.Spades)  // 15 + 10 = 25 → bust
    val leftover = card(Rank.Two, Suits.Heart)
    val initialDeck = deckOf(List(bustCard, leftover))

    val (outDeck, outHand, status) = dealer.play(initialDeck, initialHand)

    // After drawing one card, rest should be leftover
    assert(outDeck.cards == List(leftover))
    // Hand should contain the bust card
    assert(outHand.cards.last == bustCard)
    assert(status == DealerBust)
  }

  test("play on already-bust hand returns status DealerBust without drawing") {
    val bustedHand = handOf(
      card(Rank.King, Suits.Heart),
      card(Rank.Queen, Suits.Diamonds),
      card(Rank.Two, Suits.Clubs)
    ) // 10 + 10 + 2 = 22 → already bust
    val initialDeck = deckOf(List(card(Rank.Ace, Suits.Spades)))
    val (outDeck, outHand, status) = dealer.play(initialDeck, bustedHand)

    // No drawing occurs
    assert(outDeck eq initialDeck)
    assert(outHand eq bustedHand)
    assert(status == DealerBust)
  }
}
