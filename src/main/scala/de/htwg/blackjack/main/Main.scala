package de.htwg.blackjack.main

import com.google.inject.Guice
import de.htwg.blackjack.view.TuiView
import de.htwg.blackjack.di.{ApplicationContext, BlackjackModule}
import de.htwg.blackjack.GUI.BlackjackGuiApp
import de.htwg.blackjack.controller.IGameController
import scalafx.application.JFXApp3

object Main {
  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(new BlackjackModule)
    val controller = injector.getInstance(classOf[IGameController])

    // 1) TUI-View injizieren/registrieren
    injector.getInstance(classOf[TuiView]) // TuiView registriert sich selbst

    // 2) GUI-View als Observer registrieren — aber **mit** der Instanz,
    //    nicht mit dem Namen des Typs!
    val guiInstance = injector.getInstance(classOf[BlackjackGuiApp])
    controller.addObserver(guiInstance)

    // 3) GUI starten:  
    //    wir können nicht `launch(classOf[…])` nehmen, weil das
    //    eine neue, ungesehene Instanz erzeugt. Stattdessen rufen
    //    wir die bereits injizierte Instanz direkt an:
    guiInstance.main(args) // oder: guiInstance.startApp()

  }
}
