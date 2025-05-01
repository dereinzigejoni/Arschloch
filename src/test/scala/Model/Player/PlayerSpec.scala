package Model.Player

import de.htwg.Model.Card.Card
import de.htwg.Model.Player.Player
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PlayerSpec extends AnyWordSpec with Matchers {

  val card1: Card = Card("10", "♠")
  val card2: Card = Card("A", "♥")
  val initialHand: List[Card] = List(card1, card2)

  "A Player" should {
    "be initialized with name, hand, optional rank and human flag" in {
      val player = Player("Alice", initialHand, Some(1), isHuman = false)
      player.name shouldBe "Alice"
      player.hand shouldBe initialHand
      player.rank shouldBe Some(1)
      player.isHuman shouldBe false
    }

    "remove specified cards from hand" in {
      val player = Player("Bob", initialHand)
      val updatedPlayer = player.removeCards(List(card1))
      updatedPlayer.hand shouldBe List(card2)
    }

    "not change hand if none of the given cards match" in {
      val player = Player("Bob", initialHand)
      val otherCard = Card("3", "♦")
      val updatedPlayer = player.removeCards(List(otherCard))
      updatedPlayer.hand shouldBe initialHand
    }

    "have cards if hand is non-empty" in {
      val player = Player("Charlie", List(card1))
      player.hasCards shouldBe true
    }

    "not have cards if hand is empty" in {
      val player = Player("Dana", Nil)
      player.hasCards shouldBe false
    }
  }
}
