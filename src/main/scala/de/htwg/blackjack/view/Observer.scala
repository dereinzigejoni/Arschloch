package de.htwg.blackjack.view

import de.htwg.blackjack.model.Hand

trait Observer {
  def update(playerHand: Hand, dealerHand: Hand, gameOver: Boolean): Unit
}
