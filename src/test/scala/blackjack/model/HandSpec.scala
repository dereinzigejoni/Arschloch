// src/test/scala/de/htwg/blackjack/model/HandSpec.scala
package blackjack.model
import de.htwg.blackjack.model.{Card, Hand, Rank,Suits}
import org.scalatest.funsuite.AnyFunSuite

class HandSpec extends AnyFunSuite {
  test("empty hand has value 0") {
    assert(Hand.empty.value == 0)
  }

  test("hand without aces sums face values") {
    val h = Hand.empty
      .add(Card(Rank.Five, Suits.Heart))
      .add(Card(Rank.King, Suits.Spades))
    assert(h.value == 5 + 10)
    assert(!h.isBust)
  }

  test("single ace counts as 11 if ≤21") {
    val h = Hand.empty
      .add(Card(Rank.Ace, Suits.Diamonds))
      .add(Card(Rank.Five, Suits.Clubs))
    assert(h.value == 16)
  }

  test("single ace counts as 1 if bust as 11") {
    val h = Hand.empty
      .add(Card(Rank.Ace, Suits.Heart))
      .add(Card(Rank.King, Suits.Spades))
      .add(Card(Rank.Queen, Suits.Diamonds))
    // 11 + 10 + 10 = 31 → adjust Ace to 1 → 1 + 10 + 10 = 21
    assert(h.value == 21)
  }

  test("multiple aces adjust correctly") {
    val h = Hand.empty
      .add(Card(Rank.Ace, Suits.Heart))
      .add(Card(Rank.Ace, Suits.Diamonds))
      .add(Card(Rank.Nine, Suits.Clubs))
    assert(h.value == 21)
  }

  test("bust detection") {
    val h = Hand.empty
      .add(Card(Rank.Ten, Suits.Heart))
      .add(Card(Rank.Nine, Suits.Clubs))
      .add(Card(Rank.Five, Suits.Spades))
    assert(h.isBust)
  }

  test("toString shows suit and rank symbols") {
    val h = Hand.empty
      .add(Card(Rank.Ace, Suits.Heart))
      .add(Card(Rank.Ten, Suits.Spades))
    val s = h.toString
    assert(s.contains("♥A"))
    assert(s.contains("♠10"))
  }
}
