// src/main/scala/de/htwg/blackjack/command/CommandInvoker.scala
package de.htwg.blackjack.command

import scala.util.Try
import de.htwg.blackjack.controller.GameController
import de.htwg.blackjack.model.GameState

/** Verwaltet Ausführung, Undo und Redo von Commands auf einen Controller. */
class CommandInvoker(controller: GameController) {
  private var undoStack: List[GameState] = Nil
  private var redoStack: List[GameState] = Nil

  /** Führt aus, pusht alten State auf Undo-Stack und leert den Redo-Stack */
  def execute(cmd: Command): Try[GameState] = {
    val oldState = controller.getState
    cmd.execute(oldState).map { newState =>
      undoStack = oldState :: undoStack
      redoStack = Nil
      controller.setStateInternal(newState)
      newState
    }
  }

  /** Macht den letzten Schritt rückgängig, verschiebt aktuellen State auf Redo-Stack */
  def undo(): Option[GameState] = undoStack match {
    case prev :: rest =>
      val current = controller.getState
      redoStack = current :: redoStack
      controller.setStateInternal(prev)
      undoStack = rest
      Some(prev)
    case Nil =>
      None
  }

  /** Stellt den zuletzt undone Schritt wieder her, verschiebt aktuellen State zurück auf Undo-Stack */
  def redo(): Option[GameState] = redoStack match {
    case next :: rest =>
      val current = controller.getState
      undoStack = current :: undoStack
      controller.setStateInternal(next)
      redoStack = rest
      Some(next)
    case Nil =>
      None
  }
}
