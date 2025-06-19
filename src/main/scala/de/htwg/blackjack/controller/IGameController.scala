package de.htwg.blackjack.controller

import de.htwg.blackjack.model.GameState

import scala.util.Try

trait IGameController {
  def getState: GameState
  def getBudget: Double
  def tryplaceBet(amount: Double): Try[Unit]
  def playerHit(): Try[GameState]
  def playerStand(): Try[GameState]
  def playerDouble(): Try[GameState]
  def playerSplit(): Try[GameState]
  def dealerHit(): Try[GameState]
  def resolveBet(): Unit
  def addObserver(obs: GameObserver): Unit
  def removeObserver(obs: GameObserver): Unit
  def undo(): Option[GameState]
  def redo(): Option[GameState]
  def getLastRoundWin: Double

}
