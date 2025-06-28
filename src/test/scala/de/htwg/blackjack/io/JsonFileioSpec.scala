package de.htwg.blackjack.io

import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.io.json.JsonFileIO
import de.htwg.blackjack.model.*
import de.htwg.blackjack.state.GamePhases.*
import de.htwg.blackjack.state.{GamePhase, GamePhases}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import upickle.default.*

import java.io.File
import scala.util.Failure

class JsonFileioSpec extends AnyFlatSpec with Matchers {

  // Das Objekt, das wir testen
  val io = new JsonFileIO(StandardDeckFactory)
  import io.phaseRW

  behavior of "JsonFileIO Implicits"

  it should "serialize and deserialize all Rank values via rankRW" in {
    import io.rankRW
    for (r <- Rank.values) {
      val json = write(r)
      json shouldBe s""""${r.toString}""""
      read[Rank](json) shouldBe r
    }
  }

  it should "serialize and deserialize all Suits values via suitRW" in {
    import io.suitRW
    for (s <- Suits.values) {
      val json = write(s)
      json shouldBe s""""${s.toString}""""
      read[Suits](json) shouldBe s
    }
  }

  it should "serialize and deserialize a Card via cardRW" in {
    import io.cardRW
    val card = Card(Rank.Queen, Suits.Heart)
    val json = write(card)
    // Muss beides enthalten
    json should include("\"rank\":\"Queen\"")
    json should include("\"suit\":\"Heart\"")
    read[Card](json) shouldBe card
  }

  it should "serialize and deserialize a Hand via handRW" in {
    import io.handRW
    val hand = Hand(List(Card(Rank.Two, Suits.Spades), Card(Rank.Five, Suits.Clubs)))
    val json = write(hand)
    json should include("\"cards\"")
    // Prüfen, dass beide Karten drin sind
    json should include("Two")
    json should include("Five")
    read[Hand](json) shouldBe hand
  }

  it should "serialize and deserialize a Deck via deckRW" in {
    import io.deckRW
    val cards = List(Card(Rank.Ten, Suits.Diamonds), Card(Rank.Ace, Suits.Spades))
    val deck  = Deck(cards)
    val json  = write(deck)
    // Deck wird als reines JSON-Array geschrieben
    json.trim should startWith("[")
    json should include("Ten")
    json should include("Diamonds")
    json should include("Ace")
    json should include("Spades")
    val deck2 = read[Deck](json)
    deck2.cards.toList shouldBe cards
  }

  it should "serialize and deserialize all GamePhase values via phaseRW" in {
    import io.phaseRW
    val allPhases: List[GamePhase] = List(
      PlayerTurn, PlayerBustPhase,
      DealerTurn, DealerBustPhase,
      FinishedPhase, GameOver
    )
    for (ph <- allPhases) {
      val js = write[GamePhase](ph)
      js shouldBe s""""${ph.toString}""""
      read[GamePhase](js) shouldBe ph
    }
  }

  it should "throw IllegalArgumentException on unknown phase string" in {
    // direct deserialization of a bad string
    val badJson = "\"IAmNotARealPhase\""
    val ex = the [IllegalArgumentException] thrownBy read[GamePhase](badJson)
    ex.getMessage should include("Unknown phase")
  }

  //behavior of "JsonFileIO.save/load"

 

  it should "return Failure when saving to an unwriteable location" in {
    import de.htwg.blackjack.factory.StandardDeckFactory
    import de.htwg.blackjack.state.GamePhases.PlayerTurn
    val dummyState = GameState(
      deck = StandardDeckFactory.newDeck,
      playerHands = List(Hand.empty),
      dealer = Hand.empty,
      bets = List(0.0),
      activeHand = 0,
      phase = PlayerTurn,
      budget =  0.0,
      currentBet = 0.0
    )
    val dir = new File(System.getProperty("java.io.tmpdir"))
    val res = io.save(dummyState,dir.getAbsolutePath + File.separator)
    res shouldBe a [Failure[?]]
  }

  it should "return Failure when loading a non-existent file" in {
    val res = io.load("this_file_does_not_exist_hopefully.json")
    res shouldBe a [Failure[?]]
  }

  it should "return Failure when loading invalid JSON" in {
    val bad = File.createTempFile("json_bad", ".json")
    bad.deleteOnExit()
    // Schreibe ungültiges JSON
    val pw = new java.io.PrintWriter(bad)
    pw.write("not valid json")
    pw.close()

    val res = io.load(bad.getAbsolutePath)
    res shouldBe a [Failure[?]]
  }
}
