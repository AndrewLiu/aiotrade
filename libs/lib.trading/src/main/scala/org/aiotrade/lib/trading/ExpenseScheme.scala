package org.aiotrade.lib.trading

trait ExpenseScheme {
  def getOpeningExpenses(quantity: Double, averagePrice: Double): Double
  def getClosingExpenses(quantity: Double, averagePrice: Double): Double
}

object ExpenseScheme {
  val LimitedProportional1Scheme = LimitedProportionalScheme(0.05, 5, 100)
  val LimitedProportional2Scheme = LimitedProportionalScheme(0.05, 5, Double.PositiveInfinity) // no maximum limit
}

object NoExpensesScheme extends ExpenseScheme {
  def getOpeningExpenses(quantity: Double, averagePrice: Double) = 0.0
  def getClosingExpenses(quantity: Double, averagePrice: Double) = 0.0

  override 
  def hashCode = 11 * toString.hashCode

  override
  def toString = "None expenses scheme"
}

case class SimpleFixedScheme(expenses: Double = 9.95) extends ExpenseScheme {
  def getOpeningExpenses(quantity: Double, averagePrice: Double) = expenses
  def getClosingExpenses(quantity: Double, averagePrice: Double) = expenses
}


case class LimitedProportionalScheme(percentage: Double, minimum: Double, maximum: Double) extends ExpenseScheme {

  def getOpeningExpenses(quantity: Double, averagePrice: Double): Double = {
    var expenses = quantity * averagePrice / 100.0 * percentage
    if (expenses < minimum) {
      expenses = minimum
    }
    if (expenses > maximum) {
      expenses = maximum
    }
    expenses
  }

  def getClosingExpenses(quantity: Double, averagePrice: Double): Double = {
    var expenses = quantity * averagePrice / 100.0 * percentage
    if (expenses < minimum) {
      expenses = minimum
    }
    if (expenses > maximum) {
      expenses = maximum
    }
    expenses
  }
}

/**
 * Sample params:
 * @param level1 = 0.01
 * @param level1quantity = 500
 * @param level2 = 0.005
 * @param minimum = 1.0
 */
case class TwoLevelsPerShareScheme(level1: Double, level1quantity: Double, level2: Double, minimum: Double) extends ExpenseScheme {

  def getOpeningExpenses(quantity: Double, averagePrice: Double): Double = {
    var expenses = level1 * (if (quantity > level1quantity) level1quantity else quantity)
    if (quantity > level1quantity) {
      expenses += level2 * (quantity - level1quantity)
    }
    if (expenses < minimum) {
      expenses = minimum
    }
    expenses
  }

  def getClosingExpenses(quantity: Double, averagePrice: Double): Double = {
    var expenses = level1 * (if (quantity > level1quantity) level1quantity else quantity)
    if (quantity > level1quantity) {
      expenses += level2 * (quantity - level1quantity)
    }
    if (expenses < minimum) {
      expenses = minimum
    }
    expenses
  }
}

abstract class ChinaExpenseScheme extends ExpenseScheme {
  /** Applied on both sides of sell and buy, usally 0.5% - 0.05%, 0.08%  */
  def brokerageRate: Double
  /** Applied on sell side, 0.1%  */
  def stamptaxRate: Double
  /** Applied on both side of sell and buy, Shanghai: RMB1.0 per 1000 quantity, Shenzhen 0.0 */
  def transferFee: Double
  /** Usally RMB5.0 */
  def minimumBrokerageFee: Double
  
  def getOpeningExpenses(quantity: Double, averagePrice: Double): Double = {
    val amount = quantity * averagePrice
    math.max(brokerageRate * amount, minimumBrokerageFee) + 
    transferFee * (quantity / 1000 + 1)
  }
  
  def getClosingExpenses(quantity: Double, averagePrice: Double): Double = {
    val amount = quantity * averagePrice
    math.max(brokerageRate * amount, minimumBrokerageFee) + 
    transferFee * (quantity / 1000 + 1) + 
    stamptaxRate * amount
  }
}

case class ShanghaiExpenseScheme(brokerageRate: Double, stamptaxRate: Double = 0.001, transferFee: Double = 1.0, minimumBrokerageFee: Double = 5.0) extends ChinaExpenseScheme
case class ShenzhenExpenseScheme(brokerageRate: Double, stamptaxRate: Double = 0.001, transferFee: Double = 0.0, minimumBrokerageFee: Double = 5.0) extends ChinaExpenseScheme

case class ChinaFinancialFuturesScheme(brokerageRate: Double = 0.0001, stamptaxRate: Double = 0.000050) extends ExpenseScheme {
  
  def getOpeningExpenses(quantity: Double, averagePrice: Double): Double = {
    val amount = quantity * averagePrice
    (brokerageRate + stamptaxRate) * amount
  }

  def getClosingExpenses(quantity: Double, averagePrice: Double): Double = {
    val amount = quantity * averagePrice
    (brokerageRate + stamptaxRate) * amount
  }
}