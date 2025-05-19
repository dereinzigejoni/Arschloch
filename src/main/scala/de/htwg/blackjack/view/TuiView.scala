// src/main/scala/de/htwg/blackjack/view/TuiView.scala
package de.htwg.blackjack.view

import scala.util.{Failure, Success, Try}
import de.htwg.blackjack.controller.{SharedGameController, GameObserver}
import de.htwg.blackjack.model.*
import de.htwg.blackjack.state.GamePhases.*

object TuiView extends GameObserver{
  private val controller = SharedGameController.instance
  controller.addObserver(this)
  private val lineWidth = 40
  private def hBorder(c: Char = '='): String = c.toString * lineWidth
  private def padCenter(text: String): String = {
    val padding = (lineWidth - text.length) / 2
    " " * padding + text + " " * (lineWidth - text.length - padding)
  }

  override def update(gs: GameState): Unit = {
    gs.phase match {
      case PlayerTurn => renderPartial().foreach(println)
      case FinishedPhase | DealerBustPhase | PlayerBustPhase =>
        renderFull().foreach(println)
      case _ => // keine Ausgabe
    }
  }
  def printWelcome(): Unit = {
    println(hBorder())
    println(padCenter("WILLKOMMEN ZU BLACKJACK"))
    println(hBorder())
  }
  private def processChoice(choice: String): Unit = Option(choice) match {
    case Some("H") => wrap(controller.playerHit(), "Hit")
    case Some("S") => wrap(controller.playerStand(), "Stand")
    case Some("D") => wrap(controller.playerDouble(), "Double")
    case Some("P") => wrap(controller.playerSplit(), "Split")
    case Some("U") => controller.undo() match {
      case Some(_) => println("Letzter Schritt wurde rückgängig gemacht."); renderPartial()
      case None => println("Nichts zum Rückgängig machen.")
    }
    case Some("R") => controller.redo() match {
      case Some(_) => println("Schritt wiederhergestellt."); renderPartial()
      case None => println("Nichts zum Wiederherstellen.")
    }
    case Some("Q") =>
      println("\nAuf Wiedersehen!"); sys.exit(0)
    case _ =>
      println("Ungültige Eingabe. Bitte H, S, D, P, U, R oder Q.")
  }
  private def wrap(t: Try[GameState], action: String): Unit = t match {
    case Success(_) => () // alles gut, einfach weiter
    case Failure(ex) => println(s"Fehler bei $action: ${ex.getMessage}")
  }
  def run(): Unit = {
    //printWelcome()
    
    var playing = true
    askBet() // löst über Observer initiales renderPartial() aus
    
    while (playing) {
      printMenu()
      scala.util.Try(scala.io.StdIn.readLine().trim.toUpperCase) match {
        case scala.util.Failure(ex) => println(s"Eingabefehler: ${ex.getMessage}")
        
      case scala.util.Success(cmd) =>
        cmd match {
        case "H" => controller.playerHit()
          
        case "S" => controller.playerStand()
          
        case "D" => controller.playerDouble()
          
        case "P" => controller.playerSplit()
          
        case "U" => controller.undo()
          
        case "R" => controller.redo()
          
        case "Q" => playing = false
          
        case _ => println(s"Unbekannter Befehl: $cmd")
          
        }
        
        if (controller.getState.phase != PlayerTurn) {
          controller.resolveBet() // löst finales renderFull() via Observer aus
            println(f"\nDein aktuelles Budget: €${controller.getBudget}%.2f\n")
            println(hBorder('-'))
             println(padCenter("[N] Neues Spiel    [Q] Quit"))
            println(hBorder('-'))
            scala.io.StdIn.readLine().trim.toUpperCase match {
           case "N" => askBet()
            
          case _ => playing = false
            
          }
          
        }
        
      }
      
    }
  }
  def askBet(): Unit = {
    var valid = false
    while (!valid) {
      val bgt    = controller.getBudget
      val maxBet = bgt * 0.9
      println(f"Dein Budget: €$bgt%.2f – Maximaler Einsatz: €$maxBet%.2f")
      print("Bitte deinen Einsatz eingeben (H für Quit): ")

      val inputTry = Try(scala.io.StdIn.readLine())
      inputTry match {
        case Failure(ex) =>
          println(s"Eingabefehler: ${ex.getMessage}")
        case Success(inp) =>
          parseBetInput(inp, bgt) match {
            case Left("QUIT") =>
              println("Auf Wiedersehen!"); sys.exit(0)
            case Left(err) =>
              println(err)
            case Right(bet) =>
              // wir nutzen jetzt die Try-Variante
              controller.tryPlaceBet(bet) match {
                case Success(_) => valid = true
                case Failure(ex) => println(s"Einsatz-Fehler: ${ex.getMessage}")
              }
          }
      }
    }
  }
  private def printMenu(): Unit = {
    val opts = formatMenuOptions(controller.getState, controller.getBudget)
    println("\n" + opts.mkString(" | "))
  }

  def renderPartial(): Seq[String] = {
    val state = controller.getState
    val border = hBorder()
    val header = padCenter("DEINE HAND")
    val dFirst = state.dealer.cards.head
    val dealerLine = s"Dealer: $dFirst [???]"
    val hand = state.playerHands(state.activeHand)
    val cardsStr = hand.cards.mkString(" ")
    val playerLine = s"Spieler (${state.activeHand + 1}/${state.playerHands.size}): $cardsStr (Wert: ${hand.value})"

    val lines = Seq(
      "",
      border,
      header,
      border,
      dealerLine,
      playerLine,
      border
    )
    lines.foreach(println)
    lines
  }

  /** Zeigt alle Dealer-Karten, alle Spielerhände + Ergebnisse */
  def renderFull(): Seq[String] = {
    val state = controller.getState
    val topBorder = "\n" + hBorder()
    val header = padCenter("ERGEBNISSE DER RUNDE")
    val midBorder = hBorder()
    val dealerCards = state.dealer.cards.mkString(" ")
    val dealerLine = s"Dealer: $dealerCards (Wert: ${state.dealer.value})\n"

    // jede Hand mit Einsatz
    val playerLines = state.playerHands.zip(state.bets).zipWithIndex.map {
      case ((hand, bet), idx) =>
        val cs = hand.cards.mkString(" ")
        f"Hand ${idx + 1}: $cs (Wert: ${hand.value}) Einsatz: €$bet%.2f"
    }

    // Ergebniszeilen
    val resultLines = state.playerHands.map { hand =>
      val p = hand.value
      val d = state.dealer.value
      if (p > 21) "Du bist Bust – Dealer gewinnt."
      else if (d > 21) "Dealer ist Bust – Du gewinnst!"
      else if (p > d) "Du gewinnst!"
      else if (p < d) "Dealer gewinnt!"
      else "Push – unentschieden"
    }

    val bottomBorder = "\n" + hBorder('-')

    val lines = Seq(
      topBorder,
      header,
      midBorder,
      dealerLine
    ) ++
      playerLines ++
      resultLines ++
      Seq(bottomBorder)

    lines.foreach(println)
    lines
  }

  def parseBetInput(input: String, budget: Double): Either[String, Double] = {
    if (input.equalsIgnoreCase("H")) Left("QUIT")
    else Try(input.toDouble).toOption match {
      case None => Left("Bitte eine gültige Zahl eingeben.")
      case Some(bet) if bet <= 0 => Left("Einsatz muss > 0 sein.")
      case Some(bet) if bet > budget * 0.9 => Left(f"Einsatz darf max €${budget * 0.9}%.2f sein.")
      case Some(bet) => Right(bet)
    }
  }
  def formatMenuOptions(state: GameState, budget: Double): Seq[String] = {
    val idx  = state.activeHand
    val hand = state.playerHands(idx)
    val base = Seq("[H]it", "[S]tand", "[U]ndo", "[R]edo", "[Q]uit")
    val dbl  = if (hand.cards.size == 2 && budget >= state.bets(idx)) Seq("[D]ouble") else Nil
    val spl  = if (hand.cards.size == 2 && hand.cards.head.value == hand.cards(1).value && budget >= state.bets(idx)) Seq("[P]split") else Nil
    base ++ dbl ++ spl
  }
}
