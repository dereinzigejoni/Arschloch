package de.htwg.blackjack.main
import de.htwg.blackjack.GUI.BlackjackGuiApp
import de.htwg.blackjack.view.TuiView

object Main {
  def main(args: Array[String]): Unit = {
    // TUI parallel starten
    new Thread(() => TuiView.run(), "TUI-Thread").start()
    // GUI-App starten
    BlackjackGuiApp.main(args)
  }
}
