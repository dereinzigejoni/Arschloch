package htwg.de.Game
import htwg.de.Card.Card
import htwg.de.Player.Player
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

    // Beim Präsidenten sollen die Karten NICHT entfernt werden, sondern hinzugefügt werden
    val updatedPresident = president.copy(hand = president.hand ++ cardsFromArschloch)
    val updatedArschloch = arschloch.copy(hand = arschloch.hand.diff(cardsFromArschloch) ++ cardsFromPresident)

    (updatedPresident, updatedArschloch)
  }
  def playRound(players: List[Player]): List[Player] = {
    println("\n🔄 Eine neue Runde beginnt...")
    def playTurn(
                  players: List[Player],
                  lastPlayed: Option[List[Card]],
                  ranking: List[Player],
                  passCounter: Int,
                  resetCounter: Int = 0
                ): List[Player] = {
      // Falls die Liste leer ist, geben wir das (umgekehrte) Ranking zurück.
      if (players.isEmpty) return ranking.reverse

      // Aktive Spieler sind diejenigen, die noch Karten haben.
      val remainingPlayers = players.filter(_.hand.nonEmpty)

      // Wenn nur noch ein aktiver Spieler übrig ist und alle anderen bereits fertig sind,
      // beende die Runde, indem dieser Spieler als Letzter eingestuft wird.
      if (remainingPlayers.length == 1 && ranking.length + 1 == players.length) {
        val lastPlayer = remainingPlayers.head
        println(s"\n💩 ${lastPlayer.name} ist das Arschloch, weil er als Letzter Karten hat!")
        return (ranking :+ lastPlayer.copy(rank = Some(players.length - 1))).reverse
      }

      println(s"\nAktuelle oberste Karte(n): ${lastPlayed.map(_.mkString(", ")).getOrElse("Kein Stapel")}")
      val currentPlayer = players.head

      // Wenn der aktuelle Spieler keine Karten mehr hat, überspringe ihn,
      // aber lasse ihn in der Liste, um die Spielreihenfolge beizubehalten.
      if (currentPlayer.hand.isEmpty) {
        println(s"${currentPlayer.name} hat keine Karten mehr und wird übersprungen.")
        // Spieler bleibt in der Rotation, wird aber ans Ende der Liste gesetzt.
        val nextPlayers = players.tail :+ currentPlayer
        return playTurn(nextPlayers, lastPlayed, ranking, passCounter, resetCounter)
      }

      // Der aktuelle Spieler führt seinen Zug aus.
      val (playedCards, updatedPlayer) = currentPlayer.playCard(lastPlayed)
      val newLastPlayed = playedCards.orElse(lastPlayed)
      // Falls der Spieler nach seinem Zug keine Karten mehr hat und noch nicht im Ranking ist,
      // füge ihn zum Ranking hinzu.
      val newRanking = if (updatedPlayer.hand.isEmpty && !ranking.exists(_.name == updatedPlayer.name))
        ranking :+ updatedPlayer
      else ranking
      val newPassCounter = if (playedCards.isEmpty) passCounter + 1 else 0

      // Wenn alle aktiven Spieler passen (oder bei zwei verbleibenden Spielern beide passen),
      // wird der Stapel erneuert.
      if (newPassCounter == remainingPlayers.length || (remainingPlayers.length == 2 && newPassCounter == 2)) {
        if (resetCounter >= 50) {
          println("\n⚠ Keiner kann mehr spielen! Neue Runde wird gestartet.")
          return ranking.reverse
        }
        println("\n🔄 Alle haben gepasst oder nur zwei Spieler übrig: Stapel wird erneuert!")
        val nextPlayers = players.tail :+ updatedPlayer
        return playTurn(nextPlayers, None, newRanking, 0, resetCounter + 1)
      }

      // Den aktuellen Spieler ans Ende der Liste setzen und weiterspielen.
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
    updatedPlayers
  }
  @tailrec
  def mainGameLoop(players: List[Player]): Unit = {
    println("\n🎲 Spiel startet mit folgenden Spielern:")
    players.foreach(p => println(s"- ${p.name} (Mensch: ${p.isHuman})"))
    val resetPlayers = players.map(_.copy(rank = None))
    val shuffledPlayers = shuffleAndDeal(resetPlayers)
    val ranked = players.filter(_.rank.isDefined).sortBy(_.rank.get)
    val finalPlayers: List[Player] = if (ranked.length >= 2) {
      val arschloch = ranked.head
      val president = ranked.last
      println(s"\n🔁 Tausche Karten zwischen Präsident (${president.name}) und Arschloch (${arschloch.name})...")
      val (newPresident, newArschloch) = exchangeCards(
        shuffledPlayers.find(_.name == president.name).getOrElse(president),
        shuffledPlayers.find(_.name == arschloch.name).getOrElse(arschloch)
      )
      shuffledPlayers.map {
        case p if p.name == president.name => newPresident
        case p if p.name == arschloch.name => newArschloch
        case other => other
      }
    } else shuffledPlayers
    val newPlayers = playRound(finalPlayers)
    println("\n--- Drücke 'n' für die nächste Runde oder 'q' zum Beenden ---")
    val input = readLine()
    if (input.toLowerCase == "q") println("👋 Spiel beendet! Danke fürs Spielen!")
    else if (input.toLowerCase == "n") mainGameLoop(newPlayers)
    else println("Tippe entweder 'n' für eine neue runde oder 'q' zum beeenden ein")
  }
  def askForPlayers(): List[Player] = {
    println("\n🎭 Willkommen bei Arschloch!")
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