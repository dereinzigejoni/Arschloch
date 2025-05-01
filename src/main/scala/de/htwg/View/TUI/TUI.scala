package de.htwg.View.TUI

import de.htwg.Model.Card.Card
import de.htwg.View.UI.UI

class TUI extends UI {
  override def printLine(msg: String): Unit = println(msg)

  override def readLine(prompt: String): String = {
    if (prompt.nonEmpty) print(prompt)
    scala.io.StdIn.readLine()
  }

  /** Rekursive Ausgabe einer Kartenliste mit Indizes */
  def printCards(cards: List[Card], index: Int = 1): Unit =
    cards match {
      case Nil => ()
      case c :: cs =>
        printLine(s"${index}: ${c.value}${c.suit}")
        printCards(cs, index + 1)
    }

  def render(card: Card): String = s"${card.value}${card.suit}"
}
