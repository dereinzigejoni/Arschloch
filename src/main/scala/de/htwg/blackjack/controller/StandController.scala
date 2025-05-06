package de.htwg.blackjack.controller

class StandCommand(controller: GameController) extends Command {
  def undo(): Unit = controller.resetDealerAndPlayer() // Beispiel
}