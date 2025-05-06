package blackjack.model
import de.htwg.blackjack.model.{Card, Clubs, Diamonds, Hand, Hearts, Spades}
import org.scalatest.funsuite.AnyFunSuite

class HandSpec extends AnyFunSuite {

  test("empty hand has value 0") {
    assert(Hand.empty.value == 0)
  }

  test("hand without aces sums face values") {
    val h = Hand.empty.add(Card("5", Hearts)).add(Card("K", Spades))
    assert(h.value == 5 + 10)
    assert(!h.isBust)
  }

  test("single ace counts as 11 if <=21") {
    val h = Hand.empty.add(Card("A", Diamonds)).add(Card("5", Clubs))
    assert(h.value == 16)
  }

  test("single ace counts as 1 if bust as 11") {
    val h = Hand.empty.add(Card("A", Hearts))
      .add(Card("K", Spades))
      .add(Card("Q", Diamonds))
    // 11 + 10 + 10 = 31 → adjust Ace to 1 → 1 + 10 + 10 = 21
    assert(h.value == 21)
  }

  test("multiple aces adjust correctly") {
    val h = Hand.empty
      .add(Card("A", Hearts))
      .add(Card("A", Diamonds))
      .add(Card("9", Clubs))
    // Werte: 11+11+9=31 → eine Ace=1 → 21
    assert(h.value == 21)
  }

  test("bust detection") {
    val h = Hand.empty
      .add(Card("10", Hearts))
      .add(Card("9", Clubs))
      .add(Card("5", Spades))
    assert(h.isBust)
  }

  test("toString shows cards") {
    val h = Hand.empty.add(Card("A", Hearts)).add(Card("10", Spades))
    assert(h.toString.contains("A♥"))
    assert(h.toString.contains("10♠"))
  }
}
