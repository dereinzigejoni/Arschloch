package de.htwg.blackjack.model
import scala.util.{Try,Success,Failure}

class Player(var budget: Int) {
  private var hand: Hand = Hand()
  private var currentBet: Option[Bet] = None

  def placeBet(amount: Int): Try[Unit] = Try {
    require(amount > 0, "Bet must be positive")
    require(amount <= budget / 2, s"Max bet is half your budget (${budget/2})")
    currentBet = Some(Bet(amount))
    budget -= amount
  }

  def getBet: Option[Bet] = currentBet

  def win(isBJ: Boolean): Unit = currentBet.foreach { b =>
    val mult = if (isBJ) 2.5 else 2.0
    budget += (b.amount * mult).toInt
    currentBet = None
  }

  def lose(): Unit = currentBet.foreach(_ => currentBet = None)

  def addCard(c: Card): Unit = hand = hand.add(c)
  def resetHand(): Unit    = hand = Hand()
  def getHand: Hand        = hand
}
