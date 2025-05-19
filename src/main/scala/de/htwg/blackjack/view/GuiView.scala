// src/main/scala/de/htwg/blackjack/view/GuiView.scala
package de.htwg.blackjack.view

import scala.swing._
import scala.swing.event._
import javax.swing.ImageIcon
import de.htwg.blackjack.controller.{GameController, Observer}
import de.htwg.blackjack.model.GameState

class GuiView extends Frame with Observer {
  title = "Scala-Blackjack"

  private val controller = new GameController()
  controller.addObserver(this)

  // Karten‐Panel
  private val dealerPanel = new FlowPanel()
  private val playerPanel = new FlowPanel()

  // Chips‐Panel
  private val chipValues = Seq(1,10,25,50,100,500,1000)
  private var betSum     = 0
  private val betLabel   = new Label("Aktueller Einsatz: €0")
  private val chipButtons = chipValues.map { v =>
    new Button {
      icon = new ImageIcon(getClass.getResource(s"/img/chips/$v.png"))
      tooltip = s"€$v"
      reactions += {
        case ButtonClicked(_) =>
          betSum += v
          betLabel.text = s"Akt. Einsatz: €$betSum"
      }
    }
  }
  private val chipsPanel = new FlowPanel(chipButtons: _*) {
    border = Swing.EmptyBorder(10,10,10,10)
  }

  // Action‐Buttons
  private val hitBtn    = new Button("Hit")
  private val standBtn  = new Button("Stand")
  private val doubleBtn = new Button("Double")
  private val splitBtn  = new Button("Split")
  private val undoBtn   = new Button("Undo")
  private val redoBtn   = new Button("Redo")

  listenTo(hitBtn, standBtn, doubleBtn, splitBtn, undoBtn, redoBtn)
  reactions += {
    case ButtonClicked(`hitBtn`)    => controller.playerHit()
    case ButtonClicked(`standBtn`)  => controller.playerStand()
    case ButtonClicked(`doubleBtn`) => controller.playerDouble()
    case ButtonClicked(`splitBtn`)  => controller.playerSplit()
    case ButtonClicked(`undoBtn`)   => controller.undo()
    case ButtonClicked(`redoBtn`)   => controller.redo()
  }

  contents = new BorderPanel {
    layout(new FlowPanel(dealerPanel) { border = Swing.TitledBorder("Dealer") }) = BorderPanel.Position.North
    layout(new FlowPanel(playerPanel) { border = Swing.TitledBorder("Spieler") }) = BorderPanel.Position.Center
    layout(new BoxPanel(Orientation.Vertical) {
      contents ++= Seq(betLabel, chipsPanel,
        new FlowPanel(hitBtn, standBtn, doubleBtn, splitBtn, undoBtn, redoBtn))
    }) = BorderPanel.Position.South
  }

  size = new Dimension(600, 400)
  centerOnScreen()
  visible = true

  /** Observer‐Callback, zeichnet GUI neu */
  override def update(state: GameState): Unit = {
    // Dealer‐Karten
    dealerPanel.contents.clear()
    state.dealer.cards.foreach { c =>
      dealerPanel.contents += new Label {
        icon = new ImageIcon(getClass.getResource(s"/img/cards/${c.rank.symbol}${c.suit.symbol}.png"))
      }
    }
    // Spielerhände (nur aktive Hand hier beispielhaft)
    playerPanel.contents.clear()
    state.playerHands(state.activeHand).cards.foreach { c =>
      playerPanel.contents += new Label {
        icon = new ImageIcon(getClass.getResource(s"/img/cards/${c.rank.symbol}${c.suit.symbol}.png"))
      }
    }
    contents.validate()
    repaint()
  }

  def start(): Unit = {
    // initial: set Bet via Chips, dann:
    chipsPanel.contents.foreach {
      case b: Button => b.enabled = true
    }
    // place initial bet?
    controller.placeBet(betSum.toDouble)
  }
}
