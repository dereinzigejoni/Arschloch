package de.htwg.blackjack.di

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.controller.IGameController
import de.htwg.blackjack.bet.IBetService
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.GUI.IAnimationService
import scalafx.scene.layout.Pane

class ApplicationContextSpec extends AnyFunSuite {

  test("gameController liefert eine nicht-null IGameController-Instanz und ist singleton") {
    val ctrl1 = ApplicationContext.gameController
    val ctrl2 = ApplicationContext.gameController
    assert(ctrl1 != null, "gameController darf nicht null sein")
    assert(ctrl1.isInstanceOf[IGameController], "gameController muss IGameController implementieren")
    assert(ctrl1 eq ctrl2, "gameController sollte immer dieselbe Instanz zurückliefern (Singleton)")
  }

  test("betService liefert eine nicht-null IBetService-Instanz und ist singleton") {
    val svc1 = ApplicationContext.betService
    val svc2 = ApplicationContext.betService
    assert(svc1 != null, "betService darf nicht null sein")
    assert(svc1.isInstanceOf[IBetService], "betService muss IBetService implementieren")
    assert(svc1 eq svc2, "betService sollte immer dieselbe Instanz zurückliefern (Singleton)")
  }

  test("deckFactory ist das StandardDeckFactory-Singleton") {
    val df = ApplicationContext.deckFactory
    assert(df != null, "deckFactory darf nicht null sein")
    assert(df eq StandardDeckFactory, "deckFactory muss genau StandardDeckFactory sein")
    assert(df.isInstanceOf[IDeckFactory], "deckFactory muss IDeckFactory implementieren")
  }

  test("animationService erzeugt pro Aufruf neue Instanzen von IAnimationService") {
    val pane1 = new Pane
    val pane2 = new Pane
    val anim1 = ApplicationContext.animationService(pane1)
    val anim2 = ApplicationContext.animationService(pane2)
    assert(anim1 != null, "animationService darf nicht null zurückliefern")
    assert(anim1.isInstanceOf[IAnimationService], "animationService muss IAnimationService implementieren")
    assert(anim1 ne anim2, "animationService sollte für unterschiedliche Pane-Parameter neue Instanzen liefern")
  }
}
