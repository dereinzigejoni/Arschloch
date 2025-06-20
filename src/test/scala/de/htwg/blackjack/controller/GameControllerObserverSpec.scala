package de.htwg.blackjack.controller

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model._
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.state.GamePhases._
import scala.util.Try

class GameControllerObserverSpec extends AnyFunSuite {

  // Ein Test‐Observer, der zählt, wie oft update(gs) gerufen wird.
  private class CountingObs extends GameObserver {
    var count = 0
    var lastState: GameState = _
    override def update(gs: GameState): Unit = {
      count += 1
      lastState = gs
    }
  }

  // Helfer zum Erzeugen einfacher Karten/Hände/Decks
  private def card(rank: Rank, suit: Suits): Card = Card(rank, suit)
  private def handOf(cards: Card*): Hand = cards.foldLeft(Hand.empty)(_ add _)
  private def deckOf(cards: List[Card]): Deck = Deck(cards)

  // Controller mit konservativer Dealer‐Strategie
  private val controller = new GameController(new ConservativeDealer)

  test("Observer wird bei placeBet einmal benachrichtigt") {
    val obs = new CountingObs
    controller.addObserver(obs)
    // action
    controller.tryplaceBet(100.0).get
    // assert
    assert(obs.count == 1, "placeBet sollte genau eine Benachrichtigung auslösen")
    assert(obs.lastState.bets == List(100.0))
    controller.removeObserver(obs)
  }

  test("Observer wird bei playerHit benachrichtigt") {
    // Setup: State auf PlayerTurn mit einer Hand, die eine Karte ziehen kann
    val initial = GameState(
      deck        = deckOf(List(card(Rank.Ten, Suits.Heart))),
      playerHands = List(handOf(card(Rank.Five, Suits.Spades), card(Rank.Five, Suits.Diamonds))),
      dealer      = Hand.empty,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 10.0
    )
    controller.setState(initial)

    val obs = new CountingObs
    controller.addObserver(obs)
    controller.playerHit().get

    assert(obs.count == 1, "playerHit sollte genau eine Benachrichtigung auslösen")
    assert(obs.lastState.playerHands.head.cards.exists(_.rank == Rank.Ten))
    controller.removeObserver(obs)
  }

  test("Observer wird bei playerStand benachrichtigt") {
    // Setup: zwei Hände → beim ersten Stand nur activeHand++
    val st = GameState(
      deck        = deckOf(Nil),
      playerHands = List(handOf(card(Rank.Ten, Suits.Heart)), handOf(card(Rank.Nine, Suits.Spades))),
      dealer      = Hand.empty,
      bets        = List(10.0, 5.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 10.0
    )
    controller.setState(st)

    val obs = new CountingObs
    controller.addObserver(obs)
    controller.playerStand().get

    assert(obs.count == 1)
    assert(obs.lastState.activeHand == 1)
    controller.removeObserver(obs)
  }

  test("Observer wird bei playerDouble benachrichtigt") {
    val initial = GameState(
      deck        = deckOf(List(card(Rank.Ten, Suits.Spades))),
      playerHands = List(handOf(card(Rank.Six, Suits.Clubs), card(Rank.Seven, Suits.Diamonds))),
      dealer      = Hand.empty,
      bets        = List(20.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 20.0
    )
    controller.setState(initial)

    val obs = new CountingObs
    controller.addObserver(obs)
    controller.playerDouble().get

    assert(obs.count == 1)
    assert(obs.lastState.bets.head == 40.0)
    controller.removeObserver(obs)
  }

  test("Observer wird bei playerSplit benachrichtigt") {
    val c1    = card(Rank.Eight, Suits.Heart)
    val c2    = card(Rank.Eight, Suits.Spades)
    val drawA = card(Rank.Two, Suits.Clubs)
    val drawB = card(Rank.Three, Suits.Diamonds)
    val initial = GameState(
      deck        = deckOf(List(drawA, drawB)),
      playerHands = List(handOf(c1, c2)),
      dealer      = Hand.empty,
      bets        = List(15.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 15.0
    )
    controller.setState(initial)

    val obs = new CountingObs
    controller.addObserver(obs)
    controller.playerSplit().get

    assert(obs.count == 1)
    assert(obs.lastState.playerHands.size == 2)
    controller.removeObserver(obs)
  }

  test("Observer wird bei dealerHit benachrichtigt") {
    val initial = GameState(
      deck        = deckOf(List(card(Rank.Ten, Suits.Clubs))),
      playerHands = Nil,
      dealer      = Hand.empty,
      bets        = Nil,
      activeHand  = 0,
      phase       = DealerTurn,
      budget      = 0.0,
      currentBet  = 0.0
    )
    controller.setState(initial)

    val obs = new CountingObs
    controller.addObserver(obs)
    controller.dealerHit().get

    assert(obs.count == 1)
    assert(obs.lastState.dealer.cards.head.rank == Rank.Ten)
    controller.removeObserver(obs)
  }

  test("Observer wird bei resolveBet benachrichtigt") {
    // Setup: Simuliere eine beendete Runde mit DealerBustPhase, um resolveBet durchlaufen zu lassen
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(handOf(card(Rank.Ten, Suits.Spades))),
      dealer      = handOf(card(Rank.King, Suits.Heart), card(Rank.Six, Suits.Diamonds)), // 16
      bets        = List(10.0),
      activeHand  = 0,
      phase       = DealerBustPhase,
      budget      = 90.0,  // nach einem Bet von 10
      currentBet  = 10.0
    )
    controller.setState(gs)

    val obs = new CountingObs
    controller.addObserver(obs)
    controller.resolveBet()

    assert(obs.count == 1)
    // Budget sollte nach Payout und Push größer als vorher sein
    assert(controller.getBudget > 90.0)
    controller.removeObserver(obs)
  }

  test("removeObserver verhindert weitere Benachrichtigungen") {
    val obs = new CountingObs
    controller.addObserver(obs)
    controller.removeObserver(obs)
    controller.tryplaceBet(50.0).get
    assert(obs.count == 0, "Nach removeObserver darf kein update mehr kommen")
  }
}
