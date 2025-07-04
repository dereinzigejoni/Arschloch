// src/main/scala/de/htwg/blackjack/main/TuiMain.scala
package de.htwg.blackjack.main

import de.htwg.blackjack.di.ApplicationContext
import de.htwg.blackjack.view.TuiView

object TuiMain {

  /** Einstiegspunkt für die reine TUI (ohne GUI) */
  def main(args: Array[String]): Unit = {
    // 1) Controller per Dependency Injection holen
    val controller = ApplicationContext.gameController

    // 2) TuiView erstellen und als Observer beim Controller registrieren
    val tui = new TuiView(controller)
    controller.addObserver(tui)

    // 3) TUI starten (blockiert bis zum Quit)
    tui.run()
  }
}
