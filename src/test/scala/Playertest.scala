import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import htwg.de.Player.Player
import htwg.de.Card.Card

class Playertest extends AnyWordSpec with Matchers {
  "A Human Player" should {
    "allow playing multiple individual cards by entering comma separated indices" in {
      // Erzeuge einen Spieler mit drei Karten.
      val card1 = Card("5", "♥")
      val card2 = Card("5", "♦")
      val card3 = Card("7", "♣")
      // Hier bewusst unsortiert, damit die Sortierung getestet wird.
      val player = Player("TestHuman", List(card1, card3, card2), 0, true)

      // Definiere einen Input-Provider, der "1,3" zurückgibt.
      val inputProvider = () => "1,2"

      val (played, updatedPlayer) = player.playCard(None, inputProvider)
      played.isDefined shouldBe true
      played.get should have length 2
      // Bei der sortierten Hand (5♥, 6♦, 7♣) entspricht "1,3" den Karten card1 und card3.
      played.get should contain theSameElementsAs List(card1, card2)
      updatedPlayer.hand should have length 1
    }
  }
  "A KI Player" should {
    "eine gültige Karte spielen, wenn möglich" in {
      val card1 = Card("5", "♥")
      val card2 = Card("7", "♦")
      val card3 = Card("9", "♣")
      // KI-Spieler (isHuman = false)
      val player = Player("TestKI", List(card1, card2, card3), 0, false)
      // Ohne Vorgabe (lastPlayed = None) sollte die KI basierend auf der Logik (bei Handlänge < 5)
      // die Karte mit dem höchsten Wert spielen.
      val (played, updatedPlayer) = player.playCard(None)
      played.isDefined shouldBe true
      // Erwartet wird die Gruppe, die den höchsten Wert enthält – in diesem Fall Karte "9".
      played.get.head.value shouldEqual card3.value
      updatedPlayer.hand should not contain card3
    }

    "passen, wenn keine spielbare Karten-Gruppe vorhanden ist" in {
      val card1 = Card("5", "♥")
      // KI-Spieler besitzt eine Karte, aber lastPlayed erfordert beispielsweise 2 Karten.
      val player = Player("TestKI", List(card1), 0, false)
      val lastPlayed = Some(List(Card("6", "♦"), Card("6", "♣")))
      val (played, updatedPlayer) = player.playCard(lastPlayed)
      played shouldBe None
      updatedPlayer.hand shouldEqual List(card1)
    }

    "sofort zurückgeben, wenn ein Rang definiert ist" in {
      val card1 = Card("5", "♥")
      val player = Player("RankedPlayer", List(card1), 0, false, Some(1))
      val (played, updatedPlayer) = player.playCard(None)
      played shouldBe None
      updatedPlayer shouldEqual player
    }
  }
}
