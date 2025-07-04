// src/main/scala/de/htwg/blackjack/io/json/JsonFileIO.scala
package de.htwg.blackjack.io.json
import com.google.inject.Inject
import de.htwg.blackjack.io.IFileIO
import de.htwg.blackjack.model.{Card, Deck, GameState, Hand, Rank, Suits}
import de.htwg.blackjack.model.deck.IDeckFactory
import de.htwg.blackjack.state.{GamePhase, GamePhases}
import de.htwg.blackjack.state.GamePhases.*
import scala.util.Try
import upickle.default.*
class JsonFileIO @Inject()(deckFactory: IDeckFactory) extends IFileIO {
  implicit val rankRW: ReadWriter[Rank] = readwriter[String].bimap(_.toString, Rank.valueOf)
  implicit val suitRW: ReadWriter[Suits] = readwriter[String].bimap(_.toString, Suits.valueOf)
  implicit val cardRW: ReadWriter[Card] = macroRW
  implicit val handRW: ReadWriter[Hand] = macroRW
  implicit val deckRW: ReadWriter[Deck] = readwriter[Seq[Card]].bimap[Deck](deck => deck.cards.toSeq, seq  => Deck(seq.toList))
  implicit val phaseRW: ReadWriter[GamePhase] = readwriter[String].bimap[GamePhase](_.toString,
      {
        case "PlayerTurn"       => PlayerTurn
        case "PlayerBustPhase"  => PlayerBustPhase
        case "DealerTurn"       => DealerTurn
        case "DealerBustPhase"  => DealerBustPhase
        case "FinishedPhase"    => FinishedPhase
        case "GameOver"         => GameOver
        case other              =>
          throw new IllegalArgumentException(s"Unknown phase: $other")
      })
  implicit val gsRW: ReadWriter[GameState] = macroRW
  override def save(gs: GameState, filePath: String): Try[Unit] = Try {
    val json = write(gs, indent = 2)
    os.write.over(os.pwd / filePath, json.getBytes("UTF-8"))
  }
  override def load(filePath: String): Try[GameState] = Try {
    val text = os.read(os.pwd / filePath)
    read[GameState](text)
  }
}
