package de.htwg.blackjack.model

case class Hand(cards: List[Card]):
  def add(card: Card): Hand = Hand(cards :+ card)
  def value: Int =
    if cards.size == 2 then
      // Bei genau 2 Karten: Standardwerte (Ace=11) verwenden
      cards.map(_.value).sum
    else
      // Bei mehr als 2 Karten: jedes Ass nur als 1 zählen
      cards.map {
        case c if c.rank == Rank.Ace => 1
        case c                        => c.value
      }.sum


  def isBust: Boolean = value > 21

object Hand:
  val empty: Hand = Hand(Nil)