package de.htwg.blackjack.model

import org.scalatest.funsuite.AnyFunSuite

class DeckSpec extends AnyFunSuite {

  // Hilfsmethode zum Erzeugen einer Test‐Karte
  private def card(rank: Rank, suit: Suits): Card = Card(rank, suit)

  test("draw liefert das erste Element und entfernt es aus dem Deck") {
    val c1   = card(Rank.Ace, Suits.Spades)
    val c2   = card(Rank.King, Suits.Heart)
    val deck = Deck(List(c1, c2))

    val (drawn, rest) = deck.draw()
    assert(drawn == c1, "drawn sollte das erste Element sein")
    assert(rest.cards == List(c2), "rest.cards sollte nur noch die zweite Karte enthalten")
  }

  test("draw auf leerem Deck wirft RuntimeException") {
    val empty = Deck(Nil)
    intercept[RuntimeException] {
      empty.draw()
    }
  }

  test("fresh erzeugt ein Deck mit 52 eindeutigen Karten aller Kombinationen") {
    val deck  = Deck.fresh()
    val cards = deck.cards

    // Größe und Eindeutigkeit
    assert(cards.size == Rank.values.size * Suits.values.size,
      s"Erwartet ${Rank.values.size * Suits.values.size} Karten, war aber ${cards.size}")
    assert(cards.distinct.size == cards.size, "Kartenliste enthält Duplikate")

    // Inhalt entspricht allen Kombinationen aus Rank × Suits
    val expected = for {
      r <- Rank.values
      s <- Suits.values
    } yield Card(r, s)
    assert(cards.toSet == expected.toSet,
      "Deck.fresh() enthält nicht exakt alle Rank-Suit-Kombinationen")
  }

  test("shuffled erhält die gleichen Elemente wie die Eingabeliste") {
    val sample = List(
      card(Rank.Two, Suits.Clubs),
      card(Rank.Three, Suits.Diamonds),
      card(Rank.Four, Suits.Heart)
    )
    val deck = Deck.shuffled(sample)

    // gleiche Multimenge und gleiche Größe
    assert(deck.cards.toSet == sample.toSet,
      "Deck.shuffled() verändert nicht den Inhalt der Liste")
    assert(deck.cards.size == sample.size,
      "Deck.shuffled() ändert nicht die Anzahl der Elemente")
  }
}
