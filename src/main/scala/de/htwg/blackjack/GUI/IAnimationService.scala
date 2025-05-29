package de.htwg.blackjack.GUI

import de.htwg.blackjack.model.Card
import scalafx.scene.layout.Pane

trait IAnimationService {
  def dealCard(target: Pane, card: Card): Unit
  def revealCard(target: Pane, card:Card, backIdx: Int): Unit
  def doublePlacement(target: Pane, card: Card): Unit
}
