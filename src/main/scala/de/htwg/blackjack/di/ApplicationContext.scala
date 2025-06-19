// src/main/scala/de/htwg/blackjack/di/ApplicationContext.scala
package de.htwg.blackjack.di

import de.htwg.blackjack.controller.{IGameController, GameController}
import de.htwg.blackjack.strategy.interfacE.DealerStrategy
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.bet.{IBetService, BetService}
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.GUI.{IAnimationService, AnimationService,ICardImageProvider, CardImageProvider}


import scalafx.scene.layout.Pane

/**
 * Application‐wide singleton instances of all core components.  
 * Views and services retrieve their dependencies here.  
 */
object ApplicationContext {

  /** Lieferant für die Dealer-Strategie (kann später leicht ausgetauscht werden). */
  private lazy val dealerStrategy: DealerStrategy =
    new ConservativeDealer()

  /** Der einzige GameController, in den wir die Dealer-Strategie injizieren. */
  lazy val gameController: IGameController =
    new GameController(dealerStrategy)

  /** BetService implementiert alles rund ums Wetten über den Controller. */
  lazy val betService: IBetService =
    new BetService(gameController)

  /** Factory für gemischte Karten-Decks. */
  lazy val deckFactory: IDeckFactory =
    StandardDeckFactory

  /** Einzelner Provider für Karte-Images, wiederverwendbar in allen GUI-Komponenten. */
  private lazy val cardImageProvider: ICardImageProvider =
    new CardImageProvider()
  


  /**
   * Erzeugt einen neuen IAnimationService für das übergebene deckPane.
   * Muss _nach_ dem Erzeugen des Pane in der GUI aufgerufen werden.
   */
  def animationService(deckPane: Pane): IAnimationService =
    new AnimationService(deckPane, cardImageProvider)
}
