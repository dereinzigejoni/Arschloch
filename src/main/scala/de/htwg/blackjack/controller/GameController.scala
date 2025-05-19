package de.htwg.blackjack.controller
import de.htwg.blackjack.command.*
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.model.*
import de.htwg.blackjack.state.GamePhases
import de.htwg.blackjack.state.GamePhases.{DealerBustPhase, DealerTurn, PlayerTurn}
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.strategy.interfacE.DealerStrategy

import scala.compiletime.uninitialized
import scala.util.Try
class GameController(dealerStrat: DealerStrategy = new ConservativeDealer) {
  private var budget: Double     = 4000.0
  private var currentBet: Double = 0.0
  private var state: GameState  = uninitialized
  private var observers: List[GameObserver] = Nil
  private var history: List[(GameState, Double, Command)] = Nil
  private val invoker = new CommandInvoker(this)

  def setStateInternal(gs: GameState): Unit = {
    state = gs
    notifyObservers()
  }

  def addObserver(obs: GameObserver): Unit = observers ::= obs

  def removeObserver(obs: GameObserver): Unit = observers = observers.filterNot(_ == obs)

  private def notifyObservers(): Unit = observers.foreach(_.update(state))


  def getBudget: Double = budget
  def getState: GameState = state
  def setState(s: GameState): Unit = state = s
  def setBudget(b: Double): Unit = budget = b
  def execute(cmd: Command): Try[GameState] = {
    val oldState = state
    val oldBudget = state.budget
    cmd.execute(state).map { newState =>
      history = (oldState, oldBudget, cmd) :: history
      state = newState
      newState
    }
  }
  def placeBet(bet: Double): Unit = {
    if (bet <= 0) throw new IllegalArgumentException("Einsatz muss > 0 sein")
    if (bet > budget * 0.9)
      throw new IllegalArgumentException(f"Einsatz muss ≤ 90%% Deines Budgets sein (max: ${budget * 0.9}%.2f)")

    currentBet = bet
    budget    -= bet

    // State setzen _und_ Observer benachrichtigen
    setStateInternal(initRound(bet))
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
      budget = budget ,
      currentBet = bet
    )

  }
  def tryPlaceBet(bet: Double): Try[Unit] = Try(placeBet(bet))
  def playerHit(): Try[GameState] = invoker.execute(HitCommand)
  def playerStand(): Try[GameState] = invoker.execute(StandCommand)
  def playerDouble(): Try[GameState] = invoker.execute(DoubleCommand)
  def playerSplit(): Try[GameState] = invoker.execute(SplitCommand)
  def undo(): Option[GameState] = {
    val res = invoker.undo()
    res.foreach(_ => notifyObservers())
    res
  }
  def redo(): Option[GameState] = {
    val res = invoker.redo()
    res.foreach(_ => notifyObservers())
    res
  }
  private def dealerPlay(): Unit = state = state.phase.stand(state)
  def resolveBet(): Unit = {
    val newState = GamePhases.Payout(state).pay(state)
    state = newState
    budget = newState.budget // Controller-Budget auf den neuen Stand setzen
    notifyObservers()
  }

  def dealerHit(): Try[GameState] = Try {
    val (card, newDeck) = state.deck.draw()
    val newDealerHand = state.dealer.add(card)
    val newPhase = if (newDealerHand.value > 21) DealerBustPhase else DealerTurn
    val newState = state.copy(
      deck = newDeck,
      dealer = newDealerHand,
      phase = newPhase
    )
    state = newState
    notifyObservers()
    newState
  }
}
