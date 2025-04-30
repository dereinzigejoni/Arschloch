import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import scala.Console.{withIn, withOut}
import de.htwg.Card.Card
import de.htwg.Player.Player
import de.htwg.game.ArschlochGame
import de.htwg.game.ArschlochGame.playRound

// Helper für KI-Dummy-Player
private def dummyPlayer(name: String, card: Card): Player =
  new Player(name, Seq(card), rank = None, isHuman = false) {

    override def playCard(
                           lastPlayed: Option[Seq[Card]],
                           indexProvider: () => Seq[Int]
                         ): (Option[Seq[Card]], Player) = {
      // Wir ignorieren indexProvider und spielen immer unsere eine Karte
      (Some(Seq(card)), this.copy(hand = Seq.empty))
    }

  }



class ArschlochGameSpec extends AnyWordSpec with Matchers {

  "ArschlochGame.suits und values" should {
    "entsprechen exakt den Literal-Arrays" in {
      ArschlochGame.suits shouldEqual Array("♥", "♦", "♠", "♣")
      ArschlochGame.values shouldEqual Array("2","3","4","5","6","7","8","9","10","J","Q","K","A")
    }
  }

  "ArschlochGame.getValue" should {
    "wandelt Zahlen- und Bildkarten korrekt um" in {
      Seq("2"->2, "10"->10, "J"->11, "Q"->12, "K"->13, "A"->14).foreach {
        case (s,v) => ArschlochGame.getValue(s) shouldBe v
      }
    }
    "wirft NumberFormatException für ungültige Strings" in {
      intercept[NumberFormatException] {
        ArschlochGame.getValue("X")
      }
    }
  }

  "createDeck" should {
    val deck = ArschlochGame.createDeck()

    "erzeugt 52 eindeutige Karten" in {
      deck should have size 52
      deck.distinct should have size 52
    }
    "beginnt mit 2♥ und endet mit A♣" in {
      deck.head shouldBe Card("2","♥")
      deck.last shouldBe Card("A","♣")
    }
    "enthält jede Kombination aus suit und value" in {
      for (s <- ArschlochGame.suits; v <- ArschlochGame.values)
        deck should contain (Card(v, s))
    }
  }

  "shuffleAndDeal" should {
    "teilt gleichmäßig und droppt Restkarten" in {
      val players4 = Array.fill(4)( Player("P", Seq.empty, None, isHuman = false) )
      val dealt4 = ArschlochGame.shuffleAndDeal(players4)
      dealt4.foreach(_.hand.length shouldBe 13)
      dealt4.flatMap(_.hand) should have size 52

      val players3 = Array.fill(4)( Player("P", Seq.empty, None, isHuman = false) )
      val dealt3 = ArschlochGame.shuffleAndDeal(players3)
      dealt3.foreach(_.hand.length shouldBe 13)
      dealt3.flatMap(_.hand).length shouldBe 52
    }
    "gibt alle Karten an einen einzelnen Spieler" in {
      val solo = Array( Player("Solo", Seq.empty, None, isHuman = true) )
      val dealt = ArschlochGame.shuffleAndDeal(solo)
      dealt.head.hand should have size 52
    }
  }

  "exchangeCards" should {
    val pres = Player("Prez", Array(
      Card("2","♠"), Card("3","♠"), Card("4","♠"), Card("5","♠")
    ), None, isHuman = false)
    val ars = Player("Ass", Array(
      Card("J","♠"), Card("Q","♠"), Card("K","♠"), Card("A","♠")
    ), None, isHuman = false)

    "tauscht korrekt die zwei schlechtesten/gutesten Karten aus" in {
      val (newPrez, newAss) = ArschlochGame.exchangeCards(pres, ars)
      newPrez.hand.map(_.value) should contain allOf("A","K")
      newPrez.hand.map(_.value) shouldNot contain allOf("2","3")
      newAss.hand.map(_.value) should contain allOf("2","3")
      newAss.hand.map(_.value) shouldNot contain allOf("A","K")
    }

    "gibt passende Nachrichten aus" in {
      val out = new ByteArrayOutputStream()
      withOut(new PrintStream(out)) {
        ArschlochGame.exchangeCards(pres, ars)
      }
      val txt = out.toString
      txt should include (s"${ars.name} gibt seine besten Karten an ${pres.name}")
      txt should include (s"${pres.name} gibt seine schlechtesten Karten an ${ars.name}")
    }
  }

  "playRound" when {
    "ein Spieler übergeben wird" should {
      "gibt das Original-Array zurück" in {
        val p     = Player("Solo", Array(Card("2","♥")), None, isHuman = false)
        val input = Array(p)
        val out = playRound(input)
        out shouldBe theSameInstanceAs (input)
      }
    }
  }

  "askForPlayers" should {
    "defaultet auf 4/2 und weist korrekte Flags zu" in {
      val input = Seq("x","y","Alice","Bob").mkString("\n")
      val in = new ByteArrayInputStream(input.getBytes)
      val out = new ByteArrayOutputStream()
      withIn(in) {
        withOut(new PrintStream(out)) {
          val pls = ArschlochGame.askForPlayers()
          pls should have size 4
          pls.take(2).forall(_.isHuman) shouldBe true
          pls.drop(2).forall(!_.isHuman) shouldBe true
        }
      }
    }
    "nimmt gültige Grenzen an und defaultet Namen bei Leerstring" in {
      val input = Seq("3","2","","Bob").mkString("\n")
      val in = new ByteArrayInputStream(input.getBytes)
      val pls = withIn(in) { ArschlochGame.askForPlayers() }
      pls.size shouldBe 3
      pls.head.name shouldBe "Spieler1"
      pls(1).name shouldBe "Bob"
    }
  }

  "mainGameLoop" should {
    "druckt Startliste und beendet auf 'q'" in {
      val players = Array(Player("A", Seq.empty, None, true))
      val in = new ByteArrayInputStream("q\n".getBytes)
      val out = new ByteArrayOutputStream()
      withIn(in) {
        withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(players)
          }
        }
      }
      val txt = out.toString
      txt should include("Spiel startet mit folgenden Spielern:")
      txt should include("Spiel beendet")
    }
    "handhabt 'n' und dann 'q' korrekt" in {
      val players = Array(Player("A", Seq.empty, None, true))
      val in = new ByteArrayInputStream(Seq("n","q").mkString("\n").getBytes)
      val out = new ByteArrayOutputStream()
      withIn(in) {
        withOut(new PrintStream(out)) {
          noException shouldBe thrownBy {
            ArschlochGame.mainGameLoop(players)
          }
        }
      }
      val txt = out.toString
      txt should include("für die nächste Runde")
      txt should include("Spiel beendet")
    }
  }
}
