package de.htwg.blackjack.view
import de.htwg.blackjack.controller.GameController
import de.htwg.blackjack.model.Hand

import scala.io.StdIn
import scala.util.{Failure, Try}


class TuiView(ctrl: GameController) extends Observer {
  ctrl.register(this)
  def run(): Unit = {
    println("=== Blackjack TUI ===")
    var cont = true
    while (cont) {
      println(s"Budget: ${ctrl.getPlayerBudget}")
      println(s"Setze (max ${ctrl.getPlayerBudget/2}):")
      Try(StdIn.readInt()) match {
        case Failure(e) => println("Ungültige Eingabe")
        case scala.util.Success(bet) =>
          ctrl.startNewRound(bet).recover { case x => println(x.getMessage) }
      }
      while (!ctrl.isGameOver) {
        println("1=Hit, 2=Stand, 3=Undo")
        StdIn.readInt() match {
          case 1 => ctrl.hit().recover {case x => println(x.getMessage)}
          case 2 => ctrl.stand().recover{case x => println(x.getMessage)}
          case 3 => ctrl.undo()
          case _ => println("Falsche Wahl")
        }
      }
      println("Nochmal? y/n")
      cont = StdIn.readLine().toLowerCase == "y"
    }
  }

  override def update(p: Hand, d: Hand, over: Boolean): Unit = {
    println(s"Dealer: ${d.cards} (${d.bestValue}), Spieler: ${p.cards} (${p.bestValue})" +
      (if (over) " -> Spiel vorbei" else ""))
  }

}
