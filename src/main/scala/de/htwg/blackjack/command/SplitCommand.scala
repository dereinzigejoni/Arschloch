package de.htwg.blackjack.command

import de.htwg.blackjack.model.GameState

import scala.util.Try
object SplitCommand extends Command {
  def execute(gs: GameState): Try[GameState] = Try(gs.phase.split(gs))
}