package de.htwg.blackjack.main

import com.google.inject.Guice
import com.google.inject.name.Names
import de.htwg.blackjack.GUI.BlackjackGuiApp
import de.htwg.blackjack.controller.IGameController
import de.htwg.blackjack.di.BlackjackModule
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.io.IFileIO
import de.htwg.blackjack.model.{GameState, Hand}
import de.htwg.blackjack.state.GamePhases
import de.htwg.blackjack.view.TuiView

import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(new BlackjackModule)
    val controller = injector.getInstance(classOf[IGameController])
    injector.getInstance(classOf[TuiView])
    // 3) Beispiel-GameState erzeugen
    val deck0       = StandardDeckFactory.newDeck
    val (p1, deck1) = deck0.draw()
    val (p2, deck2) = deck1.draw()
    val (d1, deck3) = deck2.draw()
    val (d2, deck4) = deck3.draw()
    val playerHand  = Hand.empty.add(p1).add(p2)
    val dealerHand  = Hand.empty.add(d1).add(d2)
    val sampleState = GameState(
      deck        = deck4,
      playerHands = List(playerHand),
      dealer      = dealerHand,
      bets        = List(50.0),
      activeHand  = 0,
      phase       = GamePhases.FinishedPhase,
      budget      = 1000.0,
      currentBet  = 50.0
    )

    // 4) XML- und JSON-Implementierungen holen
    val xmlKey  = com.google.inject.Key.get(classOf[IFileIO], Names.named("xml"))
    val jsonKey = com.google.inject.Key.get(classOf[IFileIO], Names.named("json"))
    val xmlIo   = injector.getInstance(xmlKey)
    val jsonIo  = injector.getInstance(jsonKey)

    // 5) Speichern als Beispiel-Files
    xmlIo.save(sampleState, "example-game-state.xml") match {
      case Success(_) => println("Beispiel XML gespeichert: example-game-state.xml")
      case Failure(ex) => println(s"XML speichern fehlgeschlagen: ${ex.getMessage}")
    }
    jsonIo.save(sampleState, "example-game-state.json") match {
      case Success(_) => println("Beispiel JSON gespeichert: example-game-state.json")
      case Failure(ex) => println(s"JSON speichern fehlgeschlagen: ${ex.getMessage}")
    }
    
    val guiInstance = injector.getInstance(classOf[BlackjackGuiApp])
    
    controller.addObserver(guiInstance)
    guiInstance.main(args)

  }
}





