package htwg.de.TUI

import htwg.de.Game.ArschlochGame

object TUI {
  def main(args: Array[String]): Unit = {
    val players = ArschlochGame.askForPlayers()
    ArschlochGame.mainGameLoop(players)
  }
}