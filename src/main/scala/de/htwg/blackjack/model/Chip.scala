package de.htwg.blackjack.model

sealed trait Chip { def value: Int }
object Chip {
  case object One         extends Chip {val value = 1}
  case object Ten         extends Chip { val value = 10 }
  case object Twentyfive  extends Chip {val value = 25}
  case object Fifty       extends Chip { val value = 50 }
  case object Hundred     extends Chip { val value = 100 }
  case object FiveHundred extends Chip { val value = 500 }
}
