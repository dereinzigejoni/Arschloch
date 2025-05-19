// src/main/scala/de/htwg/blackjack/Main.scala
package de.htwg.blackjack

import de.htwg.blackjack.gui.BlackjackGuiApp
import de.htwg.blackjack.view.TuiView

object Main {
  def main(args: Array[String]): Unit = {
    // TUI im Hintergrund starten
    new Thread(() => TuiView.run(), "TUI-Thread").start()
    // GUI starten (blockiert erst, zeigt Fenster)
    BlackjackGuiApp.main(args)
  }
}
