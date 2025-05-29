package de.htwg.blackjack

import de.htwg.blackjack.gui.BlackjackGuiApp
import de.htwg.blackjack.view.TuiView

object Main {
  def main(args: Array[String]): Unit = {
    // TUI parallel starten
    new Thread(() => TuiView.run(), "TUI-Thread").start()
    // GUI-App starten
    BlackjackGuiApp.main(args)
  }
}
