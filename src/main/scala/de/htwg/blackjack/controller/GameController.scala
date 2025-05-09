package de.htwg.blackjack.controller
import de.htwg.blackjack.model._
import scala.compiletime.uninitialized
class GameController {
  private var budget: Double     = 4000.0
  private var currentBet: Double = 0.0
  private var state: GameState  = uninitialized
  def getBudget: Double = budget
  def getState: GameState = state
  def placeBet(bet: Double): Unit = {
    if (bet <= 0) throw new IllegalArgumentException("Einsatz muss > 0 sein")
    if (bet > budget * 0.9) throw new IllegalArgumentException(f"einsatz muss <= 90%% deines Budgets (max:${budget * 0.9}%.2f sein")
    currentBet = bet
    budget -= bet
    state = initRound(bet)
  }
  private def initRound(bet: Double): GameState = {
    val deck0 = Deck.fresh()
    // Ziehe p1, d1, p2, d2 …
    val (p1, deck1) = deck0.draw()
    val (d1, deck2) = deck1.draw()
    val (p2, deck3) = deck2.draw()
    val (d2, deck4) = deck3.draw()
    val playerHand = Hand.empty.add(p1).add(p2)
    val dealerHand = Hand.empty.add(d1).add(d2)
    GameState(
      deck       = deck4,
      playerHands= List(playerHand),
      dealer     = dealerHand,
      bets       = List(bet),
      activeHand = 0,
      status     = InProgress
    )
  }
  def playerHit(): Unit = {
    if (state.status == InProgress) {
      val (card, d2) = state.deck.draw()
      val idx        = state.activeHand
      val newHand    = state.playerHands(idx).add(card)
      val newStatus  = if (newHand.isBust) PlayerBust else InProgress
      state = state.copy(deck=d2,playerHands = state.playerHands.updated(idx,newHand),status = newStatus)
    }
  }
  def playerDouble(): Unit = {
    val idx = state.activeHand
    val bet = state.bets(idx)
    if (state.playerHands(idx).cards.size == 2 && budget >= bet) {
      budget -= bet
      val newBets = state.bets.updated(idx, bet * 2)
      val (card, d2) = state.deck.draw()
      val newHand    = state.playerHands(idx).add(card)
      state = state.copy(
        deck        = d2,
        playerHands = state.playerHands.updated(idx, newHand),
        bets        = newBets
      )
      nextOrDealer()
    }
  }
  def playerSplit(): Unit = {
    val idx = state.activeHand
    val hand = state.playerHands(idx)
    if (hand.cards.size == 2 && hand.cards.head.rank == hand.cards(1).rank) {
      val bet = state.bets(idx)
      require(budget >= bet, "Nicht genug Budget zum Splitten")
      budget -= bet
      val (c1 :: c2 :: Nil) = hand.cards
      val (cardA, d2) = state.deck.draw()
      val (cardB, d3) = d2.draw()
      val h1 = Hand.empty.add(c1).add(cardA)
      val h2 = Hand.empty.add(c2).add(cardB)
      val newHands = state.playerHands.patch(idx, Seq(h1, h2), 1)
      val newBets  = state.bets.patch(idx, Seq(bet, bet), 1)
      state = state.copy(
        deck        = d3,
        playerHands = newHands,
        bets        = newBets
      )
    }
  }
  def playerStand(): Unit = nextOrDealer()
  private def nextOrDealer(): Unit = {
    val next = state.activeHand + 1
    if (next < state.playerHands.size) {
      state = state.copy(activeHand = next)
    } else {
      dealerPlay()
    }
  }
  private def dealerPlay(): Unit = {
    var deck2 = state.deck
    var dHand = state.dealer
    while (dHand.value < 17) {
      val (c, nd) = deck2.draw()
      dHand = dHand.add(c)
      deck2 = nd
    }
    val st = if (dHand.isBust) DealerBust else Finished
    state = state.copy(deck = deck2, dealer = dHand, status = st)
  }
  def resolveBet(): Unit = {
    val finalState = state
    val payouts = finalState.playerHands.zip(finalState.bets).map {
      case (hand, bet) =>
        finalState.status match {
          case DealerBust => if (isNatural(hand)) 2.7 * bet else 2.0 * bet
          case Finished if hand.value > finalState.dealer.value => if (isNatural(hand)) 2.7 * bet else 2.0 * bet
          case Finished if hand.value == finalState.dealer.value => 1.0 * bet
          case _ => 0.0
        }
    }
    budget += payouts.sum
    currentBet = 0.0
  }
  private def isNatural(hand: Hand): Boolean = hand.cards.size == 2 && hand.value == 21
}
