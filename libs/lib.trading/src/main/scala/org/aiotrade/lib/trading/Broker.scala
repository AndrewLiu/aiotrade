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

  @throws(classOf[BrokerException])
  def connect: Unit

  @throws(classOf[BrokerException])
  def disconnect: Unit

  @throws(classOf[BrokerException])
  def submit(order: Order)

  @throws(classOf[BrokerException])
  def cancel(order: Order): Unit

  @throws(classOf[BrokerException])
  def modify(order: Order): Unit
  def isAllowOrderModify: Boolean
  
  def allowedTypes: List[OrderType]
  def allowedSides: List[OrderSide]
  def allowedValidity: List[OrderValidity]
  def allowedRoutes: List[OrderRoute]
  def canTrade(sec: Sec): Boolean
  def getSecurityBySymbol(symbol: String): Sec
  def getSymbolBySecurity(sec: Sec)
  def accounts: Array[Account]
  def executingOrders: collection.Map[Sec, collection.Iterable[Order]]

  /**
   * Used only for paper work
   */
  def processTrade(sec: Sec, time: Long, price: Double, quantity: Double) {}
}


case class BrokerException(message: String, cause: Throwable) extends Exception(message, cause)

trait OrderDelta {
  def order: Order
}
object OrderDelta {
  case class Added(order: Order) extends OrderDelta
  case class Removed(order: Order) extends OrderDelta
  case class Updated(order: Order) extends OrderDelta  
}

case class OrderDeltasEvent(broker: Broker, deltas: Seq[OrderDelta])
