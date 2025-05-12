package de.htwg.blackjack.strategy.interfacE

import de.htwg.blackjack.model.{Deck, Hand, Status}

trait DealerStrategy {
  def play(deck:Deck,dealerHand: Hand): (Deck,Hand,Status)
}
