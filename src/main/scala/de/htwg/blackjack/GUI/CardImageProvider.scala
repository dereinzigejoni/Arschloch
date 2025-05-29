// src/main/scala/de/htwg/blackjack/gui/card/CardImageProvider.scala
package de.htwg.blackjack.GUI

import de.htwg.blackjack.model.Card
import scalafx.scene.image.{Image, ImageView}

import scala.collection.mutable

class CardImageProvider extends ICardImageProvider {
  // Cache wie gehabt
  private val cardCache = mutable.Map.empty[String, Image]
  preload()

  private def preload(): Unit = {
    Option(getClass.getResourceAsStream("/img/cards/back.png")).foreach(is => cardCache("back") = new Image(is))
    val suits = Seq("Clubs","Diamonds","Heart","Spades")
    val ranks = Seq("A","2","3","4","5","6","7","8","9","10","J","Q","K")
    for {
      s <- suits
      r <- ranks
      key = s + r
      path = s"/img/cards/$key.png"
      is <- Option(getClass.getResourceAsStream(path))
    } cardCache(key) = new Image(is)
  }

  override def loadCardImage(card: Card): ImageView = {
    val key = card.suit.toString + card.rank.symbol
    new ImageView(cardCache(key)) {
      fitWidth = 80; fitHeight = 120
    }
  }

  override def loadBackImage(): ImageView = {
    new ImageView(cardCache("back")) {
      fitWidth = 80; fitHeight = 120
    }
  }
}
