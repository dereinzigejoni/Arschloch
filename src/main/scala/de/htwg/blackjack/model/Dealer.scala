package de.htwg.blackjack.model

trait DealerStrategy {
  def shouldHit(hand: Hand): Boolean
}
object DealerStrategy {
  object Default extends DealerStrategy {
    def shouldHit(hand: Hand): Boolean = hand.bestValue < 17
  }
}

class Dealer(strategy: DealerStrategy = DealerStrategy.Default) {
  private var hand: Hand = Hand()

  def addCard(c: Card): Unit   = hand = hand.add(c)
  def resetHand(): Unit        = hand = Hand()
  def getHand: Hand            = hand
  def play(deck: Deck): Unit   =
    while (strategy.shouldHit(hand)) deck.draw().foreach(addCard)
}
