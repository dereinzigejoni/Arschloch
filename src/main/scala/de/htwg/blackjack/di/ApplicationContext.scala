package de.htwg.blackjack.di
import de.htwg.blackjack.controller.{IGameController, GameController}
import de.htwg.blackjack.strategy.interfacE.DealerStrategy
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.bet.{IBetService, BetService}
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.GUI.{IAnimationService, AnimationService,ICardImageProvider, CardImageProvider}
import scalafx.scene.layout.Pane
object ApplicationContext {
  private lazy val dealerStrategy: DealerStrategy = new ConservativeDealer()
  lazy val gameController: IGameController = new GameController(dealerStrategy)
  lazy val betService: IBetService = new BetService(gameController)
  lazy val deckFactory: IDeckFactory = StandardDeckFactory
  private lazy val cardImageProvider: ICardImageProvider = new CardImageProvider()
  def animationService(deckPane: Pane): IAnimationService = new AnimationService(deckPane, cardImageProvider)
}
