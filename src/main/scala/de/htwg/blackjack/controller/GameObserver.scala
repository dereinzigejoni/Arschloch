// src/main/scala/de/htwg/blackjack/controller/GameObserver.scala
package de.htwg.blackjack.controller

import de.htwg.blackjack.model.GameState
/** Interface für alle, die State-Änderungen beobachten wollen. */
trait GameObserver {
  /** Wird aufgerufen, sobald sich der GameState ändert. */
  def update(gs: GameState): Unit
}
