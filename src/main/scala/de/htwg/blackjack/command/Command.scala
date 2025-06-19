package de.htwg.blackjack.command

import de.htwg.blackjack.model.GameState

import scala.util.Try
trait Command {
  def execute(gs: GameState): Try[GameState]
}