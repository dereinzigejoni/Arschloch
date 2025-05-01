package de.htwg.View.UI

/** Abstrakte Ein/Ausgabe-Schnittstelle */
trait UI {
  def printLine(msg: String): Unit
  def readLine(prompt: String = ""): String
}
