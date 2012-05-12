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
  
  def buyPriceRule(quote: Quote): Double = {
    quote.open
  }

  def sellPriceRule(quote: Quote): Double = {
    quote.open
  }
  
  def buyQuantityRule(quote: Quote, price: Double, fund: Double): Int = {
    val quantity = maxQuantity(quote.volume, price, fund)
    roundQuantity(quantity)
  }
  
  def sellQuantityRule(quote: Quote, price: Double, quantity: Double): Int = {
    math.min(quantity, quote.volume * quantityPerLot * tradableProportionOfVolume).toInt
  }

  def cutLossRule(position: Position): Boolean = {
    position.profitRatio < -0.05
  }
  
  def takeProfitRule(position: Position): Boolean = {
    position.profitRatio < position.maxProfitRatio * 0.6
  }

  // -- helper
  
  protected def maxQuantity(volume: Double, price: Double, fund: Double) = {
    math.min(fund / price, volume * quantityPerLot * tradableProportionOfVolume)
  }
  
  protected def roundQuantity(quantity: Double): Int = {
    quantity.toInt / quantityPerLot * quantityPerLot
  }
  
}