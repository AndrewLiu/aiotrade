package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher
import java.util.Currency
import java.util.Locale
import java.util.UUID
import scala.collection.mutable

abstract class Account(val description: String, protected var _balance: Double, 
                       val currency: Currency = Currency.getInstance(Locale.getDefault)
) extends Publisher {
  
  val id = UUID.randomUUID.getMostSignificantBits
  
  protected val _transactions = new ArrayList[TradeTransaction]()
  
  val initialEquity = _balance
  def balance = _balance
  def credit(funds: Double) {_balance += funds}
  def debit (funds: Double) {_balance -= funds}

  def transactions = _transactions.toArray

  def equity: Double
  def availableFunds: Double
}

abstract class TradableAccount($description: String, $balance: Double, 
                               $currency: Currency = Currency.getInstance(Locale.getDefault)
) extends Account($description, $balance, $currency) {
  protected val _secToPosition = new mutable.HashMap[Sec, Position]()

  def positions = _secToPosition
  def positionGainLoss: Double
  def positionEquity: Double
  def calcFundsToOpen(quantity: Double, price: Double): Double
  def processFillingOrder(time: Long, order: Order)

  def tradingRule: TradingRule
}

class StockAccount($description: String, $balance: Double, val tradingRule: TradingRule, 
                   $currency: Currency = Currency.getInstance(Locale.getDefault)
) extends TradableAccount($description, $balance, $currency) {

  def positionGainLoss = _secToPosition.foldRight(0.0){(x, s) => s + x._2.gainLoss}
  def positionEquity   = _secToPosition.foldRight(0.0){(x, s) => s + x._2.equity}

  def equity = _balance + positionEquity
  def availableFunds = _balance
  
  def calcFundsToOpen(quantity: Double, price: Double) = {
    quantity * price + 
    tradingRule.expenseScheme.getOpeningExpenses(quantity, price)
  }
  
  def processFillingOrder(time: Long, order: Order) {
    val expenses = order.side match {
      case OrderSide.Buy | OrderSide.SellShort => 
        tradingRule.expenseScheme.getOpeningExpenses(order.filledQuantity, order.averagePrice)
      case OrderSide.Sell | OrderSide.BuyCover => 
        tradingRule.expenseScheme.getClosingExpenses(order.filledQuantity, order.averagePrice)
      case _ => 0.0
    }
    
    val expensesTransaction = ExpensesTransaction(time, expenses)
    val transaction = TradeTransaction(time, order, order.transactions, if (expenses != 0.0) expensesTransaction else null)
    _transactions += transaction
    
    _balance -= transaction.amount

    val quantity = order.side match {
      case OrderSide.Sell | OrderSide.SellShort => -order.filledQuantity 
      case OrderSide.Buy  | OrderSide.BuyCover  =>  order.filledQuantity
    }
    val averagePrice = order.averagePrice
    
    _secToPosition.get(order.sec) match {
      case None => 
        val position = Position(this, time, order.sec, quantity, averagePrice)
        _secToPosition(order.sec) = position
        publish(PositionOpened(this, position))
        
      case Some(position) =>
        position.add(time, quantity, averagePrice)
        if (position.quantity == 0) {
          _secToPosition -= order.sec
          publish(PositionClosed(this, position))
        } else {
          publish(PositionChanged(this, position))
        }
    }
  }
  
  override 
  def toString = "%1$s, availableFunds=%2$.0f, equity=%3$.0f, positions=%4$s".format(
    description, availableFunds, equity, positions.values.size
  )
}

class FutureAccount($description: String, $balance: Double, val tradingRule: TradingRule, 
                    $currency: Currency = Currency.getInstance(Locale.getDefault)
) extends TradableAccount($description, $balance, $currency) {
  
  def riskLevel = positionMargin / equity * 100
  def positionMargin = positionEquity * tradingRule.marginRate

  def positionGainLoss = _secToPosition.foldRight(0.0){(x, s) => s + x._2.gainLoss * tradingRule.multiplier}
  def positionEquity   = _secToPosition.foldRight(0.0){(x, s) => s + x._2.equity   * tradingRule.multiplier}
  
  def equity = _balance + positionGainLoss
  def availableFunds = equity - positionMargin
  
  def calcFundsToOpen(quantity: Double, price: Double) = {
    quantity * price * tradingRule.multiplier * tradingRule.marginRate + 
    tradingRule.expenseScheme.getOpeningExpenses(quantity, price * tradingRule.multiplier)
  }
  
  def processFillingOrder(time: Long, order: Order) {
    val expenses = order.side match {
      case OrderSide.Buy | OrderSide.SellShort => 
        tradingRule.expenseScheme.getOpeningExpenses(order.filledQuantity, order.averagePrice)
      case OrderSide.Sell | OrderSide.BuyCover => 
        tradingRule.expenseScheme.getClosingExpenses(order.filledQuantity, order.averagePrice)
      case _ => 0.0
    }
    
    val expensesTransaction = ExpensesTransaction(time, expenses)
    val transaction = TradeTransaction(time, order, order.transactions, if (expenses != 0.0) expensesTransaction else null)
    _transactions += transaction
    
    _balance -= expensesTransaction.amount

    val quantity = order.side match {
      case OrderSide.Sell | OrderSide.SellShort => -order.filledQuantity 
      case OrderSide.Buy  | OrderSide.BuyCover  =>  order.filledQuantity
    }
    val averagePrice = order.averagePrice
    
    _secToPosition.get(order.sec) match {
      case None => 
        val position = Position(this, time, order.sec, quantity, averagePrice)
        _secToPosition(order.sec) = position
        publish(PositionOpened(this, position))
        
      case Some(position) =>
        order.side match {
          case OrderSide.Buy | OrderSide.SellShort => 
          case OrderSide.Sell | OrderSide.BuyCover => 
            val offsetGainLoss = (averagePrice - position.price) * quantity * tradingRule.multiplier
            _balance += offsetGainLoss
          case _ =>
        }

        position.add(time, quantity, averagePrice)
        if (position.quantity == 0) {
          _secToPosition -= order.sec
          publish(PositionClosed(this, position))
        } else {
          publish(PositionChanged(this, position))
        }
    }
  }
  
  override 
  def toString = "%1$s, availableFunds=%2$.0f, equity=%3$.0f, positionEquity=%4$.0f, positionMargin=%5$.0f, risk=%6$.2f%%, positions=%7$s".format(
    description, availableFunds, equity, positionEquity, positionMargin, riskLevel, positions.values.map(_.quantity).mkString("(", ",", ")")
  )
}

class CashAccount($description: String, $balance: Double,
                  $currency: Currency = Currency.getInstance(Locale.getDefault)
) extends Account($description, $balance, $currency) {

  def equity = _balance
  def availableFunds = _balance

  override 
  def toString = "%1$s, availableFunds=%2$.0f".format(
    description, availableFunds
  )

}