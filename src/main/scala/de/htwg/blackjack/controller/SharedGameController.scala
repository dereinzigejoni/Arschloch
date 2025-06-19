// src/main/scala/de/htwg/blackjack/controller/SharedGameController.scala
package de.htwg.blackjack.controller

import de.htwg.blackjack.controller.IGameController
import de.htwg.blackjack.di.ApplicationContext

/** Einzige Controller-Instanz für GUI und TUI – aus dem DI-Container geholt */
object SharedGameController {
  val instance: IGameController = ApplicationContext.gameController
}
