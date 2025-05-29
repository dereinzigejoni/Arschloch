package de.htwg.blackjack.GUI.card
import de.htwg.blackjack.model.Card
import scalafx.scene.image.ImageView
/** Liefert ImageViews für Karten und Rückseite */
trait ICardImageProvider {
  def loadCardImage(card: Card): ImageView
  def loadBackImage(): ImageView
}
