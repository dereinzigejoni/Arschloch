package de.htwg.blackjack.controller
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.model.*
import de.htwg.blackjack.state.GamePhases
import de.htwg.blackjack.state.GamePhases.PlayerTurn
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.strategy.interfacE.DealerStrategy

import scala.compiletime.uninitialized
class GameController(dealerStrat: DealerStrategy = new ConservativeDealer) {
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
    val deck0 = StandardDeckFactory.newDeck
    // Ziehe p1, d1, p2, d2 …
    val (p1, deck1) = deck0.draw()
    val (d1, deck2) = deck1.draw()
    val (p2, deck3) = deck2.draw()
    val (d2, deck4) = deck3.draw()
    val playerHand = Hand.empty.add(p1).add(p2)
    val dealerHand = Hand.empty.add(d1).add(d2)
    GameState(
      deck = deck4,
      playerHands = List(playerHand),
      dealer = dealerHand,
      bets = List(bet),
      activeHand = 0,
      phase = PlayerTurn,
      budget = budget - bet,
      currentBet = bet
    )

  }

  def playerHit(): Unit = state = state.phase.hit(state)
  def playerDouble(): Unit = state = state.phase.double(state)
  def playerSplit(): Unit = state = state.phase.split(state)
  def playerStand(): Unit = state = state.phase.stand(state)

  private def dealerPlay(): Unit = state = state.phase.stand(state)
  def resolveBet(): Unit = state = GamePhases.Payout(state).pay(state)

  //private def isNatural(hand: Hand): Boolean = hand.cards.size == 2 && hand.value == 21
}
