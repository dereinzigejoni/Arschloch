import htwg.de.Card.Card
import org.scalatest.funsuite.AnyFunSuite

class Cardtest extends AnyFunSuite {

  test("Card toString should return a string with value and suit") {
    val card = Card("A", "♠")
    assert(card.toString == "A♠")
  }

  test("Card with value and suit should be correctly represented") {
    val card = Card("10", "♦")
    assert(card.toString == "10♦")
  }

  test("Card with single character value should be correctly represented") {
    val card = Card("K", "♥")
    assert(card.toString == "K♥")
  }

  test("Card with different suit should be correctly represented") {
    val card = Card("7", "♣")
    assert(card.toString == "7♣")
  }

  test("Card with number value should be correctly represented") {
    val card = Card("2", "♠")
    assert(card.toString == "2♠")
  }

  test("Card with invalid value should still return a valid string") {
    val card = Card("Q", "♠")
    assert(card.toString == "Q♠")
  }
}
