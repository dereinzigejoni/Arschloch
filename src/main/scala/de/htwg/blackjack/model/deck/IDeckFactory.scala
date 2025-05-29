package de.htwg.blackjack.model.deck

import de.htwg.blackjack.model.Deck

trait IDeckFactory {
  def newDeck: Deck
}
