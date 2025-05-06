// src/test/scala/blackjack/view/TuiViewRunSpec.scala
package blackjack.view

import de.htwg.blackjack.view.TuiView
import org.scalatest.funsuite.AnyFunSuite

import scala.compiletime.uninitialized
import java.io.*
import scala.Console.withOut

class ExitException(code: Int) extends RuntimeException(s"Exit($code)")

class TuiViewRunSpec extends AnyFunSuite {
  private var inOrig: InputStream = uninitialized
  private var outOrig: PrintStream = uninitialized


  test("printWelcome() gibt genau die Willkommens­nachricht aus") {
    val baos = new ByteArrayOutputStream()
    withOut(new PrintStream(baos)) {
      TuiView.printWelcome()
    }
    val out = baos.toString.trim
    assert(out == "Willkommen zu Scala-Blackjack mit Betsystem!")
  }

 
}
