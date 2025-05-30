package de.htwg.blackjack.util

import java.util.concurrent.locks.{Condition, ReentrantLock}

class ObservableSync {
  private val lock: ReentrantLock = new ReentrantLock()
  private val condition: Condition = lock.newCondition()

  def waitForUpdate(): Unit = {
    lock.lock()
    try condition.await()
    finally lock.unlock()
  }

  def signalUpdate(): Unit = {
    lock.lock()
    try condition.signalAll()
    finally lock.unlock()
  }
}
