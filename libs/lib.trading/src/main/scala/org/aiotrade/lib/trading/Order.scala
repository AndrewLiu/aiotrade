package org.aiotrade.lib.trading

import java.util.Date
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher


class Order(val account: Account, val sec: Sec, var quantity: Double, var price: Double, val side: OrderSide, val tpe: OrderType = OrderType.Market, var route: OrderRoute = null) extends Publisher {
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
  private var _transactions = new ArrayList[SecurityTransaction]()
  
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
    if (_status != status) {
      val oldValue = _status
      _status = status
      publish(OrderEvent.StatusChanged(this, oldValue, status))
      if (status == OrderStatus.Filled) {
        publish(OrderEvent.Completed(this))
      }
    }
  }

  def remainQuantity = quantity - _filledQuantity
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
  
  def fill(time: Long, price: Double, size: Double) {
    val executedQuantity = math.min(size, remainQuantity)
    if (executedQuantity > 0) {
      var oldTotalAmount = _filledQuantity * _averagePrice
      _filledQuantity += executedQuantity
      _averagePrice = (oldTotalAmount + executedQuantity * price) / _filledQuantity

      side match {
        case OrderSide.Buy | OrderSide.BuyCover =>
          _transactions += SecurityTransaction(time, sec,  executedQuantity, price * account.tradingRule.multiplier * account.tradingRule.marginRate)
        case OrderSide.Sell | OrderSide.SellShort =>
          _transactions += SecurityTransaction(time, sec, -executedQuantity, price * account.tradingRule.multiplier * account.tradingRule.marginRate)
        case _ =>
      }

      if (remainQuantity == 0) {
        status = OrderStatus.Filled
      } else { 
        status = OrderStatus.Partial
      }

      log.info("Order Filled: %s".format(this))

      account.processFillingOrder(time, this)
    }
  }
  
  override
  def toString = {
    "Order: time=%1$tY.%1$tm.%1$td, sec=%2$s, tpe=%3$s, side=%4$s, quantity(filled)=%5$d(%6$d), price=%7$ 5.2f, status=%8$s, stopPrice=%9$ 5.2f, validity=%10$s, expiration=%11$s, refrence=%12$s".format(
      new Date(time), sec.uniSymbol, tpe, side, quantity.toInt, _filledQuantity.toInt, price, status, stopPrice, validity, expireTime, reference
    )
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
  case object SellShort extends OrderSide("SellShort")
  case object BuyCover extends OrderSide("BuyCover")
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
