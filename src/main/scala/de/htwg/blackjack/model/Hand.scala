package de.htwg.blackjack.model

case class Hand(cards: List[Card]):
  def add(card: Card): Hand = Hand(cards :+ card)
  def value: Int =
    val total = cards.map(_.value).sum
    val aces  = cards.count(_.rank == Rank.Ace)
    Iterator.iterate((total, aces)) {
      case (v, a) if v > 21 && a > 0 => (v - 10, a - 1)
      case x => x
    }.dropWhile((v, _) => v > 21 && aces > 0).next()._1

  def isBust: Boolean = value > 21

object Hand:
  val empty: Hand = Hand(Nil)