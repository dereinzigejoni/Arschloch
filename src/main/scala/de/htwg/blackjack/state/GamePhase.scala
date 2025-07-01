package de.htwg.blackjack.state
import de.htwg.blackjack.model.{GameState, Hand, Rank}
sealed trait GamePhase {
  def hit (gs: GameState): GameState
  def stand(gs: GameState): GameState
  def double(gs: GameState): GameState
  def split(gs: GameState): GameState
}
object GamePhases {
  case object PlayerTurn extends GamePhase {
    def hit(gs: GameState): GameState = {
      val (card, d2) = gs.deck.draw()
      val idx = gs.activeHand
      val newHand = gs.playerHands(idx).add(card)
      val newPhase = if (newHand.isBust) DealerTurn else PlayerTurn
      gs.copy(deck = d2, playerHands = gs.playerHands.updated(idx, newHand), phase = newPhase)
    }
    def stand(gs: GameState): GameState =
      if (gs.activeHand + 1 < gs.playerHands.size)
        gs.copy(activeHand = gs.activeHand + 1)
      else
        gs.copy(phase = DealerTurn)
    def double(gs: GameState): GameState = {
      val idx = gs.activeHand
      val bet = gs.bets(idx)

      if (gs.playerHands(idx).cards.size == 2 && gs.budget >= bet && gs.deck.cards.nonEmpty) {
        val doubledBet = bet * 2
        val (card, d2) = gs.deck.draw()
        val newHand = gs.playerHands(idx).add(card)
        val newHands = gs.playerHands.updated(idx, newHand)
        val newBets  = gs.bets.updated(idx, doubledBet)
        val newBudget = gs.budget - bet

        val newPhase =
          if (newHand.isBust || idx + 1 >= newHands.size) DealerTurn
          else PlayerTurn

        gs.copy(
          deck        = d2,
          playerHands = newHands,
          bets        = newBets,
          budget      = newBudget,
          activeHand  = if (newPhase == PlayerTurn) idx + 1 else idx,
          phase       = newPhase
        )
      } else gs
    }
    def split(gs: GameState): GameState = {
      val idx = gs.activeHand
      val hand = gs.playerHands(idx)

      if (
        hand.cards.size == 2 &&
          hand.cards.head.value == hand.cards(1).value &&
          gs.budget >= gs.bets(idx) &&
          gs.deck.cards.size >= 2
      ) {
        val bet = gs.bets(idx)
        val (c1 :: c2 :: Nil) = (hand.cards: @unchecked)
        val (cardA, d2) = gs.deck.draw()
        val (cardB, d3) = d2.draw()

        val h1 = Hand.empty.add(c1).add(cardA)
        val h2 = Hand.empty.add(c2).add(cardB)

        val newHands = gs.playerHands.patch(idx, Seq(h1, h2), 1)
        val newBets  = gs.bets.patch(idx, Seq(bet, bet), 1)
        val newBudget = gs.budget - bet

        gs.copy(
          deck        = d3,
          playerHands = newHands,
          bets        = newBets,
          budget      = newBudget
        )
      } else gs
    }
  }
  case object DealerTurn extends GamePhase {
    def hit(gs: GameState): GameState = gs
    def stand(gs: GameState): GameState = {
      var deck = gs.deck
      var dealerHand = gs.dealer

      while (dealerHand.value < 17 && deck.cards.nonEmpty) {
        val (c, d2) = deck.draw()
        dealerHand = dealerHand.add(c)
        deck = d2
      }

      val newPhase =
        if (dealerHand.isBust) DealerBustPhase
        else FinishedPhase

      gs.copy(deck = deck, dealer = dealerHand, phase = newPhase)
    }
    def double(gs: GameState): GameState = gs
    def split(gs: GameState): GameState = gs
  }
  case object DealerBustPhase extends GamePhase {
    def hit(gs: GameState): GameState = gs
    def stand(gs: GameState): GameState = gs
    def double(gs: GameState): GameState = gs
    def split(gs: GameState): GameState = gs
  }

  case object FinishedPhase extends GamePhase {
    def hit(gs: GameState): GameState = gs
    def stand(gs: GameState): GameState = gs
    def double(gs: GameState): GameState = gs
    def split(gs: GameState): GameState = gs
  }
  case class Payout(oldState: GameState) extends GamePhase {
    def hit(gs: GameState): GameState = gs
    def stand(gs: GameState): GameState = gs
    def double(gs: GameState): GameState = gs
    def split(gs: GameState): GameState = gs
    def pay(gs: GameState): GameState = {
      // für jede Hand den passenden Multiplikator ermitteln
      val payouts: Seq[Double] = oldState.playerHands
        .zip(oldState.bets)
        .map { case (hand, bet) =>
          val multiplier = oldState.phase match {
            case DealerBustPhase if !hand.isBust =>
              if (isNatural(hand)) 2.7 else 2.0
            case FinishedPhase if !hand.isBust && hand.value > oldState.dealer.value =>
              if (isNatural(hand)) 2.7 else 2.0
            case FinishedPhase if hand.value == oldState.dealer.value =>
              1.0
            case _ =>
              0.0
          }
          bet * multiplier
        }

      // Summe der Auszahlungen und Budget anpassen
      val totalPayout = payouts.sum
      gs.copy(
        budget     = gs.budget + totalPayout,
        currentBet = 0.0,
        phase      = GameOver
      )
    }

    private def isNatural(hand: Hand): Boolean = {
      // Genau 2 Karten und Gesamtwert 21 = Blackjack
      hand.cards.size == 2 && hand.value == 21
    }
  }
  case object PlayerBustPhase extends GamePhase{
    def hit(gs: GameState): GameState = gs
    def stand(gs: GameState): GameState = gs
    def double(gs: GameState): GameState = gs
    def split(gs: GameState): GameState = gs
  }
  case object GameOver extends GamePhase {
    def hit(gs: GameState): GameState = gs
    def stand(gs: GameState): GameState = gs
    def double(gs: GameState): GameState = gs
    def split(gs: GameState): GameState = gs
  }
}

