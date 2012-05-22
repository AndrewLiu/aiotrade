package org.aiotrade.lib.trading

import org.aiotrade.lib.securities.model.Quote

/**
 * 
 * @author Caoyuan Deng
 */
class TradingRule {
  val quantityPerLot = 100
  val tradableProportionOfVolume = 0.1
  val expenseScheme: ExpenseScheme = ShenzhenExpenseScheme(0.0008)
  
  // -- usally for futures
  val marginRate: Double = 1.0
  /** contract multiplier,  price per index point, 300.0 in China Index Future, 1 for stock */
  val multiplier: Double = 1.0
  
  def buyPriceRule(quote: Quote): Double = {
    quote.open
  }

  def sellPriceRule(quote: Quote): Double = {
    quote.open
  }
  
  def buyQuantityRule(quote: Quote, price: Double, funds: Double): Int = {
    if (isTradable(quote)) {
      val quantity = maxQuantity(quote.volume, price, funds)
      roundQuantity(quantity)
    } else {
      0
    }
  }
  
  def sellQuantityRule(quote: Quote, price: Double, quantity: Double): Int = {
    if (isTradable(quote)) {
      math.min(quantity, quote.volume * quantityPerLot * tradableProportionOfVolume).toInt
    } else {
      0
    }
  }

  def cutLossRule(position: Position): Boolean = {
    position.gainLossRatio < -0.05
  }
  
  def takeProfitRule(position: Position): Boolean = {
    position.gainLossRatio < position.maxGainLossRatio * 0.6
  }

  // -- helper
  
  protected def isTradable(quote: Quote): Boolean = {
    !(quote.open == quote.close && quote.open == quote.high && quote.open == quote.low)
  }

  protected def maxQuantity(volume: Double, price: Double, funds: Double) = {
    math.min(funds / (price * multiplier * marginRate), volume * quantityPerLot * tradableProportionOfVolume)
  }
  
  protected def roundQuantity(quantity: Double): Int = {
    quantity.toInt / quantityPerLot * quantityPerLot
  }
}