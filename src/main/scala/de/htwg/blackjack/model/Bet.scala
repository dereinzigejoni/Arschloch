package de.htwg.blackjack.model
case class Bet(amount: Int) {require(amount > 0, "Bet must be positive")}