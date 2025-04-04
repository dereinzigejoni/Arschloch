import htwg.de.Player.Player
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import htwg.de.Card.Card
import htwg.de.Game.ArschlochGame

class PlayerTest extends AnyWordSpec with Matchers {

  // Hilfsfunktion zum Erzeugen einer Karte mit dem Symbol ♠
  private def card(value: String): Card = Card(value, "♠")

  "A non-human Player" should {

    "play a valid card group when lastPlayed is None" in {
      // Beispiel: Spieler besitzt zwei 4er und eine 2.
      // Gruppierung: List(List(4♠, 4♠), List(2♠)).
      // Da die Hand weniger als 5 Karten enthält, wählt die KI den höchsten Wert, also die 4er.
      val player = Player("KI", List(card("4"), card("4"), card("2")), 0, isHuman = false)
      val (playedOpt, updatedPlayer) = player.playCard(None)

      playedOpt shouldBe defined
      val playedCards = playedOpt.get
      // Alle gespielten Karten sollten den Wert "4" haben.
      all(playedCards.map(_.value)) shouldEqual "4"
      // Nach dem Zug sollte nur noch die 2 im Handbestand verbleiben.
      updatedPlayer.hand shouldEqual List(card("2"))
    }

    "pass (return None) when no playable group is found with lastPlayed defined" in {
      // lastPlayed definiert eine Gruppe mit einer Karte vom Wert "K"
      val lastPlayedGroup = List(card("K"))
      // Spieler besitzt nur niedrigere Karten, sodass kein Zug möglich ist.
      val player = Player("KI", List(card("2"), card("2"), card("3")), 0, isHuman = false)
      val (playedOpt, updatedPlayer) = player.playCard(Some(lastPlayedGroup))

      playedOpt shouldBe empty
      // Hand bleibt unverändert.
      updatedPlayer.hand shouldEqual player.hand
    }
  }

  "A human Player" should {
    "return immediately if rank is defined" in {
      // Ist rank gesetzt, wird playCard gleich beendet (ohne Input).
      val player = Player("Human", List(card("A"), card("K")), 0, isHuman = true, rank = Some(1))
      val (playedOpt, updatedPlayer) = player.playCard(None)
      playedOpt shouldBe empty
      updatedPlayer shouldEqual player
    }
  }
}
