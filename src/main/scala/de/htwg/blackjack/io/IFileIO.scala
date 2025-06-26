package de.htwg.blackjack.io

import de.htwg.blackjack.model.GameState

import scala.util.Try

trait IFileIO {
  def save(gs: GameState, filePath: String): Try[Unit]
  def load(filePath: String): Try[Unit]

}
