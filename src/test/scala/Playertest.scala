import de.htwg.Player.{Card, Player}

import java.io.ByteArrayInputStream
import org.scalatest.funsuite.AnyFunSuite

// Dummy-Implementierung für Testzwecke
object ArschlochGame_Dummy {
  def getValue(value: String): Int = value match {
    case "2"  => 2
    case "3"  => 3
    case "4"  => 4
    case "5"  => 5
    case "6"  => 6
    case "7"  => 7
    case "8"  => 8
    case "9"  => 9
    case "10" => 10
    case "J"  => 11
    case "Q"  => 12
    case "K"  => 13
    case "A"  => 14
    case _    => 0
  }
}

class PlayerTest extends AnyFunSuite {

  // Beispielkarten, die wir verwenden
  val cardA_Spades = Card("A", "♠")
  val cardK_Spades = Card("K", "♠")
  val card10_Diamonds = Card("10", "♦")
  val card7_Clubs = Card("7", "♣")
  val card2_Spades = Card("2", "♠")
  val cardQ_Hearts = Card("Q", "♥")

  /** Testet einen menschlichen Spieler, der einen gültigen Zug spielt.
   * Wir simulieren als Eingabe "1" (erste Auswahl).
   */
  test("Human player plays a valid card") {
    val humanPlayer = Player("Alice", List(cardA_Spades, cardK_Spades), 0, isHuman = true)
    val lastPlayed: Option[List[Card]] = Some(List(card7_Clubs))

    // Simuliere Eingabe "1" für die erste Gruppe
    val simulatedInput = new ByteArrayInputStream("1\n".getBytes)
    Console.withIn(simulatedInput) {
      val (playedCardsOpt, updatedPlayer) = humanPlayer.playCard(lastPlayed)
      // Erwartung: Die erste Gruppe (entspricht cardA_Spades) wird gespielt.
      assert(playedCardsOpt.isDefined)
      assert(playedCardsOpt.get == List(cardA_Spades))
      // Hand wird um diese Karte reduziert.
      assert(updatedPlayer.hand == List(cardK_Spades))
    }
  }

  /** Testet einen menschlichen Spieler, der passt.
   * Wir simulieren als Eingabe "0".
   */
  test("Human player passes") {
    val humanPlayer = Player("Bob", List(card10_Diamonds, card7_Clubs), 0, isHuman = true)
    val lastPlayed: Option[List[Card]] = Some(List(card7_Clubs))

    // Simuliere Eingabe "0" (Passen)
    val simulatedInput = new ByteArrayInputStream("0\n".getBytes)
    Console.withIn(simulatedInput) {
      val (playedCardsOpt, updatedPlayer) = humanPlayer.playCard(lastPlayed)
      // Erwartung: Kein Zug wird gemacht
      assert(playedCardsOpt.isEmpty)
      // Hand bleibt unverändert
      assert(updatedPlayer.hand == humanPlayer.hand)
    }
  }

  /** Testet den rekursiven Fall beim menschlichen Spieler, wenn der gewählte Zug ungültig ist.
   */
  test("Human player: first invalid move then valid move") {
    // Hand: Zwei "A" (Gruppe mit 2 Karten) und ein "K" (Gruppe mit 1 Karte)
    val humanPlayer = Player("Charlie", List(cardA_Spades, Card("A", "♦"), cardK_Spades), 0, isHuman = true)
    val lastPlayed: Option[List[Card]] = Some(List(card7_Clubs)) // Erwartet 1 Karte

    // Simuliere Eingaben: zuerst "1" (Gruppe mit 2 Karten, ungültig), dann "2" (Gruppe mit 1 Karte, gültig)
    val simulatedInput = new ByteArrayInputStream("1\n2\n".getBytes)
    Console.withIn(simulatedInput) {
      val (playedCardsOpt, updatedPlayer) = humanPlayer.playCard(lastPlayed)
      // Erwartung: Letztlich wird die gültige Gruppe (1 Karte) gespielt, also die "K"-Karte.
      assert(playedCardsOpt.isDefined)
      assert(playedCardsOpt.get == List(cardK_Spades))
      // Die Hand ohne die gespielte "K"-Karte; die "A"-Karten bleiben erhalten.
      assert(updatedPlayer.hand == List(cardA_Spades, Card("A", "♦")))
    }
  }

  /** Testet die KI-Logik:
   * - Wenn eine spielbare Gruppe vorhanden ist, spielt die KI eine Karte.
   */
  test("AI player plays a valid card") {
    val aiPlayer = Player("KI", List(card2_Spades, cardQ_Hearts), 0, isHuman = false)
    val lastPlayed: Option[List[Card]] = Some(List(card7_Clubs))

    // Keine Eingabe notwendig, da KI-Logik verwendet wird
    val (playedCardsOpt, updatedAIPlayer) = aiPlayer.playCard(lastPlayed)
    // Erwartung: Eine spielbare Gruppe wird gespielt.
    assert(playedCardsOpt.isDefined)
    // Gespielte Karten sollen nicht mehr in der Hand sein.
    playedCardsOpt.get.foreach(card => assert(!updatedAIPlayer.hand.contains(card)))
  }

  /** Testet die KI-Logik, wenn kein gültiger Zug möglich ist, und die KI passt.
   */
  test("AI player passes when no valid card is playable") {
    // KI-Spieler, dessen Karten beide nicht stärker als die letzte gespielte Karte sind.
    // Hier: Letzter Zug ist ein Ass ("A", 14), und KI-Karten sind "2" (2) und "Q" (12)
    val aiPlayer = Player("KI", List(card2_Spades, cardQ_Hearts), 0, isHuman = false)
    val lastPlayed: Option[List[Card]] = Some(List(cardA_Spades))

    val (playedCardsOpt, updatedAIPlayer) = aiPlayer.playCard(lastPlayed)
    // Erwartung: Keine Karte ist spielbar, daher passt die KI.
    assert(playedCardsOpt.isEmpty)
    // Hand bleibt unverändert.
    assert(updatedAIPlayer.hand == aiPlayer.hand)
  }



}
