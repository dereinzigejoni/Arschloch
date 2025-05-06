package de.htwg.blackjack.controller
import de.htwg.blackjack.model.Card

class HitCommand(controller: GameController, card: Card) extends Command {
  def undo(): Unit = controller.removeLastPlayerCard()
}
