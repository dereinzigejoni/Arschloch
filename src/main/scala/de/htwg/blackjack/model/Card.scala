package de.htwg.blackjack.model

enum Rank(val symbol: String, val value: Int):
  case Two extends Rank("2", 2)
  case Three extends Rank("3", 3)
  case Four extends Rank("4", 4)
  case Five extends Rank("5", 5)
  case Six extends Rank("6", 6)
  case Seven extends Rank("7", 7)
  case Eight extends Rank("8", 8)
  case Nine extends Rank("9", 9)
  case Ten extends Rank("10", 10)
  case Jack extends Rank("J", 10)
  case Queen extends Rank("Q", 10)
  case King extends Rank("K", 10)
  case Ace extends Rank("A", 11)
  
enum Suits(val symbol: String):
  case Heart extends Suits("♥")
  case Diamonds extends Suits("♦")
  case Clubs extends Suits("♣")
  case Spades extends Suits("♠")


case class Card(rank: Rank, suit: Suits):
  def value: Int = rank.value

  override def toString: String = s"${suit.symbol}${rank.symbol}"
