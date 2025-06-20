package de.htwg.blackjack.controller

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.controller.IGameController

class SharedGameControllerSpec extends AnyFunSuite {

  test("instance ist nicht null") {
    assert(SharedGameController.instance != null)
  }

  test("instance implementiert IGameController") {
    assert(SharedGameController.instance.isInstanceOf[IGameController])
  }

  test("instance verhält sich wie Singleton (immer dieselbe Referenz)") {
    val inst1 = SharedGameController.instance
    val inst2 = SharedGameController.instance
    assert(inst1 eq inst2, "SharedGameController.instance sollte immer dieselbe Referenz zurückliefern")
  }
}
