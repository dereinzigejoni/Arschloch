package de.htwg.blackjack.model

import org.scalatest.funsuite.AnyFunSuite

class HandSpec extends AnyFunSuite {

  // Hilfsmethode zum Erzeugen einer Karte
  private def c(rank: Rank, suit: Suits): Card = Card(rank, suit)

  test("empty hand has no cards, value 0 and isBust false") {
    val h = Hand.empty
    assert(h.cards.isEmpty, "Hand.empty.cards should be empty")
    assert(h.value == 0, "Empty hand value should be 0")
    assert(!h.isBust, "Empty hand must not be bust")
  }

  test("add appends a card to the hand") {
    val h1 = Hand.empty
    val card1 = c(Rank.Five, Suits.Heart)
    val h2 = h1.add(card1)
    assert(h2.cards == List(card1), "add should append the card to the cards list")
  }

  test("value of a single Ace is counted as 1") {
    val h = Hand(List(c(Rank.Ace, Suits.Spades))) // size != 2 → Ace → 1
    assert(h.value == 1, "Single Ace should count as 1")
    assert(!h.isBust, "Single Ace (value 1) should not be bust")
  }

  test("value of a single non-Ace card equals its rank value") {
    val h = Hand(List(c(Rank.Ten, Suits.Clubs)))
    assert(h.value == 10, "Single Ten should count as 10")
    assert(!h.isBust, "Value 10 should not be bust")
  }

  test("two-card hand sums values directly (Ace as 11)") {
    val h = Hand(List(c(Rank.Ace, Suits.Heart), c(Rank.Nine, Suits.Diamonds)))
    // size == 2 → sum rank.value: 11 + 9 = 20
    assert(h.value == 20, "Two-card Ace+Nine should count as 20")
    assert(!h.isBust, "Value 20 should not be bust")
  }

  test("two-card hand with two Aces sums to 22 and is bust") {
    val h = Hand(List(c(Rank.Ace, Suits.Clubs), c(Rank.Ace, Suits.Spades)))
    // size == 2 → sum 11 + 11 = 22
    assert(h.value == 22, "Two Aces in a two-card hand should count as 22")
    assert(h.isBust, "Value 22 should be bust")
  }

  test("multi-card hand treats each Ace as 1") {
    val h = Hand(List(
      c(Rank.Ace, Suits.Heart),
      c(Rank.Five, Suits.Spades),
      c(Rank.Six, Suits.Clubs)
    )) // size == 3 → Ace counted as 1
    assert(h.value == 1 + 5 + 6, s"Expected value 12 but was ${h.value}")
    assert(!h.isBust, "Value 12 should not be bust")
  }

  test("multi-card hand without Aces sums rank values") {
    val h = Hand(List(
      c(Rank.Seven, Suits.Diamonds),
      c(Rank.Eight, Suits.Heart),
      c(Rank.Five, Suits.Clubs)
    ))
    assert(h.value == 7 + 8 + 5, s"Expected value 20 but was ${h.value}")
    assert(!h.isBust, "Value 20 should not be bust")
  }

  test("isBust true for multi-card hand exceeding 21") {
    val h = Hand(List(
      c(Rank.Ten, Suits.Spades),
      c(Rank.Ten, Suits.Heart),
      c(Rank.Two, Suits.Diamonds)
    ))
    assert(h.value == 22, s"Expected value 22 but was ${h.value}")
    assert(h.isBust, "Value 22 should be bust")
  }
}
