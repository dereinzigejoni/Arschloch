// src/main/scala/de/htwg/blackjack/gui/animation/AnimationService.scala
package de.htwg.blackjack.GUI

import com.google.inject.Inject
import de.htwg.blackjack.model.Card
import scalafx.scene.layout.Pane
import scalafx.animation.{PauseTransition, RotateTransition, TranslateTransition}
import scalafx.util.Duration
import scalafx.geometry.Point3D
import scalafx.scene.image.ImageView
import scalafx.Includes.*
import javafx.scene.image.ImageView as JfxImageView


class AnimationService @Inject (deckPane: Pane, cardImgs: ICardImageProvider) extends IAnimationService{
  override def dealCard(target: Pane, card: Card): Unit = {
    // 1) val mit Typ
    val cardView: ImageView = cardImgs.loadCardImage(card)

    // 2) Konfiguration
    cardView.opacity = 0
    target.children.add(cardView)

    val translate = new TranslateTransition(Duration(400), cardView) {
      fromX = deckPane.layoutX.value - cardView.layoutX.value
      fromY = deckPane.layoutY.value - cardView.layoutY.value
      toX   = 0; toY = 0
    }
    translate.onFinished = _ => {
      cardView.opacity = 1
      val flip = new RotateTransition(Duration(300), cardView) {
        fromAngle = -90; toAngle = 0; axis = new Point3D(0,1,0)
      }
      flip.play()
    }
    translate.play()
  }

  override def revealCard(target: Pane, card: Card, backIdx: Int): Unit = {
    target.children.lift(backIdx) match {
      case Some(backNode: JfxImageView) =>
        // wrap the JavaFX node in a ScalaFX ImageView
        val back = new ImageView(backNode)
        val flip1 = new RotateTransition(Duration(300), back) {
          fromAngle = 0;
          toAngle = 90;
          axis = new Point3D(0, 1, 0)
        }
        flip1.onFinished = _ => {
          target.children.remove(backNode)
          val front = cardImgs.loadCardImage(card)
          front.rotate = -90
          target.children.add(front)
          new RotateTransition(Duration(300), front) {
            fromAngle = -90;
            toAngle = 0;
            axis = new Point3D(0, 1, 0)
          }.play()
        }
        flip1.play()

      case _ =>
      // either out of bounds or not an ImageView, do nothing
    }
  }

  override def doublePlacement(target: Pane, card: Card): Unit = {
    val cardView: ImageView = cardImgs.loadCardImage(card)
    cardView.rotate = 90
    cardView.opacity = 0
    target.children.add(cardView)
    val translate = new TranslateTransition(Duration(400), cardView){
      fromX = deckPane.layoutX.value - cardView.layoutX.value
      fromY = deckPane.layoutY.value - cardView.layoutY.value
      toX = 0; toY = 0
    }
    translate.onFinished = _ => cardView.opacity = 1
    translate.play()
  }
}
