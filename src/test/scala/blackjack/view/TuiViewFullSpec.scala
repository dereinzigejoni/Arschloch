// src/test/scala/blackjack/view/TuiViewFullSpec.scala
package blackjack.view

import org.scalatest.funsuite.AnyFunSuite
import de.htwg.blackjack.model.*
import de.htwg.blackjack.view.TuiView

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
}
