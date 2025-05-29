// src/main/scala/de/htwg/blackjack/gui/animation/AnimationService.scala
package de.htwg.blackjack.GUI.animation

//import de.htwg.blackjack.GUI.animation.IAnimationService
import de.htwg.blackjack.model.Card
import scalafx.scene.layout.Pane
import scalafx.animation.{PauseTransition, RotateTransition, TranslateTransition}
import scalafx.util.Duration
import javafx.geometry.Point3D

class AnimationService(deckPane: Pane) extends IAnimationService {
  override def dealCard(target: Pane, card: Card): Unit = {
    val view = /* lade ImageView aus CardCache */
      view.opacity = 0; target.children.add(view)
    val t = new TranslateTransition(Duration(400), view)
    /* from deckPane → to target */
    t.onFinished = _ => {
      view.opacity = 1
      new RotateTransition(Duration(300), view) {
        fromAngle = -90; toAngle = 0; axis = new Point3D(0,1,0)
      }.play()
    }
    t.play()
  }

  override def revealCard(target: Pane, card: Card, backIndex: Int): Unit = {
    target.children.lift(backIndex) match {
      case Some(back: javafx.scene.image.ImageView) =>
        val f1 = new RotateTransition(Duration(300), back) {
          fromAngle = 0; toAngle = 90; axis = new Point3D(0,1,0)
        }
        f1.onFinished = _ => {
          target.children.remove(back)
          val front = /* load front image */
            front.rotate = -90; target.children.add(front)
          new RotateTransition(Duration(300), front) {
            fromAngle = -90; toAngle = 0; axis = new Point3D(0,1,0)
          }.play()
        }
        f1.play()
      case _ =>
    }
  }

  override def doublePlacement(target: Pane, card: Card): Unit = {
    val view = /* lade ImageView */
      view.rotate = 90; view.opacity = 0; target.children.add(view)
    val t = new TranslateTransition(Duration(400), view)
    /* from deckPane → to target */
    t.onFinished = _ => view.opacity = 1
    t.play()
  }
}
