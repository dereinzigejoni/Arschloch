package de.htwg.blackjack.main
import com.google.inject.Guice
import de.htwg.blackjack.GUI.BlackjackGuiApp
import de.htwg.blackjack.controller.IGameController
import de.htwg.blackjack.di.BlackjackModule
import de.htwg.blackjack.view.TuiView
object Main {
  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(new BlackjackModule)
    val controller = injector.getInstance(classOf[IGameController])
    injector.getInstance(classOf[TuiView]) 
    val guiInstance = injector.getInstance(classOf[BlackjackGuiApp])
    controller.addObserver(guiInstance)
    guiInstance.main(args) 

  }
}
