// src/main/scala/blackjack/model/GameState.scala
package de.htwg.blackjack.model
import de.htwg.blackjack.state.GamePhase
sealed trait Status
case object InProgress    extends Status
case object PlayerBust    extends Status
case object DealerBust    extends Status
case object Finished      extends Status
case class GameState(
                      deck: Deck,
                      playerHands: List[Hand],
                      dealer: Hand,
                      bets: List[Double],
                      activeHand: Int,
                      phase: GamePhase,
                      budget: Double,
                      currentBet: Double
                    )