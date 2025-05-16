package de.htwg.blackjack.command
import de.htwg.blackjack.model.GameState
import scala.util.Try
object HitCommand extends Command {
   def execute(gs: GameState): Try[GameState] = Try(gs.phase.hit(gs)) // die rohe Aktion auf Controller
}