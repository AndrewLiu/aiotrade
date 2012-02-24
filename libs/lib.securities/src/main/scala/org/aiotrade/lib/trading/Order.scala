package org.aiotrade.lib.trading

import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher


class Order protected (val account: Account, val tpe: OrderType, val side: OrderSide, val sec: Sec, val quantity: Double, val price: Double, var route: OrderRoute) extends Publisher {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private var _id: Long = _
  
  private var _time: Long = System.currentTimeMillis
  private var _expireTime: Long = Long.MinValue
  private var _stopPrice: Double = Double.NaN
  private var _validity: OrderValidity = _
  private var _reference: String = _
  
  // --- executing related
  private var _status: OrderStatus = OrderStatus.New
  private var _filledQuantity: Double = _
  private var _averagePrice: Double = _
  private var _message: String = _
  private var _transactions = new ArrayList[Transaction]()
  
  def id = _id
  def id_=(id: Long) {
    this._id = id
  }

  def time = _time
  def time_=(time: Long) {
    this._time = time
  }
  
  def stopPrice = _stopPrice
  def stopPrice_=(stopPrice: Double) {
    this._stopPrice = stopPrice
  }
  
  def validity = _validity
  def validity_=(validity: OrderValidity) {
    this._validity = validity
  }

  def expireTime = _expireTime
  def expireTime_=(time: Long) {
    this._expireTime = time
  }

  def reference = _reference
  def reference_=(reference: String) {
    this._reference = reference
  }

  // --- executing related
  
  def status = _status
  def status_=(status: OrderStatus) {
    val oldValue = _status
    if (_status != status) {
      _status = status
      publish(OrderEvent.StatusChanged(this, oldValue, status))
      if (status == OrderStatus.Filled) {
        publish(OrderEvent.Completed(this))
      }
    }
  }

  def filledQuantity = _filledQuantity
  def filledQuantity_=(filledQuantity: Double) {
    val oldValue = _filledQuantity
    if (filledQuantity != Long.MinValue && filledQuantity != _filledQuantity) {
      _filledQuantity = filledQuantity
      publish(OrderEvent.FilledQuantityChanged(this, oldValue, filledQuantity))
    }
  }
  
  def averagePrice = _averagePrice
  def averagePrice_=(averagePrice: Double) {
    val oldValue = _averagePrice
    if (averagePrice != Double.NaN && averagePrice != _averagePrice) {
      _averagePrice = averagePrice
      publish(OrderEvent.AveragePriceChanged(this, oldValue, averagePrice))
    }
  }

  def message = _message
  def message_=(message: String) {
    _message = message
  }
  
  def transactions = _transactions.toArray
  def addTransaction(transaction: Transaction) {
    _transactions += transaction
  }
  
  def fill(size: Double, price: Double): OrderStatus =  {
    var totalPrice = filledQuantity * averagePrice
    val remainQuantity = quantity - filledQuantity

    val executedQuantity = if (size > remainQuantity) {
      // should only happen in case of paper work
      remainQuantity
    } else {
      size
    }
    
    filledQuantity += executedQuantity
    totalPrice += executedQuantity * price
    averagePrice = totalPrice / filledQuantity

    if (executedQuantity > 0) {
      side match {
        case OrderSide.Buy | OrderSide.BuyCover =>
          addTransaction(new SecurityTransaction(sec, executedQuantity, price))
        case OrderSide.Sell | OrderSide.SellShort =>
          addTransaction(new SecurityTransaction(sec, -executedQuantity, price))
        case _ =>
      }
    }

    log.info("Order Filled: %s".format(this))

    if (filledQuantity == quantity) {
      status = OrderStatus.Filled
      account.processCompletedOrder(this)
    } else {
      status = OrderStatus.Partial
    }
    
    status
  }
  
  override
  def toString = {
    val sb = new StringBuilder()
    sb.append("Order: time=" + time)
    sb.append(", sec=" + sec.uniSymbol)
    sb.append(", tpe=" + tpe)
    sb.append(", side=" + side)
    sb.append(", quantity=" + quantity)
    sb.append(", price=" + price)
    sb.append(", stopPrice=" + stopPrice)
    sb.append(", timeInForce=" + validity)
    sb.append(", expiration=" + expireTime)
    sb.append(", reference=" + reference)
    sb.toString
  }
}

object Order {
  def apply(account: Account, tpe: OrderType, side: OrderSide, sec: Sec, quantity: Double, price: Double, route: OrderRoute) = {
    new Order(account, tpe, side, sec, quantity, price, route)
  }
  
  def apply(account: Account, tpe: OrderType, side: OrderSide, sec: Sec, quantity: Double, price: Double) = {
    new Order(account, tpe, side, sec, quantity, price, null)
  }

  def apply(account: Account, tpe: OrderType, side: OrderSide, sec: Sec, quantity: Double) = {
    new Order(account, OrderType.Market, side, sec, quantity, Double.NaN, null)
  }
}

trait OrderRoute {
  def id: String
  def name: String
}

abstract class OrderSide(val name: String)
object OrderSide {
  case object Buy extends OrderSide("Buy")
  case object Sell extends OrderSide("Sell")
  case object BuyCover extends OrderSide("BuyCover")
  case object SellShort extends OrderSide("SellShort")
}

abstract class OrderType(val name: String)
object OrderType {
  case object Market extends OrderType("Market")
  case object Limit extends OrderType("Limit")
  case object Stop extends OrderType("Stop")
  case object StopLimit extends OrderType("StopLimit")
}

abstract class OrderValidity(val name: String)
object OrderValidity {
  case object Day extends OrderValidity("Day")
  case object ImmediateOrCancel extends OrderValidity("ImmediateOrCancel")
  case object AtOpening extends OrderValidity("AtOpening")
  case object AtClosing extends OrderValidity("AtClosing")
  case object GoodTillCancel extends OrderValidity("GoodTillCancel")
  case object GoodTillDate extends OrderValidity("GoodTillDate")
}

abstract class OrderStatus(val name: String)
object OrderStatus {
  case object New extends OrderStatus("New")
  case object PendingNew extends OrderStatus("PendingNew")
  case object Partial extends OrderStatus("Partial")
  case object Filled extends OrderStatus("Filled")
  case object Canceled extends OrderStatus("Canceled")
  case object Rejected extends OrderStatus("Rejected")
  case object PendingCancel extends OrderStatus("PendingCancel")
  case object Expired extends OrderStatus("Expired")
}

trait OrderEvent {
  def order: Order
}
object OrderEvent {
  case class Completed(order: Order) extends OrderEvent
  case class IdChanged(order: Order, oldValue: String, value: String) extends OrderEvent
  case class StatusChanged(order: Order, oldValue: OrderStatus, value: OrderStatus) extends OrderEvent
  case class FilledQuantityChanged(order: Order, oldValue: Double, value: Double) extends OrderEvent
  case class AveragePriceChanged(order: Order, oldValue: Double, value: Double) extends OrderEvent 
}
