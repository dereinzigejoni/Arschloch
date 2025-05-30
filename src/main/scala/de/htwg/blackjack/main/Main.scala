package de.htwg.blackjack.main
import de.htwg.blackjack.GUI.BlackjackGuiApp
import de.htwg.blackjack.controller.SharedGameController
import de.htwg.blackjack.util.ObservableSync
import de.htwg.blackjack.view.TuiView

object Main {
  def main(args: Array[String]): Unit = {
    val controller = SharedGameController.instance
    val sync = new ObservableSync()
    val tui = new TuiView(controller, sync)
    new Thread(new Runnable {
      override def run(): Unit = tui.run()
    }, "TUI-Thread").start()
    BlackjackGuiApp.main(args)
  }
}
