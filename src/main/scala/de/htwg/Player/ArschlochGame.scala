package de.htwg.Player
import scala.annotation.tailrec
import scala.io.StdIn.readLine
import scala.util.Random

object ArschlochGame {
  val suits = List("♥", "♦", "♠", "♣")
  val values = List("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")

  def getValue(card: String): Int = card match {
    case "J" => 11
    case "Q" => 12
    case "K" => 13
    case "A" => 14
    case _ => card.toInt
  }

  def createDeck(): List[Card] =
    for { suit <- suits; value <- values } yield Card(value, suit)

  def shuffleAndDeal(players: List[Player]): List[Player] = {
    val deck = Random.shuffle(createDeck())
    val cardsPerPlayer = deck.length / players.length

    players.zipWithIndex.map { case (player, i) =>
      player.copy(hand = deck.slice(i * cardsPerPlayer, (i + 1) * cardsPerPlayer))
    }
  }

  def exchangeCards(president: Player, arschloch: Player): (Player, Player) = {
    val sortedArschlochHand = arschloch.hand.sortBy(c => getValue(c.value))(Ordering[Int].reverse)
    val sortedPresidentHand = president.hand.sortBy(c => getValue(c.value))


    val cardsFromArschloch = sortedArschlochHand.take(2)
    val cardsFromPresident = sortedPresidentHand.take(2)

    println(s"\n🔁 Kartentausch:")
    println(s"${arschloch.name} gibt seine besten Karten an ${president.name}: ${cardsFromArschloch.mkString(", ")}")
    println(s"${president.name} gibt seine schlechtesten Karten an ${arschloch.name}: ${cardsFromPresident.mkString(", ")}")

    val updatedArschloch = arschloch.copy(
      hand = (arschloch.hand.diff(cardsFromArschloch)) ++ cardsFromPresident
    )

    val updatedPresident = president.copy(
      hand = (president.hand.diff(cardsFromPresident)) ++ cardsFromArschloch
    )

    (updatedPresident, updatedArschloch)
  }


  def playRound(players: List[Player]): List[Player] = {
    println("\n🔄 Eine neue Runde beginnt...")

    def playTurn(players: List[Player], lastPlayed: Option[List[Card]], ranking: List[Player], passCounter: Int, resetCounter: Int = 0): List[Player] = {
      val remainingPlayers = players.filter(_.hand.nonEmpty)

      if (remainingPlayers.length == 1) {
        val lastPlayer = remainingPlayers.head
        println(s"\n💩 ${lastPlayer.name} ist das Arschloch, weil er als Letzter Karten hat!")
        return (ranking :+ lastPlayer.copy(rank = Some(players.length - 1))).reverse
      }

      println(s"\nAktuelle oberste Karte(n): ${lastPlayed.map(_.mkString(", ")).getOrElse("Kein Stapel")}")

      val currentPlayer = players.head

      if (currentPlayer.hand.isEmpty) {
        return playTurn(players.tail, lastPlayed, ranking, passCounter, resetCounter)
      }

      val (playedCards, updatedPlayer) = currentPlayer.playCard(lastPlayed)
      
      
      val newLastPlayed = playedCards.orElse(lastPlayed)
      val newRanking = if (updatedPlayer.hand.isEmpty) ranking :+ updatedPlayer else ranking
      val newPassCounter = if (playedCards.isEmpty) passCounter + 1 else 0

      if (newPassCounter == players.length || (remainingPlayers.length == 2 && newPassCounter == 2)) {
        if (resetCounter >= 50) {
          println("\n⚠ Keiner kann mehr spielen! Neue Runde wird gestartet.")
          return ranking.reverse
        }
        println("\n🔄 Alle haben gepasst oder nur zwei Spieler übrig: Stapel wird erneuert!")
        return playTurn(players.tail :+ updatedPlayer, None, newRanking, 0, resetCounter + 1)
      }

      val nextPlayers = players.tail :+ updatedPlayer

      playTurn(nextPlayers, newLastPlayed, newRanking, newPassCounter, resetCounter)
    }
    val ranking = playTurn(players, None, List(), 0)

    if (ranking.isEmpty || ranking.length < 2) {
      println("\n⚠ Fehler: Ungültige Rangliste. Das Spiel wird neu gestartet.")
      return players
    }

    val updatedPlayers = ranking.zipWithIndex.map { case (p, rank) => p.copy(rank = Some(rank)) }

    val president = updatedPlayers.last
    val arschloch = updatedPlayers.head

    if (president == arschloch) {
      println("\n⚠ Fehler: Präsident und Arschloch sind identisch. Das Spiel wird neu gestartet.")
      return players
    }

    println(s"\n🏆 ${president.name} ist Präsident! 💩 ${arschloch.name} ist Arschloch!")

   
    
    val (newPresident, newArschloch) = exchangeCards(president, arschloch)
    val finalPlayers = updatedPlayers.map {
      case p if p.name == president.name => newPresident
      case p if p.name == arschloch.name => newArschloch
      case other => other
    }

    finalPlayers
  }


  @tailrec
  def mainGameLoop(players: List[Player]): Unit = {
    println("\n🎲 Spiel startet mit folgenden Spielern:")
    players.foreach(p => println(s"- ${p.name} (Mensch: ${p.isHuman})"))

    val newPlayers = playRound(shuffleAndDeal(players))
    println("\n--- Drücke 'a' für die nächste Runde oder 'q' zum Beenden ---")
    val input = readLine()
    if (input.toLowerCase == "q") {
      println("👋 Spiel beendet! Danke fürs Spielen!")
    }else {
      mainGameLoop(newPlayers)
    }
  }

  def askForPlayers(): List[Player] = {
    println("\n🎭 Willkommen bei Arschloch!")

    // Anzahl der Spieler (min. 3, max. 6)
    val totalPlayers = {
      print("Wie viele Spieler insgesamt? (3-6): ")
      readLine().toIntOption match {
        case Some(n) if n >= 3 && n <= 6 => n
        case _ =>
          println("Ungültige Eingabe! Standardmäßig 4 Spieler.")
          4
      }
    }

    // Anzahl der menschlichen Spieler (mind. 1, max. totalPlayers)
    val numHumans = {
      print(s"Wie viele davon sind menschliche Spieler? (1-$totalPlayers): ")
      readLine().toIntOption match {
        case Some(n) if n >= 1 && n <= totalPlayers => n
        case _ =>
          println("Ungültige Eingabe! Standardmäßig 2 menschliche Spieler.")
          2
      }
    }

    // Menschliche Spieler hinzufügen
    val humanPlayers = (1 to numHumans).map { i =>
      print(s"Name von Spieler $i: ")
      val name = readLine().trim
      Player(if (name.nonEmpty) name else s"Spieler$i", List(), 0, isHuman = true)
    }.toList

    // KI-Spieler hinzufügen
    val aiPlayers = (1 to (totalPlayers - numHumans)).map(i => Player(s"KI-$i", List(), 0, isHuman = false)).toList

    humanPlayers ++ aiPlayers
  }

}