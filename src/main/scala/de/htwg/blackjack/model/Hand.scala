package de.htwg.blackjack.model
case class Hand(cards: List[Card]) {
  def add(card: Card): Hand = Hand(card :: cards)
  def value: Int = {
    val total = cards.map(_.value).sum
    val aces  = cards.count(_.rank == "A")
    val variants = (0 to aces).map(i => total - i * 10)
    if (variants.exists(_ <= 21)) variants.filter(_ <= 21).max
    else variants.min
  }
  def isBust: Boolean = value > 21
  override def toString: String = cards.mkString(" ")
}
object Hand {def empty: Hand = Hand(Nil)}