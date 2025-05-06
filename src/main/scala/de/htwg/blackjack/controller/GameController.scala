package de.htwg.blackjack.controller
import de.htwg.blackjack.model._
import de.htwg.blackjack.view.Observer
import scala.util.{Try,Success,Failure}

class GameController {
  private val deck    = new Deck()
  private val player  = new Player(4000)
  private val dealer  = new Dealer()
  private var gameOver = false
  private val undoMgr = new UndoManager
  private var observers = List.empty[Observer]

  def register(o: Observer): Unit = observers ::= o
  private def notifyAll(): Unit  =
    observers.foreach(_.update(player.getHand, dealer.getHand, gameOver))

  // Hilfsmethoden für Command-Undo
  private[controller] def removeLastPlayerCard(): Unit = {
    // siehe Hand-Klasse; hier Dummy-Reset
    player.resetHand()
  }
  private[controller] def resetDealerAndPlayer(): Unit = {
    player.resetHand(); dealer.resetHand()
  }

  def startNewRound(bet: Int): Try[Unit] = for {
    _ <- player.placeBet(bet)
  } yield {
    deck.shuffle()
    player.resetHand(); dealer.resetHand(); gameOver = false
    undoMgr.clear()
    // 2 Karten each
    Seq(player, dealer, player).foreach { p =>
      deck.draw().foreach(c => p.addCard(c))
    }
    notifyAll()
  }

  def hit(): Try[Unit] = Try {
    val c = deck.draw().getOrElse(throw new RuntimeException("Deck leer"))
    player.addCard(c)
    undoMgr.push(new HitCommand(this, c))
    if (player.getHand.isBust) {
      gameOver = true; player.lose()
    }
    notifyAll()
  }

  def stand(): Try[Unit] = Try {
    undoMgr.push(new StandCommand(this))
    dealer.play(deck); gameOver = true
    val ph = player.getHand; val dh = dealer.getHand
    val win = !ph.isBust && (dh.isBust || ph.bestValue > dh.bestValue)
    if (win) player.win(ph.isBlackjack) else player.lose()
    notifyAll()
  }

  def undo(): Unit = { undoMgr.undo(); notifyAll() }

  def getPlayerBudget: Int = player.budget
  def isGameOver: Boolean = gameOver
}