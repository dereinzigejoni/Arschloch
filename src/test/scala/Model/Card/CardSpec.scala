package Model.Card

import de.htwg.Model.Card.Card
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CardSpec extends AnyWordSpec with Matchers {

  "A Card" should {
    "return correct rank for numbered cards" in {
      Card("2", "♠").rank shouldBe 2
      Card("10", "♣").rank shouldBe 10
    }

    "return correct rank for face cards" in {
      Card("J", "♦").rank shouldBe 11
      Card("Q", "♥").rank shouldBe 12
      Card("K", "♠").rank shouldBe 13
      Card("A", "♣").rank shouldBe 14
    }
  }

  "Card.rankOf" should {
    "convert number strings to integers" in {
      Card.rankOf("7") shouldBe 7
      Card.rankOf("10") shouldBe 10
    }

    "map face card values to correct rank" in {
      Card.rankOf("J") shouldBe 11
      Card.rankOf("Q") shouldBe 12
      Card.rankOf("K") shouldBe 13
      Card.rankOf("A") shouldBe 14
    }
  }

  "Card.all" should {
    "contain 52 unique cards" in {
      Card.all.distinct.length shouldBe 52
    }

    "contain all combinations of 13 values and 4 suits" in {
      val suits = Set("♥", "♦", "♠", "♣")
      val values = Set("2","3","4","5","6","7","8","9","10","J","Q","K","A")

      Card.all.forall(card => values.contains(card.value) && suits.contains(card.suit)) shouldBe true
    }
  }
}