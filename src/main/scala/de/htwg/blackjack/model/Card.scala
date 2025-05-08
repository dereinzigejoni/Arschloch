package de.htwg.blackjack.model
sealed trait Suit { def symbol: String }
case object Hearts   extends Suit { val symbol = "♥" }
case object Diamonds extends Suit { val symbol = "♦" }
case object Clubs    extends Suit { val symbol = "♣" }
case object Spades   extends Suit { val symbol = "♠" }
case class Card(rank: String, suit: Suit) {
  val value: Int = rank match {
    case "A" => 11
    case "K" | "Q" | "J" => 10
    case r   => r.toInt
  }
  override def toString: String = s"$rank${suit.symbol}"
}
