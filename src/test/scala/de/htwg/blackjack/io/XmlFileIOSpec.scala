// src/test/scala/de/htwg/blackjack/io/xml/XmlFileIOSpec.scala
package de.htwg.blackjack.io

import java.io.File
import scala.util.{Failure, Success}
import scala.xml.XML
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import de.htwg.blackjack.model.*
import de.htwg.blackjack.factory.StandardDeckFactory
import de.htwg.blackjack.io.xml.XmlFileIO
import de.htwg.blackjack.state.GamePhases
import de.htwg.blackjack.state.GamePhases.PlayerTurn

class XmlFileIOSpec extends AnyFlatSpec with Matchers {

  val factory = StandardDeckFactory
  val io = new XmlFileIO(factory)

  def makeSampleState(): GameState = {
    val deck0       = factory.newDeck
    val (p1, d1)    = deck0.draw()
    val (p2, d2)    = d1.draw()
    val (c1, d3)    = d2.draw()
    val (c2, d4)    = d3.draw()
    val playerHand  = Hand.empty.add(p1).add(p2)
    val dealerHand  = Hand.empty.add(c1).add(c2)
    GameState(
      deck        = d4,
      playerHands = List(playerHand),
      dealer      = dealerHand,
      bets        = List(50.0),
      activeHand  = 0,
      phase       = PlayerTurn,
      budget      = 1000.0,
      currentBet  = 50.0
    )
  }

  behavior of "XmlFileIO.save"

  it should "write all required XML elements and attributes" in {
    val state = makeSampleState()
    val tmp = File.createTempFile("xmlfileio", ".xml")
    tmp.deleteOnExit()

    // save should succeed
    io.save(state, tmp.getAbsolutePath) shouldBe a [Success[_]]

    // read the file content
    val xml = XML.loadFile(tmp)
    // budget element
    (xml \ "budget").text.toDouble shouldBe 1000.0
    // currentBet element
    (xml \ "currentBet").text.toDouble shouldBe 50.0
    // activeHand
    (xml \ "activeHand").text.toInt shouldBe 0
    // one bet
    val bets = (xml \ "bets" \ "bet").map(_.text.toDouble)
    bets shouldBe Seq(50.0)
    // dealer cards count
    val dealerCards = (xml \ "dealer" \ "card")
    dealerCards.size shouldBe 2
    // attributes suit and rank present
    dealerCards.foreach { node =>
      node.attribute("suit").isDefined shouldBe true
      node.attribute("rank").isDefined shouldBe true
    }
    // one player element
    val players = (xml \ "players" \ "player")
    players.size shouldBe 1
    val player = players.head
    player.attribute("idx").map(_.text.toInt).get shouldBe 0
    player.attribute("bet").map(_.text.toDouble).get shouldBe 50.0
    // player cards count matches
    (player \ "card").size shouldBe 2
  }

  behavior of "XmlFileIO.load"

  it should "load back exactly the saved GameState (except dummy deck)" in {
    val original = makeSampleState()
    val tmp = File.createTempFile("xmlfileio_roundtrip", ".xml")
    tmp.deleteOnExit()
    io.save(original, tmp.getAbsolutePath).get

    val result = io.load(tmp.getAbsolutePath)
    result shouldBe a [Success[_]]
    val loaded = result.get

    // Loaded fields should match
    loaded.budget     shouldBe original.budget
    loaded.currentBet shouldBe original.currentBet
    loaded.activeHand shouldBe original.activeHand
    loaded.playerHands shouldBe original.playerHands
    loaded.dealer.cards shouldBe original.dealer.cards
    loaded.bets shouldBe original.bets
    // phase is always PlayerTurn in load
    loaded.phase shouldBe PlayerTurn
  }

  it should "load multiple players correctly" in {
    // create XML manually with two players
    val xml =
      <game>
        <budget>500.0</budget>
        <currentBet>20.0</currentBet>
        <activeHand>1</activeHand>
        <bets>
          <bet>20.0</bet><bet>30.0</bet>
        </bets>
        <dealer>
          <card suit="Spades" rank="Ace"/><card suit="Heart" rank="Two"/>
        </dealer>
        <players>
          <player idx="0" bet="20.0">
            <card suit="Clubs" rank="Three"/>
          </player>
          <player idx="1" bet="30.0">
            <card suit="Diamonds" rank="King"/>
          </player>
        </players>
      </game>

    val tmp = File.createTempFile("xmlfileio_multi", ".xml")
    tmp.deleteOnExit()
    XML.save(tmp.getAbsolutePath, xml, "UTF-8", xmlDecl = true)

    val result = io.load(tmp.getAbsolutePath)
    result shouldBe a [Success[_]]
    val gs = result.get

    gs.budget     shouldBe 500.0
    gs.currentBet shouldBe 20.0
    gs.activeHand shouldBe 1
    gs.bets        should contain inOrderOnly (20.0, 30.0)
    gs.playerHands.map(_.cards) should contain inOrderOnly (
      Seq(Card(Rank.Three, Suits.Clubs)),
      Seq(Card(Rank.King, Suits.Diamonds))
    )
    gs.dealer.cards should contain inOrderOnly (
      Card(Rank.Ace, Suits.Spades),
      Card(Rank.Two, Suits.Heart)
    )
  }

  it should "return Failure when saving to an unwriteable location" in {
    val state = makeSampleState()
    val dir = new File(System.getProperty("java.io.tmpdir"))
    val res = io.save(state, dir.getAbsolutePath + File.separator)
    res shouldBe a [Failure[_]]
  }

  it should "return Failure when loading non-existent file" in {
    val res = io.load("no_such_file.xml")
    res shouldBe a [Failure[_]]
  }

  it should "return Failure when loading invalid XML" in {
    val bad = File.createTempFile("xml_bad", ".xml")
    bad.deleteOnExit()
    val pw = new java.io.PrintWriter(bad)
    pw.write("not valid xml")
    pw.close()

    val res = io.load(bad.getAbsolutePath)
    res shouldBe a [Failure[_]]
  }
}
