package de.htwg.Player

import de.htwg.Card.Card
import de.htwg.game.ArschlochGame

import scala.annotation.tailrec

case class Player(
                   name: String,
                   hand: Array[Card],
                   rank: Option[Int] = None,
                   isHuman: Boolean = true
                 ) {

  /**
   * Spielt eine Karte oder mehrere Karten (bei Human) bzw. eine oder mehrere Karten (bei KI).
   *
   * @param lastPlayed Optional die zuletzt gespielte Karte(n).
   * @param indexProvider Funktion, um die ausgewählten Indizes zu liefern (1-basiert).
   *                      Rückgabe Array(0) bedeutet Passen.
   * @return Tuple: Opt. gespielte Karten und aktualisierter Player.
   */
  def playCard(
                lastPlayed: Option[Array[Card]],
                indexProvider: () => Array[Int]
              ): (Option[Array[Card]], Player) = {

    // KI-Logik ignoriert indexProvider
    if (!isHuman) {
      if (rank.isDefined) return (None, this)
      lastPlayed match {
        case None =>
          val maxVal  = hand.map(c => ArschlochGame.getValue(c.value)).max
          val play    = hand.filter(c => ArschlochGame.getValue(c.value) == maxVal)
          val newHand = hand.diff(play)
          (Some(play), this.copy(hand = newHand))
        case Some(prev) =>
          val requiredSize = prev.length
          val prevValue    = ArschlochGame.getValue(prev.head.value)

          val playable = hand
            .groupBy(_.value)
            .values
            .filter(arr => arr.length == requiredSize)
            .filter(arr => ArschlochGame.getValue(arr.head.value) > prevValue)
            .toSeq

          if (playable.nonEmpty) {
            val bestGroup = playable.minBy(arr => ArschlochGame.getValue(arr.head.value))
            val newHand   = hand.diff(bestGroup)
            (Some(bestGroup), this.copy(hand = newHand))
          } else (None, this)
      }
    } else {
      // Menschlicher Spieler
      @tailrec
      def loop(currentHand: Array[Card]): (Option[Array[Card]], Player) = {
        // Karten anzeigen
        println(s"$name, deine Karten:")
        def printHand(list: Array[Card], idx: Int): Unit = list match {
          case Array() => ()
          case Array(head, tail @ _*) =>
            println(s"$idx: ${head.value}${head.suit}")
            printHand(tail.toArray, idx + 1)
        }
        printHand(currentHand, 1)
        println("0: Passen")

        val idxs = indexProvider()
        if (idxs.sameElements(Array(0))) return (None, this)

        // Validierung
        if (idxs.exists(i => i < 1 || i > currentHand.length)) {
          println("Ungültige Zahlenauswahl!")
          return loop(currentHand)
        }

        val chosen = idxs.map(i => currentHand(i - 1))
        lastPlayed match {
          case Some(prev) if chosen.length != prev.length =>
            println("Ungültige Anzahl Karten!")
            loop(currentHand)
          case Some(prev) if chosen.exists(c => ArschlochGame.getValue(c.value) <= ArschlochGame.getValue(prev.head.value)) =>
            println("Karten müssen höher sein!")
            loop(currentHand)
          case _ =>
            val newHand = currentHand.diff(chosen)
            println(s"$name spielt: ${chosen.map(c => s"${c.value}${c.suit}").mkString(", ")}")
            (Some(chosen), this.copy(hand = newHand))
        }
      }
      loop(hand)
    }
  }
}
