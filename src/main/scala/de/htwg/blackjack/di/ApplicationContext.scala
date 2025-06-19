// src/main/scala/de/htwg/blackjack/di/ApplicationContext.scala
package de.htwg.blackjack.di

import de.htwg.blackjack.GUI.*
import scalafx.scene.layout.Pane

object ApplicationContext {
  val gameController = Module.controller
  val betService     = Module.betService
  val deckFactory    = Module.deckFactory

  private val cardImageProvider = new CardImageProvider

  def animationService(deckPane: Pane): IAnimationService =
    new AnimationService(deckPane, cardImageProvider)
}
