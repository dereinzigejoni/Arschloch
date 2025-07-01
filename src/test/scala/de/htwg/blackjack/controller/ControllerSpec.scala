// src/test/scala/de/htwg/blackjack/controller/ControllerSuite.scala
package de.htwg.blackjack.controller

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.util.{Try, Success, Failure}

// Platzhalter‐Traits/Ersatz‐Implementierungen – bitte durch Eure realen Typen ersetzen
trait GameState
trait Command { def execute(gs: GameState): Try[GameState] }
trait Observer { def update(gs: GameState): Unit }
trait Invoker {
  def redo(): Option[GameState]
  def undo(): Option[GameState]
}

// Eure Controller‐Klasse sollte etwa so aussehen:
// class Controller { protected var state: GameState = _; protected var budget: Double = _; ... }

class ControllerSpec extends AnyFunSuite with Matchers {

  /** Minimaler Test‐Controller, um intern auf `state` und `budget` zuzugreifen */
  class TestController extends Controller {
    def currentState: GameState = state
    def currentBudget: Double = budget
  }

  test("setState should update state and notify observers") {
    val ctrl = new TestController
    val s = new GameState { override def toString = "S" }

    var seen: Option[GameState] = None
    ctrl.addObserver(new Observer {
      override def update(gs: GameState): Unit = seen = Some(gs)
    })

    ctrl.setState(s)

    ctrl.currentState shouldBe s
    seen shouldBe Some(s)
  }

  test("setBudget should update the budget") {
    val ctrl = new TestController
    ctrl.setBudget(42.0)
    ctrl.currentBudget shouldBe 42.0
  }

  test("execute should run the command, update state and notify observers") {
    val ctrl = new TestController
    val s0 = new GameState { override def toString = "zero" }
    ctrl.setState(s0)

    val s1 = new GameState { override def toString = "one" }
    val cmd = new Command {
      override def execute(gs: GameState): Try[GameState] = Success(s1)
    }

    var notified: Option[GameState] = None
    ctrl.addObserver(gs => notified = Some(gs))

    val result = ctrl.execute(cmd)
    result shouldBe a[Success[_]]
    result.get shouldBe s1

    ctrl.currentState shouldBe s1
    notified shouldBe Some(s1)
  }

  test("redo should call invoker.redo, update state and notify observers") {
    val ctrl = new TestController
    val sRedo = new GameState { override def toString = "redo" }

    // Stub‐Invoker injizieren
    ctrl.setInvoker(new Invoker {
      override def redo() = Some(sRedo)
      override def undo() = None
    })

    var seen: Option[GameState] = None
    ctrl.addObserver(gs => seen = Some(gs))

    val res = ctrl.redo()
    res shouldBe Some(sRedo)

    ctrl.currentState shouldBe sRedo
    seen shouldBe Some(sRedo)
  }

  test("loadGame should replace the entire state and notify observers") {
    val ctrl = new TestController
    val loaded = new GameState { override def toString = "loaded" }

    var seen: Option[GameState] = None
    ctrl.addObserver(gs => seen = Some(gs))

    ctrl.loadGame(loaded)

    ctrl.currentState shouldBe loaded
    seen shouldBe Some(loaded)
  }
}
