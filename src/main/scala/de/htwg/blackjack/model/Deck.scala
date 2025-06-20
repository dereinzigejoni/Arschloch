package de.htwg.blackjack.model
import scala.util.Random
case class Deck(cards: List[Card]):
  def draw(): (Card, Deck) = cards match
    case head :: tail => (head, Deck(tail))
    case Nil => throw new RuntimeException("Deck ist leer!")
object Deck:
  def fresh(): Deck =
    val suits = Suits.values.toList
    val ranks = Rank.values.toList
    val allCards = for s <- suits; r <- ranks yield Card(r, s)
    Deck(Random.shuffle(allCards))
  def shuffled(cards: List[Card]): Deck =
    Deck(Random.shuffle(cards))
