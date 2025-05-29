package de.htwg.blackjack.bet

import scala.util.Try

/** Bet-Logik isoliert vom GUI */
trait IBetService {
  def placeBet(amount: Double): Try[Unit]
  def clearBet(): Unit
  def currentBet: Double
  def budget: Double
}
