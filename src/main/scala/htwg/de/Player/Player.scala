package htwg.de.Player

import htwg.de.Card.Card
import htwg.de.Game.ArschlochGame

import scala.io.StdIn.readLine
case class Player(name: String, hand: List[Card], points: Int, isHuman: Boolean, rank: Option[Int] = None) {
  def playCard(
                lastPlayed: Option[List[Card]],
                inputProvider: () => String = () => readLine().trim
              ): (Option[List[Card]], Player) = {
    if (rank.isDefined) return (None, this)
    if (isHuman && hand.nonEmpty) {
      val sortedHand = hand.sortBy(card => ArschlochGame.getValue(card.value))
      println(s"\n$name, deine aktuellen Karten:")
      sortedHand.zipWithIndex.foreach { case (card, index) =>
        println(s"${index + 1}: $card")
      }
      println("0: Passen")
      val input = inputProvider()
      if (input == "0") {
        println(s"$name passt.")
        return (None, this.copy(hand = sortedHand))
      }
      val indicesEither =
        try {
          Right(input.split(",").map(_.trim.toInt).toList)
        } catch {
          case _: Exception => Left("Ungültige Eingabe")
        }

      indicesEither match {
        case Left(_) =>
          println("Ungültige Eingabe! Bitte versuche es erneut.")
          playCard(lastPlayed, inputProvider)
        case Right(indices) =>
          if (indices.exists(i => i < 1 || i > sortedHand.size)) {
            println("Ungültige Zahlenauswahl!")
            return playCard(lastPlayed, inputProvider)
          }
          if (lastPlayed.isDefined && indices.length != lastPlayed.get.length) {
            println("Ungültiger Zug! Du musst die gleiche Anzahl von Karten spielen wie der vorherige Spieler.")
            return playCard(lastPlayed, inputProvider)
          }
          val chosenCards = indices.map(i => sortedHand(i - 1))
          if (lastPlayed.isEmpty || ArschlochGame.getValue(chosenCards.head.value) > ArschlochGame.getValue(lastPlayed.get.head.value)) {
            println(s"$name spielt: ${chosenCards.mkString(", ")}")
            val indicesSet = indices.map(_ - 1).toSet
            val newHandUnsorted = sortedHand.zipWithIndex.filterNot { case (_, idx) => indicesSet.contains(idx) }.map(_._1)
            val newHand = newHandUnsorted.sortBy(card => ArschlochGame.getValue(card.value))
            (Some(chosenCards), this.copy(hand = newHand))
          } else {
            println("Ungültiger Zug! Die Karten müssen stärker sein.")
            playCard(lastPlayed, inputProvider)
          }
      }
    } else {
      // KI-Logik – unverändert
      val groupedCards = hand.groupBy(_.value).values.toList
      val playableGroups = groupedCards.filter(cards =>
        lastPlayed.isEmpty || (lastPlayed.get.length == cards.length && ArschlochGame.getValue(cards.head.value) > ArschlochGame.getValue(lastPlayed.get.head.value))
      )
      val chosenCards = if (hand.length < 5) {
        playableGroups.sortBy(cards => -ArschlochGame.getValue(cards.head.value)).headOption
      } else {
        playableGroups.sortBy(cards => ArschlochGame.getValue(cards.head.value)).headOption
      }
      chosenCards match {
        case Some(cards) =>
          println(s"$name (KI) spielt: ${cards.mkString(", ")}")
          (Some(cards), this.copy(hand = hand.diff(cards)))
        case None =>
          println(s"$name (KI) kann nicht spielen und passt.")
          (None, this)
      }
    }
  }



}
