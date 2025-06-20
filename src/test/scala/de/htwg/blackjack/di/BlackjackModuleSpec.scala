package de.htwg.blackjack.di

import org.scalatest.funsuite.AnyFunSuite
import com.google.inject.{Guice, Injector}
import de.htwg.blackjack.strategy.interfacE.DealerStrategy
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.controller.{IGameController, GameController}
import de.htwg.blackjack.bet.{IBetService, BetService}
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.factory.StandardDeckFactory

class BlackjackModuleSpec extends AnyFunSuite {

  // Ein Injector, der genau Deinen Modul-Bindings folgt
  private val injector: Injector = Guice.createInjector(new BlackjackModule)

  test("DealerStrategy bindet zu ConservativeDealer und ist nicht singleton") {
    val strat1 = injector.getInstance(classOf[DealerStrategy])
    val strat2 = injector.getInstance(classOf[DealerStrategy])

    assert(strat1 != null, "DealerStrategy darf nicht null sein")
    assert(strat1.isInstanceOf[ConservativeDealer],
      s"Expected ConservativeDealer, but got ${strat1.getClass.getName}")
    assert(strat1 ne strat2,
      "DealerStrategy sollte kein Singleton sein (zwei Aufrufe → unterschiedliche Instanzen)")
  }

  test("IGameController bindet zu GameController und ist singleton") {
    val ctrl1 = injector.getInstance(classOf[IGameController])
    val ctrl2 = injector.getInstance(classOf[IGameController])

    assert(ctrl1 != null, "IGameController darf nicht null sein")
    assert(ctrl1.isInstanceOf[GameController],
      s"Expected GameController, but got ${ctrl1.getClass.getName}")
    assert(ctrl1 eq ctrl2,
      "IGameController sollte ein Singleton sein (zwei Aufrufe → gleiche Instanz)")
  }

  test("IBetService bindet zu BetService und ist singleton") {
    val svc1 = injector.getInstance(classOf[IBetService])
    val svc2 = injector.getInstance(classOf[IBetService])

    assert(svc1 != null, "IBetService darf nicht null sein")
    assert(svc1.isInstanceOf[BetService],
      s"Expected BetService, but got ${svc1.getClass.getName}")
    assert(svc1 eq svc2,
      "IBetService sollte ein Singleton sein (zwei Aufrufe → gleiche Instanz)")
  }

  test("IDeckFactory bindet exakt an StandardDeckFactory und ist singleton-Instance") {
    val df1 = injector.getInstance(classOf[IDeckFactory])
    val df2 = injector.getInstance(classOf[IDeckFactory])

    assert(df1 != null, "IDeckFactory darf nicht null sein")
    assert(df1 eq StandardDeckFactory,
      s"Expected StandardDeckFactory instance, but got ${df1.getClass.getName}")
    assert(df1 eq df2,
      "IDeckFactory sollte ein Singleton sein (zwei Aufrufe → gleiche Instanz)")
  }
}
