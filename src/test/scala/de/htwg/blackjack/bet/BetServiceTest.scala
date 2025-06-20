// src/test/scala/de/htwg/blackjack/bet/BetServiceTest.scala
package de.htwg.blackjack.bet

import org.scalatest.funsuite.AnyFunSuite
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers

import de.htwg.blackjack.controller.IGameController
import de.htwg.blackjack.model.GameState

import scala.util.{Success, Failure, Try}

class BetServiceTest extends AnyFunSuite with Matchers with MockFactory {

  test("placeBet should delegate to controller and return Success") {
    val mockController = mock[IGameController]
    val betAmount = 20.0

    (mockController.tryplaceBet _).expects(betAmount).returning(Success(()))

    val betService = new BetService(mockController)
    betService.placeBet(betAmount) shouldBe Success(())
  }

  test("placeBet should delegate to controller and return Failure") {
    val mockController = mock[IGameController]
    val betAmount = -10.0

    (mockController.tryplaceBet _).expects(betAmount).returning(Failure(new IllegalArgumentException("Invalid bet")))

    val betService = new BetService(mockController)
    val result = betService.placeBet(betAmount)

    result shouldBe a[Failure[_]]
    result.failed.get.getMessage should include ("Invalid bet")
  }

  test("clearBet does nothing") {
    val mockController = mock[IGameController]
    val betService = new BetService(mockController)

    noException should be thrownBy betService.clearBet()
  }

  
  /*test("currentBet should return value from controller.getState.currentBet") {
    val mockController = mock[IGameController]
    val mockState = stub[GameState]

    //((() => mockState.currentBet).when().returns(42.0) )// ✅ korrekt
    ((() => mockState.currentBet).when().returns(42.0))
    ((() => mockController.getState)).expects().returning(mockState)

    val betService = new BetService(mockController)
    betService.currentBet shouldBe 42.0
  }*/

  test("budget should return value from controller") {
    val mockController = mock[IGameController]

    ((() => mockController.getBudget)).expects().returning(100.0)

    val betService = new BetService(mockController)
    betService.budget shouldBe 100.0
  }
}
