package de.htwg.blackjack.di

import de.htwg.blackjack.bet.{BetService, IBetService}
import de.htwg.blackjack.controller.{GameController, IGameController}
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.model.deck.IDeckFactory

class BlackjackModule  {
  override def configure(): Unit = {
    // Controller als Singleton
    bind(classOf[IGameController])
      .to(classOf[GameController])
      .in(classOf[Singleton])

    // BetService braucht den Controller
    bind(classOf[IBetService])
      .to(classOf[BetService])
      .in(classOf[Singleton])

    // DeckFactory als Instanz
    bind(classOf[IDeckFactory])
      .toInstance(StandardDeckFactory)
  }
}
