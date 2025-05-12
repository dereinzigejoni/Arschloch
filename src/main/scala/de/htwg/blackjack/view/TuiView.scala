// src/main/scala/de/htwg/blackjack/view/TuiView.scala
package de.htwg.blackjack.view

import de.htwg.blackjack.controller.GameController
import de.htwg.blackjack.model._
import de.htwg.blackjack.state.GamePhases._

object TuiView {

  private val controller = new GameController()

  private val lineWidth = 40
  private def hBorder(c: Char = '='): String = c.toString * lineWidth
  private def padCenter(text: String): String = {
    val padding = (lineWidth - text.length) / 2
    " " * padding + text + " " * (lineWidth - text.length - padding)
  }

  def printWelcome(): Unit = {
    println(hBorder())
    println(padCenter("WILLKOMMEN ZU BLACKJACK"))
    println(hBorder())
  }

  def run(): Unit = {
    printWelcome()
    var playing = true

    while (playing) {
      askBet()

      // Spielerphase
      while (controller.getState.phase == PlayerTurn) {
        renderPartial()
        printMenu()
        scala.io.StdIn.readLine().trim.toUpperCase match {
          case "H" => controller.playerHit()
          case "S" => controller.playerStand()
          case "D" => controller.playerDouble()
          case "P" => controller.playerSplit()
          case "Q" =>
            println("\nAuf Wiedersehen!")
            sys.exit(0)
          case _ =>
            println("Ungültige Eingabe. Bitte H, S, D, P oder Q.")
        }
      }

      // Dealer- und Payout-Phase
      controller.playerStand()  // löst DealerTurn aus
      controller.resolveBet()   // ruft Payout(state).pay(state)

      renderFull()
      println(f"\nDein aktuelles Budget: €${controller.getBudget}%.2f\n")

      // Neustart oder Ende
      println(hBorder('-'))
      println(padCenter("[N] Neues Spiel    [Q] Quit"))
      println(hBorder('-'))
      scala.io.StdIn.readLine().trim.toUpperCase match {
        case "N" => // nächste Runde
        case "Q" =>
          println("\nGame over. Danke fürs Spielen!")
          playing = false
        case _ =>
          println("Ungültige Eingabe, beende das Spiel.")
          playing = false
      }
    }
  }

  private def askBet(): Unit = {
    var valid = false
    while (!valid) {
      val budget = controller.getBudget
      val maxBet = budget * 0.9
      println(hBorder())
      println(f"Budget: €$budget%.2f   Max-Einsatz: €$maxBet%.2f")
      print("Einsatz eingeben (H = Quit): ")
      scala.io.StdIn.readLine().trim.toUpperCase match {
        case "H" =>
          println("\nAuf Wiedersehen!")
          sys.exit(0)
        case inp =>
          parseBetInput(inp, controller.getBudget) match {
            case Left(err)  => println(s"Fehler: $err")
            case Right(bet) =>
              controller.placeBet(bet)
              valid = true
          }
      }
    }
  }

  private def printMenu(): Unit = {
    val opts = formatMenuOptions(controller.getState, controller.getBudget)
    println("\n" + opts.mkString(" | "))
  }

  private def renderPartial(): Unit = {
    val state = controller.getState
    println("\n" + hBorder())
    println(padCenter("DEINE HAND"))
    println(hBorder())
    // Dealer zeigt nur die erste Karte
    val dFirst = state.dealer.cards.head
    println(s"Dealer: ${dFirst} [???]")
    // Spieler: alle Karten
    val hand = state.playerHands(state.activeHand)
    val cardsStr = hand.cards.mkString(" ")
    println(s"Spieler (${state.activeHand + 1}/${state.playerHands.size}): $cardsStr (Wert: ${hand.value})")
    println(hBorder())
  }

  private def renderFull(): Unit = {
    val state = controller.getState
    println("\n" + hBorder())
    println(padCenter("ERGEBNISSE DER RUNDE"))
    println(hBorder())
    // Dealer: alle Karten
    val dealerCards = state.dealer.cards.mkString(" ")
    println(s"Dealer: $dealerCards (Wert: ${state.dealer.value})\n")

    // Spielerhände + Einsatz
    state.playerHands.zip(state.bets).zipWithIndex.foreach {
      case ((hand, bet), idx) =>
        val cards = hand.cards.mkString(" ")
        println(f"Hand ${idx + 1}: $cards (Wert: ${hand.value}) Einsatz: €$bet%.2f")
    }

    // Wer gewinnt?
    state.playerHands.foreach { hand =>
      val p = hand.value
      val d = state.dealer.value
      val result =
        if (p > 21) "Du bist Bust – Dealer gewinnt."
        else if (d > 21) "Dealer ist Bust – Du gewinnst!"
        else if (p > d) "Du gewinnst!"
        else if (p < d) "Dealer gewinnt!"
        else "Push – unentschieden"
      println(result)
    }

    println("\n" + hBorder('-'))
  }

  def parseBetInput(input: String, budget: Double): Either[String, Double] = {
    if (input.equalsIgnoreCase("H")) Left("QUIT")
    else scala.util.Try(input.toDouble).toOption match {
      case None                 => Left("Bitte eine gültige Zahl eingeben.")
      case Some(bet) if bet <= 0 => Left("Einsatz muss > 0 sein.")
      case Some(bet) if bet > budget * 0.9 =>
        Left(f"Einsatz darf max €${budget * 0.9}%.2f sein.")
      case Some(bet)            => Right(bet)
    }
  }

  /** Menü-Optionen (keine I/O) */
  def formatMenuOptions(state: GameState, budget: Double): Seq[String] = {
    val idx  = state.activeHand
    val hand = state.playerHands(idx)

    val base = Seq("[H]it", "[S]tand", "[Q]uit")
    val dbl  = if (hand.cards.size == 2 && budget >= state.bets(idx)) Seq("[D]ouble") else Nil
    val spl  = if (
      hand.cards.size == 2 &&
        hand.cards(0).value == hand.cards(1).value &&
        budget >= state.bets(idx)
    ) Seq("[P]split") else Nil

    base ++ dbl ++ spl
  }
}
