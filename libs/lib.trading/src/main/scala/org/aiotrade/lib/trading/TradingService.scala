package org.aiotrade.lib.trading

import org.aiotrade.lib.util.actors.Publisher

trait TradingService extends Publisher {
  def broker: Broker
  def accounts: List[Account]
  def benchmark: Benchmark
  def param: Param
}

trait Param extends Publisher {
  /** Used in the image title */
  def titleDescription: String = toString
  /** Used in the image file name */
  def shortDescription: String = toString
}
  
object NoParam extends Param {
  override val shortDescription = ""
  override def toString = "P()"
}
