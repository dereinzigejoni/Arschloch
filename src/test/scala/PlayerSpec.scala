import org.scalatest.funsuite.AnyFunSuite
import de.htwg.Card.Card
class PlayerSpec extends AnyFunSuite {
  test("Player case class default values") {
    val player = Player("Alice", Array.empty)
    assert(player.name == "Alice")
    assert(player.hand.isEmpty)
    assert(player.rank.isEmpty)
    assert(player.isHuman)
  }
  test("AI player with defined rank should pass immediately") {
    val hand = Array(Card("10", "♥"))
    val ai = Player("AI", hand, rank = Some(1), isHuman = false)
    val (playedOpt, updated) = ai.playCard(None, () => Array.empty)
    assert(playedOpt.isEmpty)
    assert(updated == ai)
  }
  test("AI player without rank and no lastPlayed plays all highest value cards") {
    val c1 = Card("10", "♥")
    val c2 = Card("K", "♦")
    val c3 = Card("K", "♣")
    val ai = Player("AI", Array(c1, c2, c3), rank = None, isHuman = false)
    val (playedOpt, updated) = ai.playCard(None, () => Array.empty)
    assert(playedOpt.isDefined && playedOpt.get.sameElements(Array(c2, c3)))
    assert(updated.hand.sameElements(Array(c1)))
  }
  test("AI player responds to lastPlayed by playing minimal higher group") {
    val prev = Array(Card("J", "H"), Card("J", "D"))
    val c1 = Card("J", "♦")
    val c2 = Card("Q", "♥")
    val c3 = Card("Q", "♦")
    val c4 = Card("K", "♣")
    val ai = Player("AI", Array(c1, c2, c3, c4), rank = None, isHuman = false)
    val (playedOpt, updated) = ai.playCard(Some(prev), () => Array.empty)
    assert(playedOpt.isDefined && playedOpt.get.sameElements(Array(c2, c3)))
    assert(updated.hand.sameElements(Array(c1, c4)))
  }
  test("AI player with no playable cards returns pass") {
    val prev = Array(Card("K", "♥"))
    val c1 = Card("2", "♣")
    val ai = Player("AI", Array(c1), rank = None, isHuman = false)
    val (playedOpt, updated) = ai.playCard(Some(prev), () => Array.empty)
    assert(playedOpt.isEmpty)
    assert(updated == ai)
  }
  test("AI player with equal-value group does not play and passes") {
    val prev = Array(Card("Q", "♥"), Card("Q", "♣"))
    val c1 = Card("Q", "♦")
    val c2 = Card("7", "♠")
    val ai = Player("AI", Array(c1, c2), rank = None, isHuman = false)
    val (playedOpt, updated) = ai.playCard(Some(prev), () => Array.empty)
    assert(playedOpt.isEmpty)
    assert(updated == ai)
  }
  test("Human player passes when indexProvider returns Array(0)") {
    val hand = Array(Card("2", "♥"), Card("3", "♣"))
    val human = Player("Bob", hand, rank = None, isHuman = true)
    val (playedOpt, updated) = human.playCard(None, () => Array(0))
    assert(playedOpt.isEmpty)
    assert(updated.hand.sameElements(hand))
  }
  test("Human player selects valid cards without lastPlayed") {
    val c1 = Card("4", "♥")
    val c2 = Card("5", "♣")
    val c3 = Card("6", "♦")
    val human = Player("Bob", Array(c1, c2, c3), rank = None, isHuman = true)
    val (playedOpt, updated) = human.playCard(None, () => Array(2, 3))
    assert(playedOpt.isDefined && playedOpt.get.sameElements(Array(c2, c3)))
    assert(updated.hand.sameElements(Array(c1)))
  }
  test("Human player rejects invalid index and retries") {
    val c1 = Card("7", "♥")
    val c2 = Card("8", "♣")
    var calls = 0
    val provider = () => {
      calls += 1
      if (calls == 1) Array(3) // invalid
      else Array(1, 2)
    }
    val human = Player("Bob", Array(c1, c2), rank = None, isHuman = true)
    val (playedOpt, updated) = human.playCard(None, provider)
    assert(playedOpt.isDefined && playedOpt.get.sameElements(Array(c1, c2)))
    assert(updated.hand.isEmpty)
  }
  test("Human player enforces matching size to lastPlayed") {
    val prev = Array(Card("9", "♥"), Card("9", "♣"))
    val c1 = Card("9", "♦")
    val c2 = Card("10", "♥")
    val c3 = Card("10", "♣")
    var calls = 0
    val provider = () => {
      calls += 1
      if (calls == 1) Array(3)    // wrong count
      else Array(2, 3)
    }
    val human = Player("Bob", Array(c1, c2, c3), rank = None, isHuman = true)
    val (playedOpt, updated) = human.playCard(Some(prev), provider)
    assert(playedOpt.isDefined && playedOpt.get.sameElements(Array(c2, c3)))
    assert(updated.hand.sameElements(Array(c1)))
  }
  test("Human player enforces higher value than lastPlayed") {
    val prev = Array(Card("9", "♥"))
    val c1 = Card("8", "♥")
    val c2 = Card("10", "♣")
    var calls = 0
    val provider = () => {
      calls += 1
      if (calls == 1) Array(1)    // too low
      else Array(2)
    }
    val human = Player("Bob", Array(c1, c2), rank = None, isHuman = true)
    val (playedOpt, updated) = human.playCard(Some(prev), provider)
    assert(playedOpt.isDefined && playedOpt.get.sameElements(Array(c2)))
    assert(updated.hand.sameElements(Array(c1)))
  }
  test("Human player rejects equal-value selection and retries") {
    val prev = Array(Card("J", "♥"))
    val c1 = Card("J", "♣")
    val c2 = Card("Q", "♦")
    var calls = 0
    val provider = () => {
      calls += 1
      if (calls == 1) Array(1)    // equal to prev
      else Array(2)
    }
    val human = Player("Bob", Array(c1, c2), rank = None, isHuman = true)
    val (playedOpt, updated) = human.playCard(Some(prev), provider)
    assert(playedOpt.isDefined && playedOpt.get.sameElements(Array(c2)))
    assert(updated.hand.sameElements(Array(c1)))
  }
}
