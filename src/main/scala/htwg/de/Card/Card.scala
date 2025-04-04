package htwg.de.Card

case class Card(value: String, suit: String) { override def toString: String = s"$value$suit"}
