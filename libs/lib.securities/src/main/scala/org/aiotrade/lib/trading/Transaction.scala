package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec
import java.util.UUID

trait Transaction {
  def id: Long
  def time: Long
  def description: String
  def amount: Double
  def transactions: Array[Transaction]
  
  /**
   * Gets the order that originated this transaction, if any.
   *
   * @return the order reference, or <code>null</code> if the transaction wasn't
   * originated from an order.
   */
  def order: Order
}

class ExpenseTransaction(val time: Long, val amount: Double) extends Transaction {
  def this(amount: Double) = this(System.currentTimeMillis, amount)

  val id = UUID.randomUUID.getMostSignificantBits
  val description = "Expenses"
  val order: Order = null
  val transactions: Array[Transaction] = Array[Transaction]()
}

class SecurityTransaction(val sec: Sec, quantity: Double, price: Double) extends Transaction {
  val id = UUID.randomUUID.getMostSignificantBits
  val time = System.currentTimeMillis
  val description = "%s %s at %s".format(sec.uniSymbol, quantity, price)
  val amount = quantity * price
  val order: Order = null
  val transactions: Array[Transaction] = Array[Transaction]()
}

class TradeTransaction(val order: Order, chunks: Array[Transaction], expenses: Transaction) extends Transaction {
  val id = UUID.randomUUID.getMostSignificantBits
  val time = System.currentTimeMillis
  val description = "Order %s".format(order)

  val transactions = if (expenses != null) {
    (new ArrayList() ++ chunks + expenses).toArray
  } else chunks

  val amount = {
    var value = 0.0
    var i = 0
    while (i < transactions.length) {
      value += transactions(i).amount
      i += 1
    }
    value
  }
}
