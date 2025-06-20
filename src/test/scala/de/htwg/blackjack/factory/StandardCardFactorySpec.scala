package de.htwg.blackjack.factory

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.factory.StandardCardFactory
import de.htwg.blackjack.model.{Card, Rank, Suits}

class StandardCardFactorySpec extends AnyFunSuite {

  test("allRanks enthält exakt alle Rank-Werte in der gleichen Reihenfolge") {
    val expected = Rank.values.toSeq
    val actual   = StandardCardFactory.allRanks
    assert(actual == expected,
      s"Expected allRanks to be $expected but was $actual")
  }

  test("allSuits enthält exakt alle Suits-Werte in der gleichen Reihenfolge") {
    val expected = Suits.values.toSeq
    val actual   = StandardCardFactory.allSuits
    assert(actual == expected,
      s"Expected allSuits to be $expected but was $actual")
  }

  test("createCard erstellt eine Card mit übergebenem Rank und Suit") {
    for {
      rank <- StandardCardFactory.allRanks
      suit <- StandardCardFactory.allSuits
    } {
      val card = StandardCardFactory.createCard(rank, suit)
      assert(card.rank  == rank,
        s"Expected card.rank == $rank but was ${card.rank}")
      assert(card.suit == suit,
        s"Expected card.suit == $suit but was ${card.suit}")
    }
  }
}
