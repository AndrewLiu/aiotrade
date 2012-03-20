package org.aiotrade.lib.backtest

/**
 * 
 * @author Caoyuan Deng
 */
import org.aiotrade.lib.trading.Position

class TradeRule {
  var quantityPerLot = 100
  var tradableProportionOfVolume = 0.1

  def buyPriceRule(o: Double, h: Double, l: Double, c: Double): Double = {
    o
  }

  def sellPriceRule(o: Double, h: Double, l: Double, c: Double): Double = {
    o
  }
  
  def buyQuantityRule(volume: Double, price: Double, fund: Double): Int = {
    val quantity = maxQuantity(volume, price, fund)
    roundQuantity(quantity)
  }
  
  def sellQuantityRule(volume: Double, price: Double, quantity: Double): Int = {
    math.min(quantity, volume * tradableProportionOfVolume).toInt
  }

  protected def maxQuantity(volume: Double, price: Double, fund: Double) = {
    math.min(fund / price, volume * tradableProportionOfVolume)
  }
  
  protected def roundQuantity(quantity: Double): Int = {
    quantity.toInt / quantityPerLot * quantityPerLot
  }
  
  def buyTimeRule {}
  
  def sellTimeRule {}
  
  def cutLossRule(position: Position): Boolean = {
    val profit = (position.currentPrice - position.price) / position.price
    profit < -0.05
  }
  
  def takeProfitRule(position: Position): Boolean = {
    val profit = (position.currentPrice - position.price) / position.price
    val maxProfit = (position.highestPrice - position.price) / position.price
    profit < maxProfit * 0.4
  }
}