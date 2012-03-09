package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.util.actors.Publisher
import java.util.Currency
import java.util.Locale
import java.util.UUID

class Account(var description: String, val currency: Currency = Currency.getInstance(Locale.getDefault)) extends Publisher {
  val id: Long = UUID.randomUUID.getMostSignificantBits
  
  private var _balance = 0.0
  private var _transactions = new ArrayList[Transaction]()
  private var _positions = new ArrayList[Position]()
  private var _expenseScheme: ExpenseScheme = _

  def balance: Double = _balance
  def balance_=(balance: Double) {
    _balance = balance
  }

  def expenseScheme = _expenseScheme
  def expenseScheme_=(expenseScheme: ExpenseScheme) {
    _expenseScheme = expenseScheme
  }

  def transactions = _transactions.toArray
  def positions = _positions.toArray

  def processCompletedOrder(time: Long, order: Order) {
    val expenses = if (expenseScheme != null) {
      order.side match {
        case OrderSide.Buy => expenseScheme.getBuyExpenses(order.filledQuantity, order.averagePrice)
        case OrderSide.Sell => expenseScheme.getSellExpenses(order.filledQuantity, order.averagePrice)
        case _ => Double.NaN
      }
    } else Double.NaN
    
    val transaction = new TradeTransaction(time, order, order.transactions, if (expenses != Double.NaN) new ExpenseTransaction(expenses) else null)
    _transactions += transaction

    _balance -= transaction.amount

    val quantity = if (order.side == OrderSide.Sell) -order.filledQuantity else order.filledQuantity
    val averagePrice = transaction.amount / order.filledQuantity
    
    _positions find (_.sec == order.sec) match {
      case None => 
        val position = Position(time, order.sec, quantity, averagePrice)
        _positions += position
        publish(PositionOpened(this, position))
        
      case Some(position) =>
        position.add(time, quantity, averagePrice)
        if (position.quantity == 0) {
          _positions -= position
          publish(PositionClosed(this, position))
        } else {
          publish(PositionChanged(this, position))
        }
    }
  }
}

