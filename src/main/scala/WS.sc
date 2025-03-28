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

  def playRound(players: List[Player]): List[Player] = {
    println("\n🔄 Eine neue Runde beginnt...")

    def playTurn(players: List[Player], lastPlayed: Option[List[Card]], ranking: List[Player], passCounter: Int, resetCounter: Int = 0): List[Player] = {
      val remainingPlayers = players.filter(_.hand.nonEmpty)

      // **Check: Nur noch ein Spieler übrig?**
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

      // **Maximale Anzahl von Stapel-Resets verhindern**
      if (newPassCounter == players.length || (remainingPlayers.length == 2 && newPassCounter == 2)) {
        if (resetCounter >= 5) { // 5 Resets in Folge -> Abbruch
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

    // **Rollenvergabe KORRIGIEREN**
    val updatedPlayers = ranking.zipWithIndex.map { case (p, rank) => p.copy(rank = Some(rank)).updatePoints(rank) }

    val president = updatedPlayers.head
    val arschloch = updatedPlayers.last

    // **Doppelbelegung vermeiden**
    if (president == arschloch) {
      println("\n⚠ Fehler: Präsident und Arschloch sind identisch. Das Spiel wird neu gestartet.")
      return players
    }

    println(s"\n🏆 ${president.name} ist Präsident! 💩 ${arschloch.name} ist Arschloch!")

    updatedPlayers
  }


  def mainGameLoop(players: List[Player]): Unit = {
    println("\n🎲 Spiel startet mit folgenden Spielern:")
    players.foreach(p => println(s"- ${p.name} (Mensch: ${p.isHuman})"))

    val newPlayers = playRound(shuffleAndDeal(players))

    println("\n📊 Aktueller Punktestand:")
    newPlayers.foreach(p => println(s"${p.name}: ${p.points} Punkte"))

    println("\n--- Drücke ENTER für die nächste Runde oder 'q' zum Beenden ---")
    val input = readLine()
    if (input.toLowerCase == "q") {
      println("👋 Spiel beendet! Danke fürs Spielen!")
      return
    }

    mainGameLoop(newPlayers)
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
import scala.io.StdIn.readLine

case class Player(name: String, hand: List[Card], points: Int, isHuman: Boolean, rank: Option[Int] = None) {

  def playCard(lastPlayed: Option[List[Card]]): (Option[List[Card]], Player) = {
    if (rank.isDefined) {
      return (None, this) // Spieler mit Rang überspringen
    }

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
          println(s"${name} spielt: ${chosenCards.mkString(", ")}")
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

  def updatePoints(rank: Int): Player = rank match {
    case 0 if isHuman => this.copy(points = points + 1000) // Präsident
    case last if last == 3 && isHuman => this.copy(points = points - 500) // Arschloch
    case _ if isHuman => this.copy(points = points + 200) // Bürger erhalten kleine Boni
    case _ => this
  }
}

case class Card(value: String, suit: String) {
  override def toString: String = s"$value$suit"
}
