package de.htwg.blackjack.factory
import de.htwg.blackjack.model.{Card,Rank,Suits}
object StandardCardFactory {
  val allRanks: Seq[Rank] = Rank.values.toSeq
  val allSuits: Seq[Suits] = Suits.values.toSeq

  def createCard(rank: Rank, suit: Suits): Card =
    Card(rank, suit)
}
