package de.htwg.blackjack.factory
import de.htwg.blackjack.model.{Card, Deck}

import scala.util.Random
trait DeckFactory {
  def newDeck: Deck

  def shuffled(cards: List[Card]): Deck =
    Deck(Random.shuffle(cards))
}