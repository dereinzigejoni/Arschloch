// src/test/scala/de/htwg/blackjack/view/TuiViewPatternsSpec.scala
package de.htwg.blackjack.view

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.view.TuiView
import de.htwg.blackjack.model.*
import de.htwg.blackjack.state.GamePhase
import de.htwg.blackjack.state.GamePhases.*

class TuiViewPatternsSpec extends AnyFunSuite {

  /** Helper zum schnellen Erzeugen eines GameState mit allen Parametern. */
  private def mkState(
                       playerHands: List[Hand],
                       dealer:      Hand,
                       bets:        List[Double],
                       activeHand:  Int         = 0,
                       phase:       GamePhase   = PlayerTurn,
                       budget:      Double      = 100.0,
                       currentBet:  Double      = 0.0
                     ): GameState =
    GameState(
      deck        = Deck(Nil),
      playerHands = playerHands,
      dealer      = dealer,
      bets        = bets,
      activeHand  = activeHand,
      phase       = phase,
      budget      = budget,
      currentBet  = currentBet
    )

  test("parseBetInput: QUIT-Erkennung") {
    assert(TuiView.parseBetInput("H", 100)     == Left("QUIT"))
    assert(TuiView.parseBetInput("h", 100)     == Left("QUIT"))
  }

  test("parseBetInput: ungültige Zahl") {
    assert(TuiView.parseBetInput("foo", 100)   == Left("Bitte eine gültige Zahl eingeben."))
    assert(TuiView.parseBetInput("", 100)      == Left("Bitte eine gültige Zahl eingeben."))
  }

  test("parseBetInput: <= 0 ist nicht erlaubt") {
    assert(TuiView.parseBetInput("0", 100)     == Left("Einsatz muss > 0 sein."))
    assert(TuiView.parseBetInput("-5", 100)    == Left("Einsatz muss > 0 sein."))
  }

  test("parseBetInput: Einsatz über 90% Budget") {
    val budget = 100.0
    val Left(msg) = TuiView.parseBetInput("91", budget)
    assert(msg.contains("Einsatz darf max €90.0"))
  }

  test("parseBetInput: gültiger Einsatz") {
    assert(TuiView.parseBetInput("50", 100)    == Right(50.0))
    assert(TuiView.parseBetInput("  10.5 ", 20)== Right(10.5))
  }

  test("formatMenuOptions: nur Hit/Stand/Quit wenn 1 Karte") {
    val oneCardHand = Hand.empty.add(Card(Rank.Five, Suits.Clubs))
    val gs = mkState(
      playerHands = List(oneCardHand),
      dealer      = Hand.empty,
      bets        = List(10.0)
    )
    val opts = TuiView.formatMenuOptions(gs, gs.budget)
    assert(opts == Seq("[H]it", "[S]tand", "[Q]uit"))
  }

  test("formatMenuOptions: Double verfügbar bei 2 Karten und Budget") {
    val twoCards = Hand.empty
      .add(Card(Rank.Seven, Suits.Heart))
      .add(Card(Rank.Eight, Suits.Diamonds))
    val gs = mkState(
      playerHands = List(twoCards),
      dealer      = Hand.empty,
      bets        = List(5.0),
      budget      = 10.0
    )
    val opts = TuiView.formatMenuOptions(gs, gs.budget)
    assert(opts == Seq("[H]it", "[S]tand", "[Q]uit", "[D]ouble"))
  }

  test("formatMenuOptions: Split verfügbar bei Paar und Budget") {
    val pairHand = Hand.empty
      .add(Card(Rank.Nine, Suits.Spades))
      .add(Card(Rank.Nine, Suits.Clubs))
    val gs = mkState(
      playerHands = List(pairHand),
      dealer      = Hand.empty,
      bets        = List(5.0),
      budget      = 10.0
    )
    val opts = TuiView.formatMenuOptions(gs, gs.budget)
    assert(opts == Seq("[H]it", "[S]tand", "[Q]uit", "[P]split"))
  }

  test("formatMenuOptions: Double und Split gemeinsam") {
    val pairHand = Hand.empty
      .add(Card(Rank.King, Suits.Heart))
      .add(Card(Rank.King, Suits.Diamonds))
    val gs = mkState(
      playerHands = List(pairHand),
      dealer      = Hand.empty,
      bets        = List(10.0),
      budget      = 20.0
    )
    val opts = TuiView.formatMenuOptions(gs, gs.budget)
    assert(opts == Seq("[H]it", "[S]tand", "[Q]uit", "[D]ouble", "[P]split"))
  }
}
