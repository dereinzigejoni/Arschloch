package de.htwg.blackjack.model

import scala.util.Random

class Deck {
  private var cards: List[Card] =
    for {
      suit <- List(Suit.Hearts, Suit.Diamonds, Suit.Clubs, Suit.Spades)
      rank <- List(Rank.Two, Rank.Three, Rank.Four, Rank.Five, Rank.Six,
        Rank.Seven, Rank.Eight, Rank.Nine, Rank.Ten,
        Rank.Jack, Rank.Queen, Rank.King, Rank.Ace)
    } yield Card(rank, suit)

  def shuffle(): Unit =
    cards = Random.shuffle(cards)

  def draw(): Option[Card] = cards match {
    case h :: t => cards = t; Some(h)
    case Nil    => None
  }
}

