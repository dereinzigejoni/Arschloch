// src/main/scala/de/htwg/blackjack/bet/BetService.scala
package de.htwg.blackjack.bet

import de.htwg.blackjack.controller.IGameController
import scala.util.Try

class BetService(controller: IGameController) extends IBetService {
  override def placeBet(amount: Double): Try[Unit] = controller.tryplaceBet(amount)
  override def clearBet(): Unit                             = () // GUI setzt currentBet selber zurück
  override def currentBet: Double                           = controller.getState.currentBet
  override def budget: Double                               = controller.getBudget
}
