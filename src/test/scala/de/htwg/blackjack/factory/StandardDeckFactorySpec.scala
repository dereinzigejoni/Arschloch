package de.htwg.blackjack.factory

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.factory.{StandardCardFactory, StandardDeckFactory}
import de.htwg.blackjack.model.{Card, Deck, Rank, Suits}

class StandardDeckFactorySpec extends AnyFunSuite {

  test("newDeck erzeugt ein Deck mit 52 eindeutigen Karten") {
    val deck: Deck = StandardDeckFactory.newDeck
    val cards = deck.cards

    // Größe und Eindeutigkeit
    assert(cards.size == 52, s"Erwartet 52 Karten, war aber ${cards.size}")
    assert(cards.distinct.size == 52, "Kartenliste enthält Duplikate")

    // Menge aller Karten entspricht dem kartesischen Produkt aus Rängen × Farben
    val expected = for {
      r <- StandardCardFactory.allRanks
      s <- StandardCardFactory.allSuits
    } yield Card(r, s)

    assert(cards.toSet == expected.toSet, "Deck enthält nicht exakt alle möglichen Karten")
  }

  test("Jede Kombination aus Rank und Suit ist im neuen Deck vorhanden") {
    val cards = StandardDeckFactory.newDeck.cards

    for {
      r <- StandardCardFactory.allRanks
      s <- StandardCardFactory.allSuits
    } {
      assert(
        cards.contains(Card(r, s)),
        s"Deck fehlt Karte mit Rank=$r und Suit=$s"
      )
    }
  }

  test("Sortiertes Deck entspricht der erwarteten Reihenfolge (buildDeck ohne Shuffle)") {
    val deck: Deck = StandardDeckFactory.newDeck
    val cards = deck.cards

    // Sortierkriterium: Rang-Index, dann Suit-Index
    val sortedCards = cards.sortBy { card =>
      (
        StandardCardFactory.allRanks.indexOf(card.rank),
        StandardCardFactory.allSuits.indexOf(card.suit)
      )
    }

    // Erwartete Reihenfolge: für jeden Rang alle Suits in Factory-Reihenfolge
    val expectedSorted = StandardCardFactory.allRanks.toList.flatMap { r =>
      StandardCardFactory.allSuits.toList.map { s =>
        Card(r, s)
      }
    }

    assert(
      sortedCards == expectedSorted,
      "Nach Sortierung weicht das Deck von der erwarteten, ungemischten Reihenfolge ab"
    )
  }
}
