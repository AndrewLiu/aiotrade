package org.aiotrade.lib.trading

import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.logging.Logger
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Execution
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable

class PaperBroker(val name: String) extends Broker {
  private val log = Logger.getLogger(getClass.getName)
  private val orderIdFormatter = new SimpleDateFormat("yyMMddHHmmssSSS")
  
  val id: Long = UUID.randomUUID.getMostSignificantBits
  
  /** immutable constant */
  private val EMPTY_EXECUTORS = new mutable.HashSet[OrderExecutor]()
  
  private val pendingSecToExecutors = new mutable.HashMap[Sec, mutable.HashSet[OrderExecutor]]()

  val allowedSides = List(
    OrderSide.Buy, 
    OrderSide.Sell
  )

  val allowedTypes = List(
    OrderType.Limit, 
    OrderType.Market
  )
  
  val allowedValidity = List(
    OrderValidity.Day
  )

  val allowedRoutes = List[OrderRoute]()

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

  def orderExecutors = pendingSecToExecutors
  
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
  override 
  def processTrade(execution: Execution) {
    var deltas = List[OrderDelta]()

    val sec = execution.sec
    val executors = pendingSecToExecutors synchronized {pendingSecToExecutors.getOrElse(sec, EMPTY_EXECUTORS)}
    
    var executorsToRemove = List[OrderExecutor]()
    for (executor <- executors) {
      val order = executor.order
      order.status match {
        case OrderStatus.PendingNew | OrderStatus.Partial =>
          order.tpe match {
            
            case OrderType.Market =>
              deltas ::= OrderDelta.Updated(executor)
              order.fill(execution.time, execution.price, execution.volume)
                
            case OrderType.Limit =>
              order.side match {
                case OrderSide.Buy if execution.price <= order.price =>
                  deltas ::= OrderDelta.Updated(executor)
                  order.fill(execution.time, execution.price, execution.volume)
                    
                case OrderSide.Sell if execution.price >= order.price => 
                  deltas ::= new OrderDelta.Updated(executor)
                  order.fill(execution.time, execution.price, execution.volume)

                case _ =>
              }
                
            case _ =>
          }
          
        case _ =>
      }
        
      if (order.status == OrderStatus.Filled) {
        executorsToRemove ::= executor
      }
    }
    
    if (executorsToRemove.length > 0) {
      pendingSecToExecutors synchronized {
        executors --= executorsToRemove
        if (executors.isEmpty) {
          pendingSecToExecutors -= sec
        } else {
          pendingSecToExecutors(sec) = executors
        }
      }
    }

    if (deltas.length > 0) {
      publish(OrderDeltasEvent(this, deltas))
    }
  }

  def accounts: Array[Account] = {
    Array[Account]() // @todo get from db
  }
  
  private class PaperOrderExecutor(_order: => Order) extends OrderExecutor(this, _order) {
    
    @throws(classOf[BrokerException])
    override 
    def cancel {
      pendingSecToExecutors synchronized {
        val executors = pendingSecToExecutors.getOrElse(order.sec, EMPTY_EXECUTORS)
        executors -= this
        if (executors.isEmpty) {
          pendingSecToExecutors -= order.sec
        } else {
          pendingSecToExecutors(order.sec) = executors
        }
      }
      
      order.status = OrderStatus.Canceled
      
      log.info("Order Cancelled: %s".format(order))
        
      broker.publish(OrderDeltasEvent(broker, Array(OrderDelta.Updated(this))))
    }

    @throws(classOf[BrokerException])
    override
    def submit {
      pendingSecToExecutors synchronized {
        val executors = pendingSecToExecutors.getOrElse(order.sec, new mutable.HashSet[OrderExecutor]())
        executors += this
        pendingSecToExecutors(order.sec) = executors
      }

      order.id = orderIdFormatter.format(new Date()).toLong
      order.status = OrderStatus.PendingNew

      log.info("Order Submitted: %s".format(order))

      broker.publish(OrderDeltasEvent(broker, Array(OrderDelta.Updated(this))))
    }
  }
}
