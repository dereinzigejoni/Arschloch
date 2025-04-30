package de.htwg.Card
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import de.htwg.Card.Card

class Cardtest extends AnyWordSpec with Matchers {
  "A Card" should {
    "die korrekte String-Repräsentation zurückgeben" in {
      val card = Card("A", "♠")
      card.toString shouldEqual "A♠"
    }
  }
}
