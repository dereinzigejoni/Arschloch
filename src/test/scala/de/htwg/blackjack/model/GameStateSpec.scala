package de.htwg.blackjack.model

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.{Card, Deck, GameState, Hand, Rank, Suits}
import de.htwg.blackjack.state.GamePhases.PlayerTurn

class GameStateSpec extends AnyFunSuite {

  // Hilfsmethoden zum Erzeugen einfacher Karten, Hände und Decks
  private def card(rank: Rank, suit: Suits): Card =
    Card(rank, suit)

  private val emptyDeck: Deck = Deck(Nil)
  private val emptyHand: Hand = Hand.empty

  private val baseState = GameState(
    deck        = emptyDeck,
    playerHands = List(emptyHand),
    dealer      = emptyHand,
    bets        = List(5.0),
    activeHand  = 0,
    phase       = PlayerTurn,
    budget      = 100.0,
    currentBet  = 5.0
  )

  test("GameState-Felder werden korrekt gesetzt und zurückgegeben") {
    assert(baseState.deck        == emptyDeck)
    assert(baseState.playerHands == List(emptyHand))
    assert(baseState.dealer      == emptyHand)
    assert(baseState.bets        == List(5.0))
    assert(baseState.activeHand  == 0)
    assert(baseState.phase       == PlayerTurn)
    assert(baseState.budget      == 100.0)
    assert(baseState.currentBet  == 5.0)
  }

  test("GameState structural equality und hashCode") {
    val copyState = baseState.copy()
    assert(baseState == copyState,
      s"Expected identical states to be equal: $baseState vs $copyState")
    assert(baseState.hashCode == copyState.hashCode,
      "Equal states must have equal hashCode")
    val different = baseState.copy(budget = 50.0)
    assert(baseState != different,
      "States with different fields should not be equal")
  }

  test("GameState.copy ändert nur das angegebene Feld") {
    val modified = baseState.copy(activeHand = 1, budget = 80.0)
    assert(modified.activeHand == 1)
    assert(modified.budget     == 80.0)
    // alle anderen Felder unverändert
    assert(modified.deck        == baseState.deck)
    assert(modified.playerHands == baseState.playerHands)
    assert(modified.dealer      == baseState.dealer)
    assert(modified.bets        == baseState.bets)
    assert(modified.phase       == baseState.phase)
    assert(modified.currentBet  == baseState.currentBet)
  }

  test("Status-Fallobjekte sind unterscheidbar und covern alle Fälle") {
    // Status ist sealed trait in GameState.scala
    val statuses: Set[Status] = Set(InProgress, PlayerBust, DealerBust, Finished)
    assert(statuses.size == 4, "Es muss genau 4 Status-Objekte geben")
    statuses.foreach {
      case InProgress    => succeed
      case PlayerBust    => succeed
      case DealerBust    => succeed
      case Finished      => succeed
      case other         => fail(s"Unbekannter Status: $other")
    }
  }
}
