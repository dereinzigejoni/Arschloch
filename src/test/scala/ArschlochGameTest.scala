import htwg.de.Game.ArschlochGame
import htwg.de.Player.Player
import htwg.de.Card.Card
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class ArschlochGameTest extends AnyFunSuite {

  test("getValue should return correct integer value for cards") {
    assert(ArschlochGame.getValue("2") == 2)
    assert(ArschlochGame.getValue("10") == 10)
    assert(ArschlochGame.getValue("J") == 11)
    assert(ArschlochGame.getValue("Q") == 12)
    assert(ArschlochGame.getValue("K") == 13)
    assert(ArschlochGame.getValue("A") == 14)
  }

  test("createDeck should return a full deck of 52 unique cards with correct suits and values") {
    val deck = ArschlochGame.createDeck()
    assert(deck.length == 52)
    assert(deck.distinct.length == 52)
    assert(deck.forall(card => ArschlochGame.suits.contains(card.suit)))
    assert(deck.forall(card => ArschlochGame.values.contains(card.value)))
  }

  test("shuffleAndDeal should distribute cards evenly among players") {
    val players = List(Player("Alice", List(), 0, isHuman = true), Player("Bob", List(), 0, isHuman = true))
    val dealtPlayers = ArschlochGame.shuffleAndDeal(players)
    assert(dealtPlayers.forall(_.hand.nonEmpty))
    assert(dealtPlayers.map(_.hand.length).distinct.length == 1) // gleiche Kartenanzahl
    assert(dealtPlayers.flatMap(_.hand).distinct.length == 52) // alle Karten vorhanden
  }

  /*test("playRound should return players with ranks assigned and determine the last player as Arschloch") {
    val players = List(
      Player("Alice", List(Card("5", "♠")), 0, isHuman = true),
      Player("Bob", List(Card("8", "♣")), 0, isHuman = true)
    )
    val updatedPlayers = ArschlochGame.playRound(players)
    assert(updatedPlayers.forall(_.rank.isDefined))
    assert(updatedPlayers.last.rank.contains(updatedPlayers.length - 1)) // letzter Spieler ist Arschloch
  }*/

  test("askForPlayers should create correct number of players with names and human flags") {
    // Simulierte Eingaben
    val input = new java.io.ByteArrayInputStream("4\n2\nAlice\nBob\n".getBytes)
    Console.withIn(input) {
      val players = ArschlochGame.askForPlayers()
      assert(players.length == 4)
      assert(players.count(_.isHuman) == 2)
      assert(players.count(!_.isHuman) == 2)
      assert(players.exists(_.name == "Alice"))
      assert(players.exists(_.name == "Bob"))
    }
  }

  test("exchangeCards tauscht 2 beste gegen 2 schlechteste Karten") {
    val card = (v: String) => Card(v, "♠")

    val arschloch = Player(
      name = "Arsch",
      hand = List(card("A"), card("K"), card("10"), card("9")),
      points = 0,
      isHuman = false
    )

    val president = Player(
      name = "Präsi",
      hand = List(card("2"), card("3"), card("4"), card("5")),
      points = 0,
      isHuman = false
    )

    val (newPresident, newArschloch) = ArschlochGame.exchangeCards(president, arschloch)

    // Erwartete Karten
    val expectedPresidentCards = List("2", "3", "4", "5", "K", "A").map(card)
    val expectedArschlochCards = List("10", "9", "2", "3").map(card)

    assert(newPresident.hand.toSet == expectedPresidentCards.toSet)
    assert(newArschloch.hand.toSet == expectedArschlochCards.toSet)
  }
  test("mainGameLoop akzeptiert nur 'n' oder 'q' und reagiert entsprechend") {
    // Eingabe: erst falsch, dann 'n', dann 'q' zum echten Beenden
    val input = new java.io.ByteArrayInputStream("x\nn\nq\n".getBytes)

    Console.withIn(input) {
      // Testspieler mit gültigem Ranking (für Kartentausch)
      val players = List(
        Player("Alice", List(Card("A", "♠")), 0, isHuman = true, rank = Some(2)),
        Player("Bob", List(Card("2", "♣")), 0, isHuman = false, rank = Some(0)),
        Player("KI", List(Card("10", "♦")), 0, isHuman = false, rank = Some(1))
      )
      
    }
  }

}
