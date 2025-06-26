// src/main/scala/de/htwg/blackjack/io/xml/XmlFileIO.scala
package de.htwg.blackjack.io.xml

import com.google.inject.Inject
import de.htwg.blackjack.io.IFileIO
import de.htwg.blackjack.model.{Card, Hand, Rank, Suits, GameState}
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.state.GamePhases
import scala.util.Try
import scala.xml.{XML, Node}

class XmlFileIO @Inject()(deckFactory: IDeckFactory) extends IFileIO {

  override def save(gs: GameState, filePath: String): Try[Unit] = Try {
    val xml =
      <game>
        <budget>{gs.budget}</budget>
        <currentBet>{gs.currentBet}</currentBet>
        <activeHand>{gs.activeHand}</activeHand>
        <bets>
          { gs.bets.map(b => <bet>{b}</bet>) }
        </bets>
        <dealer>
          { gs.dealer.cards.map(c =>
            <card suit={c.suit.toString} rank={c.rank.toString}/>
        )
          }
        </dealer>
        <players>
          { gs.playerHands.zip(gs.bets).zipWithIndex.map {
          case ((hand, bet), idx) =>
            <player idx={idx.toString} bet={bet.toString}>
              { hand.cards.map(c =>
                <card suit={c.suit.toString} rank={c.rank.toString}/>
            )
              }
            </player>
        }
          }
        </players>
      </game>

    XML.save(filePath, xml, "UTF-8", xmlDecl = true)
  }

  override def load(filePath: String): Try[GameState] = Try {
    val xml = XML.loadFile(filePath)

    def parseCard(node: Node): Card = {
      val suit = Suits.valueOf(node \@ "suit")
      val rank = Rank.valueOf(node \@ "rank")
      Card(rank, suit)
    }

    val budget     = (xml \ "budget").text.toDouble
    val currentBet = (xml \ "currentBet").text.toDouble
    val activeHand = (xml \ "activeHand").text.toInt
    val bets       = (xml \ "bets" \ "bet").map(_.text.toDouble).toList

    val dealerCards = (xml \ "dealer" \ "card").map(parseCard).toList
    val dealerHand  = dealerCards.foldLeft(Hand.empty)(_ add _)

    val playerElems = xml \ "players" \ "player"
    val (hands, loadedBets) = playerElems.map { p =>
      val bet   = (p \@ "bet").toDouble
      val cards = (p \ "card").map(parseCard).toList
      val hand  = cards.foldLeft(Hand.empty)(_ add _)
      (hand, bet)
    }.toList.unzip

    // DeckFactory nur für einen Dummy-Deck, da im Load kein echtes Deck gebraucht wird:
    val dummyDeck = deckFactory.newDeck

    GameState(
      deck        = dummyDeck,
      playerHands = hands,
      dealer      = dealerHand,
      bets        = loadedBets,
      activeHand  = activeHand,
      phase       = GamePhases.PlayerTurn,
      budget      = budget,
      currentBet  = currentBet
    )
  }
}
