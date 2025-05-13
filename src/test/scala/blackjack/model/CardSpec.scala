// src/test/scala/de/htwg/blackjack/model/CardSpec.scala
package blackjack.model

import de.htwg.blackjack.model.{Card, Rank,Suits}
import org.scalatest.funsuite.AnyFunSuite

class CardSpec extends AnyFunSuite {
  test("numeric ranks map to their integer values") {
    val c2  = Card(Rank.Two, Suits.Heart)
    val c10 = Card(Rank.Ten, Suits.Clubs)
    assert(c2.value  == 2)
    assert(c10.value == 10)
  }
  test("face cards J, Q, K map to value 10") {
    val j = Card(Rank.Jack, Suits.Diamonds)
    val q = Card(Rank.Queen, Suits.Spades)
    val k = Card(Rank.King, Suits.Heart)
    assert(j.value == 10)
    assert(q.value == 10)
    assert(k.value == 10)
  }
  test("ace maps to value 11") {
    val a = Card(Rank.Ace, Suits.Clubs)
    assert(a.value == 11)
  }
  test("toString returns suit symbol then rank symbol") {
    val aHearts    = Card(Rank.Ace, Suits.Heart)
    val tenSpades  = Card(Rank.Ten, Suits.Spades)
    val queenDiam  = Card(Rank.Queen, Suits.Diamonds)
    assert(aHearts.toString   == "♥A")
    assert(tenSpades.toString == "♠10")
    assert(queenDiam.toString == "♦Q")
  }
  test("suit symbols are correct") {
    assert(Suits.Heart.symbol   == "♥")
    assert(Suits.Diamonds.symbol == "♦")
    assert(Suits.Clubs.symbol    == "♣")
    assert(Suits.Spades.symbol   == "♠")
  }
  test("cards with same rank and suit are equal") {
    val c1 = Card(Rank.Seven, Suits.Heart)
    val c2 = Card(Rank.Seven, Suits.Heart)
    val c3 = Card(Rank.Seven, Suits.Diamonds)
    assert(c1 == c2)
    assert(c1.hashCode == c2.hashCode)
    assert(c1 != c3)
  }
}
