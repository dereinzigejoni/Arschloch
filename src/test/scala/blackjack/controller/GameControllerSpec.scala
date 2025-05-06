// src/test/scala/blackjack/controller/GameControllerSpec.scala
package blackjack.controller

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model._
import de.htwg.blackjack.controller.GameController

class GameControllerSpec extends AnyFunSuite {

  // Helferklasse, um private Felder zu setzen
  private class TestableController extends GameController {
    def setField(name: String, value: Any): Unit = {
      val f = classOf[GameController].getDeclaredField(name)
      f.setAccessible(true)
      f.set(this, value.asInstanceOf[AnyRef])
    }
    def setState(gs: GameState): Unit = {
      val f = classOf[GameController].getDeclaredField("state")
      f.setAccessible(true)
      f.set(this, gs)
    }
    def setBudget(b: Double): Unit = setField("budget", java.lang.Double.valueOf(b))
  }

  test("placeBet: valid bet reduziert Budget und legt bets-List an") {
    val ctl = new GameController()
    val start = ctl.getBudget
    val bet   = start * 0.5
    ctl.placeBet(bet)
    assert(ctl.getBudget == start - bet)
    val state = ctl.getState
    assert(state.bets.head == bet)
  }

  test("placeBet: zero oder negative Einsätze sind illegal") {
    val ctl = new GameController()
    intercept[IllegalArgumentException](ctl.placeBet(0))
    intercept[IllegalArgumentException](ctl.placeBet(-5))
  }

  test("placeBet: Einsatz > 90% Budget ist illegal") {
    val ctl    = new GameController()
    val tooHigh = ctl.getBudget * 0.91
    intercept[IllegalArgumentException](ctl.placeBet(tooHigh))
  }

  test("resolveBet: normaler Sieg zahlt 2×") {
    val ctl = new TestableController
    ctl.setBudget(100.0)
    // State mit einem Hand-Bet von 20
    val p = Hand.empty.add(Card("K", Hearts)).add(Card("9", Spades)) // 19
    val d = Hand.empty.add(Card("10", Diamonds)).add(Card("8", Clubs)) // 18
    val gs = GameState(
      deck        = Deck(Nil),
      playerHands = List(p),
      dealer      = d,
      bets        = List(20.0),
      activeHand  = 0,
      status      = Finished
    )
    ctl.setState(gs)
    ctl.resolveBet()
    // 100 + 20*2 = 140
    assert(ctl.getBudget == 140.0)
  }

  test("resolveBet: Natural Blackjack zahlt 2.7×") {
    val ctl = new TestableController
    ctl.setBudget(50.0)
    val p = Hand.empty.add(Card("A", Hearts)).add(Card("K", Spades)) // 21 natural
    val d = Hand.empty.add(Card("10", Diamonds)).add(Card("9", Clubs)) // 19
    val gs = GameState(Deck(Nil), List(p), d, List(10.0), 0, Finished)
    ctl.setState(gs)
    ctl.resolveBet()
    // 50 + 10*2.7 = 77
    assert(ctl.getBudget == 77.0)
  }

  test("resolveBet: DealerBust zahlt 2×") {
    val ctl = new TestableController
    ctl.setBudget(30.0)
    val p = Hand.empty.add(Card("10", Hearts)).add(Card("8", Spades)) // 18
    val d = Hand.empty.add(Card("K", Diamonds))
      .add(Card("Q", Clubs))
      .add(Card("5", Hearts)) // 25 bust
    val gs = GameState(Deck(Nil), List(p), d, List(5.0), 0, DealerBust)
    ctl.setState(gs)
    ctl.resolveBet()
    // kein Natural → 30 + 5*2 = 40
    assert(ctl.getBudget == 40.0)
  }

  test("resolveBet: Push gibt Einsatz zurück") {
    val ctl = new TestableController
    ctl.setBudget(80.0)
    val p = Hand.empty.add(Card("9", Hearts)).add(Card("8", Spades)) // 17
    val d = Hand.empty.add(Card("10", Diamonds)).add(Card("7", Clubs)) // 17
    val gs = GameState(Deck(Nil), List(p), d, List(20.0), 0, Finished)
    ctl.setState(gs)
    ctl.resolveBet()
    // 80 + 20 = 100
    assert(ctl.getBudget == 100.0)
  }

  test("resolveBet: Verlust behält Budget") {
    val ctl = new TestableController
    ctl.setBudget(60.0)
    val p = Hand.empty.add(Card("9", Hearts)).add(Card("8", Spades))  // 17
    val d = Hand.empty.add(Card("10", Diamonds)).add(Card("8", Clubs)) // 18
    val gs = GameState(Deck(Nil), List(p), d, List(30.0), 0, Finished)
    ctl.setState(gs)
    ctl.resolveBet()
    // Verlust → 60
    assert(ctl.getBudget == 60.0)
  }

  test("playerHit verändert State nur im InProgress-Fall") {
    val ctl = new TestableController
    val gs = GameState(Deck(Nil), List(Hand.empty), Hand.empty, List(0.0), 0, PlayerBust)
    ctl.setState(gs)
    ctl.playerHit()
    assert(ctl.getState eq gs)
  }

  test("playerHit fügt Karte hinzu und bleibt InProgress") {
    val ctl = new TestableController
    val deck = Deck(List(Card("2", Hearts)))
    val p    = Hand.empty.add(Card("5", Clubs))
    val gs   = GameState(deck, List(p), Hand.empty, List(0.0), 0, InProgress)
    ctl.setState(gs)

    ctl.playerHit()
    val st = ctl.getState
    assert(st.playerHands(0).cards.size == 2)
    assert(st.status == InProgress)
  }

  test("playerHit führt zum Bust") {
    val ctl = new TestableController
    val deck = Deck(List(Card("K", Diamonds)))
    val p    = Hand.empty.add(Card("K", Hearts)).add(Card("2", Clubs)) // 12
    val gs   = GameState(deck, List(p), Hand.empty, List(0.0), 0, InProgress)
    ctl.setState(gs)

    ctl.playerHit()
    val st = ctl.getState
    assert(st.playerHands(0).isBust)
    assert(st.status == PlayerBust)
  }

  test("playerStand löst DealerPlay aus") {
    val ctl = new TestableController
    // Dealer bei 10, deck liefert 7 → 17
    val deck = Deck(List(Card("7", Clubs)))
    val p    = Hand.empty.add(Card("5", Hearts))
    val d    = Hand.empty.add(Card("10", Diamonds))
    val gs   = GameState(deck, List(p), d, List(0.0), 0, InProgress)
    ctl.setState(gs)

    ctl.playerStand()
    val st = ctl.getState
    assert(st.status == Finished)
    assert(st.dealer.value >= 17)
  }

  test("playerDouble verdoppelt Einsatz und zieht genau eine Karte") {
    val ctl = new TestableController
    ctl.setBudget(100.0)
    ctl.placeBet(10.0)
    val initialState = ctl.getState
    val firstDeck    = initialState.deck
    // Stelle sicher, dass next Card im Deck "3" ist
    val deckWith3    = Deck(List(Card("3", Hearts)) ++ firstDeck.cards)
    // Override state.deck
    ctl.setState(initialState.copy(deck = deckWith3))
    ctl.playerDouble()
    val st = ctl.getState
    // Einsatz in bets list sollte 20.0 sein
    assert(st.bets(0) == 20.0)
    // Hand hat jetzt 3 Karten (2 + 1)
    assert(st.playerHands(0).cards.size == 3)
    // Status nach Double ist entweder InProgress für next hand oder Finished wenn nur eine Hand
    assert(st.status != InProgress || st.dealer.cards.nonEmpty)
  }

  test("playerSplit teilt Paar in zwei Hände") {
    val ctl = new TestableController
    ctl.setBudget(100.0)
    ctl.placeBet(20.0)
    val initial = ctl.getState
    // Setze Hand auf Paar "5","5"
    val pairHand = Hand.empty.add(Card("5", Clubs)).add(Card("5", Diamonds))
    ctl.setState(initial.copy(playerHands = List(pairHand)))
    // Deck liefert für Hand1 und Hand2 je eine "2" bzw. "3"
    val deck2 = Deck(List(Card("2", Hearts), Card("3", Spades)))
    ctl.setState(initial.copy(playerHands = List(pairHand), deck = deck2))
    ctl.playerSplit()
    val st = ctl.getState
    // Nun sollten 2 Hände existieren
    assert(st.playerHands.size == 2)
    // Bets-Liste ebenfalls in Länge 2
    assert(st.bets == List(20.0, 20.0))
  }
  // Ergänze ans Ende von src/test/scala/blackjack/controller/GameControllerSpec.scala

  test("playerDouble verdoppelt Einsatz, zieht genau eine Karte und bucht korrekt ab") {
    val ctl = new TestableController
    // Starte mit Budget 100, setze Einsatz 10
    ctl.setBudget(100.0)
    ctl.placeBet(10.0)
    val beforeBudget = ctl.getBudget // 90.0
    val state0 = ctl.getState
    // Lege ein Deck mit einer definierten Karte (z.B. "3"♣) oben drauf
    val card3 = Card("3", Clubs)
    val deckWith3 = Deck(card3 :: state0.deck.cards)
    ctl.setState(state0.copy(deck = deckWith3))

    // Führe Double Down aus
    ctl.playerDouble()
    val st = ctl.getState

    // Nach Double: bets.head == 20, weil 10*2
    assert(st.bets.head == 20.0)
    // Genau eine zusätzliche Karte gezogen: ursprüngliche Hand hatte 2 Karten
    assert(st.playerHands.head.cards.size == 3)
    // Budget wurde um den zweiten Einsatzbuchungsbetrag reduziert: vor Double 90.0, Einsatz=10 => jetzt 80.0
    assert(ctl.getBudget == beforeBudget - 10.0)
  }

  test("playerSplit teilt ein Paar und bucht doppelten Einsatz ab") {
    val ctl = new TestableController
    // Starte mit Budget 100, setze Einsatz 20
    ctl.setBudget(100.0)
    ctl.placeBet(20.0)
    // Baue eine Hand mit Paar "5","5"
    val pairHand = Hand.empty.add(Card("5", Diamonds)).add(Card("5", Hearts))
    val st0 = ctl.getState.copy(playerHands = List(pairHand))
    // Lege ein Deck, das für Hand1 eine "2"♣ und für Hand2 eine "3"♠ liefert
    val deck2 = Deck(
      Card("2", Clubs) ::
        Card("3", Spades) ::
        st0.deck.cards
    )
    ctl.setState(st0.copy(deck = deck2))

    // Führe Split aus
    ctl.playerSplit()
    val st = ctl.getState

    // Nach Split: zwei Hände, je mit 5 + je einer neuen Karte
    assert(st.playerHands.size == 2)
    assert(st.playerHands(0).cards.map(_.rank) == List("2", "5"))
    assert(st.playerHands(1).cards.map(_.rank) == List("3", "5"))
    // Bets-Liste wurde auf [20,20] erweitert
    assert(st.bets == List(20.0, 20.0))
    // Budget wurde um den zweiten Einsatz reduziert: von 80 (nach placeBet) auf 60
    assert(ctl.getBudget == 60.0)
  }

}
