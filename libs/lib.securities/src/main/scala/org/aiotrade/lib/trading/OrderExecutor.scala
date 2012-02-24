package org.aiotrade.lib.trading


import org.aiotrade.lib.util.actors.Publisher


abstract class OrderExecutor(val broker: Broker, val order: Order) extends Publisher {

  @throws(classOf[BrokerException])
  def submit

  @throws(classOf[BrokerException])
  def cancel

  def isAllowModify: Boolean = false
  
  @throws(classOf[BrokerException])
  def modify(order: Order) {
    throw BrokerException("Modify not allowed", null)
  }

  override 
  def toString = {
    "OrderExecutor for: " + order
  }
}
