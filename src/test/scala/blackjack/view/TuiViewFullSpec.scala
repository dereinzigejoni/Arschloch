package blackjack.view
import de.htwg.blackjack.controller.GameController
import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.*
import de.htwg.blackjack.state.GamePhase
import de.htwg.blackjack.state.GamePhases.{DealerBustPhase, PlayerBustPhase, PlayerTurn}
import de.htwg.blackjack.view.TuiView

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import scala.Console.{withIn, withOut}
class TuiViewFullSpec extends AnyFunSuite {
  private def mkState(
                       playerHands: List[Hand],
                       dealer:      Hand,
                       bets:        List[Double],
                       activeHand:  Int        = 0,
                       phase:       GamePhase  = PlayerTurn,
                       budget:      Double     = 100.0,
                       currentBet:  Double     = 0.0
                     ): GameState =
    GameState(
      deck        = Deck(Nil),
      playerHands = playerHands,
      dealer      = dealer,
      bets        = bets,
      activeHand  = activeHand,
      phase       = phase,
      budget      = budget,
      currentBet  = currentBet
    )
  private def captureOutput(block: => Unit): String = {
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos)
    Console.withOut(ps) {block}
    baos.toString.trim
  }
  private val dummyDeck: Deck = Deck(List.empty)
 
  /** Holt den privaten Controller aus TuiView per Reflection */
  private def getController: GameController = {
    val f = classOf[TuiView.type].getDeclaredField("controller")
    f.setAccessible(true)
    f.get(TuiView).asInstanceOf[GameController]
  }
  test("formatRenderFull for PlayerBustPhase (captured stdout)") {
    val p = Hand.empty
      .add(Card(Rank.King, Suits.Heart))
      .add(Card(Rank.King, Suits.Spades))
    // capture everything printed by renderFull
    val baos = new ByteArrayOutputStream()
    withOut(baos) {
      TuiView.renderFull()   // still returns Unit
    }
    // split into lines
    val output = baos.toString("UTF-8")
    val lines = output.split(System.lineSeparator).toSeq
    assert(lines.exists(_.contains("Du bist Bust – Dealer gewinnt.")))
  }
  test("formatRenderFull for DealerBust") {
    val pHands = List(Hand.empty.add(Card(Rank.Ten, Suits.Heart)).add(Card(Rank.Seven, Suits.Clubs)))
    val dealer = Hand.empty.add(Card(Rank.King, Suits.Diamonds)).add(Card(Rank.Queen, Suits.Spades)).add(Card(Rank.Five, Suits.Heart)) // Bust: 10+10+5
    val baos = new ByteArrayOutputStream()
    withOut(baos) {
      TuiView.renderFull() // still returns Unit
    }
    val output = baos.toString("UTF-8")
    val lines = output.split(System.lineSeparator).toSeq
    assert(lines.exists(_.contains("Dealer ist Bust – Du gewinnst!")))
  }
  test("formatRenderFull for Finished w Player win") {
    val pHands = List(Hand.empty.add(Card(Rank.Ten, Suits.Heart)).add(Card(Rank.Six, Suits.Clubs)))
    val dealer = Hand.empty.add(Card(Rank.Nine, Suits.Diamonds)).add(Card(Rank.Five, Suits.Spades))
    val baos = new ByteArrayOutputStream()
    withOut(baos) {
      TuiView.renderFull() // still returns Unit
    }
    val output = baos.toString("UTF-8")
    val lines = output.split(System.lineSeparator).toSeq
    assert(lines.exists(_.startsWith("Du gewinnst!")))
  }
  test("formatRenderFull for Finished w Dealer win") {
    val pHands = List(Hand.empty.add(Card(Rank.Eight, Suits.Heart)).add(Card(Rank.Seven, Suits.Clubs)))
    val dealer = Hand.empty.add(Card(Rank.Ten, Suits.Diamonds)).add(Card(Rank.Nine, Suits.Spades))
    val baos = new ByteArrayOutputStream()
    withOut(baos) {
      TuiView.renderFull() // still returns Unit
    }
    val output = baos.toString("UTF-8")
    val lines = output.split(System.lineSeparator).toSeq
    assert(lines.exists(_.startsWith("Dealer gewinnt!")))
  }
  test("formatRenderFull for Finished w Push") {
    val pHands = List(Hand.empty.add(Card(Rank.Nine, Suits.Heart)).add(Card(Rank.Eight, Suits.Diamonds)))
    val dealer = Hand.empty.add(Card(Rank.Ten, Suits.Clubs)).add(Card(Rank.Seven, Suits.Spades))
    val baos = new ByteArrayOutputStream()
    withOut(baos) {
      TuiView.renderFull() // still returns Unit
    }
    val output = baos.toString("UTF-8")
    val lines = output.split(System.lineSeparator).toSeq
    assert(lines.exists(_.startsWith("Push – unentschieden")))
  }
  test("formatRenderFull list stake and Hand-nb") {
    val pHands = List(Hand.empty.add(Card(Rank.Ace, Suits.Heart)).add(Card(Rank.King, Suits.Spades)), Hand.empty.add(Card(Rank.Five, Suits.Clubs)).add(Card(Rank.Five, Suits.Diamonds)))
    val dealer = Hand.empty.add(Card(Rank.Nine, Suits.Heart)).add(Card(Rank.Seven, Suits.Clubs))
    val bets   = List(100.0, 50.0)
    val baos = new ByteArrayOutputStream()
    withOut(baos) {
      TuiView.renderFull() // still returns Unit
    }
    val output = baos.toString("UTF-8")
    val lines = output.split(System.lineSeparator).toSeq
    assert(lines.exists(_.contains("Hand 1/2: ♠K ♥A (Wert: 21) Einsatz: 100.0")))
    assert(lines.exists(_.contains("Hand 2/2: ♦5 ♣5 (Wert: 10) Einsatz: 50.0")))
  }
  test("formatRenderPartial prints expected lines")  {
    val pHand = Hand.empty.add(Card(Rank.Ten, Suits.Heart))
    val dHand = Hand.empty.add(Card(Rank.Ace, Suits.Spades))
    val out = new ByteArrayOutputStream()
    withOut(new PrintStream(out)) {
      // just call the method—don't try to foreach on its return value
      TuiView.renderPartial()
    }
    val printed = out.toString("UTF-8")
    assert(printed.contains("Dealer: A♠ [???]"))
    assert(printed.contains("10♥ (Wert: 10)"))
  }
  test("askBet reduce Budget and play stake") {
    val controller = getController
    val fb = controller.getClass.getDeclaredField("budget")
    fb.setAccessible(true)
    fb.set(controller, java.lang.Double.valueOf(100.0))
    val in = new ByteArrayInputStream("50\n".getBytes)
    withIn(in) {TuiView.askBet()}
    val state = controller.getState
    assert(state.bets.head == 50.0)
    assert(controller.getBudget == 50.0)
  }
  test("formatRenderFull print all Cards and Bust-Resultat") {
    val controller = getController
    // State für DealerBust
    val pHand = Hand.empty.add(Card(Rank.Ten, Suits.Heart)).add(Card(Rank.Eight, Suits.Diamonds))
    val dHand = Hand.empty
      .add(Card(Rank.King, Suits.Clubs))
      .add(Card(Rank.Queen, Suits.Spades))
      .add(Card(Rank.Five, Suits.Heart)) // 10+10+5=25 → Bust
    val out = new ByteArrayOutputStream()
    withOut(new PrintStream(out)) {
      TuiView.renderPartial()
    }
    val printed = out.toString("UTF-8")
    assert(printed.contains("======= RUNDENRESULTAT ======="))
    assert(printed.contains("Dealer: 5♥ Q♠ K♣ (Wert: 25)"))
    assert(printed.contains("Dealer ist Bust – Du gewinnst!"))
  }
  // 1) Immer Hit, Stand, Undo, Quit
  test("formatMenuOptions always with Hit, Stand, Undo, Quit") {
    // Erstelle eine Hand mit einer Karte, z.B. 5♥
    val pHand = Hand.empty.add(Card(Rank.Five, Suits.Heart))
    // Dealer braucht hier nur irgendeine Hand
    val dealer = Hand.empty.add(Card(Rank.Two, Suits.Spades))
    val st = mkState(
      playerHands = List(pHand),
      dealer = dealer,
      bets = List(10.0) // Einsatz egal
    )
    val opts = TuiView.formatMenuOptions(st, budget = 100.0)

    assert(opts.contains("[H]it"))
    assert(opts.contains("[S]tand"))
    assert(opts.contains("[U]ndo"))
    assert(opts.contains("[Q]uit"))
  }

  // 2) [D]ouble, wenn genau 2 Karten und genug Budget
  test("formatMenuOptions adds [D]ouble when exactly 2 cards and budget suffices") {
    // Hand mit 2 Karten, z.B. 5♥ und 8♣
    val pHand = Hand.empty
      .add(Card(Rank.Five, Suits.Heart))
      .add(Card(Rank.Eight, Suits.Clubs))
    val dealer = Hand.empty.add(Card(Rank.Two, Suits.Spades))
    val st = mkState(
      playerHands = List(pHand),
      dealer = dealer,
      bets = List(20.0) // Einsatz 20
    )
    val opts = TuiView.formatMenuOptions(st, budget = 100.0)

    assert(opts.contains("[D]ouble"))
  }

  test("formatMenuOptions without [D]ouble when budget insufficient") {
    // gleiche Hand wie oben
    val pHand = Hand.empty
      .add(Card(Rank.Five, Suits.Heart))
      .add(Card(Rank.Eight, Suits.Clubs))
    val dealer = Hand.empty.add(Card(Rank.Two, Suits.Spades))
    val st = mkState(
      playerHands = List(pHand),
      dealer = dealer,
      bets = List(90.0) // Einsatz 90
    )
    val opts = TuiView.formatMenuOptions(st, budget = 80.0)

    assert(!opts.contains("[D]ouble"))
  }
  test("formatMenuOptions ohne [P]split, wenn kein Paar") {
    val noPairHand = Hand.empty
      .add(Card(Rank.Six,  Suits.Clubs))
      .add(Card(Rank.Five, Suits.Heart))
    val dealer = Hand.empty.add(Card(Rank.Two, Suits.Diamonds))
    val gs = mkState(
      playerHands = List(noPairHand),
      dealer      = dealer,
      bets        = List(10.0)
    )
    val opts = TuiView.formatMenuOptions(gs, budget = 100.0)
    assert(!opts.contains("[P]split"))
  }
  test("formatRenderPartial renders dealer and active hand correctly") {
    // 1. Prepare data
    val playerHand = Hand.empty.add(Card(Rank.King, Suits.Heart))
    val dealerHand = Hand.empty.add(Card(Rank.Ace, Suits.Spades))

    

    // 2. Call the refactored renderPartial
    val lines: Seq[String] = TuiView.renderPartial()

    // 3. Assertions
    assert(lines.head.contains("BLACKJACK"), "Erwarte Hinweis auf Blackjack")
    assert(lines.exists(_.startsWith("Dealer:")), "Dealer-Zeile fehlt")
    assert(lines.exists(_.contains("Spieler Hand 1/1")), "Spieler-Hand-Zähler falsch")
  }
  test("formatMenuOptions always lists Hit, Stand and Quit") {
    val noPairHand = Hand.empty
      .add(Card(Rank.Six, Suits.Clubs))
      .add(Card(Rank.Five, Suits.Heart))
    val dealer = Hand.empty.add(Card(Rank.Two, Suits.Diamonds))
    val gs = mkState(
      playerHands = List(noPairHand),
      dealer = dealer,
      bets = List(10.0)
    )
    val opts = TuiView.formatMenuOptions(gs, budget = 100.0)
    assert(opts.contains("[H]it"), "Hit-Option fehlt")
    assert(opts.contains("[S]tand"), "Stand-Option fehlt")
    assert(opts.contains("[Q]uit"), "Quit-Option fehlt")
  }
  test("formatRenderFull renders full round result") {
    val playerHand = Hand(List(Card(Rank.Nine, Suits.Heart)))
    val dealerHand = Hand(List(Card(Rank.Seven, Suits.Spades)))
    val baos = new ByteArrayOutputStream()
    withOut(baos) {
      TuiView.renderFull()  
    }
    val output = baos.toString("UTF-8")
    val lines = output.split(System.lineSeparator).toSeq
    assert(lines.exists(_.contains("Dealer:")), "Dealer-Ausgabe fehlt")
    assert(lines.exists(_.startsWith("Hand 1:")), "Spielerhand-Ausgabe fehlt")
  }
  test("formatMenuOptions without Double and Split only Hit, Stand, Undo, Quit") {

    val hand = Hand.empty
      .add(Card(Rank.Two,   Suits.Heart))
      .add(Card(Rank.Three, Suits.Clubs))
    val dealer = Hand.empty.add(Card(Rank.Four, Suits.Diamonds))
    val st = mkState(
      playerHands = List(hand),
      dealer      = dealer,
      bets        = List(20.0)  
    )
    val budget = 5.0            
    val opts = TuiView.formatMenuOptions(st, budget)
    assert(opts == Seq("[H]it", "[S]tand", "[U]ndo", "[Q]uit"))
  }

  test("when budget ≥ bet then appears [D]ouble") {
    val hand = Hand.empty
      .add(Card(Rank.Two, Suits.Heart))
      .add(Card(Rank.Three, Suits.Clubs))
    val dealer = Hand.empty.add(Card(Rank.Four, Suits.Diamonds))
    val state = mkState(
      playerHands = List(hand),
      dealer = dealer,
      bets = List(10.0)
    )
    val opts = TuiView.formatMenuOptions(state, budget = 10.0)
    assert(opts.contains("[D]ouble"))
    assert(!opts.contains("[P]split"))
  }

  test("at pair appears [P]split") {
    val hand = Hand.empty
      .add(Card(Rank.Five, Suits.Heart))
      .add(Card(Rank.Five, Suits.Diamonds))
    val dealer = Hand.empty.add(Card(Rank.Seven, Suits.Spades))
    val state = mkState(
      playerHands = List(hand),
      dealer = dealer,
      bets = List(20.0)
    )
    val opts = TuiView.formatMenuOptions(state, budget = 20.0)
    assert(opts.contains("[P]split"))
  }

  test("at pair with enough budget appears both [D]ouble and [P]split") {
    val hand = Hand.empty
      .add(Card(Rank.Six, Suits.Spades))
      .add(Card(Rank.Six, Suits.Clubs))
    val dealer = Hand.empty.add(Card(Rank.Eight, Suits.Heart))
    val state = mkState(
      playerHands = List(hand),
      dealer = dealer,
      bets = List(30.0)
    )
    val opts = TuiView.formatMenuOptions(state, budget = 30.0)
    assert(opts == Seq(
      "[H]it", "[S]tand", "[U]ndo", "[Q]uit",
      "[D]ouble", "[P]split"
    ))
  }
  test("printWelcome() prints welcome line") {
    val baos = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(baos)) {TuiView.printWelcome()}
    assert(baos.toString.trim == "Willkommen zu Blackjack")
  }
  test("renderPartial() print Header, covered Dealer-Karte and Hand") {
    val baos = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(baos)) {TuiView.renderPartial().foreach(println)}
    val out = baos.toString.linesIterator.toList
    assert(out(1).contains("BLACKJACK"))
    assert(out.exists(_.contains("Dealer: 5♦ [???]")))
    assert(out.exists(_.contains("Spieler Hand 1/1: 8♣ 7♥ (Wert: 15)")))
  }
  test("formatRenderFull() print round-Header, Dealer- and Spielerline and result") {
    val p = Hand.empty.add(Card(Rank.Ten, Suits.Heart)).add(Card(Rank.Six, Suits.Clubs)) // 16
    val d = Hand.empty.add(Card(Rank.Nine, Suits.Diamonds)).add(Card(Rank.Seven, Suits.Spades)) // 16
    //val state = mkState(List(p), d, List(20.0), Finished)
    val baos = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(baos)) {TuiView.renderPartial().foreach(println)}
    val lines = baos.toString.linesIterator.toList
    assert(lines(1).contains("RUNDENRESULTAT"))
    assert(lines.exists(_.startsWith("Dealer: 7♠ 9♦")))
    assert(lines.exists(_.startsWith("Hand 1: 6♣ 10♥")))
    assert(lines.exists(_.contains("Push")))
  }
  test("formatRenderFull for PlayerBust shows Bust-Meldung") {
    // 1) Erstelle eine Bust-Hand
    val pHand  = Hand.empty
      .add(Card(Rank.King, Suits.Heart))
      .add(Card(Rank.King, Suits.Spades))
    // 2) Dealer-Hand (beliebige Karte)
    val dHand  = Hand.empty.add(Card(Rank.Two, Suits.Diamonds))
    // 3) GameState mit PlayerBustPhase
    val state = mkState(
      playerHands = List(pHand),
      dealer      = dHand,
      bets        = List(0.0),
      phase       = PlayerBustPhase
    )
    // Controller auf diesen Zustand setzen
    val controller = getController
    controller.setState(state)

    // 4) Output abfangen
    val baos = new ByteArrayOutputStream()
    withOut(new PrintStream(baos)) {
      TuiView.renderFull()   // liefert Unit, druckt aber deine Zeilen
    }
    val printed = baos.toString("UTF-8")
    assert(printed.contains("Du bist Bust – Dealer gewinnt."))
  }
  test("formatRenderFull() for DealerBust shows Dealer-Bust-Meldung") {
    // 1) Spieler-Hand mit 16
    val p = Hand.empty
      .add(Card(Rank.Nine, Suits.Clubs))
      .add(Card(Rank.Seven, Suits.Diamonds))
    // 2) Dealer-Hand mit 25 (Bust)
    val d = Hand.empty
      .add(Card(Rank.King, Suits.Clubs))
      .add(Card(Rank.Queen, Suits.Spades))
      .add(Card(Rank.Five, Suits.Heart))

    // 3) GameState mit DealerBustPhase
    val state = mkState(
      playerHands = List(p),
      dealer = d,
      bets = List(30.0),
      phase = DealerBustPhase
    )
    // Controller auf diesen Zustand setzen
    val controller = getController
    controller.setState(state)

    // 4) Output abfangen
    val baos = new ByteArrayOutputStream()
    withOut(new PrintStream(baos)) {
      TuiView.renderFull() // druckt die Zeilen
    }

    // 5) Assertion
    val printed = baos.toString("UTF-8")
    assert(printed.contains("Dealer ist Bust – Du gewinnst!"))
  }
  test("askBet with non-numeric input prints error and retries") {
    val controller = getController
    val fb = controller.getClass.getDeclaredField("budget")
    fb.setAccessible(true)
    fb.set(controller, java.lang.Double.valueOf(100.0))
    val in = new ByteArrayInputStream("foo\n50\n".getBytes)
    val out = new ByteArrayOutputStream()
    withIn(in) {withOut(new PrintStream(out)) {TuiView.askBet()}}
    val printed = out.toString
    assert(printed.contains("Bitte eine Zahl eingeben."))
    assert(controller.getState.bets.head == 50.0)
  }
  test("askBet with negative input prints error and retries") {
    val controller = getController
    val fb = controller.getClass.getDeclaredField("budget")
    fb.setAccessible(true)
    fb.set(controller, java.lang.Double.valueOf(100.0))
    val in = new ByteArrayInputStream("-10\n75\n".getBytes)
    val out = new ByteArrayOutputStream()
    withIn(in) {withOut(new PrintStream(out)) {TuiView.askBet()}}
    val printed = out.toString
    assert(printed.contains("Einsatz muss > 0 sein"))
    assert(controller.getState.bets.head == 75.0)
  }
  test("askBet with too high input prints max bet error and retries") {
    val controller = getController
    val fb = controller.getClass.getDeclaredField("budget")
    fb.setAccessible(true)
    fb.set(controller, java.lang.Double.valueOf(100.0))
    val in = new ByteArrayInputStream("95\n20\n".getBytes)
    val out = new ByteArrayOutputStream()
    withIn(in) {withOut(new PrintStream(out)) {TuiView.askBet()}}
    val printed = out.toString
    assert(printed.contains("Einsatz muss ≤ 90% (max 90,00)"))
    assert(controller.getState.bets.head == 20.0)
  }
}