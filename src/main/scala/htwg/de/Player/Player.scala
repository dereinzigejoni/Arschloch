package htwg.de.Player

import htwg.de.Card.Card
import htwg.de.Game.ArschlochGame

import scala.io.StdIn.readLine
case class Player(name: String, hand: List[Card], points: Int, isHuman: Boolean, rank: Option[Int] = None) {
  def playCard(lastPlayed: Option[List[Card]]): (Option[List[Card]], Player) = {
    if (rank.isDefined) return (None, this)
    if (isHuman && hand.nonEmpty) {
      println(s"\n$name, deine aktuellen Karten:")
      hand.groupBy(_.value).values.toList.zipWithIndex.foreach { case (cards, index) =>
        println(s"${index + 1}: ${cards.mkString(", ")}")
      }
      println("0: Passen")
      val choice = readLine().toIntOption.getOrElse(0)
      val groupedCards = hand.groupBy(_.value).values.toList
      if (choice >= 1 && choice <= groupedCards.length) {
        val chosenCards = groupedCards(choice - 1)
        if (lastPlayed.isDefined && chosenCards.length != lastPlayed.get.length) {
          println("Ungültiger Zug! Du musst die gleiche Anzahl von Karten spielen wie der vorherige Spieler.")
          return playCard(lastPlayed)
        }
        if (lastPlayed.isEmpty || ArschlochGame.getValue(chosenCards.head.value) > ArschlochGame.getValue(lastPlayed.get.head.value)) {
          println(s"$name spielt: ${chosenCards.mkString(", ")}")
          (Some(chosenCards), this.copy(hand = hand.diff(chosenCards)))
        } else {
          println("Ungültiger Zug! Die Karten müssen stärker sein.")
          playCard(lastPlayed)
        }
      } else {
        println(s"${name} passt.")
        (None, this)
      }
    } else {
      // KI-Logik
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
          println(s"${name} (KI) spielt: ${cards.mkString(", ")}")
          (Some(cards), this.copy(hand = hand.diff(cards)))
        case None =>
          println(s"${name} (KI) kann nicht spielen und passt.")
          (None, this)
      }
    }
  }

}
