package org.aiotrade.lib.backtest

import java.text.SimpleDateFormat
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.math.signal.Sign
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.signal.SignalEvent
import org.aiotrade.lib.math.signal.SignalX
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Execution
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.trading.Account
import org.aiotrade.lib.trading.Order
import org.aiotrade.lib.trading.OrderSide
import org.aiotrade.lib.trading.PaperBroker
import org.aiotrade.lib.trading.ShanghaiExpenseScheme
import scala.collection.mutable

case class Trigger(sec: Sec, qouteSer: QuoteSer, time: Long, side: Side)
case class TradeTime(time: Long, index: Int, referIndex: Int)

/**
 * 
 * @author Caoyuan Deng
 */
class TradingService(account: Account, tradeRule: TradeRule, referSer: QuoteSer, secPicking: SecPicking) {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val timestamps = referSer.timestamps.clone
  private val freq = referSer.freq
  private val broker = new PaperBroker("Backtest")
  
  private val pendingOrders = new ArrayList[OrderCompose]()
  
  private var currentReferIdx = -1
  
  class OrderCompose(val sec: Sec, val side: OrderSide, decisionReferIdx: Int) {
    val ser = sec.serOf(freq).get
    private var _price = Double.NaN
    private var _fund = Double.NaN
    private var _quantity = Double.NaN
    private var _afterIdx = 0

    def price(price: Double) = {
      _price = price
      this
    }

    def fund(fund: Double) = {
      _fund = fund
      this
    }
    
    def quantity(quantity: Double) = {
      _quantity = quantity
      this
    }
        
    def after(idx: Int) = {
      _afterIdx = idx
      this
    }
    
    def referIndex = decisionReferIdx + _afterIdx

    def toOrder() = {
      val time = timestamps(referIndex)
      val idx = ser.indexOfOccurredTime(time)
      side match {
        case OrderSide.Buy =>
          if (_price.isNaN) {
            _price = tradeRule.buyPriceRule(ser.open(idx), ser.high(idx), ser.low(idx), ser.close(idx))
          }
          if (_quantity.isNaN) {
            _quantity += tradeRule.buyQuantityRule(ser.volume(idx), _price, _fund)
          }
        case OrderSide.Sell =>
          if (_price.isNaN) {
            _price = tradeRule.sellPriceRule(ser.open(idx), ser.high(idx), ser.low(idx), ser.close(idx))
          }
          if (_quantity.isNaN) {
            _quantity -= tradeRule.sellQuantityRule(ser.volume(idx), _price, _quantity)
          }
        case _ =>
      }
        
      new Order(account, sec, _quantity, _price, side)
    }
  }
  
  private val triggers = new ArrayList[Trigger]()
  
  def signalGot(signal: SignalEvent) {
    triggers += toTrigger(signal)
  }
  
  private def toTrigger(signal: SignalEvent) = {
    val sec = signal.source.baseSer.serProvider.asInstanceOf[Sec]
    val ser = sec.serOf(freq).get
    val time = signal.signal.time
    val side = signal.signal.kind.asInstanceOf[Side]
    Trigger(sec, ser, time, side)
  }
  
  def go(fromTime: Long, toTime: Long) {
    val fromIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
    val toIdx = timestamps.indexOfNearestOccurredTimeBefore(toTime)
    var i = fromIdx
    while (i <= toIdx) {
      currentReferIdx = i
      at(i, timestamps(i))
      processPendingOrders
      i += 1
    }
  }
  
  /**
   * @param idx: index of timestamps
   * @param time: current time
   */
  def at(idx: Int, time: Long) {
    val triggers = scanTriggers(idx - 1)
    for (Trigger(sec, qouteSer, triggerTime, side) <- triggers) {
      side match {
        case Side.EnterLong =>
          buy (sec)
        case Side.ExitLong =>
          sell (sec)
        case Side.EnterShort =>
        case Side.ExitShort =>
        case _ =>
      }
    }
  }
  
  def buy(sec: Sec): OrderCompose = {
    val order = new OrderCompose(sec, OrderSide.Buy, currentReferIdx)
    pendingOrders += order
    order
  }

  def sell(sec: Sec): OrderCompose = {
    val order = new OrderCompose(sec, OrderSide.Sell, currentReferIdx)
    pendingOrders += order
    order
  }
  
  private def processPendingOrders {
    val time = timestamps(currentReferIdx)
    var toRemove = List[OrderCompose]()
    var currentBuy = List[OrderCompose]()
    var currentSell = List[OrderCompose]()
    var i = 0
    while (i < pendingOrders.length) {
      val order = pendingOrders(i)
      if (order.referIndex < currentReferIdx) {
        toRemove ::= order
      } else if (order.referIndex == currentReferIdx) {
        if (order.ser.exists(time)) {
          order.side match {
            case OrderSide.Buy => currentBuy ::= order
            case OrderSide.Sell => currentSell ::= order
            case _ =>
          }
        } else {
          order.side match {
            case OrderSide.Buy => // @todo pending after n days?
            case OrderSide.Sell => order after (1)
            case _ =>
          }
        }
      }
      i += 1
    }
    
    val fundPerSec = account.balance / currentBuy.length
    val buyOrders = currentBuy map {x => x fund (fundPerSec) toOrder}
    val sellOrders = currentSell map {x => x toOrder}
    
    // sell first. How about the returning funds?
    sellOrders map broker.prepareOrder foreach {orderExecutor => 
      orderExecutor.submit
      pseudoExecute(orderExecutor.order)
    }
    
    buyOrders map broker.prepareOrder foreach {orderExecutor => 
      orderExecutor.submit
      pseudoExecute(orderExecutor.order)
    }

    // @todo process unfilled orders from broker
    pendingOrders synchronized {
      pendingOrders --= toRemove
    }
  }
  
  private def pseudoExecute(order: Order) {
    val execution = new Execution
    execution.time = order.time
    execution.price = order.price
    execution.volume = order.quantity
    broker.processTrade(order.sec, execution)
  }
  
  protected def scanTriggers(fromIdx: Int, toIdx: Int = -1): List[Trigger] = {
    val toIdx1 = if (toIdx == -1) fromIdx else toIdx
    scanTriggers(timestamps(math.max(fromIdx, 0)), timestamps(math.max(toIdx1, 0)))
  }
  
  protected def scanTriggers(fromTime: Long, toTime: Long): List[Trigger] = {
    var result = List[Trigger]()
    var i = 0
    while (i < triggers.length) {
      val trigger = triggers(i)
      if (trigger.time >= fromTime && trigger.time <= toTime && secPicking.isValid(trigger.sec, toTime)) {
        result ::= trigger
      }
      i += 1
    }
    result
  }
}

object TradingService {
  
  private val df = new SimpleDateFormat("yyyy.MM.dd")
  
  def main(args: Array[String]) {
    val referSec = Exchange.secOf("000001.SS").get
    val referSer = referSec.serOf(TFreq.DAILY).get
    referSec.loadSerFromPersistence(referSer, false)
    
    val secPicking = new SecPicking
    
    val account = new Account("Backtest", 10000000.0, ShanghaiExpenseScheme(0.0008))
    val tradeRule = new TradeRule
    
    val tradingService = new TradingService(account, tradeRule, referSer, secPicking) {
      override 
      def at(idx: Int, time: Long) {
        for (Trigger(sec, qouteSer, triggerTime, side) <- scanTriggers(idx - 1)) {
          side match {
            case Side.EnterLong =>
              buy (sec) fund (1000.0) after(1)
            case Side.ExitLong =>
              sell (sec)
            case Side.EnterShort =>
            case Side.ExitShort =>
            case _ =>
          }
        }
      }
    }
    
    val fromTime = df.parse("2011.4.3").getTime
    val toTime = df.parse("2012.4.3").getTime
    tradingService.go(fromTime, toTime)
  }
}
