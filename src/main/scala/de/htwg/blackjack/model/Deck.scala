package de.htwg.blackjack.model

import scala.util.Random

case class Deck(cards: List[Card]) {
  def draw(): (Card, Deck) = cards match {
    case head :: tail => (head, Deck(tail))
    case Nil => throw new RuntimeException("Deck ist leer!")
  }
  def shuffle: Deck = Deck(Random.shuffle(cards))
}

object Deck {
  private val ranks = List("A","2","3","4","5","6","7","8","9","10","J","Q","K")
  private val suits = List(Hearts, Diamonds, Clubs, Spades)
  def fresh(): Deck = {
    val all = for {
      r <- ranks
      s <- suits
    } yield Card(r, s)
    Deck(all).shuffle
  }
}
