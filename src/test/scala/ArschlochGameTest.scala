import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import htwg.de.Game.ArschlochGame
import htwg.de.Card.Card
import htwg.de.Player.Player

import scala.io.StdIn.readLine

// Dummy-Spieler für Tests, die playCard deterministisch ausführen.
// Damit können wir einen Spieler definieren, der seine einzige Karte spielt.
def dummyPlayer(name: String, card: Card): Player = new Player(name, List(card), 0, true) {
  override def playCard(lastPlayed: Option[List[Card]],
                        inputProvider: () => String = () => readLine().trim): (Some[List[Card]], Player) = {
    (Some(List(card)), copy(hand = List()))
  }
}

class ArschlochGameTest extends AnyWordSpec with Matchers {

  "getValue" should {
    "den richtigen numerischen Wert für Zahlen und Bildkarten zurückgeben" in {
      ArschlochGame.getValue("J") shouldEqual 11
      ArschlochGame.getValue("Q") shouldEqual 12
      ArschlochGame.getValue("K") shouldEqual 13
      ArschlochGame.getValue("A") shouldEqual 14
      ArschlochGame.getValue("5") shouldEqual 5
    }
  }

  "createDeck" should {
    "ein vollständiges Deck mit 52 Karten erzeugen" in {
      val deck = ArschlochGame.createDeck()
      deck should have size (52)
      deck.foreach { card =>
        ArschlochGame.suits should contain (card.suit)
        ArschlochGame.values should contain (card.value)
      }
    }
  }

  "shuffleAndDeal" should {
    "allen Spielern die gleiche Anzahl an Karten austeilen" in {
      val players = List(
        Player("Alice", List(), 0, true),
        Player("Bob", List(), 0, true),
        Player("Charlie", List(), 0, true),
        Player("David", List(), 0, true)
      )
      val dealtPlayers = ArschlochGame.shuffleAndDeal(players)
      // Bei 52 Karten und 4 Spielern müssen es 13 Karten pro Spieler sein.
      dealtPlayers.foreach { p =>
        p.hand should have size 13
      }
      dealtPlayers.flatMap(_.hand) should have size (52)
    }
  }

  "exchangeCards" should {
    "die Karten korrekt zwischen Präsident und Arschloch tauschen" in {
      val card2H = Card("2", "♥")  // Wert: 2
      val card3D = Card("3", "♦")  // Wert: 3
      val cardKC = Card("K", "♣")  // Wert: 13
      val cardAS = Card("A", "♠")  // Wert: 14

      // Präsident: Hand unsortiert, aber beim Sortieren (ascending) wird [2H, 3D, KC] generiert,
      // sodass die schlechtesten zwei Karten [2H, 3D] sind.
      val president = Player("President", List(card2H, card3D, cardKC), 0, true)
      // Arschloch: Beim Sortieren (descending) wird aus [KC, A♠, 3D] die Reihenfolge [A♠, KC, 3D] erzeugt,
      // sodass die besten zwei Karten [A♠, KC] sind.
      val arschloch = Player("Arschloch", List(cardKC, cardAS, card3D), 0, false)

      val (newPresident, newArschloch) = ArschlochGame.exchangeCards(president, arschloch)

      // Präsident erhält die Ursprungs­karten plus [A♠, KC]
      newPresident.hand should contain allElementsOf (List(card2H, card3D, cardKC, cardAS, cardKC))
      // Arschloch: aus ursprünglicher Hand [KC, A♠, 3D] wird [A♠, KC] entfernt und [2H, 3D] hinzugefügt.
      newArschloch.hand should contain allElementsOf (List(card3D, card2H, card3D))
    }
  }

  "playRound" should {
    "eine vollständige Rangliste zurückliefern, wenn Spieler je eine Karte haben" in {
      val card1 = Card("2", "♥")
      val card2 = Card("3", "♦")
      val card3 = Card("4", "♣")

      val players = List(
        dummyPlayer("Alice", card1),
        dummyPlayer("Bob", card2),
        dummyPlayer("Charlie", card3)
      )

      val ranking = ArschlochGame.playRound(players)
      ranking.foreach(p => p.rank shouldBe defined)
      ranking should have size players.size
    }

    "Spieler ohne Karten überspringen" in {
      val card = Card("5", "♥")

      // Dummy-Spieler, der nur spielt, wenn er Karten hat.
      def dummyPlayerOpt(name: String, maybeCard: Option[Card]): Player = new Player(name, maybeCard.toList, 0, true) {
        override def playCard(lastPlayed: Option[List[Card]],
                              inputProvider: () => String = () => readLine().trim): (Option[List[Card]], Player) =
          if (hand.nonEmpty) (Some(List(hand.head)), copy(hand = List())) else (None, this)
      }

      val players = List(
        dummyPlayerOpt("Alice", Some(card)),  // spielt die einzige Karte
        dummyPlayerOpt("Bob", None),           // besitzt keine Karte
        dummyPlayerOpt("Charlie", Some(card))  // spielt ebenfalls
      )

      val ranking = ArschlochGame.playRound(players)
      ranking.foreach(p => p.rank shouldBe defined)
      ranking should have size players.size
    }
  }

}
