package de.htwg.TUI
import de.htwg.Player.ArschlochGame
object TUI {
  def main(args: Array[String]): Unit = {
    val players = ArschlochGame.askForPlayers()
    ArschlochGame.mainGameLoop(players)
  }
}