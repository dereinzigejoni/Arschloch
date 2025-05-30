// src/main/scala/de/htwg/blackjack/di/ApplicationContext.scala
package de.htwg.blackjack.di

import de.htwg.blackjack.GUI.{CardImageProvider, IAnimationService, ICardImageProvider}
import de.htwg.blackjack.controller.{GameController, IGameController}
import de.htwg.blackjack.bet.{BetService, IBetService}
import de.htwg.blackjack.GUI.AnimationService
import scalafx.scene.layout.Pane

import scala.language.postfixOps
// **Richtiges** Interface importieren:
import de.htwg.blackjack.model.deck.IDeckFactory
// und Deine Factory
import de.htwg.blackjack.factory.StandardDeckFactory

object ApplicationContext {
  lazy val gameController: IGameController      = new GameController()
  lazy val betService:    IBetService            = new BetService(gameController)

  // Hier das Interface, wie es bei Dir deklariert ist:
  lazy val deckFactory:   IDeckFactory           = StandardDeckFactory

  lazy val cardImageProvider: ICardImageProvider = new CardImageProvider()
  def animationService(deckPane: Pane): IAnimationService = new AnimationService(deckPane, cardImageProvider)
}
