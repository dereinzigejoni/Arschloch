// src/main/scala/de/htwg/arschlochgame/model/Card.scala
package de.htwg.Model.Card

case class Card(value: String, suit: String) {
  def rank: Int = Card.rankOf(value)
}

object Card {
  def rankOf(v: String): Int = v match {
    case "J" => 11
    case "Q" => 12
    case "K" => 13
    case "A" => 14
    case s   => s.toInt
  }

  /** Erzeugt alle 52 Karten rekursiv */
  val all: List[Card] = {
    def loop(values: List[String], suits: List[String], acc: List[Card]): List[Card] =
      (values, suits) match {
        case (Nil, _)         => acc
        case (v :: vs, Nil)   => loop(vs, List("♥","♦","♠","♣"), acc)
        case (v :: vs, s :: ss) => loop(v :: vs, ss, Card(v,s) :: acc)
      }
    loop(
      List("2","3","4","5","6","7","8","9","10","J","Q","K","A"),
      List("♥","♦","♠","♣"),
      Nil
    )
  }
}