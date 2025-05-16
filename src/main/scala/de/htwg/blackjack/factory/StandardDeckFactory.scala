package de.htwg.blackjack.factory
import de.htwg.blackjack.model.{Card, Deck, Rank, Suits}
import scala.annotation.tailrec
object StandardDeckFactory extends DeckFactory {
  def newDeck: Deck = Deck.shuffled(buildDeck(StandardCardFactory.allRanks.toList, StandardCardFactory.allSuits.toList))
  private def buildDeck(ranks: List[Rank], suits: List[Suits]): List[Card] = {
    @tailrec
    def combine(rs: List[Rank], acc: List[Card]): List[Card] = rs match {
      case Nil => acc
      case r :: rest =>
        val newCards = suits.map(s => StandardCardFactory.createCard(r, s))
        combine(rest, acc ++ newCards)
    }
    combine(ranks, Nil)
  }
}




