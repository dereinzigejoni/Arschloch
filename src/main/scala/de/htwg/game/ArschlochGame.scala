package de.htwg.game

import de.htwg.Card.Card
import de.htwg.Player.Player

import scala.annotation.tailrec
import scala.io.StdIn.readLine
import scala.util.Random

object ArschlochGame {
  val suits  = Array("♥", "♦", "♠", "♣")
  val values = Array("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")

  def getValue(card: String): Int = card match {
    case "J" => 11
    case "Q" => 12
    case "K" => 13
    case "A" => 14
    case _    => card.toInt
  }

  def createDeck(): Seq[Card] =
    suits.toSeq.flatMap(suit => values.toSeq.map(value => Card(value, suit)))

  def shuffleAndDeal(players: Array[Player]): Array[Player] = {
    val deck           = Random.shuffle(createDeck())
    val cardsPerPlayer = deck.length / players.length
    players.zipWithIndex.map { case (player, i) =>
      player.copy(
        hand = deck
          .slice(i * cardsPerPlayer, (i + 1) * cardsPerPlayer)
          .toArray
      )
    }
  }

  def exchangeCards(president: Player, arschloch: Player): (Player, Player) = {
    val sortedArschloch = arschloch.hand.sortBy(c => getValue(c.value))(Ordering[Int].reverse)
    val sortedPresident = president.hand.sortBy(c => getValue(c.value))

    val fromArschloch  = sortedArschloch.take(2)
    val fromPresident  = sortedPresident.take(2)

    println(s"\n🔁 Kartentausch:")
    println(s"${arschloch.name} gibt seine besten Karten an ${president.name}: ${fromArschloch.mkString(", ")}")
    println(s"${president.name} gibt seine schlechtesten Karten an ${arschloch.name}: ${fromPresident.mkString(", ")}")

    val updatedPresident = president.copy(
      hand = (president.hand.diff(fromPresident) ++ fromArschloch).sortBy(c => getValue(c.value))
    )
    val updatedArschloch = arschloch.copy(
      hand = (arschloch.hand.diff(fromArschloch) ++ fromPresident).sortBy(c => getValue(c.value))
    )

    (updatedPresident, updatedArschloch)
  }

  // Rekursive Anzeige der Hand:
  private def displayHandRec(hand: Array[Card], index: Int = 0): Unit = {
    if (index < hand.length) {
      val c = hand(index)
      println(s"${index + 1}: ${c.value}${c.suit}")
      displayHandRec(hand, index + 1)
    }
  }

  // Hilfsmethode zum Ausgeben einer Spielerhand:
  def showPlayerHand(player: Player): Unit = {
    println(s"${player.name}, deine aktuellen Karten:")
    displayHandRec(player.hand.toArray)
  }

  def playRound(players: Array[Player]): Array[Player] = {
    val totalPlayers = players.length
    println("\n🔄 Eine neue Runde beginnt...")

    val (emptied, toPlay) = players.partition(_.hand.isEmpty)
    val initialRanking = emptied.zipWithIndex.map { case (p, idx) =>
      p.copy(rank = Some(idx))
    }

    val finalRanking = playTurn(toPlay, None, initialRanking, 0)

    if (finalRanking.length < totalPlayers) {
      println("\n⚠ Fehler: Ungültige Rangliste. Das Spiel wird neu gestartet.")
      return players
    }

    val president = finalRanking.last
    val arschloch = finalRanking.head

    if (president.name == arschloch.name) {
      println("\n⚠ Fehler: Präsident und Arschloch identisch. Neustart.")
      return players
    }

    println(s"\n🏆 ${president.name} ist Präsident!  💩 ${arschloch.name} ist Arschloch!")
    finalRanking
  }

  private def playTurn(
                        players: Array[Player],
                        lastPlayed: Option[Array[Card]],
                        ranking: Array[Player],
                        passCounter: Int,
                        resetCounter: Int = 0
                      ): Array[Player] = {
    if (players.isEmpty) return ranking.reverse

    val active = players.filter(_.hand.nonEmpty)

    if (active.length == 1 && ranking.length + 1 == (ranking.length + active.length +  (players.length - ranking.length - active.length))) {
      val loser = active.head
      println(s"\n💩 ${loser.name} ist das Arschloch, weil er als Letzter Karten hat!")
      val finalRanking = ranking :+ loser.copy(rank = Some(ranking.length))
      return finalRanking.reverse
    }

    println(s"\nAktuelle oberste Karte(n): ${lastPlayed.map(_.mkString(", ")).getOrElse("Kein Stapel")}")

    val current = active.head
    println(s"${current.name} spielt.")
    showPlayerHand(current)
    val (playedOpt, updatedPlayer) =
      current.playCard(lastPlayed, () => Array(scala.io.StdIn.readLine().toInt))

    val newLastPlayed = playedOpt.orElse(lastPlayed)

    val newRanking =
      if (updatedPlayer.hand.isEmpty && !ranking.exists(_.name == updatedPlayer.name))
        ranking :+ updatedPlayer.copy(rank = Some(ranking.length))
      else
        ranking

    val newPassCounter = if (playedOpt.isEmpty) passCounter + 1 else 0

    val remaining = players.filterNot(_ == current) :+ updatedPlayer

    if (newPassCounter == active.length || (active.length == 2 && newPassCounter == 2)) {
      if (resetCounter >= 50) {
        println("\n⚠ Keiner kann mehr spielen! Neue Runde wird gestartet.")
        return ranking.reverse
      }
      println("\n🔄 Alle haben gepasst oder nur zwei Spieler übrig: Stapel wird erneuert!")
      return playTurn(remaining, None, newRanking, 0, resetCounter + 1)
    }

    playTurn(remaining, newLastPlayed, newRanking, newPassCounter, resetCounter)
  }

  @tailrec
  def mainGameLoop(players: Array[Player]): Unit = {
    println("\n🎲 Spiel startet mit folgenden Spielern:")
    players.foreach(p => println(s"- ${p.name} (Mensch: ${p.isHuman})"))

    val reset    = players.map(_.copy(rank = None))
    val dealt    = shuffleAndDeal(reset)
    val ranked   = players.filter(_.rank.isDefined).sortBy(_.rank.get.toInt)
    val exchanged =
      if (ranked.length >= 2) {
        val loser = ranked.head
        val winner = ranked.last
        println(s"\n🔁 Tausche Karten zwischen Präsident (${winner.name}) und Arschloch (${loser.name})...")
        val (newWinner, newLoser) = exchangeCards(
          dealt.find(_.name == winner.name).get,
          dealt.find(_.name == loser.name).get
        )
        dealt.map {
          case p if p.name == winner.name => newWinner
          case p if p.name == loser.name  => newLoser
          case other                       => other
        }
      } else dealt

    val next = playRound(exchanged)

    println("\n--- Drücke 'n' für die nächste Runde oder 'q' zum Beenden ---")
    readLine().trim.toLowerCase match {
      case "q" => println("👋 Spiel beendet! Danke fürs Spielen!")
      case "n" => mainGameLoop(next)
      case _   => println("Bitte 'n' oder 'q' eingeben."); mainGameLoop(next)
    }
  }

  def askForPlayers(): Array[Player] = {
    println("\n🎭 Willkommen bei Arschloch!")

    print("Wie viele Spieler insgesamt? (3-6): ")
    val total = readLine().toIntOption.filter(n => n >= 3 && n <= 6).getOrElse {
      println("Ungültig! Standard 4.");
      4
    }

    print(s"Wie viele menschliche Spieler? (1-$total): ")
    val humans = readLine().toIntOption.filter(n => n >= 1 && n <= total).getOrElse {
      println("Ungültig! Standard 2.");
      2
    }

    val humanPlayers = (1 to humans).map { i =>
      print(s"Name Spieler $i: ")
      val name = readLine().trim
      Player(if (name.nonEmpty) name else s"Spieler$i", Array.empty, None, isHuman = true)
    }

    val aiPlayers = (1 to (total - humans)).map { i =>
      Player(s"KI-$i", Array.empty, None, isHuman = false)
    }

    (humanPlayers ++ aiPlayers).toArray
  }
  
}
