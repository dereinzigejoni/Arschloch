package de.htwg.blackjack.di

import com.google.inject.{Guice, Injector, Key}
import com.google.inject.name.Names
import de.htwg.blackjack.GUI.{AnimationService, CardImageProvider, IAnimationService, ICardImageProvider}
import de.htwg.blackjack.controller.{GameController, IGameController}
import de.htwg.blackjack.bet.{BetService, IBetService}
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.strategy.interfacE.DealerStrategy
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.io.IFileIO
import scalafx.scene.layout.Pane

object ApplicationContext {
  private val injector: Injector =
    Guice.createInjector(new BlackjackModule)
  private lazy val dealerStrategy: DealerStrategy = new ConservativeDealer()
  lazy val gameController: IGameController = new GameController(dealerStrategy)
  lazy val betService: IBetService = new BetService(gameController)
  lazy val deckFactory: IDeckFactory = StandardDeckFactory

  // 3) Hole Dir IFileIO per Named‐Binding:
  //    - XML‐Variante:
  def xmlFileIO: IFileIO =
    injector.getInstance(
      Key.get(classOf[IFileIO], Names.named("xml"))
    )

  //    - JSON‐Variante:
  def jsonFileIO: IFileIO =
    injector.getInstance(
      Key.get(classOf[IFileIO], Names.named("json"))
    )

  //    - Default (unannotated) – im Modul auf XML gesetzt:
  def fileIO: IFileIO =
    injector.getInstance(classOf[IFileIO])
  private lazy val cardImageProvider: ICardImageProvider = new CardImageProvider()
  def animationService(deckPane: Pane): IAnimationService = new AnimationService(deckPane, cardImageProvider)
}
