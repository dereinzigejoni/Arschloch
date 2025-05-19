package de.htwg.blackjack.gui

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.image._
import scalafx.geometry._
import scalafx.Includes._
import de.htwg.blackjack.controller.{SharedGameController, GameObserver}
import de.htwg.blackjack.model.Card
import de.htwg.blackjack.state.GamePhases.*
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.animation.{RotateTransition, TranslateTransition}
import scalafx.scene.media.{Media, MediaPlayer}
import scalafx.util.Duration
import javafx.geometry.Point3D

import scala.util.{Failure, Success, Try}

object BlackjackGuiApp extends JFXApp3 with GameObserver {

  private val controller = SharedGameController.instance
  controller.addObserver(this)

  // State für Einsatz
  private var currentBet = 0.0
  private val chipValues = Seq(1, 10, 25, 50, 100, 500, 1000)

  // UI-Panes
  private var welcomePane: VBox    = _
  private var betPane: VBox        = _
  private var gamePane: VBox       = _
  private var resultPane: VBox     = _
  private var rootStack: StackPane = _

  // UI-Controls
  private var budgetLabel: Label      = _
  private var betLabel: Label         = _
  private var placeBetBtn: Button     = _
  private var clearBetBtn: Button     = _
  private var chipBox: HBox           = _
  private var dealerArea: HBox        = _
  private var playerArea: HBox        = _
  private var deckStack: StackPane    = _
  private var resultText: TextArea    = _

  // Background music player
  private var bgPlayer: MediaPlayer   = _

  override def start(): Unit = {
    // --- Background Music ---
    val media = new Media(getClass.getResource("/audio/background.mp3").toExternalForm)
    bgPlayer = new MediaPlayer(media) {
      cycleCount = MediaPlayer.Indefinite
      volume = 0.3
    }
    bgPlayer.play()

    // --- Welcome Pane ---
    val startBtn = new Button("Start") { onAction = _ => showBetScreen() }
    welcomePane = new VBox(20, new Label("WILLKOMMEN ZU BLACKJACK"), startBtn)
    welcomePane.alignment = Pos.Center

    // --- Bet Pane ---
    budgetLabel = new Label()
    betLabel    = new Label("Einsatz: €0.00")
    placeBetBtn = new Button("Einsatz platzieren") {
      disable = true
      onAction = _ =>
        controller.tryPlaceBet(currentBet) match {
          case Success(_) =>
            disable         = true
            chipBox.disable = true
            controller.setStateInternal(controller.getState)
          case Failure(ex) =>
            new Alert(Alert.AlertType.Error) {
              initOwner(stage)
              title       = "Einsatzfehler"
              headerText  = "Ungültiger Einsatz"
              contentText = ex.getMessage
            }.showAndWait()
        }
    }
    clearBetBtn = new Button("Löschen") {
      onAction = _ =>
        currentBet = 0.0
        betLabel.text = "Einsatz: €0.00"
        placeBetBtn.disable = true
        chipBox.disable = false
    }
    chipBox = createChipBox()
    betPane = new VBox(15,
      new Label("Dein Budget:"), budgetLabel,
      new HBox(10, betLabel, placeBetBtn, clearBetBtn),
      chipBox
    )
    betPane.padding   = Insets(20)
    betPane.alignment = Pos.Center

    // --- Game Pane ---
    dealerArea = new HBox(10) { padding = Insets(10) }
    playerArea = new HBox(10) { padding = Insets(10) }

    // --- Deck Stack ---
    deckStack = new StackPane() {
      val back = loadBackImage()
      back.rotate = 180
      children.add(back)
      padding = Insets(10)
    }
    val deckBox = new VBox(new Label("Deck"), deckStack)
    deckBox.alignment = Pos.TopCenter

    val hitBtn   = createActionButton("Hit",    () => controller.playerHit(),   "Hit")
    val standBtn = createActionButton("Stand",  () => controller.playerStand(),"Stand")
    val dblBtn   = createActionButton("Double", () => controller.playerDouble(),"Double")
    val splBtn   = createActionButton("Split",  () => controller.playerSplit(),"Split")
    val undoBtn  = new Button("Undo") { onAction = _ => controller.undo().foreach(_ => ()) }
    val redoBtn  = new Button("Redo") { onAction = _ => controller.redo().foreach(_ => ()) }
    val quitBtn  = new Button("Quit") { onAction = _ => sys.exit(0) }
    val btnBox   = new HBox(10, Seq(hitBtn, standBtn, dblBtn, splBtn, undoBtn, redoBtn, quitBtn): _*)

    gamePane = new VBox(15,
      new HBox(50, new VBox(new Label("Dealer-Hand:"), dealerArea), deckBox),
      new Separator(),
      new VBox(new Label("Deine Hand:"), playerArea),
      new Separator(),
      btnBox
    )
    gamePane.padding   = Insets(20)
    gamePane.alignment = Pos.TopCenter

    // --- Result Pane ---
    resultText = new TextArea { editable = false; wrapText = true }
    val newGameBtn = new Button("Neues Spiel") { onAction = _ => showBetScreen() }
    val exitBtn    = new Button("Beenden") { onAction = _ => sys.exit(0) }
    resultPane = new VBox(15, resultText, new HBox(10, newGameBtn, exitBtn))
    resultPane.padding   = Insets(20)
    resultPane.alignment = Pos.Center

    // --- Root Stack ---
    rootStack = new StackPane { children = Seq(welcomePane, betPane, gamePane, resultPane) }
    Seq(betPane, gamePane, resultPane).foreach(_.visible = false)

    stage = new PrimaryStage {
      title = "Blackjack"
      scene = new Scene(rootStack, 1000, 700)
    }
  }

  override def update(gs: de.htwg.blackjack.model.GameState): Unit = {
    budgetLabel.text = f"€${controller.getBudget}%.2f"
    gs.phase match {
      case PlayerTurn => showGameScreen(); renderPartialInGui(gs)
      case DealerTurn =>
        showGameScreen()
        animateDealerReveal(gs.dealer.cards.tail.head)
        renderPartialInGui(gs)
      case FinishedPhase | DealerBustPhase | PlayerBustPhase =>
        controller.resolveBet()
        showResultScreen(renderFullText(gs))
      case _ =>
    }
  }

  private def showWelcome(): Unit = {
    welcomePane.visible = true; betPane.visible = false
    gamePane.visible    = false; resultPane.visible = false
  }
  private def showBetScreen(): Unit = {
    welcomePane.visible = false; betPane.visible = true
    gamePane.visible    = false; resultPane.visible = false
    currentBet = 0.0; betLabel.text = "Einsatz: €0.00"; chipBox.disable = false; placeBetBtn.disable = true
  }
  private def showGameScreen(): Unit = {
    welcomePane.visible = false; betPane.visible = false
    gamePane.visible    = true;  resultPane.visible = false
  }
  private def showResultScreen(text: String): Unit = {
    welcomePane.visible = false; betPane.visible = false
    gamePane.visible    = false; resultPane.visible = true; resultText.text = text
  }

  private def renderPartialInGui(gs: de.htwg.blackjack.model.GameState): Unit = {
    dealerArea.children.clear()
    playerArea.children.clear()
    // Dealer: alle Karten offen
    gs.dealer.cards.foreach(c => dealerArea.children.add(loadCardImage(c)))
    // Player: alle Karten offen
    gs.playerHands(gs.activeHand).cards.foreach(c => animateDeal(playerArea, c))
  }

  private def animateDealerReveal(card: Card): Unit = {
    dealerArea.children.lift(1) match {
      case Some(backView: javafx.scene.image.ImageView) =>
        val flip1 = new RotateTransition(Duration(300), backView) {
          fromAngle = 0; toAngle = 90; axis = new Point3D(0, 1, 0)
        }
        flip1.onFinished = _ => {
          dealerArea.children.remove(backView)
          val frontView = loadCardImage(card)
          frontView.rotate = -90
          dealerArea.children.add(frontView)
          val flip2 = new RotateTransition(Duration(300), frontView) {
            fromAngle = -90; toAngle = 0; axis = new Point3D(0, 1, 0)
          }
          flip2.play()
        }
        flip1.play()
      case _ =>
    }
  }

  private def animateDeal(target: Pane, card: Card): Unit = {
    val cardView = loadCardImage(card)
    cardView.opacity = 0
    target.children.add(cardView)
    val translate = new TranslateTransition(Duration(400), cardView) {
      fromX = deckStack.layoutX.value - cardView.layoutX.value
      fromY = deckStack.layoutY.value - cardView.layoutY.value
      toX   = 0; toY = 0
    }
    translate.onFinished = _ => {
      cardView.opacity = 1
      val flip = new RotateTransition(Duration(300), cardView) {
        fromAngle = -90; toAngle = 0; axis = new Point3D(0, 1, 0)
      }
      flip.play()
    }
    translate.play()
  }

  private def renderFullText(gs: de.htwg.blackjack.model.GameState): String = {
    def hBorder(c: Char = '='): String = c.toString * 40
    def padCenter(s: String): String = {
      val width = 40; val pad = (width - s.length) / 2
      " " * pad + s + " " * (width - s.length - pad)
    }
    val sb = new StringBuilder
    sb.append(hBorder()).append("\n").append(padCenter("ERGEBNISSE DER RUNDE")).append("\n").append(hBorder()).append("\n")
    sb.append(s"Dealer: ${gs.dealer.cards.mkString(" ")} (Wert: ${gs.dealer.value})\n")
    gs.playerHands.zip(gs.bets).zipWithIndex.foreach { case ((hand, bet), idx) =>
      sb.append(f"Hand ${idx+1}: ${hand.cards.mkString(" ")} (Wert: ${hand.value}) Einsatz: €$bet%.2f\n")
    }
    gs.playerHands.foreach { hand => val p = hand.value; val d = gs.dealer.value; sb.append(
      if (p>21)  "Du bist Bust – Dealer gewinnt."
      else if (d>21) "Dealer ist Bust – Du gewinnst!"
      else if (p>d)   "Du gewinnst!"
      else if (p<d)   "Dealer gewinnt!"
      else             "Push – unentschieden"
    ).append("\n") }
    sb.append(hBorder('-')).toString()
  }

  private def createChipBox(): HBox = new HBox(15) {
    padding = Insets(10)
    children = chipValues.map { v =>
      val iv = new ImageView(new Image(getClass.getResourceAsStream(s"/img/chips/chip$v.png"))) {
        fitWidth = 60; fitHeight = 60
        onMouseClicked = _ => {
          currentBet += v; betLabel.text = f"Einsatz: €$currentBet%.2f"; placeBetBtn.disable = false
        }
      }; iv
    }
  }

  private def createActionButton(text: String, op: () => Try[_], actionName: String): Button = new Button(text) {
    onAction = _ => wrap(op().map(_ => controller.getState), actionName)
  }

  private def wrap(t: Try[_], action: String): Unit = t match {
    case Success(_) => ()
    case Failure(ex) => new Alert(Alert.AlertType.Error) { contentText = s"$action fehlgeschlagen: ${ex.getMessage}" }.showAndWait()
  }

  private val cardCache = scala.collection.mutable.Map.empty[String, Image]
  preloadCards()
  private def preloadCards(): Unit = {
    Option(getClass.getResourceAsStream("/img/cards/back.png")).foreach(is => cardCache("back") = new Image(is))
    val suits = Seq("Clubs","Diamonds","Hearts","Spades"); val ranks = Seq("A","2","3","4","5","6","7","8","9","10","J","Q","K")
    for { s<-suits; r<-ranks } { val key=s+r; val path=s"/img/cards/$key.png"; Option(getClass.getResourceAsStream(path)).foreach(is=>cardCache(key)=new Image(is)) }
  }
  private def loadCardImage(c: Card): ImageView = { val key=c.suit.toString+c.rank.symbol; val img=cardCache.getOrElse(key,cardCache("back")); new ImageView(img){fitWidth=80;fitHeight=120} }
  private def loadBackImage(): ImageView = { val img=cardCache("back"); new ImageView(img){fitWidth=80;fitHeight=120} }
}
