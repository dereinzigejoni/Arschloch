package de.htwg.blackjack.command

import de.htwg.blackjack.controller.GameController
import de.htwg.blackjack.model.{Deck, GameState, Hand}
import de.htwg.blackjack.state.{GamePhase, GamePhases}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funsuite.AnyFunSuite

import scala.util.{Failure, Success, Try}

/**
 * Test helpers and fixtures for GameState and CommandInvoker.
 */
object TestData {
  // Create a minimal deck and hand for testing
  val defaultDeck: Deck = Deck(List.empty)
  val defaultHand: Hand = Hand(List.empty)

  // Example initial and next states
  val initialState: GameState = GameState(
    deck = defaultDeck,
    playerHands = List(defaultHand),
    dealer = defaultHand,
    bets = List(5.0),
    activeHand = 0,
    phase = GamePhases.PlayerTurn,
    budget = 100.0,
    currentBet = 5.0
  )
  val newState: GameState = initialState.copy(budget = 95.0)
}

class CommandInvokerTest extends AnyFunSuite with MockFactory {
  import TestData._

  // Dummy command for testing
  class DummyCommand(result: Try[GameState]) extends Command {
    var executedWith: Option[GameState] = None
    override def execute(state: GameState): Try[GameState] = {
      executedWith = Some(state)
      result
    }
  }

  test("execute should apply command, update controller state, push old state to undo stack, and clear redo stack") {
    // Arrange
    val controller = mock[GameController]
    ((() => controller.getState)).expects().returning(initialState).once()
    val command = new DummyCommand(Success(newState))
    (controller.setStateInternal _).expects(newState).once()

    val invoker = new CommandInvoker(controller)

    // Act
    val result = invoker.execute(command)

    // Assert
    assert(result == Success(newState))
    // After execute, undo should return the initial state
    ((() => controller.getState)).expects().returning(newState).once()
    (controller.setStateInternal _).expects(initialState).once()
    val undoResult = invoker.undo()
    assert(undoResult.contains(initialState))
  }

  test("execute should propagate failure without modifying stacks or controller state") {
    // Arrange
    val controller = mock[GameController]
    ((() => controller.getState)).expects().returning(initialState).once()
    val exception = new RuntimeException("fail")
    val command = new DummyCommand(Failure(exception))
    // controller.setStateInternal should not be called
    (controller.setStateInternal _).expects(*).never()

    val invoker = new CommandInvoker(controller)

    // Act
    val result = invoker.execute(command)

    // Assert
    assert(result.isFailure)
    assert(invoker.undo().isEmpty)
    assert(invoker.redo().isEmpty)
  }

  test("undo should return None when undo stack is empty") {
    val controller = mock[GameController]
    val invoker = new CommandInvoker(controller)
    assert(invoker.undo().isEmpty)
  }

  test("redo should return None when redo stack is empty") {
    val controller = mock[GameController]
    val invoker = new CommandInvoker(controller)
    assert(invoker.redo().isEmpty)
  }

  test("undo should set state to previous and push current state to redo stack") {
    val controller = mock[GameController]
    val command = new DummyCommand(Success(newState))

    ((() => controller.getState)).expects().returning(initialState).once()
    (controller.setStateInternal _).expects(newState).once()
    val invoker = new CommandInvoker(controller)
    invoker.execute(command)

    // Prepare mocks for undo
    ((() => controller.getState)).expects().returning(newState).once()
    (controller.setStateInternal _).expects(initialState).once()

    val undoResult = invoker.undo()
    assert(undoResult.contains(initialState))
  }

  test("redo should set state to next and push current state to undo stack") {
    val controller = mock[GameController]
    val command = new DummyCommand(Success(newState))

    ((() => controller.getState)).expects().returning(initialState).once()
    (controller.setStateInternal _).expects(newState).once()
    val invoker = new CommandInvoker(controller)
    invoker.execute(command)

    // Setup undo
    ((() => controller.getState)).expects().returning(newState).once()
    (controller.setStateInternal _).expects(initialState).once()
    invoker.undo()

    // Setup redo
    ((() => controller.getState)).expects().returning(initialState).once()
    (controller.setStateInternal _).expects(newState).once()

    val redoResult = invoker.redo()
    assert(redoResult.contains(newState))
  }
}
