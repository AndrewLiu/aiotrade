package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher
import java.util.Currency
import java.util.Locale
import java.util.UUID
import scala.collection.mutable

class Account(private var _description: String, 
              private var _balance: Double, 
              private var _expenseScheme: ExpenseScheme = NoExpensesScheme, 
              val currency: Currency = Currency.getInstance(Locale.getDefault)) extends Publisher {
  
  val id = UUID.randomUUID.getMostSignificantBits
  
  private val _transactions = new ArrayList[Transaction]()
  private val _secToPositions = new mutable.HashMap[Sec, Position]()
  
  val initialAsset = _balance
  def asset = {
    _balance + _secToPositions.foldRight(0.0){(x, s) => s + x._2.asset}
  }

  def balance: Double = _balance
  def increaseCash(cash: Double) {
    _balance += cash
  }

  def description = _description
  def description(description: String) {
    _description = description
  }
  
  def expenseScheme = _expenseScheme
  def expenseScheme_=(expenseScheme: ExpenseScheme) {
    _expenseScheme = expenseScheme
  }

  def positions = _secToPositions
  def transactions = _transactions.toArray
  
  def processFilledOrder(time: Long, order: Order) {
    val expenses = order.side match {
      case OrderSide.Buy => _expenseScheme.getBuyExpenses(order.filledQuantity, order.averagePrice)
      case OrderSide.Sell => _expenseScheme.getSellExpenses(order.filledQuantity, order.averagePrice)
      case _ => 0.0
    }
    
    val transaction = TradeTransaction(time, order, order.transactions, if (expenses != 0.0) ExpenseTransaction(time, expenses) else null)
    _transactions += transaction

    _balance -= transaction.amount

    val quantity = if (order.side == OrderSide.Sell) -order.filledQuantity else order.filledQuantity
    val averagePrice = transaction.amount / order.filledQuantity
    
    _secToPositions.get(order.sec) match {
      case None => 
        val position = Position(time, order.sec, quantity, averagePrice)
        _secToPositions(order.sec) = position
        publish(PositionOpened(this, position))
        
      case Some(position) =>
        position.add(time, quantity, averagePrice)
        if (position.quantity == 0) {
          _secToPositions -= order.sec
          publish(PositionClosed(this, position))
        } else {
          publish(PositionChanged(this, position))
        }
    }
  }
}

