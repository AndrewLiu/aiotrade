package org.aiotrade.lib.trading

import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Execution
import org.aiotrade.lib.securities.model.Sec

class PaperBroker(val id: Long, val name: String) extends Broker {
  private val log = Logger.getLogger(getClass.getName)
  private val idFormatter = new SimpleDateFormat("yyMMddHHmmssSSS")

  private val pendingExecutors = new ArrayList[OrderExecutor]()

  def connect {
    // for paper broker, listenTo ticker server
  }

  def disconnect {
    // for paper broker, deafTo ticker server
  }

  def canTrade(sec: Sec) = {
    true
  }
  
  def getSecurityFromSymbol(symbol: String): Sec = {
    Exchange.secOf(symbol).get
  }

  def getSymbolFromSecurity(sec: Sec) = {
    sec.uniSymbol
  }

  def allowedSides = Array(
    OrderSide.Buy, OrderSide.Sell
  )

  def allowedTypes = Array(
    OrderType.Limit, OrderType.Market
  )
  
  def allowedValidity = Array(
    OrderValidity.Day
  )

  def allowedRoutes = Array[OrderRoute]()

  def orderExecutors = pendingExecutors synchronized {pendingExecutors.toArray}
  
  @throws(classOf[BrokerException])
  def prepareOrder(order: Order): OrderExecutor = {
    val executor = new PaperOrderExecutor(order)
    
    // listenTo sec's executions here
    
    publish(OrderDeltasEvent(this, Array(OrderDelta.Added(executor))))

    executor
  }

  /**
   * call me to execute the orders
   */
  def processTrade(sec: Sec, execution: Execution) {
    val deltas = new ArrayList[OrderDelta]()

    val executors = pendingExecutors synchronized {
      pendingExecutors.toArray
    }
    
    val executorsToRemove = new ArrayList[OrderExecutor]()
    var i = 0
    while (i < executors.length) {
      val executor = executors(i)
      val order = executor.order
      if (order.sec == sec) {
        val newStatus = order.status match {
          case OrderStatus.PendingNew | OrderStatus.Partial =>
            order.tpe match {
              case OrderType.Market =>
                deltas += OrderDelta.Updated(executor)
                order.fill(execution.time, execution.volume, execution.price)
                
              case OrderType.Limit =>
                order.side match {
                  case OrderSide.Buy if execution.price <= order.price =>
                    deltas += OrderDelta.Updated(executor)
                    order.fill(execution.time, execution.volume, execution.price)
                    
                  case OrderSide.Sell if execution.price >= order.price => 
                    deltas += new OrderDelta.Updated(executor)
                    order.fill(execution.time, execution.volume, execution.price)

                  case _ => order.status
                }
                
              case _ => order.status
            }
          
          case _ => order.status
        }
        
        if (newStatus == OrderStatus.Filled) {
          executorsToRemove += executor
        }
      }
      i += 1
    }
    
    if (executorsToRemove.length > 0) {
      pendingExecutors synchronized {pendingExecutors --= executorsToRemove}
    }

    if (deltas.length > 0) {
      publish(OrderDeltasEvent(this, deltas.toArray))
    }
  }



  def accounts: Array[Account] = {
    Array[Account]() // @todo get from db
  }
  
  private class PaperOrderExecutor(_order: Order) extends OrderExecutor(this, _order) {

    @throws(classOf[BrokerException])
    override 
    def cancel {
      pendingExecutors synchronized {pendingExecutors -= this}
      
      order.status = OrderStatus.Canceled

      log.info("Order Cancelled: %s".format(order))
        
      broker.publish(OrderDeltasEvent(broker, Array(OrderDelta.Updated(this))))
    }

    @throws(classOf[BrokerException])
    override
    def submit {
      pendingExecutors synchronized {pendingExecutors += this}

      order.id = (idFormatter.format(new Date())).toLong
      order.status = OrderStatus.PendingNew

      log.info("Order Submitted: %s".format(order))

      broker.publish(OrderDeltasEvent(broker, Array(OrderDelta.Updated(this))))
    }
  }
}
