package de.htwg.Controler.GameControler

import de.htwg.Model.Card.Card
import de.htwg.Model.Player.Player
import de.htwg.View.UI.UI

import scala.annotation.tailrec
import scala.util.Random

class GameController(ui: UI) {

  def start(): Unit =
    mainLoop(askForPlayers())

  private def askForPlayers(): List[Player] = {
    ui.printLine("\n🎭 Willkommen bei Arschloch!")
    ui.printLine("Wie viele Spieler insgesamt? (3–6): ")
    val total  = ui.readLine().toIntOption.filter(n => 3 to 6 contains n).getOrElse(4)
    ui.printLine(s"Wie viele menschliche Spieler? (1–$total): ")
    val humans = ui.readLine().toIntOption.filter(n => 1 to total contains n).getOrElse(2)

    // Baut die menschlichen Spielerlisten
    @tailrec
    def mkHumans(n: Int, acc: List[Player] = Nil): List[Player] =
      if (n == 0) acc.reverse
      else {
        val default = s"Spieler${acc.size+1}"
        val name    = ui.readLine(s"Name Spieler ${acc.size+1}: ").trim match {
          case "" => default
          case n  => n
        }
        mkHumans(n-1, Player(name, Nil, isHuman = true) :: acc)
      }

    val humanPlayers = mkHumans(humans)

    // Fülle die restlichen Plätze automatisch mit KI-Spielern
    val aiCount   = total - humans
    val aiPlayers = (1 to aiCount).map(i => Player(s"KI-$i", Nil, isHuman = false)).toList
    ui.printLine(s"Füge $aiCount KI-Spieler hinzu: " + aiPlayers.map(_.name).mkString(", "))

    // Mische alle Spieler und gebe sie zurück
    val allPlayers = Random.shuffle(humanPlayers ++ aiPlayers)
    ui.printLine("Finale Spielreihenfolge: " + allPlayers.map(_.name).mkString(", "))
    allPlayers
  }


  private def mainLoop(players: List[Player]): Unit = {
    var currentPlayers = players
    var continue = true

    while (continue) {
      val next = playRound(currentPlayers)
      ui.printLine("\n--- 'n' für nächste Runde, alles andere beendet ---")
      ui.readLine().toLowerCase match {
        case "n" =>
          currentPlayers = next.map(_.copy(rank = None))
        case _ =>
          ui.printLine("👋 Spiel beendet!")
          continue = false
      }
    }
  }


  private def playRound(players: List[Player]): List[Player] = {
    val deck   = Random.shuffle(Card.all)
    val hands  = deal(deck, players.length)
    val dealt  = players.zip(hands).map { case (p,h) => p.copy(hand = h) }
    playTurn(dealt, None, Nil, 0)
  }

  /** Teilt rekursiv das Deck in n gleich große Hälften auf */
  private def deal(deck: List[Card], n: Int): List[List[Card]] = {
    val size = deck.length / n
    @tailrec
    def loop(remaining: List[Card], count: Int, acc: List[List[Card]]): List[List[Card]] =
      (count, remaining) match {
        case (0, _)              => acc.reverse
        case (_, rem)            =>
          val (hand, rest) = rem.splitAt(size)
          loop(rest, count-1, hand :: acc)
      }
    loop(deck, n, Nil)
  }

  @scala.annotation.tailrec
  private def playTurn(
                        players: List[Player],
                        last:    Option[List[Card]],
                        ranking: List[Player],
                        passes:  Int
                      ): List[Player] = {
    val active = players.filter(_.hasCards)
    if (active.isEmpty) ranking.reverse
    else {
      val current = active.head
      ui.printLine(s"\nAktuell: ${last.map(_.map(_.toString).mkString(",")).getOrElse("Kein Stapel")}")
      ui.printLine(s"${current.name} ist dran.")

      // Spieler führt seinen Zug aus
      val (playedOpt, updated) =
        if (current.isHuman) humanTurn(current, last)
        else                 aiTurn(current, last)

      // Neues „letztes“ Blatt, neues Ranking
      val newLast  = playedOpt.orElse(last)
      val newlyOut = if (!updated.hasCards && !ranking.exists(_.name == updated.name))
        updated.copy(rank = Some(ranking.size))
      else
        updated
      val newRank = if (newlyOut.rank.isDefined) ranking :+ newlyOut else ranking

      // Build Liste ohne current
      val others = players.filterNot(_.name == current.name)
      // Variante A: Trick wird fortgesetzt ⇒ current rotiert ans Ende
      val continuedPlayers = others :+ updated
      // Variante B: Alle haben gepasst ⇒ Stapel leert sich ⇒ Gewinner (updated) führt neuen Trick an
      if (passes + (if (playedOpt.isEmpty) 1 else 0) >= active.size) {
        ui.printLine("🔄 Alle haben gepasst, Stapel leert sich.")
        // Neuer Trick: updated ganz an den Anfang
        playTurn(updated :: others, None, newRank, 0)
      } else {
        // Trick fortsetzen: neuer head = nächster Spieler
        playTurn(continuedPlayers, newLast, newRank, passes + (if (playedOpt.isEmpty) 1 else 0))
      }
    }
  }


  private def humanTurn(
                         p:    Player,
                         last: Option[List[Card]]
                       ): (Option[List[Card]], Player) = {
    ui.printLine(s"${p.name}, deine Karten:")
    ui match {
      case tui: de.htwg.View.TUI.TUI => tui.printCards(p.hand)
      case _                                   => p.hand.zipWithIndex.foreach { case (c,i) =>
        ui.printLine(s"${i+1}: ${c.value}${c.suit}")
      }
    }
    val idxs = ui.readLine("Indices (z.B. 1 3 5) oder 0 zum Passen: ")
      .split("\\s+").flatMap(_.toIntOption).toList
    if (idxs == List(0)) (None, p)
    else {
      val chosen = idxs.map(i => p.hand(i-1))
      (Some(chosen), p.removeCards(chosen))
    }
  }

  private def aiTurn(
                      p:    Player,
                      last: Option[List[Card]]
                    ): (Option[List[Card]], Player) = {

    val playable: List[Card] = last match {
      case None =>
        // Kein vorheriger Zug: spiele höchste Karte
        p.hand.sortBy(c => -Card.rankOf(c.value))

      case Some(prev) if prev.nonEmpty =>
        val count = prev.size
        val threshold = Card.rankOf(prev.head.value)

        // finde alle Karten, die stärker sind als vorheriger Zug
        // und baue alle möglichen Kombinationen gleicher Größe
        p.hand
          .filter(c => Card.rankOf(c.value) > threshold)
          .combinations(count)
          .toList
          .sortBy(c => Card.rankOf(c.head.value))
          .reverse
          .flatten
    }

    // wähle die erste gültige Kombination
    val toPlay = playable.groupBy(_.value).values.flatten.toList.take(last.map(_.size).getOrElse(1))

    if (toPlay.nonEmpty)
      (Some(toPlay), p.removeCards(toPlay))
    else
      (None, p)
  }


}
