package de.htwg.blackjack.bet

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import de.htwg.blackjack.controller.IGameController
import de.htwg.blackjack.model.GameState
import scala.util.{Try, Success, Failure}

class BetServiceTest extends AnyFunSuite with MockitoSugar {

  test("placeBet delegiert an controller.tryplaceBet und liefert Success") {
    val controller = mock[IGameController]
    when(controller.tryplaceBet(50.0)).thenReturn(Success(()))

    val service = new BetService(controller)
    val result  = service.placeBet(50.0)

    assert(result == Success(()))
    verify(controller).tryplaceBet(50.0)
  }

  test("placeBet liefert Failure weiter, wenn controller.tryplaceBet fehlschlägt") {
    val controller = mock[IGameController]
    val ex         = new RuntimeException("Insufficient budget")
    when(controller.tryplaceBet(100.0)).thenReturn(Failure(ex))

    val service = new BetService(controller)
    val result  = service.placeBet(100.0)

    assert(result.isFailure)
    assert(result.failed.get eq ex)
    verify(controller).tryplaceBet(100.0)
  }

  test("clearBet liefert Unit und interagiert nicht mit dem Controller") {
    val controller = mock[IGameController]
    val service    = new BetService(controller)

    val result = service.clearBet()
    assert(result == ())            // gibt Unit zurück
    verifyNoInteractions(controller) // keine Methodenaufrufe
  }

  test("currentBet gibt controller.getState.currentBet zurück") {
    val controller = mock[IGameController]
    val state      = mock[GameState]
    when(state.currentBet).thenReturn(75.0)
    when(controller.getState).thenReturn(state)

    val service = new BetService(controller)
    assert(service.currentBet == 75.0)
    verify(controller).getState
  }

  test("budget gibt controller.getBudget zurück") {
    val controller = mock[IGameController]
    when(controller.getBudget).thenReturn(150.0)

    val service = new BetService(controller)
    assert(service.budget == 150.0)
    verify(controller).getBudget
  }
}
