// src/main/scala/de/htwg/blackjack/di/ApplicationContext.scala
package de.htwg.blackjack.di

import de.htwg.blackjack.controller.{GameController, IGameController}
import de.htwg.blackjack.bet.{BetService, IBetService}
import de.htwg.blackjack.GUI.animation.{AnimationService, IAnimationService}
import de.htwg.blackjack.GUI.card.{CardImageProvider, ICardImageProvider}
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
  lazy val animationService:  IAnimationService   =
    new AnimationService(/* deckPane */ null, cardImageProvider)
}
