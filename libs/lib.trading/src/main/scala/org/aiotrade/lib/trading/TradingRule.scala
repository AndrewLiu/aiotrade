package org.aiotrade.lib.trading

/**
 * 
 * @author Caoyuan Deng
 */

class TradingRule {
  val quantityPerLot = 100
  val tradableProportionOfVolume = 0.1

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
    math.min(quantity, volume * quantityPerLot * tradableProportionOfVolume).toInt
  }

  protected def maxQuantity(volume: Double, price: Double, fund: Double) = {
    math.min(fund / price, volume * quantityPerLot * tradableProportionOfVolume)
  }
  
  protected def roundQuantity(quantity: Double): Int = {
    quantity.toInt / quantityPerLot * quantityPerLot
  }
  
  def cutLossRule(position: Position): Boolean = {
    position.profitRatio < -0.05
  }
  
  def takeProfitRule(position: Position): Boolean = {
    position.profitRatio < position.maxProfitRatio * 0.6
  }
}