// src/main/scala/blackjack/view/TuiView.scala
package de.htwg.blackjack.view

import de.htwg.blackjack.controller.GameController
import de.htwg.blackjack.model._

object TuiView {


  /** Formatiert die Anzeige der aktuellen (aktiven) Hand und der verdeckten Dealer-Karte */
 def formatRenderPartial(state: GameState): Seq[String] = {
    val hand = state.playerHands(state.activeHand)
    Seq(
      "\n========== BLACKJACK ==========",
      s"Dealer: ${state.dealer.cards.head} [???]",
      s"Spieler Hand ${state.activeHand + 1}/${state.playerHands.size}: ${hand} (Wert: ${hand.value})"
    )
  }

  def parseBetInput(input: String, budget: Double): Either[String, Double] = {
    if (input.equalsIgnoreCase("H")) Left("QUIT")
    else {
      try {
        val bet = input.toDouble
        if (bet <= 0) Left("Einsatz muss > 0 sein")
        else if (bet > budget * 0.9) Left(f"Einsatz muss ≤ 90%% (max ${budget * 0.9}%.2f)")
        else Right(bet)
      } catch {
        case _: NumberFormatException => Left("Bitte eine Zahl eingeben.")
      }
    }
  }

  private val controller = new GameController()
  def printWelcome(): Unit = println("Willkommen zu Blackjack")

  def run(): Unit = {
    printWelcome()
    var playing = true
    while (playing) {
      askBet()
      var inRound = true
      while (inRound) {
        renderPartial()
        // Menü dynamisch je nach Splitt-/Double-Möglichkeit
        val st    = controller.getState
        val hand  = st.playerHands(st.activeHand)
        val opts  = Seq(
          "[H]it",
          "[S]tand"
        ) ++
          (if (hand.cards.size == 2 && controller.getBudget >= controller.getState.bets(st.activeHand)) Seq("[D]ouble") else Nil) ++
          (if (hand.cards.size == 2 && hand.cards(0).rank == hand.cards(1).rank && controller.getBudget >= controller.getState.bets(st.activeHand)) Seq("[P]split") else Nil) ++
          Seq("[Q]uit")
        println(opts.mkString(" ", " ", ""))
        scala.io.StdIn.readLine().toUpperCase match {
          case "H" => controller.playerHit()
          case "S" => controller.playerStand()
          case "D" => controller.playerDouble()
          case "P" => controller.playerSplit()
          case "Q" =>
            println("Auf Wiedersehen!")
            sys.exit(0)
          case _ => println("Ungültige Eingabe")
        }
        // Wechsle oder Ende
        inRound = controller.getState.status == InProgress
      }
      renderFull()
      controller.resolveBet()
      println(f"Dein aktuelles Budget: ${controller.getBudget}%.2f")
      println("[N]eues Spiel, [Q]uit?")
      scala.io.StdIn.readLine().toUpperCase match {
        case "N" => // weiter
        case "Q" =>
          println("Game over. Danke fürs Spielen!")
          playing = false
        case _ =>
          println("Ungültige Eingabe, beende das Spiel.")
          playing = false
      }
    }
  }


  def askBet(): Unit = {
    var valid = false
    while (!valid) {
      val budget = controller.getBudget
      val maxBet = budget * 0.9
      println(f"Dein Budget: $budget%.2f – Maximaler Einsatz: $maxBet%.2f")
      print("Bitte deinen Einsatz eingeben (H für Quit): ")
      val inp = scala.io.StdIn.readLine()
      parseBetInput(inp, budget) match {
        case Left("QUIT") =>
          println("Auf Wiedersehen!")
          sys.exit()                         // und hier
        case Left(err)    => println(err)
        case Right(bet)   =>
          controller.placeBet(bet)
          valid = true
      }
    }
  }

  private def renderPartial(): Unit =
    formatRenderPartial(controller.getState).foreach(println)

  private def renderFull(): Unit = {
    formatRenderFull(controller.getState).foreach(println)
  }

  /** Formatiert die komplette Runde (alle Hände + Dealer + Resultate) */
  def formatRenderFull(state: GameState): Seq[String] = {
    val header = "\n======= RUNDENRESULTAT ======="
    val dealer = s"Dealer: ${state.dealer} (Wert: ${state.dealer.value})"
    // Für jede Spielerhand eine Zeile mit Hand-Nummer, Karten und Wert
    val playerLines = state.playerHands.zipWithIndex.map { case (h, idx) =>
      s"Hand ${idx + 1}: ${h} (Wert: ${h.value}) Einsatz: ${state.bets(idx)}"
    }
    // Ergebnis pro Hand
    val resultLines = state.playerHands.zip(state.bets).map { case (h, bet) =>
      state.status match {
        case PlayerBust if state.activeHand == 0 /*nur erste Hand verliert*/ =>
          "Du bist Bust – Dealer gewinnt."
        case DealerBust =>
          "Dealer ist Bust – Du gewinnst!"
        case Finished =>
          val p  = h.value
          val d  = state.dealer.value
          if (p > d) "Du gewinnst!"
          else if (p < d) "Dealer gewinnt!"
          else "Push – unentschieden"
        case _ => ""
      }
    }
    Seq(header, dealer) ++ playerLines ++ resultLines.filter(_.nonEmpty)
  }

}
