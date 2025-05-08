package blackjack.view
import de.htwg.blackjack.controller.GameController
import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.*
import de.htwg.blackjack.view.TuiView
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import scala.Console.{withIn, withOut}
class TuiViewFullSpec extends AnyFunSuite {
  private def mkState(playerHands: List[Hand],dealer:Hand,bets:List[Double],status:Status): GameState = GameState(deck= Deck(Nil), playerHands = playerHands, dealer= dealer, bets= bets, activeHand  = 0, status= status)
  private def captureOutput(block: => Unit): String = {
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos)
    Console.withOut(ps) {block}
    baos.toString.trim
  }
  private val dummyDeck: Deck = Deck(List.empty)
  private def mkState(ranks: List[String], bets: List[Double]): GameState = {
    val hands = ranks.map(r => Hand.empty.add(Card(r, Hearts)).add(Card(r, Diamonds)))
    GameState(deck = Deck(Nil), playerHands = hands, dealer = Hand.empty, bets = bets, activeHand = 0, status = InProgress)
  }
  private def getController: GameController = {
    val f = classOf[TuiView.type].getDeclaredField("controller")
    f.setAccessible(true)
    f.get(TuiView).asInstanceOf[GameController]
  }
  test("formatRenderFull for PlayerBust") {
    val pHands = List(Hand.empty.add(Card("K", Hearts)).add(Card("K", Spades)))
    val dealer = Hand.empty.add(Card("5", Clubs))
    val lines  = TuiView.formatRenderFull(mkState(pHands, dealer, List(100.0), PlayerBust))
    assert(lines.exists(_.contains("Du bist Bust – Dealer gewinnt.")))
  }
  test("formatRenderFull for DealerBust") {
    val pHands = List(Hand.empty.add(Card("10", Hearts)).add(Card("7", Clubs)))
    val dealer = Hand.empty.add(Card("K", Diamonds)).add(Card("Q", Spades)).add(Card("5", Hearts)) // Bust: 10+10+5
    val lines = TuiView.formatRenderFull(mkState(pHands, dealer, List(50.0), DealerBust))
    assert(lines.exists(_.contains("Dealer ist Bust – Du gewinnst!")))
  }
  test("formatRenderFull for Finished w Player win") {
    val pHands = List(Hand.empty.add(Card("10", Hearts)).add(Card("6", Clubs)))
    val dealer = Hand.empty.add(Card("9", Diamonds)).add(Card("5", Spades))
    val lines = TuiView.formatRenderFull(mkState(pHands, dealer, List(20.0), Finished))
    assert(lines.exists(_.startsWith("Du gewinnst!")))
  }
  test("formatRenderFull for Finished w Dealer win") {
    val pHands = List(Hand.empty.add(Card("8", Hearts)).add(Card("7", Clubs)))
    val dealer = Hand.empty.add(Card("10", Diamonds)).add(Card("9", Spades))
    val lines = TuiView.formatRenderFull(mkState(pHands, dealer, List(30.0), Finished))
    assert(lines.exists(_.startsWith("Dealer gewinnt!")))
  }
  test("formatRenderFull for Finished w Push") {
    val pHands = List(Hand.empty.add(Card("9", Hearts)).add(Card("8", Diamonds)))
    val dealer = Hand.empty.add(Card("10", Clubs)).add(Card("7", Spades))
    val lines = TuiView.formatRenderFull(mkState(pHands, dealer, List(10.0), Finished))
    assert(lines.exists(_.startsWith("Push – unentschieden")))
  }
  test("formatRenderFull list stake and Hand-nb auf") {
    val pHands = List(Hand.empty.add(Card("A", Hearts)).add(Card("K", Spades)), Hand.empty.add(Card("5", Clubs)).add(Card("5", Diamonds)))
    val dealer = Hand.empty.add(Card("9", Hearts)).add(Card("7", Clubs))
    val bets   = List(100.0, 50.0)
    val lines  = TuiView.formatRenderFull(mkState(pHands, dealer, bets, Finished))
    assert(lines.exists(_.contains("Hand 1: K♠ A♥ (Wert: 21) Einsatz: 100.0")))
    assert(lines.exists(_.contains("Hand 2: 5♦ 5♣ (Wert: 10) Einsatz: 50.0")))
  }
  test("formatRenderPartial print expected lines") {
    val controller = getController
    val pHand = Hand.empty.add(Card("10", Hearts))
    val dHand = Hand.empty.add(Card("A", Spades))
    val state = GameState(deck = Deck(Nil), playerHands = List(pHand), dealer = dHand, bets = List(0.0), activeHand = 0, status = InProgress)
    val out = new ByteArrayOutputStream()
    withOut(new PrintStream(out)) {TuiView.formatRenderPartial(state).foreach(println)}
    val printed = out.toString
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
  test("formatMenuOptions always wi Hit, Stand, Quit") {
    val st = mkState(List("5"), List(10.0))
    val opts = TuiView.formatMenuOptions(st, budget = 100.0)
    assert(opts.contains("[H]it"))
    assert(opts.contains("[S]tand"))
    assert(opts.contains("[Q]uit"))
  }
  test("formatMenuOptions add [D]ouble, when enough Budget and 2 cards") {
    val st = mkState(List("5"), List(20.0))
    val opts = TuiView.formatMenuOptions(st, budget = 100.0)
    assert(opts.contains("[D]ouble"))
  }
  test("formatMenuOptions wout [D]ouble, if no Budget") {
    val st = mkState(List("5"), List(90.0))
    val opts = TuiView.formatMenuOptions(st, budget = 80.0)
    assert(!opts.contains("[D]ouble"))
  }
  test("formatMenuOptions add [P]split at pair & Budget") {
    val st = mkState(List("A", "A"), List(10.0, 10.0)) // activeHand=0 hat Paar
    val opts = TuiView.formatMenuOptions(st, budget = 100.0)
    assert(opts.contains("[P]split"))
  }
  test("formatMenuOptions wout [P]split when no pair") {
    val noPairHand = Hand.empty.add(Card("6", Clubs)).add(Card("5",Hearts))
    val gs = GameState(deck = Deck(Nil), playerHands = List(noPairHand),dealer = Hand.empty,bets=List(10.0),activeHand = 0,status = InProgress)
    val opts = TuiView.formatMenuOptions(gs, budget = 100.0)
    assert(!opts.contains("[P]split"))
  }
  test("formatNewRoundPrompt puts constant Characterstring") {assert(TuiView.formateNewRoundPrompt() == "[N]eues Spiel, [Q]uit?")}
  test("formateNewRoundPrompt returns the new-round prompt") {
    assert(TuiView.formateNewRoundPrompt() == "[N]eues Spiel, [Q]uit?")
  }
  test("formatRenderPartial renders dealer and active hand correctly") {
    val playerHand = Hand(List(Card("K", Hearts)))
    val dealerHand = Hand(List(Card("A", Spades)))
    val state = GameState(deck = dummyDeck, playerHands = List(playerHand), dealer = dealerHand, bets = List(5.0), activeHand = 0, status = InProgress)
    val lines = TuiView.formatRenderPartial(state)
    assert(lines.head.contains("BLACKJACK"), "Erwarte Hinweis auf Blackjack")
    assert(lines.exists(_.startsWith("Dealer:")), "Dealer-Zeile fehlt")
    assert(lines.exists(_.contains("Spieler Hand 1/1")), "Spieler-Hand-Zähler falsch")
  }
  test("formatMenuOptions always lists Hit, Stand and Quit") {
    val hand = Hand(List(Card("10", Clubs)))
    val state = GameState(deck = dummyDeck, playerHands = List(hand), dealer = hand, bets = List(5.0), activeHand = 0, status = InProgress)
    val opts = TuiView.formatMenuOptions(state, budget = 100)
    assert(opts.contains("[H]it"), "Hit-Option fehlt")
    assert(opts.contains("[S]tand"), "Stand-Option fehlt")
    assert(opts.contains("[Q]uit"), "Quit-Option fehlt")
  }
  test("formatRenderFull renders full round result") {
    val playerHand = Hand(List(Card("9", Hearts)))
    val dealerHand = Hand(List(Card("7", Spades)))
    val state = GameState(deck = dummyDeck, playerHands = List(playerHand), dealer = dealerHand, bets = List(10.0), activeHand = 0, status = Finished)
    val lines = TuiView.formatRenderFull(state)
    assert(lines.exists(_.contains("Dealer:")), "Dealer-Ausgabe fehlt")
    assert(lines.exists(_.startsWith("Hand 1:")), "Spielerhand-Ausgabe fehlt")
  }
  test("wout Double and Split only Hit, Stand, Quit") {
    val hand = Hand.empty.add(Card("2", Hearts)).add(Card("3", Clubs))
    val state = mkState(List(hand), Hand.empty, List(10.0), InProgress)
    val budget = 5.0 // < bet ⇒ kein Double
    val opts = TuiView.formatMenuOptions(state, budget)
    assert(opts == Seq("[H]it", "[S]tand", "[Q]uit"))
  }
  test("when Budget ≥ Bet then appears [D]ouble") {
    val hand = Hand.empty.add(Card("2", Hearts)).add(Card("3", Clubs))
    val state = mkState(List(hand), Hand.empty, List(10.0), InProgress)
    val budget = 10.0
    val opts = TuiView.formatMenuOptions(state, budget)
    assert(opts.contains("[D]ouble"))
    assert(!opts.contains("[P]split"))
  }
  test("at Pair chains appears [P]split") {
    val hand = Hand.empty.add(Card("5", Hearts)).add(Card("5", Diamonds))
    val state = mkState(List(hand), Hand.empty, List(20.0), InProgress)
    val budget = 20.0
    val opts = TuiView.formatMenuOptions(state, budget)
    assert(opts.contains("[P]split"))
  }
  test("at pair and enouth Budget appear both options") {
    val hand   = Hand.empty.add(Card("6", Spades)).add(Card("6", Clubs))
    val state  = mkState(List(hand), Hand.empty, List(30.0), InProgress)
    val budget = 30.0
    val opts   = TuiView.formatMenuOptions(state, budget)
    assert(opts == Seq("[H]it", "[S]tand","[Q]uit", "[D]ouble", "[P]split"))
  }
  test("printWelcome() prints welcome line") {
    val baos = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(baos)) {TuiView.printWelcome()}
    assert(baos.toString.trim == "Willkommen zu Blackjack")
  }
  test("renderPartial() print Header, covered Dealer-Karte and Hand") {
    val state = mkState(playerHands = List(Hand.empty.add(Card("7", Hearts)).add(Card("8", Clubs))), dealer = Hand.empty.add(Card("A", Spades)).add(Card("5", Diamonds)), bets = List(10.0), status = InProgress)
    val baos = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(baos)) {TuiView.formatRenderPartial(state).foreach(println)}
    val out = baos.toString.linesIterator.toList
    assert(out(1).contains("BLACKJACK"))
    assert(out.exists(_.contains("Dealer: 5♦ [???]")))
    assert(out.exists(_.contains("Spieler Hand 1/1: 8♣ 7♥ (Wert: 15)")))
  }
  test("formatRenderFull() print round-Header, Dealer- and Spielerline and result") {
    val p = Hand.empty.add(Card("10", Hearts)).add(Card("6", Clubs)) // 16
    val d = Hand.empty.add(Card("9", Diamonds)).add(Card("7", Spades)) // 16
    val state = mkState(List(p), d, List(20.0), Finished)
    val baos = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(baos)) {TuiView.formatRenderFull(state).foreach(println)}
    val lines = baos.toString.linesIterator.toList
    assert(lines(1).contains("RUNDENRESULTAT"))
    assert(lines.exists(_.startsWith("Dealer: 7♠ 9♦")))
    assert(lines.exists(_.startsWith("Hand 1: 6♣ 10♥")))
    assert(lines.exists(_.contains("Push")))
  }
  test("formatRenderFull() for PlayerBust shows Bust-Meldung") {
    val p = Hand.empty.add(Card("K", Hearts)).add(Card("K", Spades))
    val state = mkState(List(p), Hand.empty, List(50.0), PlayerBust)
    val baos = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(baos)){TuiView.formatRenderFull(state).foreach(println)}
    assert(baos.toString.contains("Du bist Bust – Dealer gewinnt."))
  }
  test("formatRenderFull() for DealerBust shows Dealer-Bust-Meldung") {
    val p = Hand.empty.add(Card("9", Clubs)).add(Card("7", Diamonds))
    val d = Hand.empty.add(Card("K", Clubs))
      .add(Card("Q", Spades))
      .add(Card("5", Hearts)) // 25
    val state = mkState(List(p), d, List(30.0), DealerBust)
    val baos = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(baos)) {
      TuiView.formatRenderFull(state).foreach(println)
    }
    assert(baos.toString.contains("Dealer ist Bust – Du gewinnst!"))
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