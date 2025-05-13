// src/test/scala/de/htwg/blackjack/controller/GameControllerSpec.scala
package de.htwg.blackjack.controller

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.*
import de.htwg.blackjack.state.GamePhase
import de.htwg.blackjack.state.GamePhases.*

import scala.util.{Failure, Success, Try}

class GameControllerSpec extends AnyFunSuite {

  // Hilfsklasse, um private Felder per Reflection zu setzen
  private class TestableController extends GameController {
    def setField(name: String, value: Any): Unit = {
      val f = classOf[GameController].getDeclaredField(name)
      f.setAccessible(true)
      f.set(this, value.asInstanceOf[AnyRef])
    }
    
  }

  /** Erzeugt einen GameState mit allen Parametern. */
  private def mkState(
                       deck:        Deck,
                       playerHands: List[Hand],
                       dealer:      Hand,
                       bets:        List[Double],
                       activeHand:  Int        = 0,
                       phase:       GamePhase  = PlayerTurn,
                       budget:      Double     = 100.0,
                       currentBet:  Double     = 0.0
                     ): GameState =
    GameState(
      deck        = deck,
      playerHands = playerHands,
      dealer      = dealer,
      bets        = bets,
      activeHand  = activeHand,
      phase       = phase,
      budget      = budget,
      currentBet  = currentBet
    )

  // -- placeBet-Tests --

  test("placeBet: gültiger Einsatz reduziert Budget und setzt bets.head") {
    val ctl   = new GameController()
    val start = ctl.getBudget
    val bet   = start * 0.5
    ctl.placeBet(bet)
    assert(ctl.getBudget == start - bet)
    assert(ctl.getState.bets.head == bet)
  }

  test("placeBet: Einsatz ≤ 0 wirft IllegalArgumentException") {
    val ctl = new GameController()
    intercept[IllegalArgumentException](ctl.placeBet(0))
    intercept[IllegalArgumentException](ctl.placeBet(-10))
  }

  test("placeBet: Einsatz > 90% Budget wirft IllegalArgumentException") {
    val ctl    = new GameController()
    val tooHigh = ctl.getBudget * 0.91
    intercept[IllegalArgumentException](ctl.placeBet(tooHigh))
  }

  // -- resolveBet-Tests --

  test("resolveBet: normaler Sieg zahlt 2×") {
    val ctl = new TestableController
    ctl.setBudget(100.0)
    val p = Hand.empty
      .add(Card(Rank.King, Suits.Heart))
      .add(Card(Rank.Nine, Suits.Spades)) // 10+9=19
    val d = Hand.empty
      .add(Card(Rank.Ten, Suits.Diamonds))
      .add(Card(Rank.Eight, Suits.Clubs)) // 10+8=18
    val gs = mkState(Deck(Nil), List(p), d, List(20.0), phase = FinishedPhase)
    ctl.setState(gs)
    ctl.resolveBet()
    assert(ctl.getBudget == 100.0 + 20.0 * 2)
  }

  test("resolveBet: Natural Blackjack zahlt 2.7×") {
    val ctl = new TestableController
    ctl.setBudget(50.0)
    val p = Hand.empty
      .add(Card(Rank.Ace, Suits.Clubs))
      .add(Card(Rank.King, Suits.Spades)) // natural 21
    val d = Hand.empty
      .add(Card(Rank.Ten, Suits.Diamonds))
      .add(Card(Rank.Nine, Suits.Clubs))  // 19
    val gs = mkState(Deck(Nil), List(p), d, List(10.0), phase = FinishedPhase)
    ctl.setState(gs)
    ctl.resolveBet()
    assert(ctl.getBudget == 50.0 + 10.0 * 2.7)
  }

  test("resolveBet: DealerBust zahlt 2×") {
    val ctl = new TestableController
    ctl.setBudget(30.0)
    val p = Hand.empty
      .add(Card(Rank.Ten, Suits.Heart))
      .add(Card(Rank.Eight, Suits.Spades)) // 18
    val d = Hand.empty
      .add(Card(Rank.King, Suits.Diamonds))
      .add(Card(Rank.Queen, Suits.Clubs))
      .add(Card(Rank.Five, Suits.Heart))  // bust
    val gs = mkState(Deck(Nil), List(p), d, List(5.0), phase = DealerBustPhase)
    ctl.setState(gs)
    ctl.resolveBet()
    assert(ctl.getBudget == 30.0 + 5.0 * 2)
  }

  test("resolveBet: Push gibt Einsatz zurück") {
    val ctl = new TestableController
    ctl.setBudget(80.0)
    val p = Hand.empty
      .add(Card(Rank.Nine, Suits.Heart))
      .add(Card(Rank.Eight, Suits.Spades))   // 17
    val d = Hand.empty
      .add(Card(Rank.Ten, Suits.Diamonds))
      .add(Card(Rank.Seven, Suits.Clubs))    // 17
    val gs = mkState(Deck(Nil), List(p), d, List(20.0), phase = FinishedPhase)
    ctl.setState(gs)
    ctl.resolveBet()
    assert(ctl.getBudget == 80.0 + 20.0)
  }

  test("resolveBet: Verlieren belässt Budget") {
    val ctl = new TestableController
    ctl.setBudget(60.0)
    val p = Hand.empty
      .add(Card(Rank.Nine, Suits.Heart))
      .add(Card(Rank.Eight, Suits.Spades))   // 17
    val d = Hand.empty
      .add(Card(Rank.Ten, Suits.Diamonds))
      .add(Card(Rank.Eight, Suits.Clubs))    // 18
    val gs = mkState(Deck(Nil), List(p), d, List(30.0), phase = FinishedPhase)
    ctl.setState(gs)
    ctl.resolveBet()
    assert(ctl.getBudget == 60.0)
  }

  // -- Spielzüge und Undo --

  test("playerHit macht nichts, wenn nicht in PlayerTurn") {
    val ctl = new TestableController
    val gs  = mkState(Deck(Nil), List(Hand.empty), Hand.empty, List(0.0), phase = PlayerBustPhase)
    ctl.setState(gs)
    ctl.playerHit()
    assert(ctl.getState eq gs)
  }

  test("playerHit fügt Karte hinzu und bleibt in PlayerTurn") {
    val ctl = new TestableController
    val deck = Deck(List(Card(Rank.Two, Suits.Heart)))
    val p    = Hand.empty.add(Card(Rank.Five, Suits.Clubs))
    val gs   = mkState(deck, List(p), Hand.empty, List(0.0), phase = PlayerTurn)
    ctl.setState(gs)
    ctl.playerHit()
    val st = ctl.getState
    assert(st.playerHands.head.cards.size == 2)
    assert(st.phase == PlayerTurn)
  }

  test("playerHit führt zu Bust und PlayerBustPhase") {
    val ctl = new TestableController
    val deck = Deck(List(Card(Rank.King, Suits.Diamonds)))
    val p    = Hand.empty
      .add(Card(Rank.King, Suits.Heart))
      .add(Card(Rank.Two, Suits.Clubs))   // 12
    val gs   = mkState(deck, List(p), Hand.empty, List(0.0), phase = PlayerTurn)
    ctl.setState(gs)
    ctl.playerHit()
    val st = ctl.getState
    assert(st.playerHands.head.isBust)
    assert(st.phase == PlayerBustPhase)
  }

  test("playerStand löst DealerTurn aus") {
    val ctl = new TestableController
    val deck = Deck(List(Card(Rank.Seven, Suits.Clubs)))
    val p    = Hand.empty.add(Card(Rank.Five, Suits.Heart))
    val d    = Hand.empty.add(Card(Rank.Ten, Suits.Diamonds))
    val gs   = mkState(deck, List(p), d, List(0.0), phase = PlayerTurn)
    ctl.setState(gs)
    ctl.playerStand()
    val st = ctl.getState
    assert(st.phase == FinishedPhase)
    assert(st.dealer.value >= 17)
  }

  test("playerDouble verdoppelt Einsatz, zieht Karte und bucht richtig ab") {
    val ctl = new TestableController
    ctl.setBudget(100.0)
    ctl.placeBet(10.0)
    val before = ctl.getState.budget
    val initial = ctl.getState
    val card3 = Card(Rank.Three, Suits.Clubs)
    ctl.setState(initial.copy(deck = Deck(card3 :: initial.deck.cards)))
    ctl.playerDouble()
    val st = ctl.getState
    assert(st.bets.head == 20.0)
    assert(st.playerHands.head.cards.size == 3)
    assert(ctl.getBudget == before - 10.0)
  }

  test("playerSplit teilt Paar in zwei Hände und bucht Einsatz ab") {
    val ctl = new TestableController
    ctl.setBudget(100.0)
    ctl.placeBet(20.0)
    val pair = Hand.empty
      .add(Card(Rank.Five, Suits.Clubs))
      .add(Card(Rank.Five, Suits.Diamonds))
    val base = ctl.getState.copy(playerHands = List(pair), deck = Deck(List(Card(Rank.Two, Suits.Clubs), Card(Rank.Three, Suits.Spades))))
    ctl.setState(base)
    ctl.playerSplit()
    val st = ctl.getState
    assert(st.playerHands.size == 2)
    assert(st.bets == List(20.0, 20.0))
  }

  test("undo stellt letzten Schritt und Budget wieder her") {
    val ctl = new TestableController
    // initial: lege Karte ins Deck
    val deck0 = Deck(List(Card(Rank.Two, Suits.Heart)))
    val gs0 = mkState(deck0, List(Hand.empty), Hand.empty, List(0.0), phase = PlayerTurn)
    ctl.setState(gs0)

    // mache einen Hit
    ctl.playerHit() match {
      case Success(_) => ()
      case Failure(ex) => fail("Hit fehlgeschlagen", ex)
    }
    // nun 1 Karte gezogen
    assert(ctl.getState.playerHands.head.cards.nonEmpty)

    // undo -> zurück auf gs0
    ctl.undo() match {
      case Some(prev) =>
        assert(prev.playerHands.head.cards.isEmpty)
        assert(ctl.getBudget == gs0.budget)
      case None =>
        fail("undo sollte Some liefern")
    }
  }
}
