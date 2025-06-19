// src/main/scala/de/htwg/blackjack/Main.scala
package de.htwg.blackjack

import de.htwg.blackjack.di.Module

object Main {
  def main(args: Array[String]): Unit = {
    val controller = Module.controller
    val tui = Module.tuiView
    val gui = Module.guiView

    controller.addObserver(tui)
    controller.addObserver(gui)

    // Starte GUI
    gui.startApp()
  }
}
