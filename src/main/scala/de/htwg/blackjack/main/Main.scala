package de.htwg.blackjack.main

import de.htwg.blackjack.GUI.BlackjackGuiApp
import de.htwg.blackjack.view.TuiView
import javafx.application.Application

object Main {
  def main(args: Array[String]): Unit = {
    // start the TUI in parallel
    new Thread(() => TuiView.run(), "TUI-Thread").start()
    // launch the ScalaFX GUI
    BlackjackGuiApp.main(args)
  }
}
