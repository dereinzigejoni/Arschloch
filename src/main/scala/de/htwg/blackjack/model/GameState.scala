// src/main/scala/blackjack/model/GameState.scala
package de.htwg.blackjack.model

sealed trait Status
case object InProgress    extends Status
case object PlayerBust    extends Status
case object DealerBust    extends Status
case object Finished      extends Status

case class GameState(
                      deck: Deck,
                      playerHands: List[Hand],     // jetzt eine Liste von Händen
                      dealer: Hand,
                      bets: List[Double],          // parallele Liste von Bets
                      activeHand: Int,             // Index der gerade gespielten Hand
                      status: Status
                    )
