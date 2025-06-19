// src/main/scala/de/htwg/blackjack/di/ApplicationContext.scala
package de.htwg.blackjack.di

import de.htwg.blackjack.GUI.*

object ApplicationContext {
  val gameController: Any = Modules.controller
  val betService     = Modules.betService
  val deckFactory    = Modules.deckFactory

  private val cardImageProvider = new CardImageProvider

  def animationService(deckPane: Pane): IAnimationService =
    new AnimationService(deckPane, cardImageProvider)
}
