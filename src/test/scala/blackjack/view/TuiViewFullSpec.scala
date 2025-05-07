// src/test/scala/blackjack/view/TuiViewFullSpec.scala
package blackjack.view

import de.htwg.blackjack.controller.GameController
import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.*
import de.htwg.blackjack.view.TuiView

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import scala.Console.{withIn, withOut}

class TuiViewFullSpec extends AnyFunSuite {

  // Jetzt mit playerHands, dealer, bets, activeHand = 0, status
  private def mkState(
                       playerHands: List[Hand],
                       dealer:       Hand,
                       bets:         List[Double],
                       status:       Status
                     ): GameState =
    GameState(
      deck        = Deck(Nil),
      playerHands = playerHands,
      dealer      = dealer,
      bets        = bets,
      activeHand  = 0,
      status      = status
    )

  test("formatRenderFull für PlayerBust") {
    val pHands = List(
      Hand.empty.add(Card("K", Hearts)).add(Card("K", Spades))
    )
    val dealer = Hand.empty.add(Card("5", Clubs))
    val lines  = TuiView.formatRenderFull(mkState(pHands, dealer, List(100.0), PlayerBust))
    assert(lines.exists(_.contains("Du bist Bust – Dealer gewinnt.")))
  }

  test("formatRenderFull für DealerBust") {
    val pHands = List(
      Hand.empty.add(Card("10", Hearts)).add(Card("7", Clubs))
    )
    val dealer = Hand.empty
      .add(Card("K", Diamonds))
      .add(Card("Q", Spades))
      .add(Card("5", Hearts)) // Bust: 10+10+5
    val lines = TuiView.formatRenderFull(mkState(pHands, dealer, List(50.0), DealerBust))
    assert(lines.exists(_.contains("Dealer ist Bust – Du gewinnst!")))
  }

  test("formatRenderFull für Finished mit Spieler-Sieg") {
    val pHands = List(
      Hand.empty.add(Card("10", Hearts)).add(Card("6", Clubs))  // 16
    )
    val dealer = Hand.empty
      .add(Card("9", Diamonds)).add(Card("5", Spades))         // 14
    val lines = TuiView.formatRenderFull(mkState(pHands, dealer, List(20.0), Finished))
    assert(lines.exists(_.startsWith("Du gewinnst!")))
  }

  test("formatRenderFull für Finished mit Dealer-Sieg") {
    val pHands = List(
      Hand.empty.add(Card("8", Hearts)).add(Card("7", Clubs))   // 15
    )
    val dealer = Hand.empty
      .add(Card("10", Diamonds)).add(Card("9", Spades))         // 19
    val lines = TuiView.formatRenderFull(mkState(pHands, dealer, List(30.0), Finished))
    assert(lines.exists(_.startsWith("Dealer gewinnt!")))
  }

  test("formatRenderFull für Finished mit Push") {
    val pHands = List(
      Hand.empty.add(Card("9", Hearts)).add(Card("8", Diamonds))// 17
    )
    val dealer = Hand.empty
      .add(Card("10", Clubs)).add(Card("7", Spades))             // 17
    val lines = TuiView.formatRenderFull(mkState(pHands, dealer, List(10.0), Finished))
    assert(lines.exists(_.startsWith("Push – unentschieden")))
  }

  test("formatRenderFull listet Einsatz und Hand-Nummer auf") {
    val pHands = List(
      Hand.empty.add(Card("A", Hearts)).add(Card("K", Spades)),
      Hand.empty.add(Card("5", Clubs)).add(Card("5", Diamonds))
    )
    val dealer = Hand.empty.add(Card("9", Hearts)).add(Card("7", Clubs))
    val bets   = List(100.0, 50.0)
    val lines  = TuiView.formatRenderFull(mkState(pHands, dealer, bets, Finished))

    // Hand-Nummer und Einsatz
    assert(lines.exists(_.contains("Hand 1: K♠ A♥ (Wert: 21) Einsatz: 100.0")))
    assert(lines.exists(_.contains("Hand 2: 5♦ 5♣ (Wert: 10) Einsatz: 50.0")))
  }
  test("printWelcome() gibt genau die Willkommens­nachricht aus") {
    val baos = new ByteArrayOutputStream()
    withOut(new PrintStream(baos)) {
      TuiView.printWelcome()
    }
    val out = baos.toString.trim
    assert(out == "Willkommen zu Blackjack")
  }

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
      deck = Deck(Nil),
      playerHands = List(pHand),
      dealer = dHand,
      bets = List(0.0),
      activeHand = 0,
      status = InProgress
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
      deck = Deck(Nil),
      playerHands = List(pHand),
      dealer = dHand,
      bets = List(0.0),
      activeHand = 0,
      status = DealerBust
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

  private def mkState(ranks: List[String], bets: List[Double]): GameState = {
    val hands = ranks.map(r => Hand.empty.add(Card(r, Hearts)).add(Card(r, Diamonds)))
    GameState(
      deck = Deck(Nil),
      playerHands = hands,
      dealer = Hand.empty,
      bets = bets,
      activeHand = 0,
      status = InProgress
    )
  }

  test("formatMenuOptions immer mit Hit, Stand, Quit") {
    val st = mkState(List("5"), List(10.0))
    val opts = TuiView.formatMenuOptions(st, budget = 100.0)
    assert(opts.contains("[H]it"))
    assert(opts.contains("[S]tand"))
    assert(opts.contains("[Q]uit"))
  }

  test("formatMenuOptions fügt [D]ouble hinzu, wenn genug Budget und 2 Karten") {
    val st = mkState(List("5"), List(20.0))
    val opts = TuiView.formatMenuOptions(st, budget = 100.0)
    assert(opts.contains("[D]ouble"))
  }

  test("formatMenuOptions lässt [D]ouble weg, wenn kein Budget") {
    val st = mkState(List("5"), List(90.0))
    val opts = TuiView.formatMenuOptions(st, budget = 80.0)
    assert(!opts.contains("[D]ouble"))
  }

  test("formatMenuOptions fügt [P]split hinzu bei Paar & Budget") {
    val st = mkState(List("A", "A"), List(10.0, 10.0)) // activeHand=0 hat Paar
    val opts = TuiView.formatMenuOptions(st, budget = 100.0)
    assert(opts.contains("[P]split"))
  }

  test("formatMenuOptions lässt [P]split weg, wenn kein Paar") {
    val noPairHand = Hand.empty.add(Card("6", Clubs)).add(Card("5",Hearts))
    val gs = GameState(deck = Deck(Nil), playerHands = List(noPairHand),dealer = Hand.empty,bets=List(10.0),activeHand = 0,status = InProgress)
    val opts = TuiView.formatMenuOptions(gs, budget = 100.0)
    assert(!opts.contains("[P]split"))
  }

  test("formatNewRoundPrompt liefert konstante Zeichenkette") {
    assert(TuiView.formateNewRoundPrompt() == "[N]eues Spiel, [Q]uit?")
  }

  /** Hilfsfunktion, die stdout abfängt */
  private def captureOutput(block: => Unit): String = {
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos)
    Console.withOut(ps) {
      block
    }
    baos.toString.trim
  }

  // Ein Dummy-Deck, da die TUI-Methoden eh nur den State lesen und das Deck ignorieren
  private val dummyDeck: Deck = Deck(List.empty)

  test("printWelcome prints the welcome message") {
    val out = captureOutput {
      TuiView.printWelcome()
    }
    assert(out == "Willkommen zu Blackjack")
  }

  test("formateNewRoundPrompt returns the new-round prompt") {
    assert(TuiView.formateNewRoundPrompt() == "[N]eues Spiel, [Q]uit?")
  }

  test("formatRenderPartial renders dealer and active hand correctly") {
    // Dummy-Karten und Hände
    val playerHand = Hand(List(Card("K", Hearts)))
    val dealerHand = Hand(List(Card("A", Spades)))
    val state = GameState(
      deck = dummyDeck,
      playerHands = List(playerHand),
      dealer = dealerHand,
      bets = List(5.0),
      activeHand = 0,
      status = InProgress
    )

    val lines = TuiView.formatRenderPartial(state)
    assert(lines.head.contains("BLACKJACK"), "Erwarte Hinweis auf Blackjack")
    assert(lines.exists(_.startsWith("Dealer:")), "Dealer-Zeile fehlt")
    assert(lines.exists(_.contains("Spieler Hand 1/1")), "Spieler-Hand-Zähler falsch")
  }

  test("formatMenuOptions always lists Hit, Stand and Quit") {
    val hand = Hand(List(Card("10", Clubs)))
    val state = GameState(
      deck = dummyDeck,
      playerHands = List(hand),
      dealer = hand,
      bets = List(5.0),
      activeHand = 0,
      status = InProgress
    )
    val opts = TuiView.formatMenuOptions(state, budget = 100)

    assert(opts.contains("[H]it"), "Hit-Option fehlt")
    assert(opts.contains("[S]tand"), "Stand-Option fehlt")
    assert(opts.contains("[Q]uit"), "Quit-Option fehlt")
  }

  test("formatRenderFull renders full round result") {
    val playerHand = Hand(List(Card("9", Hearts)))
    val dealerHand = Hand(List(Card("7", Spades)))
    val state = GameState(
      deck = dummyDeck,
      playerHands = List(playerHand),
      dealer = dealerHand,
      bets = List(10.0),
      activeHand = 0,
      status = Finished
    )

    val lines = TuiView.formatRenderFull(state)
    //assert(lines.exists(_.startsWith("======= RUNDENRESULTAT")), "Rundenresultat-Header fehlt")
    assert(lines.exists(_.contains("Dealer:")), "Dealer-Ausgabe fehlt")
    assert(lines.exists(_.startsWith("Hand 1:")), "Spielerhand-Ausgabe fehlt")
  }

}
