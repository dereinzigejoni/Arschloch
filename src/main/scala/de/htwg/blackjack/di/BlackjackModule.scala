package de.htwg.blackjack.di
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Singleton}
import de.htwg.blackjack.bet.{BetService, IBetService}
import de.htwg.blackjack.controller.{GameController, IGameController}
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.io.IFileIO
import de.htwg.blackjack.io.json.JsonFileIO
import de.htwg.blackjack.io.xml.XmlFileIO
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.strategy.interfacE.DealerStrategy
class BlackjackModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DealerStrategy]).to(classOf[ConservativeDealer])
    bind(classOf[IGameController]).to(classOf[GameController]).in(classOf[Singleton])
    bind(classOf[IBetService]).to(classOf[BetService]).in(classOf[Singleton])
    bind(classOf[IDeckFactory]).toInstance(StandardDeckFactory)
    // XML-Implementierung
    bind(classOf[IFileIO]).annotatedWith(Names.named("xml")).to(classOf[XmlFileIO]).in(classOf[Singleton])
    // JSON-Implementierung
    bind(classOf[IFileIO]).annotatedWith(Names.named("json")).to(classOf[JsonFileIO]).in(classOf[Singleton])
    // Default: XML
    bind(classOf[IFileIO]).to(classOf[XmlFileIO]).in(classOf[Singleton])
  }
}