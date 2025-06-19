package de.htwg.blackjack.di
import de.htwg.blackjack.controller.*
import de.htwg.blackjack.model.deck.*
import de.htwg.blackjack.bet.*
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.view.*
import de.htwg.blackjack.GUI.*
class Module {
  lazy val deckFactory: IDeckFactory = StandardDeckFactory
  lazy val controller: IGameController = new GameController()
  lazy val betService: IBetService = new BetService(controller)
  lazy val tuiView: IGameView = new TuiView(controller)
  lazy val guiView: IGameView = BlackjackGuiApp
}
