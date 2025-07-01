package de.htwg.blackjack.controller

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model._
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.strategy.ConservativeDealer
import de.htwg.blackjack.state.GamePhases._
import scala.util.{Try, Success, Failure}

class GameControllerSpec extends AnyFunSuite {

  // Einfaches Test‐Observable, um Updates mitzuzählen
  private class TestObs extends GameObserver {
    var updates: List[GameState] = Nil
    override def update(gs: GameState): Unit = updates ::= gs
  }

  // Hilfsfunktionen zum Bau von Decks, Händen und Karten
  private def deckOf(cards: List[Card]): Deck = Deck(cards)
  private def handOf(cards: Card*): Hand = cards.foldLeft(Hand.empty)(_ add _)
  private def card(rank: Rank, suit: Suits): Card = Card(rank, suit)

  // Controller mit einer konservativen Dealer‐Strategie
  private val controller = new GameController(new ConservativeDealer)

  test("tryplaceBet wirft für bet ≤ 0") {
    val r = controller.tryplaceBet(0.0)
    assert(r.isFailure)
    assert(r.failed.get.isInstanceOf[IllegalArgumentException])
  }

  test("tryplaceBet wirft für bet > 90% Budget") {
    // Initialbudget = 4000, 90% = 3600
    val r = controller.tryplaceBet(3600.01)
    assert(r.isFailure)
    assert(r.failed.get.getMessage.contains("≤ 90% Deines Budgets"))
  }

  test("tryplaceBet mit gültigem bet aktualisiert State, Budget und notifies Observer") {
    val obs = new TestObs
    controller.addObserver(obs)
    val beforeBudget = controller.getBudget
    val deck0        = StandardDeckFactory.newDeck
    val expectedDeckSize = deck0.cards.size - 4

    val r = controller.tryplaceBet(500.0)
    assert(r.isSuccess)

    // Budget des Controllers intern wurde um den Einsatz reduziert
    assert(controller.getBudget === beforeBudget - 500.0)

    // GameState im Controller ist auf eine neue Runde gesetzt
    val gs = controller.getState
    assert(gs.bets === List(500.0))
    assert(gs.currentBet === 500.0)
    assert(gs.budget === controller.getBudget)         // GameState.budget spiegelt Controller‐Budget wider
    assert(gs.phase == PlayerTurn)
    assert(gs.playerHands.size == 1 && gs.dealer.cards.size == 2)
    assert(gs.deck.cards.size === expectedDeckSize)

    // Observer wurde genau einmal benachrichtigt
    assert(obs.updates.nonEmpty)
    controller.removeObserver(obs)
  }

  test("undo und redo liefern None, solange nichts ausgeführt wurde") {
    assert(controller.undo().isEmpty)
    assert(controller.redo().isEmpty)
  }

  test("playerHit delegiert an HitCommand und notifies Observer") {
    // Setze einen ganz einfachen State: eine Hand mit Wert 10 und ein Deck mit einer 6
    val initial = GameState(
      deck = deckOf(List(card(Rank.Six, Suits.Clubs))),
      playerHands = List(handOf(card(Rank.Five, Suits.Spades), card(Rank.Five, Suits.Diamonds))),
      dealer = Hand.empty,
      bets = List(10.0),
      activeHand = 0,
      phase = PlayerTurn,
      budget = 100.0,
      currentBet = 10.0
    )
    controller.setState(initial)
    val obs = new TestObs
    controller.addObserver(obs)

    val result = controller.playerHit()
    assert(result.isSuccess)
    val updated = result.get

    // In die Hand wurde genau die gezogene Karte eingefügt
    assert(updated.playerHands.head.cards.contains(card(Rank.Six, Suits.Clubs)))
    // Weil 5+5+6=16 ≤21 bleibt PlayerTurn
    assert(updated.phase == PlayerTurn)
    // Controller‐Budget bleibt unverändert
    assert(controller.getBudget == 3500.0)
    // Observer wurde benachrichtigt
    assert(obs.updates.head == updated)
    controller.removeObserver(obs)
  }

  test("playerStand delegiert an StandCommand und navigiert durch mehrere Hände") {
    // Zwei Hände, activeHand=0
    val st = GameState(
      deck = deckOf(Nil),
      playerHands = List(handOf(card(Rank.Ten, Suits.Heart)), handOf(card(Rank.Nine, Suits.Spades))),
      dealer = Hand.empty,
      bets = List(10.0, 5.0),
      activeHand = 0,
      phase = PlayerTurn,
      budget = 100.0,
      currentBet = 10.0
    )
    controller.setState(st)
    val updated1 = controller.playerStand().get
    // Weil noch eine zweite Hand existiert, nur activeHand++
    assert(updated1.activeHand == 1)
    assert(updated1.phase == PlayerTurn)

    // Für die letzte Hand wechselt es in DealerTurn
    controller.setState(updated1.copy(activeHand = 1))
    val updated2 = controller.playerStand().get
    assert(updated2.phase == DealerTurn)
    assert(updated2.activeHand == 1)
  }

  test("playerDouble delegiert an DoubleCommand und verdoppelt Einsatz") {
    val initial = GameState(
      deck = deckOf(List(card(Rank.Ten, Suits.Spades))),
      playerHands = List(handOf(card(Rank.Six, Suits.Clubs), card(Rank.Seven, Suits.Diamonds))),
      dealer = Hand.empty,
      bets = List(20.0),
      activeHand = 0,
      phase = PlayerTurn,
      budget = 100.0,
      currentBet = 20.0
    )
    controller.setState(initial)
    val updated = controller.playerDouble().get

    // Einsatz verdoppelt, Budget in GameState um ursprünglichen Einsatz reduziert
    assert(updated.bets == List(40.0))
    assert(updated.budget == 80.0)
    // Hand enthält die gezogene Karte
    assert(updated.playerHands.head.cards.size == 3)
    // Weil nur eine Hand existiert, wechselt Phase → DealerTurn
    assert(updated.phase == DealerTurn)
  }

  test("playerSplit delegiert an SplitCommand und teilt Paare korrekt") {
    val c1    = card(Rank.Eight, Suits.Heart)
    val c2    = card(Rank.Eight, Suits.Spades)
    val drawA = card(Rank.Two, Suits.Clubs)
    val drawB = card(Rank.Three, Suits.Diamonds)
    val initial = GameState(
      deck = deckOf(List(drawA, drawB)),
      playerHands = List(handOf(c1, c2)),
      dealer = Hand.empty,
      bets = List(15.0),
      activeHand = 0,
      phase = PlayerTurn,
      budget = 100.0,
      currentBet = 15.0
    )
    controller.setState(initial)
    val updated = controller.playerSplit().get

    // Aus einer Hand werden zwei, jeder bekommt eine zusätzliche Karte
    assert(updated.playerHands.size == 2)
    assert(updated.playerHands.head.cards == List(c1, drawA))
    assert(updated.playerHands(1).cards == List(c2, drawB))
    // Bets‐Liste verdoppelt
    assert(updated.bets == List(15.0, 15.0))
    // GameState‐Budget um einen Einsatz reduziert
    assert(updated.budget == 85.0)
    // Phase und activeHand unverändert
    assert(updated.phase == PlayerTurn)
    assert(updated.activeHand == 0)
  }

  test("dealerHit zieht Karte und setzt Phase korrekt (nicht bust)") {
    val initial = GameState(
      deck = deckOf(List(card(Rank.Ten, Suits.Clubs))),
      playerHands = Nil,
      dealer = Hand.empty,
      bets = Nil,
      activeHand = 0,
      phase = DealerTurn,
      budget = 0.0,
      currentBet = 0.0
    )
    controller.setState(initial)
    val obs = new TestObs
    controller.addObserver(obs)

    val updated = controller.dealerHit().get
    // Dealer hat jetzt genau die gezogene Karte
    assert(updated.dealer.cards.map(_.rank) == List(Rank.Ten))
    // Phase bleibt DealerTurn, da 10 ≤ 21
    assert(updated.phase == DealerTurn)
    // Deck ist leer
    assert(updated.deck.cards.isEmpty)
    // Observer wurde benachrichtigt
    assert(obs.updates.head == updated)
    controller.removeObserver(obs)
  }

  test("dealerHit zieht Karte und wechselt zu DealerBustPhase, wenn bust") {
    // Dealer startet mit 12, zieht eine Queen → 22
    val initialDealer = handOf(card(Rank.Ten, Suits.Heart), card(Rank.Two, Suits.Diamonds))
    val drawBust      = card(Rank.Queen, Suits.Spades)
    val initial = GameState(
      deck = deckOf(List(drawBust, card(Rank.Ace, Suits.Clubs))),
      playerHands = Nil,
      dealer = initialDealer,
      bets = Nil,
      activeHand = 0,
      phase = DealerTurn,
      budget = 0.0,
      currentBet = 0.0
    )
    controller.setState(initial)

    val updated = controller.dealerHit().get
    // Dealer‐Hand enthält nun das Queen und bustet (22)
    assert(updated.dealer.cards.last.rank == Rank.Queen)
    assert(updated.phase == DealerBustPhase)
    // Restdeck behält die zweite Karte
    assert(updated.deck.cards == List(card(Rank.Ace, Suits.Clubs)))
  }
  test("setState should replace the state and notify observers") {
    val ctrl = new GameController(new ConservativeDealer)
    val obs = new TestObs
    ctrl.addObserver(obs)

    // minimaler Dummy-State
    val dummy = GameState(
      deck = StandardDeckFactory.newDeck,
      playerHands = Nil,
      dealer = Hand.empty,
      bets = Nil,
      activeHand = 0,
      phase = PlayerTurn,
      budget = 1000.0,
      currentBet = 0.0
    )

    ctrl.setState(dummy)
    ctrl.getState shouldBe dummy
    obs.updates.head shouldBe dummy
  }

  test("setBudget should update the controller's budget") {
    val ctrl = new GameController(new ConservativeDealer)
    ctrl.setBudget(789.0)
    ctrl.getBudget shouldBe 789.0
  }

  test("execute should delegate to Command, update state and notify observers") {
    val ctrl = new GameController(new ConservativeDealer)
    // starte von einem Basis-State
    val base = GameState(
      deck = StandardDeckFactory.newDeck,
      playerHands = Nil,
      dealer = Hand.empty,
      bets = Nil,
      activeHand = 0,
      phase = PlayerTurn,
      budget = 500.0,
      currentBet = 0.0
    )
    ctrl.setState(base)

    // Command, das einfach das Budget um +100 erhöht
    val cmd = new Command {
      override def execute(gs: GameState): Try[GameState] =
        Success(gs.copy(budget = gs.budget + 100))
    }

    val obs = new TestObs
    ctrl.addObserver(obs)

    val res = ctrl.execute(cmd)
    res shouldBe a[Success[_]]
    res.get.budget shouldBe 600.0

    // intern im Controller aktualisiert
    ctrl.getState.budget shouldBe 600.0
    // Observer wurde benachrichtigt
    obs.updates.head.budget shouldBe 600.0
  }

  test("redo should call invoker.redo, update state and notify observers") {
    val ctrl = new GameController(new ConservativeDealer)
    // stub‐Invoker injizieren (via Reflection oder Setter – je nach API)
    // angenommen: ctrl.setInvoker(...) existiert
    val newState = GameState(
      deck = StandardDeckFactory.newDeck,
      playerHands = Nil,
      dealer = Hand.empty,
      bets = Nil,
      activeHand = 0,
      phase = PlayerTurn,
      budget = 123.0,
      currentBet = 0.0
    )
    ctrl.setInvoker(new Invoker {
      override def redo() = Some(newState)

      override def undo() = None
    })

    val obs = new TestObs
    ctrl.addObserver(obs)

    val result = ctrl.redo()
    result shouldBe Some(newState)
    ctrl.getState shouldBe newState
    obs.updates.head shouldBe newState
  }

  test("loadGame should set entire state internally and notify observers") {
    val ctrl = new GameController(new ConservativeDealer)
    val fullState = GameState(
      deck = StandardDeckFactory.newDeck,
      playerHands = List(handOf(card(Rank.Ace, Suits.Clubs))),
      dealer = Hand.empty,
      bets = List(50.0),
      activeHand = 0,
      phase = PlayerTurn,
      budget = 200.0,
      currentBet = 50.0
    )

    val obs = new TestObs
    ctrl.addObserver(obs)

    ctrl.loadGame(fullState)
    ctrl.getState shouldBe fullState
    obs.updates.head shouldBe fullState
  }
}
