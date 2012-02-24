package org.aiotrade.lib.trading

import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher

trait TradingService extends Publisher {
  def brokers: Array[Broker]
  def getBroker(id: String): Broker
  def getBrokerForSecurity(sec: Sec): Broker 
  def orders: Array[OrderExecutor]
}
