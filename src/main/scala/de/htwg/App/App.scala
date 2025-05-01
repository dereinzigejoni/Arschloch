package de.htwg.App
import de.htwg.Controler.GameControler.GameController
import de.htwg.View.TUI.TUI

object App extends App{
    val ui = new TUI
    val controller = new GameController(ui)
    controller.start()
}
