package de.htwg.blackjack.gui

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.image._
import scalafx.scene.control._
import scalafx.geometry.Insets
import scalafx.Includes._
import de.htwg.blackjack.controller.{GameController, GameObserver}
import de.htwg.blackjack.model.Card
import de.htwg.blackjack.state.GamePhases._
import scala.util.{Try, Success, Failure}

object BlackjackGuiApp extends JFXApp3 with GameObserver {

  // Controller & Observer-Registrierung
  private val controller = new GameController()
  controller.addObserver(this)

  // Layout‐Container
  private var dealerArea : HBox = _
  private var cardArea   : HBox = _
  private var chipBox    : HBox = _
  private var betLabel   : Label= _
  private var placeBetBtn: Button= _

  // Spiel‐Buttons
  private var hitBtn     : Button= _
  private var standBtn   : Button= _
  private var doubleBtn  : Button= _
  private var splitBtn   : Button= _
  private var undoBtn    : Button= _
  private var redoBtn    : Button= _
  private var quitBtn    : Button= _

  // Ansammlung des aktuellen Einsatzes
  private var currentBet = 0.0

  // Karte-Cache
  private val cardCache = scala.collection.mutable.Map.empty[String, Image]

  // Beim App-Start: alle 52 Karten plus Rückseite vorladen
  private def preloadCards(): Unit = {
    val suits = Seq("Clubs", "Diamonds", "Hearts", "Spades")
    val ranks = Seq("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    (for {
      s <- suits
      r <- ranks :+ "back"
    } yield s + r).foreach { fn =>
      val path = s"/img/cards/$fn.png"
      Option(getClass.getResourceAsStream(path)).foreach { is =>
        cardCache(path) = new Image(is)
      }
    }
  }

  override def start(): Unit = {
    preloadCards()
    // 1) Kartenbereiche
    dealerArea = new HBox { spacing = 10; padding = Insets(10) }
    cardArea   = new HBox { spacing = 10; padding = Insets(10) }

    // 2) Einsatz‐Label und -Button
    betLabel = new Label("Einsatz: €0.00")
    placeBetBtn = new Button("Einsatz platzieren") {
      disable = true
      onAction = _ =>
        controller.tryPlaceBet(currentBet) match {
          case Success(_) =>
            disable     = true
            chipBox.disable = true
          case Failure(ex) =>
            new Alert(Alert.AlertType.Error) {
              initOwner(stage)
              title       = "Einsatzfehler"
              headerText  = "Ungültiger Einsatz"
              contentText = ex.getMessage
            }.showAndWait()
        }
    }

    // 3) Chips
    val chipValues = Seq(1,10,25,50,100,500,1000)
    chipBox = new HBox {
      spacing = 15; padding = Insets(10)
      children = chipValues.map { v =>
        new ImageView(new Image(
          getClass.getResourceAsStream(s"/img/chips/chip$v.png")
        )) {
          fitWidth  = 60
          fitHeight = 60
          onMouseClicked = _ => {
            currentBet += v
            betLabel.text  = f"Einsatz: €$currentBet%.2f"
            placeBetBtn.disable = false
          }
        }
      }
    }

    // 4) Spiel‐Buttons anlegen
    hitBtn =    new Button("Hit")    { onAction = _ => wrap(controller.playerHit(), "Hit")    }
    standBtn =  new Button("Stand")  { onAction = _ => wrap(controller.playerStand(), "Stand")}
    doubleBtn = new Button("Double") { onAction = _ => wrap(controller.playerDouble(), "Double")}
    splitBtn =  new Button("Split")  { onAction = _ => wrap(controller.playerSplit(), "Split") }
    undoBtn =   new Button("Undo")   { onAction = _ =>
      controller.undo() match {
        case Some(_) => () // wird über update() gerendert
        case None    => info("Nichts zum Rückgängig machen.")
      }
    }
    redoBtn =   new Button("Redo")   { onAction = _ =>
      controller.redo() match {
        case Some(_) => ()
        case None    => info("Nichts zum Wiederherstellen.")
      }
    }
    quitBtn =   new Button("Quit")   { onAction = _ => stage.close() }

    val actionBox = new HBox {
      spacing = 10; padding = Insets(10)
      children = Seq(hitBtn, standBtn, doubleBtn, splitBtn, undoBtn, redoBtn, quitBtn)
    }

    // 5) Hintergrund‐Image
    val bg = new ImageView(new Image(
      getClass.getResourceAsStream("/img/table/table.png")
    )) {
      fitWidth  = 800
      fitHeight = 600
    }

    // 6) Bühne und Szene
    stage = new JFXApp3.PrimaryStage {
      title  = "Blackjack"
      width  = 800
      height = 600
      scene = new Scene {
        root = new StackPane {
          children = Seq(
            bg,
            new VBox {
              spacing = 20; padding = Insets(20)
              children = Seq(
                new Label("Dealer-Hand:"), dealerArea,
                new Separator,
                new Label("Deine Hand:"),   cardArea,
                new Separator,
                new HBox { spacing = 20; children = Seq(betLabel, placeBetBtn) },
                chipBox,
                new Separator,
                actionBox
              )
            }
          )
        }
      }
    }
  }

  /** Observer‐Callback: Karte neu zeichnen und Buttons aktivieren/deaktivieren */
  override def update(gs: de.htwg.blackjack.model.GameState): Unit = {
    // 1) Kartenbereiche füllen
    dealerArea.children.clear()
    cardArea.children.clear()
    gs.dealer.cards.foreach(c => dealerArea.children.add(loadCardImage(c)))
    gs.playerHands(gs.activeHand).cards.foreach(c => cardArea.children.add(loadCardImage(c)))

    // 2) Buttons je Phase und Bedingungen aktivieren
    val phase = gs.phase
    val bet   = if (gs.bets.isDefinedAt(gs.activeHand)) gs.bets(gs.activeHand) else 0.0
    val budget= gs.budget

    hitBtn.disable    = phase != PlayerTurn
    standBtn.disable  = phase != PlayerTurn
    // Double nur, wenn genau 2 Karten und Budget ausreichend
    doubleBtn.disable = !(phase == PlayerTurn &&
      gs.playerHands(gs.activeHand).cards.size == 2 &&
      budget >= bet)
    // Split nur, wenn 2 gleiche Karten + Budget + Deck >=2
    splitBtn.disable  = !(phase == PlayerTurn &&
      gs.playerHands(gs.activeHand).cards.size == 2 &&
      gs.playerHands(gs.activeHand).cards.map(_.value).distinct.size == 1 &&
      budget >= bet &&
      gs.deck.cards.size >= 2)
    undoBtn.disable   = controller.undo().isEmpty
    redoBtn.disable   = controller.redo().isEmpty

    // Nach Ende der Runde: Einsatz‐UI zurücksetzen
    if (phase != PlayerTurn) {
      placeBetBtn.disable = false
      chipBox.disable     = false
      currentBet = 0.0
      betLabel.text = "Einsatz: €0.00"
    }
  }

  /** Wrappt ein Try-GameState und zeigt bei Fehler eine Meldung */
  private def wrap(t: Try[de.htwg.blackjack.model.GameState], action: String): Unit =
    t match {
      case Success(_)    => () // Anzeige per Observer.update
      case Failure(ex)   => alert(s"$action fehlgeschlagen: ${ex.getMessage}")
    }

  private def info(msg: String): Unit =
    new Alert(Alert.AlertType.Information) { contentText = msg }.showAndWait()

  private def alert(msg: String): Unit =
    new Alert(Alert.AlertType.Error) { contentText = msg }.showAndWait()

  /** Karte als ImageView laden: analog zu Chips über InputStream */
  private def loadCardImage(c: Card): ImageView = {
    val path = s"/img/cards/${c.suit.toString}${c.rank.symbol}.png"
    val img = cardCache.getOrElse(path, cardCache("/img/cards/back.png"))
    new ImageView(img) {
      fitWidth = 80
      fitHeight = 120
    }
  }

}
