
package de.htwg.Model.Player

import de.htwg.Model.Card.Card




case class Player(
                   name: String,
                   hand: List[Card],
                   rank: Option[Int] = None,
                   isHuman: Boolean = true
                 ) {
  def removeCards(cards: List[Card]): Player =
    copy(hand = hand.diff(cards))

  def hasCards: Boolean = hand.nonEmpty
}

