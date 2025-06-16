package de.htwg.blackjack.main
import de.htwg.blackjack.GUI.BlackjackGuiApp
import de.htwg.blackjack.controller.SharedGameController
import de.htwg.blackjack.util.ObservableSync
import de.htwg.blackjack.view.TuiView

// in Main.scala
object Main {
  def main(args: Array[String]]): Unit = {
    val controller = SharedGameController.instance
    controller.addObserver(new TuiView)            // <-- TUI-View als Observer
    controller.addObserver(BlackjackGuiApp)        // <-- GUI-View als Observer
    BlackjackGuiApp.main(args)
  }
}

