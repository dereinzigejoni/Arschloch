package de.htwg.blackjack.GUI

import de.htwg.blackjack.bet.IBetService
import de.htwg.blackjack.controller.{GameObserver, IGameController}
import de.htwg.blackjack.di.ApplicationContext
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.model.{Card, GameState}
import de.htwg.blackjack.state.GamePhases.*
import scalafx.animation.PauseTransition
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.geometry.*
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.image.*
import scalafx.scene.layout.*
import scalafx.scene.media.{Media, MediaPlayer}
import scalafx.util.Duration

import scala.compiletime.uninitialized
import scala.util.{Failure, Success, Try}
object BlackjackGuiApp extends JFXApp3 with GameObserver with IGameView {

  private def resetBoard(): Unit = {
    // alle angezeigten Karten entfernen
    dealerArea.children.clear()
    playerArea.children.clear()
    // Deck-Stapel neu bauen
    deckStack.children.clear()
    val back = loadBackImage()
    back.rotate = 180
    deckStack.children.add(back)
  }

  // --- Interfaces aus dem ApplicationContext ---
  private val controller: IGameController = ApplicationContext.gameController
  private val betSvc: IBetService = ApplicationContext.betService
  private var anim: IAnimationService = uninitialized
  private val deckFac: IDeckFactory = ApplicationContext.deckFactory
  //private val controller = SharedGameController.instance
  controller.addObserver(this)
  // State für Einsatz
  private var currentBet = 0.0
  private val chipValues = Seq(1, 10, 25, 50, 100, 500, 1000)
  private var animateDealerOnDealerTurn = false
  private var animateDoublePlacement       = false



  // UI-Panes
  private var welcomePane: VBox    = uninitialized
  private var betPane: VBox        = uninitialized
  private var gamePane: VBox       = uninitialized
  private var resultPane: VBox     = uninitialized
  private var rootStack: StackPane = uninitialized

  // UI-Controls
  private var budgetLabel: Label      = uninitialized
  private var betLabel: Label         = uninitialized
  private var placeBetBtn: Button     = uninitialized
  private var clearBetBtn: Button     = uninitialized
  private var chipBox: HBox           = uninitialized
  private var dealerArea: HBox        = uninitialized
  private var playerArea: HBox        = uninitialized
  private var deckStack: StackPane    = uninitialized
  private var resultText: TextArea    = uninitialized
  private var bgPlayer: MediaPlayer   = uninitialized
  override def start(): Unit = startApp()

  override def startApp(): Unit = {
    // --- Background Music ---
    val media = new Media(getClass.getResource("/audio/background.mp3").toExternalForm)
    bgPlayer = new MediaPlayer(media) {
      cycleCount = MediaPlayer.Indefinite
      volume = 0.4
    }
    bgPlayer.play()

    // --- Welcome Pane ---
    val startBtn = new Button("Start") { onAction = _ => showBetScreen() }
    welcomePane = new VBox(20, new Label("WILLKOMMEN ZU BLACKJACK"), startBtn)
    welcomePane.alignment = Pos.Center

    // --- Bet Pane ---
    budgetLabel = new Label(f"€${controller.getBudget}%.2f")
    betLabel    = new Label(f"Einsatz: €0")
    placeBetBtn = new Button("Einsatz platzieren") {
      disable = true
      onAction = _ => controller.tryplaceBet(currentBet) match {
        case Success(_) =>
          disable         = true
          chipBox.disable = true
          //controller.setStateInternal(controller.getState)
          //controller.getState
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
        betLabel.text = f"Einsatz: €$currentBet%.2f"
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

    val hitBtn = new Button("Hit") {
      onAction = _ => controller.playerHit()
    }
    val standBtn = new Button("Stand") {
      onAction = _ =>
        animateDealerOnDealerTurn = true
        controller.playerStand()
    }
    val dblBtn   = new Button("Double") {
      onAction = _ => {
        animateDoublePlacement = true
        controller.playerDouble() match {
          case Success(_) => ()
          case Failure(ex) =>
            new Alert(Alert.AlertType.Error) {
              initOwner(stage)
              contentText = s"Double fehlgeschlagen: ${ex.getMessage}"
            }.showAndWait()
        }
      }
    }
    val splBtn = new Button("Split") {
      onAction = _ =>  wrap(controller.playerSplit(), "Split")
    }
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
    anim = ApplicationContext.animationService(deckStack)


    // --- Result Pane ---
    resultText = new TextArea { editable = false; wrapText = true }
    val newGameBtn = new Button("Neues Spiel") { onAction = _ => showBetScreen() }
    val exitBtn    = new Button("Beenden")    { onAction = _ => sys.exit(0) }
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

  override def update(gs: GameState): Unit = {
    budgetLabel.text = f"€${betSvc.budget}%.2f"

    // Double-Placement Animation
    if (animateDoublePlacement && gs.playerHands.head.cards.size > 2) {
      val card = gs.playerHands(gs.activeHand).cards.last
      anim.doublePlacement(playerArea,card)
      animateDoublePlacement      = false
      animateDealerOnDealerTurn   = true
    }

    // Normaler Ablauf
    gs.phase match {
      case PlayerTurn =>
        showGameScreen()
        renderHands(gs)

      case PlayerBustPhase =>
        // erst einmal Bust-Status anzeigen…
        showGameScreen()
        renderHands(gs)

        // …und dann den Auto-Stand anstoßen
        animateDealerOnDealerTurn = true
        controller.playerStand().failed.foreach { ex =>
          new Alert(Alert.AlertType.Error) {
            initOwner(stage)
            title       = "Auto-Stand fehlgeschlagen"
            contentText = ex.getMessage
          }.showAndWait()
        }


      case DealerTurn if animateDealerOnDealerTurn =>
        animateDealerOnDealerTurn = false
        showGameScreen()
        dealerDrawLoop()

      case DealerBustPhase | FinishedPhase =>
        controller.resolveBet()
        showResultScreen(renderFullText(gs))



      case _ => // nichts
    }
  }

  private def renderHands(gs: GameState): Unit = {
    dealerArea.children.clear()
    playerArea.children.clear()

    // offene Dealer-Karte
    gs.dealer.cards.headOption.foreach(c => anim.dealCard(dealerArea, c))

    // verdeckte Dealer-Karte (neu erzeugt, bleibt im Deck unverändert)
    val hiddenCard = loadBackImage()
    hiddenCard.rotate = 180
    dealerArea.children.add(hiddenCard)

    // alle Spieler-Hände rendern
    gs.playerHands.zipWithIndex.foreach { case (hand, idx) =>
      val box = new VBox(5)
      val lbl = new Label(s"Hand ${idx + 1}") {
        if (idx == gs.activeHand) style = "-fx-font-weight:bold"
      }
      box.children.add(lbl)
      // wenn gesplittet, Spalte, sonst Zeile
      val container: Pane =
        if (gs.playerHands.size > 1) new VBox(10)
        else new HBox(10)
      hand.cards.foreach(c => anim.dealCard(container, c))
      box.children.add(container)
      box.padding = Insets(10)
      playerArea.children.add(box)
    }
  }

  /** Startet die Reveal-Animation und dann den Draw-Loop */
  private def dealerDrawLoop(): Unit = {
    val gs0 = controller.getState
    if (gs0.dealer.cards.size >= 2)
      anim.revealCard(dealerArea, gs0.dealer.cards(1),1)
    new PauseTransition(Duration(400)) { onFinished = _ => drawNext() }.play()
  }

  /** Hittet solange, bis Dealer ≥17 erreicht */
  private def drawNext(): Unit = {
    val gs = controller.getState
    if (gs.dealer.value < 17) {
      controller.dealerHit() match {
        case Success(newState) =>
          val card = newState.dealer.cards.last
          anim.dealCard(dealerArea,card)
          new PauseTransition(Duration(600)) { onFinished = _ => drawNext() }.play()
        case Failure(ex) =>
          new Alert(Alert.AlertType.Error) {
            initOwner(stage)
            title       = "Dealer-Zieh-Fehler"
            contentText = ex.getMessage
          }.showAndWait()
      }
    } else {
      new PauseTransition(Duration(3000)) {
        onFinished = _ =>
          controller.resolveBet()
          showResultScreen(renderFullText(controller.getState))
      }.play()
    }
  }

  private def showWelcome(): Unit = {
    welcomePane.visible = true; betPane.visible = false
    gamePane.visible    = false; resultPane.visible = false
  }
  private def showBetScreen(): Unit = {

    resetBoard() // <— hier das Board zurücksetzen
    welcomePane.visible = false
    betPane.visible = true
    gamePane.visible = false
    resultPane.visible = false

    currentBet = 0.0
    betLabel.text = f"Einsatz: €$currentBet%.2f"
    chipBox.disable = false
    placeBetBtn.disable = true
  }
  private def showGameScreen(): Unit = {
    welcomePane.visible = false; betPane.visible = false
    gamePane.visible    = true;  resultPane.visible = false
  }
  private def showResultScreen(text: String): Unit = {
    welcomePane.visible = false; betPane.visible = false
    gamePane.visible    = false; resultPane.visible = true
    val win = controller.getLastRoundWin
    resultText.text = text + f"\nGewinn dieser Runde: €$win%.2f"
  }




  private def renderFullText(gs: GameState): String = {
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
    gs.playerHands.foreach { hand =>
      val p = hand.value; val d = gs.dealer.value
      sb.append(
        if (p>21)    "Du bist Bust – Dealer gewinnt."
        else if (d>21) "Dealer ist Bust – Du gewinnst!"
        else if (p>d)  "Du gewinnst!"
        else if (p<d)  "Dealer gewinnt!"
        else           "Push – unentschieden"
      ).append("\n")
    }
    sb.append(hBorder('-')).toString()
  }

  private def createChipBox(): HBox = new HBox(15) {
    padding = Insets(10)
    children = chipValues.map { v =>
      new ImageView(new Image(getClass.getResourceAsStream(s"/img/chips/chip$v.png"))) {
        fitWidth = 60; fitHeight = 60
        onMouseClicked = _ => {
          currentBet += v; betLabel.text = f"Einsatz: €$currentBet%.2f"; placeBetBtn.disable = false
        }
      }
    }
  }

  private def createActionButton(text: String, op: () => Try[_], actionName: String): Button =
    new Button(text) { onAction = _ => wrap(op().map(_ => controller.getState), actionName) }

  private def wrap(t: Try[_], action: String): Unit = t match {
    case Success(_) => ()
    case Failure(ex) =>
      new Alert(Alert.AlertType.Error) { contentText = s"$action fehlgeschlagen: ${ex.getMessage}" }.showAndWait()
  }

  private val cardCache = scala.collection.mutable.Map.empty[String, Image]
  preloadCards()
  private def preloadCards(): Unit = {
    Option(getClass.getResourceAsStream("/img/cards/back.png")).foreach(is => cardCache("back") = new Image(is))
    val suits = Seq("Clubs","Diamonds","Heart","Spades")
    val ranks = Seq("A","2","3","4","5","6","7","8","9","10","J","Q","K")
    for { s <- suits; r <- ranks } {
      val key = s + r
      val path = s"/img/cards/$key.png"
      Option(getClass.getResourceAsStream(path)).foreach(is => cardCache(key) = new Image(is))
    }
  }

  private def loadCardImage(c: Card): ImageView = {
    val key = c.suit.toString + c.rank.symbol
    new ImageView(cardCache(key)) { fitWidth = 80; fitHeight = 120 }
  }


  private def loadBackImage(): ImageView = {
    new ImageView(cardCache("back")) { fitWidth = 80; fitHeight = 120 }
  }
}
