package de.htwg.blackjack.strategy

import de.htwg.blackjack.model.{DealerBust, Deck, Finished, Hand, Status}
import de.htwg.blackjack.strategy.interfacE.DealerStrategy

import scala.annotation.tailrec

class ConservativeDealer extends DealerStrategy {
  @tailrec
  private def drawTill17(deck: Deck, hand: Hand): (Deck, Hand) = {
    if (hand.value >= 17) (deck, hand)
    else {
      val (c, d2) = deck.draw()
      drawTill17(d2, hand.add(c))
    }
  }

  def play(deck: Deck, dealerHand: Hand): (Deck, Hand, Status) = {
    val (d2, finalHand) = drawTill17(deck, dealerHand)
    val st = if (finalHand.isBust) DealerBust else Finished
    (d2, finalHand, st)
  }
}





