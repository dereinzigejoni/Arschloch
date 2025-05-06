package blackjack.model

import de.htwg.blackjack.model.{Card, Deck}
import org.scalatest.funsuite.AnyFunSuite

class DeckSpec extends AnyFunSuite {

  test("fresh deck has 52 cards") {
    val d = Deck.fresh()
    assert(d.cards.size == 52)
  }

  test("draw returns head and deck shrinks by 1") {
    val d0 = Deck.fresh()
    val (c, d1) = d0.draw()
    assert(d1.cards.size == d0.cards.size - 1)
    assert(c.isInstanceOf[Card])
  }

  test("drawing from empty deck throws") {
    val emptyDeck = Deck(Nil)
    intercept[RuntimeException] {
      emptyDeck.draw()
    }
  }
}