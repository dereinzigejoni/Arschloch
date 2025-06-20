package de.htwg.blackjack.command

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.{Card, Deck, GameState, Hand, Rank,Suits}
import de.htwg.blackjack.state.GamePhases.PlayerTurn

import scala.util.Try

class DoubleCommandSpec extends AnyFunSuite {

  // Hilfsmethode zum Erzeugen eines einfachen Decks:
  private def deckOf(cards: List[Card]): Deck =
    Deck(cards) // setzt voraus, dass Deck.apply(List[Card]) existiert

  // Hand mit vorgegebenen Karten
  private def handOf(cards: Card*): Hand =
    cards.foldLeft(Hand.empty)(_ add _)

  // Einfache Karte erstellen
  private def card(rank: Rank, suit: Suits): Card =
    Card(rank, suit)

  test("double tut nichts, wenn Hand nicht genau 2 Karten hat") {
    val initialHand = handOf(card(Rank.Six, Suits.Clubs)) // nur 1 Karte
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )
    val result = DoubleCommand.execute(gs).get
    assert(result == gs)
  }

  test("double tut nichts, wenn Budget < Einsatz") {
    val initialHand = handOf(card(Rank.Six, Suits.Clubs), card(Rank.Seven, Suits.Diamonds))
    val gs = GameState(
      deck        = deckOf(List(card(Rank.Ten, Suits.Heart))),
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(50.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 20.0,   // Budget kleiner als Einsatz
      currentBet  = 0.0
    )
    val result = DoubleCommand.execute(gs).get
    assert(result == gs)
  }

  test("double tut nichts, wenn kein Deck übrig ist") {
    val initialHand = handOf(card(Rank.Six, Suits.Clubs), card(Rank.Seven, Suits.Diamonds))
    val gs = GameState(
      deck        = deckOf(Nil), // leeres Deck
      playerHands = List(initialHand),
      dealer      = Hand.empty,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )
    val result = DoubleCommand.execute(gs).get
    assert(result == gs)
  }

  test("double verdoppelt Einsatz, zieht Karte und wechselt bei Single-Hand in DealerTurn") {
    val drawCard = card(Rank.Ten, Suits.Spades)
    val gs = GameState(
      deck        = deckOf(List(drawCard)),
      playerHands = List(handOf(card(Rank.Six, Suits.Clubs), card(Rank.Seven, Suits.Diamonds))),
      dealer      = Hand.empty,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )

    val updated = DoubleCommand.execute(gs).get

    // Einsatz verdoppelt
    assert(updated.bets == List(20.0))
    // Budget um ursprünglichen Einsatz verringert
    assert(updated.budget == 90.0)
    // Hand hat nun 3 Karten und enthält das gezogene
    assert(updated.playerHands.head.cards.contains(drawCard))
    // Deck wurde aufgebraucht
    assert(updated.deck.cards.isEmpty)
    // Phase wechselt in DealerTurn (Single-Hand → idx+1 ≥ size)
    assert(updated.phase.toString.contains("DealerTurn"))
    // activeHand bleibt bei 0
    assert(updated.activeHand == 0)
  }

  test("double verdoppelt Einsatz, zieht Karte und wechselt bei Multi-Hand in PlayerTurn") {
    val drawCard = card(Rank.Ten, Suits.Heart)
    val hand1    = handOf(card(Rank.Five, Suits.Spades), card(Rank.Six, Suits.Clubs))
    val hand2    = handOf(card(Rank.Seven, Suits.Diamonds), card(Rank.Eight, Suits.Heart))
    val gs = GameState(
      deck        = deckOf(List(drawCard)),
      playerHands = List(hand1, hand2),
      dealer      = Hand.empty,
      bets        = List(5.0, 5.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 50.0,
      currentBet  = 0.0
    )

    val updated = DoubleCommand.execute(gs).get

    // Erster Einsatz verdoppelt, zweiter unverändert
    assert(updated.bets == List(10.0, 5.0))
    // Budget um 5 reduziert
    assert(updated.budget == 45.0)
    // activeHand wurde auf die nächste Hand gesetzt
    assert(updated.activeHand == 1)
    // Phase bleibt PlayerTurn, da noch eine Hand übrig ist
    assert(updated.phase == PlayerTurn)
  }
}
