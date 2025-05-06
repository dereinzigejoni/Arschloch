package de.htwg.blackjack.model

case class Hand(cards: List[Card] = Nil) {
  def add(c: Card): Hand = copy(cards = c :: cards)

  // Alle möglichen Werte (Ace als 1 oder 11)
  def values: List[Int] = {
    val sum = cards.map(_.rank.value).sum
    val aces = cards.count(_.rank == Rank.Ace)
    (0 to aces).map(i => sum - i * 10).filter(_ <= 21).toList match {
      case Nil => List(sum - aces * 10)
      case vs  => vs
    }
  }

  val bestValue: Int = values.max
  val isBlackjack: Boolean = cards.size == 2 && bestValue == 21
  val isBust: Boolean = values.min > 21
}
