package de.htwg.blackjack.controller
import scala.collection.mutable.Stack
class UndoManager {
  private val stack = Stack[Command]()
  def push(c: Command): Unit = stack.push(c)
  def undo(): Unit = if (stack.nonEmpty) stack.pop().undo()
  def clear(): Unit = stack.clear()
}
