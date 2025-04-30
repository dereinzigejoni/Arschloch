package de.htwg.TUI

import de.htwg.game.ArschlochGame
import de.htwg.Player.Player

object TUI {
  def main(args: Array[String]): Unit = {
    val players: Array[Player] = ArschlochGame.askForPlayers()
    ArschlochGame.mainGameLoop(players)
  }
}
