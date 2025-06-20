package de.htwg.blackjack.model

import org.scalatest.funsuite.AnyFunSuite

class CardSpec extends AnyFunSuite {

  test("Rank.values contains exactly all 13 ranks in declaration order with correct symbols and values") {
    val expected = Seq(
      Rank.Two    -> ("2", 2),
      Rank.Three  -> ("3", 3),
      Rank.Four   -> ("4", 4),
      Rank.Five   -> ("5", 5),
      Rank.Six    -> ("6", 6),
      Rank.Seven  -> ("7", 7),
      Rank.Eight  -> ("8", 8),
      Rank.Nine   -> ("9", 9),
      Rank.Ten    -> ("10", 10),
      Rank.Jack   -> ("J", 10),
      Rank.Queen  -> ("Q", 10),
      Rank.King   -> ("K", 10),
      Rank.Ace    -> ("A", 11)
    )

    // coverage: ordering and membership
    assert(Rank.values.toSeq == expected.map(_._1), "Rank.values must list all cases in the order declared")

    // coverage: each rank's symbol and value
    expected.foreach { case (rank, (sym, v)) =>
      assert(rank.symbol == sym, s"${rank}.symbol should be '$sym'")
      assert(rank.value  == v,   s"${rank}.value should be $v")
    }
  }

  test("Suits.values contains exactly all 4 suits in declaration order with correct symbols") {
    val expected = Seq(
      Suits.Heart    -> "♥",
      Suits.Diamonds -> "♦",
      Suits.Clubs    -> "♣",
      Suits.Spades   -> "♠"
    )

    // coverage: ordering and membership
    assert(Suits.values.toSeq == expected.map(_._1), "Suits.values must list all cases in the order declared")

    // coverage: each suit's symbol
    expected.foreach { case (suit, sym) =>
      assert(suit.symbol == sym, s"${suit}.symbol should be '$sym'")
    }
  }

  test("Card.value returns the underlying rank.value") {
    for (rank <- Rank.values) {
      val card = Card(rank, Suits.Heart)
      assert(card.value == rank.value,
        s"Card($rank, Heart).value should be ${rank.value}")
    }
  }

  test("Card.toString returns suit.symbol followed by rank.symbol") {
    val combos = for {
      rank <- Rank.values
      suit <- Suits.values
    } yield (rank, suit)

    combos.foreach { case (rank, suit) =>
      val card = Card(rank, suit)
      val expectedString = suit.symbol + rank.symbol
      assert(card.toString == expectedString,
        s"Card($rank, $suit).toString should be '$expectedString'")
    }
  }
}
