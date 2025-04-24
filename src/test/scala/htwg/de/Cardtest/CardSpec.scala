package htwg.de.Cardtest
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import htwg.de.Card.Card

class Cardtest extends AnyWordSpec with Matchers {
  "A Card" should {
    "die korrekte String-Repräsentation zurückgeben" in {
      val card = Card("A", "♠")
      card.toString shouldEqual "A♠"
    }
  }
}
