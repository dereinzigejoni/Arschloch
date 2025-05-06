// src/test/scala/blackjack/view/TuiViewWrapperSpec.scala
package blackjack.view

import org.scalatest.funsuite.AnyFunSuite
import blackjack.model.*
import de.htwg.blackjack.controller.GameController
import de.htwg.blackjack.model.{Card, Clubs, DealerBust, Deck, Diamonds, GameState, Hand, Hearts, InProgress, Spades}
import de.htwg.blackjack.view.TuiView

import java.io.*
import scala.Console.{withIn, withOut}

class TuiViewWrapperSpec extends AnyFunSuite {

  // Zugriff auf das private controller-Feld in TuiView
  private def getController: GameController = {
    val f = classOf[TuiView.type].getDeclaredField("controller")
    f.setAccessible(true)
    f.get(TuiView).asInstanceOf[GameController]
  }

  test("formatRenderPartial druckt erwartete Zeilen") {
    val controller = getController
    // State injizieren
    val pHand = Hand.empty.add(Card("10", Hearts))
    val dHand = Hand.empty.add(Card("A", Spades))
    val state = GameState(
      deck        = Deck(Nil),
      playerHands = List(pHand),
      dealer      = dHand,
      bets        = List(0.0),
      activeHand  = 0,
      status      = InProgress
    )
    // capture Ausgabe
    val out = new ByteArrayOutputStream()
    withOut(new PrintStream(out)) {
      TuiView.formatRenderPartial(state).foreach(println)
    }
    val printed = out.toString
    assert(printed.contains("Dealer: A♠ [???]"))
    assert(printed.contains("10♥ (Wert: 10)"))
  }

  test("askBet reduziert Budget und setzt Einsatz") {
    val controller = getController
    // Budget auf 100 setzen
    val fb = controller.getClass.getDeclaredField("budget")
    fb.setAccessible(true)
    fb.set(controller, java.lang.Double.valueOf(100.0))

    // Eingabe "50"
    val in = new ByteArrayInputStream("50\n".getBytes)
    withIn(in) {
      TuiView.askBet()
    }

    // Nach askBet muss die erste Bet im State 50 sein, Budget 50
    val state = controller.getState
    assert(state.bets.head == 50.0)
    assert(controller.getBudget == 50.0)
  }

  test("formatRenderFull druckt alle Karten und Bust-Resultat") {
    val controller = getController
    // State für DealerBust
    val pHand = Hand.empty.add(Card("10", Hearts)).add(Card("8", Diamonds))
    val dHand = Hand.empty
      .add(Card("K", Clubs))
      .add(Card("Q", Spades))
      .add(Card("5", Hearts)) // 10+10+5=25 → Bust
    val state = GameState(
      deck        = Deck(Nil),
      playerHands = List(pHand),
      dealer      = dHand,
      bets        = List(0.0),
      activeHand  = 0,
      status      = DealerBust
    )

    val out = new ByteArrayOutputStream()
    withOut(new PrintStream(out)) {
      TuiView.formatRenderFull(state).foreach(println)
    }
    val printed = out.toString
    assert(printed.contains("======= RUNDENRESULTAT ======="))
    assert(printed.contains("Dealer: 5♥ Q♠ K♣ (Wert: 25)"))
    assert(printed.contains("Dealer ist Bust – Du gewinnst!"))
  }
}
