package de.htwg.blackjack.controller
import de.htwg.blackjack.command.*
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.model.*
import de.htwg.blackjack.state.GamePhases
import de.htwg.blackjack.state.GamePhases.{DealerBustPhase, DealerTurn, Payout, PlayerTurn}
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.strategy.interfacE.DealerStrategy
import de.htwg.blackjack.util.ObservableSync

import scala.compiletime.uninitialized
import scala.util.Try
class GameController(dealerStrat: DealerStrategy = new ConservativeDealer) extends IGameController with GameObserver {
  private var lastRoundWin: Double = 0.0
  private var budget: Double     = 4000.0
  private var currentBet: Double = 0.0
  private var state: GameState  = uninitialized
  private var observers: List[GameObserver] = Nil
  private var history: List[(GameState, Double, Command)] = Nil
  private val invoker = new CommandInvoker(this)
  override def getBudget: Double = budget
  override def getState: GameState = state
  override def tryplaceBet(amount: Double): Try[Unit] = Try(placeBet(amount))
  override def playerHit(): Try[GameState] = invoker.execute(HitCommand)
  override def playerStand(): Try[GameState] = invoker.execute(StandCommand)
  override def playerDouble(): Try[GameState] = invoker.execute(DoubleCommand)
  override def playerSplit(): Try[GameState] = invoker.execute(SplitCommand)
  override def addObserver(obs: GameObserver): Unit = observers ::= obs
  override def removeObserver(obs: GameObserver): Unit = observers = observers.filterNot(_ == obs)
  override def update(gs: GameState): Unit = notifyObservers()

  def setStateInternal(gs: GameState): Unit = {
    state = gs
    notifyObservers()
  }

  private val observableSync = new ObservableSync()

  private def notifyObservers(): Unit = {
    observers.foreach(_.update(state))
    observableSync.signalUpdate() // <<< condition.signalAll
  }
  def setState(s: GameState): Unit = state = s
  def setBudget(b: Double): Unit = budget = b
  def execute(cmd: Command): Try[GameState] = {
    val oldState  = state
    val oldBudget = state.budget
    cmd.execute(state).map { newState =>
      history = (oldState, oldBudget, cmd) :: history
      state   = newState

      // ← HIER hinzufügen:
      notifyObservers()

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

  override def undo(): Option[GameState] = {
    val res = invoker.undo()
    res.foreach(_ => notifyObservers())
    res
  }
  override def redo(): Option[GameState] = {
    val res = invoker.redo()
    res.foreach(_ => notifyObservers())
    res
  }
  private def dealerPlay(): Unit = state = state.phase.stand(state)

  /** Endrunde: Berechne Auszahlung, update Budget und State */
  override def resolveBet(): Unit = {
    val oldBudget = budget
    // Payout für Gewinne/Blackjack
    val paidState = Payout(state).pay(state)
    state = paidState
    budget = paidState.budget
    // Push: Einsatz zurückgeben
    val dealerValue = state.dealer.value
    state.playerHands.zip(state.bets).foreach { case (hand, bet) =>
      if (hand.value <= 21 && dealerValue <= 21 && hand.value == dealerValue) {
        budget += bet
      }
    }
    // Speichere runden-spezifischen Gewinn
    lastRoundWin = budget - oldBudget
    notifyObservers()
  }

  /** Liefert den Reingewinn der zuletzt abgeschlossenen Runde */
  def getLastRoundWin: Double = lastRoundWin


  override def dealerHit(): Try[GameState] = Try {
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
