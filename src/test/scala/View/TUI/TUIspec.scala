package View.TUI

import de.htwg.Model.Card.Card
import de.htwg.View.TUI.TUI
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}

class TUIspec extends AnyWordSpec with Matchers {

  "The TUI" should {
    "render a card correctly" in {
      val tui = new TUI
      tui.render(Card("Q", "♠")) shouldBe "Q♠"
    }

    "print cards with index recursively" in {
      val cards = List(Card("10", "♥"), Card("A", "♦"))
      val outContent = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(outContent)) {
        val tui = new TUI
        tui.printCards(cards)
      }
      val output = outContent.toString.trim
      output should include("1: 10♥")
      output should include("2: A♦")
    }

    "handle empty card list in printCards" in {
      val outContent = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(outContent)) {
        val tui = new TUI
        tui.printCards(Nil)
      }
      outContent.toString shouldBe ""
    }

    "print line using printLine" in {
      val outContent = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(outContent)) {
        val tui = new TUI
        tui.printLine("Test")
      }
      outContent.toString.trim shouldBe "Test"
    }

    "readLine with prompt" in {
      val in = new ByteArrayInputStream("inputText\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          val tui = new TUI
          val input = tui.readLine("Enter: ")
          input shouldBe "inputText"
          out.toString should include("Enter:")
        }
      }
    }

    "readLine without prompt" in {
      val in = new ByteArrayInputStream("foo\n".getBytes)
      val out = new ByteArrayOutputStream()
      Console.withIn(in) {
        Console.withOut(new PrintStream(out)) {
          val tui = new TUI
          val input = tui.readLine("")
          input shouldBe "foo"
          out.toString should not include "Enter:"
        }
      }
    }
  }
}
