package de.htwg.blackjack.di
import com.google.inject.{AbstractModule, Singleton}
import de.htwg.blackjack.bet.{BetService, IBetService}
import de.htwg.blackjack.controller.{GameController, IGameController}
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.strategy.interfacE.DealerStrategy
class BlackjackModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DealerStrategy]).to(classOf[ConservativeDealer])
    bind(classOf[IGameController]).to(classOf[GameController]).in(classOf[Singleton])
    bind(classOf[IBetService]).to(classOf[BetService]).in(classOf[Singleton])
    bind(classOf[IDeckFactory]).toInstance(StandardDeckFactory)
  }
}