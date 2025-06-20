package de.htwg.blackjack.GUI
import de.htwg.blackjack.model.Card
import scalafx.scene.image.ImageView
trait ICardImageProvider {
  def loadCardImage(card: Card): ImageView
  def loadBackImage(): ImageView
}
