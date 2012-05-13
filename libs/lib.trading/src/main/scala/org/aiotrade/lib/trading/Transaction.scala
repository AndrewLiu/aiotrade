package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec
import java.util.UUID

trait Transaction {
  def id: Long
  def time: Long
  def description: String
  def amount: Double
  def subTransactions: Array[Transaction]
  
  /**
   * Gets the order that originated this transaction, if any.
   *
   * @return the order reference, or <code>null</code> if the transaction wasn't
   * originated from an order.
   */
  def order: Order
}

case class ExpenseTransaction(val time: Long, val amount: Double) extends Transaction {
  def this(amount: Double) = this(System.currentTimeMillis, amount)

  val id = UUID.randomUUID.getMostSignificantBits
  val description = "Expenses"
  val order: Order = null
  val subTransactions: Array[Transaction] = Array[Transaction]()
}

case class SecurityTransaction(val time: Long, val sec: Sec, quantity: Double, price: Double) extends Transaction {
  val id = UUID.randomUUID.getMostSignificantBits
  val description = "%s %s at %s".format(sec.uniSymbol, quantity, price)
  val amount = quantity * price
  val order: Order = null
  val subTransactions: Array[Transaction] = Array[Transaction]()
}

case class TradeTransaction(val time: Long, val order: Order, chunks: Array[Transaction], expenses: Transaction) extends Transaction {
  val id = UUID.randomUUID.getMostSignificantBits
  val description = "Order %s".format(order)

  val subTransactions = {
    val xs = new ArrayList[Transaction]() ++= chunks
    if (expenses != null) {
      xs += expenses
    }
    xs.toArray
  }

  val amount = {
    var sum = 0.0
    var i = 0
    while (i < subTransactions.length) {
      sum += subTransactions(i).amount
      i += 1
    }
    sum
  }
}
