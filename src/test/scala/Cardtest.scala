import htwg.de.Card.Card

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class CardTest extends AnyWordSpec with Matchers {

  "A Card" should {
    "be equal to another card with the same value and suit" in {
      val card1 = Card("A", "♠")
      val card2 = Card("A", "♠")
      card1 shouldEqual card2
    }

    "have a toString that returns the value and suit concatenated" in {
      val card = Card("A", "♠")
      card.toString shouldEqual "A♠"
    }

    "not be equal to a card with a different value or suit" in {
      val card1 = Card("A", "♠")
      val card2 = Card("K", "♠")
      val card3 = Card("A", "♥")
      card1 should not equal card2
      card1 should not equal card3
    }
  }
}
