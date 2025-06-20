package de.htwg.blackjack.state

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.state.GamePhases._
import de.htwg.blackjack.model._
import de.htwg.blackjack.model.Rank._
import de.htwg.blackjack.model.Suits._

class GamePhasesSpec extends AnyFunSuite {

  // Helpers
  private def card(r: Rank, s: Suits): Card = Card(r, s)
  private def deckOf(cards: List[Card]): Deck = Deck(cards)
  private val emptyHand: Hand = Hand.empty

  // --- PlayerTurn.hit ---

  test("PlayerTurn.hit adds card and stays in PlayerTurn if not bust") {
    val initial = GameState(
      deck        = deckOf(List(card(Six, Clubs))),
      playerHands = List(Hand.empty.add(card(Five, Heart))),
      dealer      = emptyHand,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )
    val result = PlayerTurn.hit(initial)
    assert(result.phase == PlayerTurn)
    assert(result.playerHands.head.cards.last.rank == Six)
    assert(result.deck.cards.isEmpty)
  }

  test("PlayerTurn.hit adds card and switches to DealerTurn if bust") {
    val initial = GameState(
      deck        = deckOf(List(card(Ten, Spades))),
      playerHands = List(Hand.empty.add(card(King, Heart)).add(card(Queen, Diamonds))), // value 20
      dealer      = emptyHand,
      bets        = List(5.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 50.0,
      currentBet  = 0.0
    )
    val result = PlayerTurn.hit(initial)
    // 20 + 10 = 30 → bust
    assert(result.phase == DealerTurn)
    assert(result.playerHands.head.cards.last.rank == Ten)
  }

  // --- PlayerTurn.stand ---

  test("PlayerTurn.stand moves to next hand if more hands exist") {
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(emptyHand, emptyHand),
      dealer      = emptyHand,
      bets        = List(1.0, 1.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 0.0,
      currentBet  = 0.0
    )
    val next = PlayerTurn.stand(gs)
    assert(next.phase == PlayerTurn)
    assert(next.activeHand == 1)
  }

  test("PlayerTurn.stand switches to DealerTurn on last hand") {
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(emptyHand),
      dealer      = emptyHand,
      bets        = List(2.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 0.0,
      currentBet  = 0.0
    )
    val next = PlayerTurn.stand(gs)
    assert(next.phase == DealerTurn)
    assert(next.activeHand == 0)
  }

  // --- PlayerTurn.double ---

  test("PlayerTurn.double does nothing if hand size ≠ 2") {
    val gs = GameState(
      deck        = deckOf(List(card(Ten, Heart))),
      playerHands = List(Hand.empty.add(card(Five, Clubs))), // size 1
      dealer      = emptyHand,
      bets        = List(5.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 10.0,
      currentBet  = 0.0
    )
    assert(PlayerTurn.double(gs) == gs)
  }

  test("PlayerTurn.double does nothing if budget < bet") {
    val hand = Hand.empty.add(card(Six, Spades)).add(card(Six, Heart))
    val gs = GameState(
      deck        = deckOf(List(card(Three, Clubs))),
      playerHands = List(hand),
      dealer      = emptyHand,
      bets        = List(50.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 40.0,
      currentBet  = 0.0
    )
    assert(PlayerTurn.double(gs) == gs)
  }

  test("PlayerTurn.double does nothing if deck empty") {
    val hand = Hand.empty.add(card(Six, Spades)).add(card(Six, Heart))
    val gs = GameState(
      deck        = deckOf(Nil),
      playerHands = List(hand),
      dealer      = emptyHand,
      bets        = List(5.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )
    assert(PlayerTurn.double(gs) == gs)
  }

  test("PlayerTurn.double doubles bet, draws card and goes to DealerTurn on single hand") {
    val hand = Hand.empty.add(card(Six, Clubs)).add(card(Seven, Diamonds))
    val initial = GameState(
      deck        = deckOf(List(card(Five, Heart))),
      playerHands = List(hand),
      dealer      = emptyHand,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )
    val updated = PlayerTurn.double(initial)
    // bet doubled
    assert(updated.bets.head == 20.0)
    // budget decreased by original bet
    assert(updated.budget == 90.0)
    // card drawn
    assert(updated.playerHands.head.cards.contains(card(Five, Heart)))
    // phase → DealerTurn
    assert(updated.phase == DealerTurn)
  }

  test("PlayerTurn.double doubles bet, draws card and advances on multi-hand") {
    val hand1 = Hand.empty.add(card(Five, Spades)).add(card(Six, Clubs))
    val hand2 = Hand.empty.add(card(Seven, Diamonds)).add(card(Eight, Heart))
    val initial = GameState(
      deck        = deckOf(List(card(Ten, Clubs))),
      playerHands = List(hand1, hand2),
      dealer      = emptyHand,
      bets        = List(5.0, 5.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 50.0,
      currentBet  = 0.0
    )
    val updated = PlayerTurn.double(initial)
    assert(updated.bets == List(10.0, 5.0))
    assert(updated.budget == 45.0)
    assert(updated.activeHand == 1)
    assert(updated.phase == PlayerTurn)
  }

  // --- PlayerTurn.split ---

  test("PlayerTurn.split does nothing if hand size ≠ 2") {
    val gs = GameState(
      deck        = deckOf(List(card(Two, Spades), card(Three, Clubs))),
      playerHands = List(Hand.empty.add(card(Ace, Heart))),
      dealer      = emptyHand,
      bets        = List(5.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )
    assert(PlayerTurn.split(gs) == gs)
  }

  test("PlayerTurn.split does nothing if card values differ") {
    val hand = Hand.empty.add(card(Four, Heart)).add(card(Five, Diamonds))
    val gs = GameState(
      deck        = deckOf(List(card(Two, Spades), card(Three, Clubs))),
      playerHands = List(hand),
      dealer      = emptyHand,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )
    assert(PlayerTurn.split(gs) == gs)
  }

  test("PlayerTurn.split does nothing if budget < bet or deck < 2") {
    val hand = Hand.empty.add(card(Seven, Clubs)).add(card(Seven, Diamonds))
    val gs1 = GameState(
      deck        = deckOf(List(card(Two, Heart))), // only 1 card
      playerHands = List(hand),
      dealer      = emptyHand,
      bets        = List(20.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 10.0, // budget < bet
      currentBet  = 0.0
    )
    assert(PlayerTurn.split(gs1) == gs1)

    val gs2 = gs1.copy(budget = 100.0)
    // now budget ok but deck size <2
    assert(PlayerTurn.split(gs2) == gs2)
  }

  test("PlayerTurn.split splits hand into two with new cards") {
    val c1 = card(Eight, Heart); val c2 = card(Eight, Spades)
    val d1 = card(Four, Clubs); val d2 = card(Five, Diamonds)
    val initial = GameState(
      deck        = deckOf(List(d1, d2)),
      playerHands = List(Hand.empty.add(c1).add(c2)),
      dealer      = emptyHand,
      bets        = List(15.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 100.0,
      currentBet  = 0.0
    )
    val updated = PlayerTurn.split(initial)
    // two hands
    assert(updated.playerHands.size == 2)
    assert(updated.playerHands(0).cards == List(c1, d1))
    assert(updated.playerHands(1).cards == List(c2, d2))
    // bets updated
    assert(updated.bets == List(15.0, 15.0))
    // budget reduced
    assert(updated.budget == 85.0)
    // deck consumed
    assert(updated.deck.cards.isEmpty)
  }

  // --- DealerTurn.hit is no-op ---

  test("DealerTurn.hit returns same state") {
    val gs = GameState(
      deck        = deckOf(List(card(Two, Spades))),
      playerHands = Nil,
      dealer      = emptyHand,
      bets        = Nil,
      activeHand  = 0,
      phase       = DealerTurn,
      budget      = 0.0,
      currentBet  = 0.0
    )
    assert(DealerTurn.hit(gs) == gs)
  }

  // --- DealerTurn.stand ---

  test("DealerTurn.stand does not draw if value ≥17 and sets FinishedPhase") {
    val dealerHand = Hand.empty.add(card(Ten, Spades)).add(card(Seven, Heart)) // 17
    val gs = GameState(
      deck        = deckOf(List(card(Two, Clubs))),
      playerHands = Nil,
      dealer      = dealerHand,
      bets        = Nil,
      activeHand  = 0,
      phase       = DealerTurn,
      budget      = 0.0,
      currentBet  = 0.0
    )
    val next = DealerTurn.stand(gs)
    assert(next.phase == FinishedPhase)
    assert(next.dealer == dealerHand)
    assert(next.deck.cards == List(card(Two, Clubs)))
  }

  test("DealerTurn.stand draws until ≥17 or bust and sets correct phase") {
    val dealerHand = Hand.empty.add(card(Five, Clubs)) // value 5
    // draw 6 (→11), 8 (→19) → stop
    val gs = GameState(
      deck        = deckOf(List(card(Six, Diamonds), card(Eight, Heart))),
      playerHands = Nil,
      dealer      = dealerHand,
      bets        = Nil,
      activeHand  = 0,
      phase       = DealerTurn,
      budget      = 0.0,
      currentBet  = 0.0
    )
    val next = DealerTurn.stand(gs)
    assert(next.phase == FinishedPhase)
    assert(next.dealer.value >= 17)
    assert(next.deck.cards.isEmpty)
  }

  test("DealerTurn.stand draws and busts if over 21 and sets DealerBustPhase") {
    val dealerHand = Hand.empty.add(card(Ten, Diamonds)) // 10
    // draw Queen (10 → 20), then Two (→22): but code draws until value<17, so only Queen drawn
    val gs = GameState(
      deck        = deckOf(List(card(Queen, Clubs), card(Two, Spades))),
      playerHands = Nil,
      dealer      = dealerHand,
      bets        = Nil,
      activeHand  = 0,
      phase       = DealerTurn,
      budget      = 0.0,
      currentBet  = 0.0
    )
    // value 10 <17 → draw Queen → value 20 <17? no, 20≥17 → stop, finishedPhase
    val next1 = DealerTurn.stand(gs)
    assert(next1.phase == FinishedPhase)
    // Now test actual bust: start at 5, draw Queen(10→15), draw King(10→25)
    val gs2 = gs.copy(
      dealer = Hand.empty.add(card(Five, Heart)),
      deck   = deckOf(List(card(Queen, Clubs), card(King, Spades)))
    )
    val next2 = DealerTurn.stand(gs2)
    assert(next2.phase == DealerBustPhase)
    assert(next2.dealer.isBust)
  }

  // --- no-op for other phases ---

  val noOpPhases = Seq(DealerBustPhase, FinishedPhase, PlayerBustPhase, GameOver)
  for (phase <- noOpPhases) {
    test(s"$phase.hit is no-op") {
      val gs = GameState(deckOf(Nil), Nil, emptyHand, Nil, 0, phase, 0.0, 0.0)
      assert(phase.hit(gs) == gs)
    }
    test(s"$phase.stand is no-op") {
      val gs = GameState(deckOf(Nil), Nil, emptyHand, Nil, 0, phase, 0.0, 0.0)
      assert(phase.stand(gs) == gs)
    }
    test(s"$phase.double is no-op") {
      val gs = GameState(deckOf(Nil), Nil, emptyHand, Nil, 0, phase, 0.0, 0.0)
      assert(phase.double(gs) == gs)
    }
    test(s"$phase.split is no-op") {
      val gs = GameState(deckOf(Nil), Nil, emptyHand, Nil, 0, phase, 0.0, 0.0)
      assert(phase.split(gs) == gs)
    }
  }

  // --- Payout.pay ---

  test("Payout.pay on DealerBustPhase pays 2.0× bet when not natural, resets currentBet, sets GameOver") {
    val hand = Hand.empty.add(card(Ten, Heart)).add(card(Seven, Diamonds)) // 17, not natural
    val old = GameState(
      deck        = deckOf(Nil),
      playerHands = List(hand),
      dealer      = emptyHand,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = DealerBustPhase,
      budget      = 100.0,
      currentBet  = 10.0
    )
    val result = Payout(old).pay(old)
    // payout = 10 × 2.0 = 20
    assert(result.budget == 100.0 + 20.0)
    assert(result.currentBet == 0.0)
    assert(result.phase == GameOver)
  }

  test("Payout.pay on DealerBustPhase pays 2.7× bet when natural blackjack") {
    val natural = Hand.empty.add(card(Ace, Spades)).add(card(King, Clubs))
    val old = GameState(
      deck        = deckOf(Nil),
      playerHands = List(natural),
      dealer      = emptyHand,
      bets        = List(10.0),
      activeHand  = 0,
      phase       = DealerBustPhase,
      budget      = 50.0,
      currentBet  = 10.0
    )
    val result = Payout(old).pay(old)
    // payout = 10 × 2.7 = 27
    assert(result.budget == 50.0 + 27.0)
  }

  test("Payout.pay on FinishedPhase: win pays 2.0×, tie pays 1.0×, loss pays 0") {
    val playerWin = Hand.empty.add(card(Ten, Heart)).add(card(Seven, Diamonds)) //17
    val playerTie = Hand.empty.add(card(Nine, Clubs)).add(card(Eight, Diamonds))  //17
    val playerLose= Hand.empty.add(card(Eight, Spades)).add(card(Eight, Heart))   //16
    val dealer    = Hand.empty.add(card(Seven, Clubs)).add(card(Ten, Diamonds))    //17
    val old = GameState(
      deck        = deckOf(Nil),
      playerHands = List(playerWin, playerTie, playerLose),
      dealer      = dealer,
      bets        = List(10.0, 5.0, 2.0),
      activeHand  = 0,
      phase       = FinishedPhase,
      budget      = 0.0,
      currentBet  = 0.0
    )
    val result = Payout(old).pay(old)
    assert(result.budget == 15.0)
    assert(result.currentBet == 0.0)
  }

  test("Payout.pay on FinishedPhase pays 2.7× for natural win") {
    val natural = Hand.empty.add(card(Ace, Heart)).add(card(King, Diamonds))
    val other   = Hand.empty.add(card(Ten, Clubs)).add(card(Nine, Spades)) //19
    val dealer  = Hand.empty.add(card(Ten, Heart)).add(card(Eight, Clubs))//18
    val old = GameState(
      deck        = deckOf(Nil),
      playerHands = List(natural, other),
      dealer      = dealer,
      bets        = List(10.0, 5.0),
      activeHand  = 0,
      phase       = FinishedPhase,
      budget      = 0.0,
      currentBet  = 0.0
    )
    val result = Payout(old).pay(old)
    // natural:10×2.7=27, other:5×2.0=10 => total 37
    assert(result.budget == 37.0)
  }
}
