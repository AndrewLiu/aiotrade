package org.aiotrade.lib.trading


import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher

/**
 * Brokers that managed accounts and receive trade to fill orders
 *
 */
trait Broker extends Publisher {
  def id: Long
  def name: String
  def connect: Unit
  def disconnect: Unit
  def allowedTypes: Array[OrderType]
  def allowedSides: Array[OrderSide]
  def allowedValidity: Array[OrderValidity]
  def allowedRoutes: Array[OrderRoute]
  def canTrade(sec: Sec): Boolean
  def getSecurityFromSymbol(symbol: String): Sec
  def getSymbolFromSecurity(sec: Sec)
  def accounts: Array[Account]
  def orderExecutors: Array[OrderExecutor]
  
  @throws(classOf[BrokerException])
  def prepareOrder(order: Order): OrderExecutor
}


case class BrokerException(message: String, cause: Throwable) extends Exception(message, cause)

trait OrderDelta {
  def order: OrderExecutor
}
object OrderDelta {
  case class Added(order: OrderExecutor) extends OrderDelta
  case class Removed(order: OrderExecutor) extends OrderDelta
  case class Updated(order: OrderExecutor) extends OrderDelta  
}

case class OrderDeltasEvent(broker: Broker, deltas: Array[OrderDelta])
