package de.htwg.blackjack.factory

import de.htwg.blackjack.model.{Card, Rank, Suits}

trait CardFactory {
  def createCard(rank: Rank, suit: Suits) : Card
  def allRanks: Seq[Rank]
  def allSuits: Seq[Suits]
}
