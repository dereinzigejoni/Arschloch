// src/main/scala/de/htwg/blackjack/io/json/JsonFileIO.scala
package de.htwg.blackjack.io.json

import com.google.inject.Inject
import de.htwg.blackjack.io.IFileIO
import de.htwg.blackjack.model.{Card, Hand, Rank, Suits, GameState}
import de.htwg.blackjack.model.deck.IDeckFactory
import scala.util.Try
import upickle.default._

class JsonFileIO @Inject()(deckFactory: IDeckFactory) extends IFileIO {

  // 1) Writer/Reader für Rank & Suits (als ihre `.name`)
  implicit val rankRW: ReadWriter[Rank] =
    readwriter[String].bimap[Rank](
      _.toString,
      s => Rank.valueOf(s)
    )

  implicit val suitRW: ReadWriter[Suits] =
    readwriter[String].bimap[Suits](
      _.toString,
      s => Suits.valueOf(s)
    )

  // 2) Card und Hand kann uPickle per macro
  implicit val cardRW: ReadWriter[Card] = macroRW
  implicit val handRW: ReadWriter[Hand] = macroRW

  // 3) GameState braucht ebenfalls ein RW
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
