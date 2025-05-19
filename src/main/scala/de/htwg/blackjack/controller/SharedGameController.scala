package de.htwg.blackjack.controller

object SharedGameController {
  /** Einzige Controller-Instanz für GUI und TUI */
  val instance: GameController = new GameController()
}
