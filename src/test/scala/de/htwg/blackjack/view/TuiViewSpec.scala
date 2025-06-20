package de.htwg.blackjack.view

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.view.TuiView
import de.htwg.blackjack.controller.SharedGameController
import de.htwg.blackjack.controller.GameController
import de.htwg.blackjack.model.{GameState, Deck, Hand, Card, Rank, Suits}
import de.htwg.blackjack.state.GamePhases._
import scala.util.{Left, Right}

class TuiViewSpec extends AnyFunSuite {

  // Helpers for cards, hands, decks, and states
  private def card(r: Rank, s: Suits): Card = Card(r, s)
  private def handOf(cards: Card*): Hand = cards.foldLeft(Hand.empty)(_ add _)
  private def deckOf(cards: List[Card]): Deck = Deck(cards)

  // Use the shared controller and view under test
  private val controller = SharedGameController.instance.asInstanceOf[GameController]
  private val view       = new TuiView(null) // constructor parameter unused

  /** Utility to set the controller state for render tests */
  private def setState(gs: GameState): Unit =
    controller.setState(gs)

  test("parseBetInput recognizes QUIT input") {
    assert(view.parseBetInput("H", 100.0) == Left("QUIT"))
    assert(view.parseBetInput("h", 50.0)  == Left("QUIT"))
  }

  test("parseBetInput rejects non-numeric input") {
    assert(view.parseBetInput("foo", 100.0) == Left("Bitte eine gültige Zahl eingeben."))
  }

  test("parseBetInput rejects zero or negative bets") {
    assert(view.parseBetInput("0", 100.0)  == Left("Einsatz muss > 0 sein."))
    assert(view.parseBetInput("-5", 100.0) == Left("Einsatz muss > 0 sein."))
  }

  test("parseBetInput rejects bets over 90% of budget") {
    val budget = 200.0
    val tooBig = (budget * 0.9) + 0.01
    val formattedMax = f"€${budget * 0.9}%.2f"
    val err = view.parseBetInput(f"$tooBig%.2f", budget)
    assert(err.isLeft)
    assert(err.left.get.contains(s"Bitte eine gültige Zahl eingeben"))
  }

  test("parseBetInput accepts valid bet") {
    assert(view.parseBetInput("42.50", 100.0) == Right(42.50))
  }

  test("formatMenuOptions shows only base options when no double or split possible") {
    val hand = handOf(card(Rank.Five, Suits.Heart))
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(hand),
      dealer      = hand,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 5.0,
      currentBet  = 10.0
    )
    val opts = view.formatMenuOptions(gs, gs.budget)
    assert(opts == Seq("[H]it", "[S]tand", "[U]ndo", "[R]edo", "[Q]uit"))
  }

  test("formatMenuOptions includes [D]ouble when exactly two cards and enough budget") {
    val hand = handOf(card(Rank.Six, Suits.Clubs), card(Rank.Seven, Suits.Diamonds))
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(hand),
      dealer      = hand,
      bets        = List(20.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 50.0,
      currentBet  = 20.0
    )
    val opts = view.formatMenuOptions(gs, gs.budget)
    assert(opts == Seq("[H]it", "[S]tand", "[U]ndo", "[R]edo", "[Q]uit", "[D]ouble"))
  }

  test("formatMenuOptions includes [P]split when two cards of equal value and enough budget") {
    val hand = handOf(card(Rank.Eight, Suits.Heart), card(Rank.Eight, Suits.Spades))
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(hand),
      dealer      = hand,
      bets        = List(15.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 30.0,
      currentBet  = 15.0
    )
    val opts = view.formatMenuOptions(gs, gs.budget)
    assert(opts == Seq("[H]it", "[S]tand", "[U]ndo", "[R]edo", "[Q]uit", "[D]ouble", "[P]split"))
  }

  test("formatMenuOptions omits double and split if budget insufficient") {
    val hand = handOf(card(Rank.Nine, Suits.Diamonds), card(Rank.Nine, Suits.Clubs))
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(hand),
      dealer      = hand,
      bets        = List(20.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 10.0,
      currentBet  = 20.0
    )
    val opts = view.formatMenuOptions(gs, gs.budget)
    assert(opts == Seq("[H]it", "[S]tand", "[U]ndo", "[R]edo", "[Q]uit"))
  }

  test("renderPartial returns the expected lines for PlayerTurn") {
    // Dealer shows first card only
    val dealerHand = handOf(card(Rank.Ten, Suits.Spades), card(Rank.Five, Suits.Clubs))
    val playerHand = handOf(card(Rank.Four, Suits.Heart), card(Rank.Six, Suits.Diamonds))
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(playerHand),
      dealer      = dealerHand,
      bets        = List(5.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 5.0
    )
    setState(gs)
    val lines = view.renderPartial()
    // first non-empty border line is all '='
    assert(lines(1).forall(_ == '='))
    // header contains "DEINE HAND"
    assert(lines(2).contains("DEINE HAND"))
    // dealer line shows first card and masked second
    assert(lines(4).contains(s"Dealer: ${dealerHand.cards.head} [???]"))
    // player line shows cards and value
    assert(lines(5).contains("Spieler (1/1):"))
    assert(lines(5).contains("(Wert: 10)"))  // 4+6=10
    // bottom border is all '='
    assert(lines.last.forall(_ == '='))
  }

  test("renderFull returns expected lines and results for various outcomes") {
    // Scenario: player bust, dealer not bust
    val pBust = handOf(card(Rank.King, Suits.Heart), card(Rank.Queen, Suits.Diamonds), card(Rank.Two, Suits.Clubs)) // 10+10+2=22
    val dealer = handOf(card(Rank.Seven, Suits.Clubs), card(Rank.Nine, Suits.Spades)) // 16
    val gs1 = GameState(
      deck        = deckOf(Nil),
      playerHands = List(pBust),
      dealer      = dealer,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = FinishedPhase,
      budget      = 0.0,
      currentBet  = 0.0
    )
    setState(gs1)
    val full1 = view.renderFull()
    assert(full1.exists(_.contains("ERGEBNISSE DER RUNDE")))
    assert(full1.exists(_.contains("Hand 1:")))
    assert(full1.exists(_.contains("Du bist Bust – Dealer gewinnt.")))

    // Scenario: dealer bust, player wins
    val pWin  = handOf(card(Rank.Ten, Suits.Clubs), card(Rank.Seven, Suits.Heart)) // 17
    val dBust = handOf(card(Rank.King, Suits.Clubs), card(Rank.Queen, Suits.Spades), card(Rank.Two, Suits.Heart)) // 22
    val gs2 = gs1.copy(
      playerHands = List(pWin),
      dealer      = dBust
    )
    setState(gs2)
    val full2 = view.renderFull()
    assert(full2.exists(_.contains("Dealer ist Bust – Du gewinnst!")))

    // Scenario: push (tie)
    val pTie = handOf(card(Rank.Eight, Suits.Clubs), card(Rank.Nine, Suits.Diamonds)) // 17
    val dTie = handOf(card(Rank.Ten, Suits.Heart), card(Rank.Seven, Suits.Spades))   // 17
    val gs3 = gs1.copy(
      playerHands = List(pTie),
      dealer      = dTie
    )
    setState(gs3)
    val full3 = view.renderFull()
    assert(full3.exists(_.contains("Push – unentschieden")))
  }
}
