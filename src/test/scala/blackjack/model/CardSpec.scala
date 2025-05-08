package blackjack.model
import de.htwg.blackjack.model.{Card, Clubs, Diamonds, Hearts, Spades}
import org.scalatest.funsuite.AnyFunSuite
class CardSpec extends AnyFunSuite {
  test("numeric ranks map to their integer values") {
    val c2  = Card("2", Hearts)
    val c10 = Card("10", Clubs)
    assert(c2.value  == 2)
    assert(c10.value == 10)
  }
  test("face cards J, Q, K map to value 10") {
    val j = Card("J", Diamonds)
    val q = Card("Q", Spades)
    val k = Card("K", Hearts)
    assert(j.value == 10)
    assert(q.value == 10)
    assert(k.value == 10)
  }
  test("ace maps to value 11") {
    val a = Card("A", Clubs)
    assert(a.value == 11)
  }
  test("toString returns rank followed by suit symbol") {
    val aHearts    = Card("A", Hearts)
    val tenSpades  = Card("10", Spades)
    val queenDiam  = Card("Q", Diamonds)
    assert(aHearts.toString   == "A♥")
    assert(tenSpades.toString == "10♠")
    assert(queenDiam.toString == "Q♦")
  }
  test("suit symbols are correct") {
    assert(Hearts.symbol   == "♥")
    assert(Diamonds.symbol == "♦")
    assert(Clubs.symbol    == "♣")
    assert(Spades.symbol   == "♠")
  }
  test("cards with same rank and suit are equal") {
    val c1 = Card("7", Hearts)
    val c2 = Card("7", Hearts)
    val c3 = Card("7", Diamonds)
    assert(c1 == c2)
    assert(c1.hashCode == c2.hashCode)
    assert(c1 != c3)
  }
}