package org.aiotrade.lib.trading.backtest

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Logger
import org.aiotrade.lib.math.indicator.SignalIndicator
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.signal.SignalEvent
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Execution
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.trading.Account
import org.aiotrade.lib.trading.Broker
import org.aiotrade.lib.trading.Order
import org.aiotrade.lib.trading.OrderSide
import org.aiotrade.lib.trading.PaperBroker
import org.aiotrade.lib.trading.Position
import org.aiotrade.lib.trading.SecPicking
import org.aiotrade.lib.trading.SecPickingEvent
import org.aiotrade.lib.trading.ShanghaiExpenseScheme
import org.aiotrade.lib.trading.TradingRule
import org.aiotrade.lib.util.ValidTime
import org.aiotrade.lib.util.actors.Publisher
import scala.collection.mutable
import scala.concurrent.SyncVar

case class Trigger(sec: Sec, position: Position, time: Long, side: Side)

/**
 * 
 * @author Caoyuan Deng
 */
class TradingService(broker: Broker, account: Account, param: Param, tradingRule: TradingRule, referSer: QuoteSer, 
                     secPicking: SecPicking, signalIndTemplates: SignalIndicator*
) extends Publisher {
  protected val log = Logger.getLogger(this.getClass.getName)
  
  private case class Go(fromTime: Long, toTime: Long)
  private val done = new SyncVar[Boolean]()
  
  protected val timestamps = referSer.timestamps.clone
  protected val freq = referSer.freq

  protected val signalIndicators = new mutable.HashSet[SignalIndicator]()
  protected val triggers = new mutable.HashSet[Trigger]()
  protected var pendingOrders = List[OrderCompose]()
  protected var buyingOrders = List[Order]()
  protected var sellingOrders = List[Order]()
  
  protected var fromTime: Long = _
  protected var toTime: Long = _
  protected var fromIdx: Int = _
  protected var toIdx: Int = _
  protected var initialReferPrice: Double = _

  /** current closed refer idx */
  protected var closeReferIdx = 0
  /** current closed refer time */
  protected def closeTime = timestamps(closeReferIdx)

  reactions += {
    case SecPickingEvent(secValidTime, side) =>
      val position = positionOf(secValidTime.ref).getOrElse(null)
      side match {
        case Side.ExitPicking if position == null =>
        case _ => triggers += Trigger(secValidTime.ref, position, secValidTime.validFrom, side)
      }
    
    case signalEvt@SignalEvent(ind, signal) if signalIndicators.contains(ind) && signal.isSign =>
      val sec = signalEvt.source.baseSer.serProvider.asInstanceOf[Sec]
      log.info("Got signal: sec=%s, signal=%s".format(sec.uniSymbol, signal))
      val time = signalEvt.signal.time
      val side = signalEvt.signal.kind.asInstanceOf[Side]
      val position = positionOf(sec).getOrElse(null)
      side match {
        case (Side.ExitLong | Side.ExitShort | Side.ExitPicking | Side.CutLoss | Side.TakeProfit) if position == null =>
        case _ => triggers += Trigger(sec, position, time, side)
      }
      
    case Go(fromTime, toTime) =>
      doGo(fromTime, toTime)

    case _ =>
  }

  private def initSignalIndicators {
    val t0 = System.currentTimeMillis
    
    if (signalIndTemplates.nonEmpty) {
      listenTo(Signal)
    
      for {
        indTemplate <- signalIndTemplates
        indClass = indTemplate.getClass
        indFactor = indTemplate.factors
        
        sec <- secPicking.allSecs
        ser <- sec.serOf(freq)
      } {
        // for each sec, need a new instance of indicator
        val ind = indClass.newInstance.asInstanceOf[SignalIndicator]
        // @Note should add to signalIndicators before compute, otherwise, the published signal may be dropped in reactions 
        signalIndicators += ind 
        ind.factors = indFactor
        ind.set(ser)
        ind.computeFrom(0)
      }
    }
    
    log.info("Inited singals in %ss.".format((System.currentTimeMillis - t0) / 1000))
  }
  
  protected def positionOf(sec: Sec): Option[Position] = account.positions.get(sec)
  
  protected def buy(sec: Sec): OrderCompose = {
    val order = new OrderCompose(sec, OrderSide.Buy, closeReferIdx)
    pendingOrders ::= order
    order
  }

  protected def sell(sec: Sec): OrderCompose = {
    val order = new OrderCompose(sec, OrderSide.Sell, closeReferIdx)
    pendingOrders ::= order
    order
  }
  
  /**
   * Main entrance for outside caller.
   * 
   * @Note we use publish(Go) to make sure doGo(...) happens only after all signals 
   *       were published (during initSignalIndicators).
   */ 
  def go(fromTime: Long, toTime: Long) {
    initSignalIndicators
    publish(Go(fromTime, toTime))
    // We should make this calling synchronized, so block here untill done
    done.get
  }
  
  private def doGo(fromTime: Long, toTime: Long) {
    this.fromIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
    this.toIdx = timestamps.indexOfNearestOccurredTimeBefore(toTime)
    this.fromTime = timestamps(fromIdx)
    this.toTime = timestamps(toIdx)
    this.initialReferPrice = referSer.open(fromIdx)
    
    var i = fromIdx
    while (i <= toIdx) {
      closeReferIdx = i
      executeOrders
      updatePositionsPrice
      
      log.info("%1$tY.%1$tm.%1$td: bought=%2$s, sold=%3$s, balance=%4$.2f, equity=%5$.2f, positions=%6$s".format(
          new Date(closeTime), buyingOrders.size, sellingOrders.size, account.balance, account.equity, account.positions.values.size)
      )
      
      // @todo process unfilled orders

      param.publish(ReportData(account.description, 0, closeTime, account.equity / account.initialEquity * 100))
      param.publish(ReportData("Refer", 0, closeTime, referSer.close(i) / initialReferPrice * 100 - 100))

      // -- todays ordered processed, now begin to check new conditions and 
      //    prepare new orders according to today's close status.
      
      secPicking.go(closeTime)
      checkStopCondition
      at(i)
      processPendingOrders


      i += 1
    }
    
    // release resources. @Todo any better way? We cannot guarrantee that only backtesing is using Function.idToFunctions
    deafTo(Signal)
    done.set(true)
    org.aiotrade.lib.math.indicator.Function.releaseAll
  }
  
  /**
   * At close of period idx, define the trading action for next period. 
   * @Note Override this method for your action.
   * @param idx: index of closed/passed period, this period was just closed/passed.
   */
  def at(idx: Int) {
    val triggers = scanTriggers(idx)
    for (Trigger(sec, position, triggerTime, side) <- triggers) {
      side match {
        case Side.EnterLong =>
          buy (sec) after (1)
        case Side.ExitLong =>
          sell (sec) after (1)
        case Side.EnterShort =>
        case Side.ExitShort =>
        case Side.CutLoss => 
          sell (sec) quantity (position.quantity) after (1)
        case Side.TakeProfit =>
          sell (sec) quantity (position.quantity) after (1)
        case _ =>
      }
    }
  }
  
  protected def checkStopCondition {
    for ((sec, position) <- account.positions) {
      if (tradingRule.cutLossRule(position)) {
        triggers += Trigger(sec, position, closeTime, Side.CutLoss)
      }
      if (tradingRule.takeProfitRule(position)) {
        triggers += Trigger(sec, position, closeTime, Side.TakeProfit)
      }
    }
  }

  private def updatePositionsPrice {
    for ((sec, position) <- account.positions; 
         ser <- sec.serOf(freq); 
         idx = ser.indexOfOccurredTime(closeTime) if idx >= 0
    ) {
      position.update(ser.close(idx))
    }
  }

  private def processPendingOrders: Unit = {
    val orderSubmitReferIdx = closeReferIdx + 1 // next trading day
    if (orderSubmitReferIdx < timestamps.length) {
      val orderSubmitReferTime = timestamps(orderSubmitReferIdx)
      var expired = List[OrderCompose]()
      var buying = new mutable.HashMap[Sec, OrderCompose]()
      var selling = new mutable.HashMap[Sec, OrderCompose]()
      for (order <- pendingOrders) {
        if (order.referIndex < orderSubmitReferIdx) {
          expired ::= order
        } else if (order.referIndex == orderSubmitReferIdx) { 
          if (order.ser.exists(orderSubmitReferTime)) {
            order.side match {
              case OrderSide.Buy => buying(order.sec) = order
              case OrderSide.Sell => selling(order.sec) = order
              case _ =>
            }
          } else {
            order.side match {
              case OrderSide.Buy => // @todo pending after n days?
              case OrderSide.Sell => order after (1) // pending 1 day
              case _ =>
            }
          }
        }
      }

      val conflicts = for (sec <- buying.keysIterator if selling.contains(sec)) yield sec
      val buyingx = (buying -- conflicts).values.toList
      val sellingx = (selling -- conflicts).values.toList

      val estimateFundPerSec = account.balance / buyingx.size 
      buyingOrders = buyingx flatMap {_ fund (estimateFundPerSec) toOrder}
      adjustBuyingOrders(buyingOrders)
      sellingOrders = sellingx flatMap {_ toOrder}
    
      // @todo process unfilled orders from broker
      pendingOrders --= expired
      pendingOrders --= buyingx
      pendingOrders --= sellingx
    }
  }
  
  /** 
   * Adjust orders for expenses etc, by reducing quantities (or number of orders @todo)
   */
  private def adjustBuyingOrders(buyingOrders: List[Order]) {
    var orders = buyingOrders.sortBy(_.price)
    var amount = 0.0
    while ({amount = calcTotalBuyingFund(buyingOrders); amount > account.balance}) {
      orders match {
        case order :: tail =>
          order.quantity -= tradingRule.quantityPerLot
          orders = tail
        case Nil => 
          orders = buyingOrders
      }
    }
  }
  
  private def calcTotalBuyingFund(orders: List[Order]) = {
    orders.foldLeft(0.0){(s, x) => s + x.quantity * x.price + account.expenseScheme.getBuyExpenses(x.quantity, x.price)}
  }
  
  private def executeOrders {
    // sell first?. If so, how about the returning funds?
    buyingOrders map broker.prepareOrder foreach {orderExecutor => 
      orderExecutor.submit
      pseudoProcessTrade(orderExecutor.order)
    }

    sellingOrders map broker.prepareOrder foreach {orderExecutor => 
      orderExecutor.submit
      pseudoProcessTrade(orderExecutor.order)
    }    
  }
  
  private def pseudoProcessTrade(order: Order) {
    val execution = new Execution
    execution.sec = order.sec
    execution.time = closeTime
    execution.price = order.price
    execution.volume = order.quantity
    broker.processTrade(execution)
  }
  
  protected def scanTriggers(fromIdx: Int, toIdx: Int = -1): mutable.HashSet[Trigger] = {
    val toIdx1 = if (toIdx == -1) fromIdx else toIdx
    scanTriggers(timestamps(math.max(fromIdx, 0)), timestamps(math.max(toIdx1, 0)))
  }
  
  protected def scanTriggers(fromTime: Long, toTime: Long): mutable.HashSet[Trigger] = {
    triggers filter {x => 
      x.time >= fromTime && x.time <= toTime && secPicking.isValid(x.sec, toTime)
    }
  }
  
  class OrderCompose(val sec: Sec, val side: OrderSide, referIdxAtDecision: Int) {
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
        
    /** on t + idx */
    def after(i: Int) = {
      _afterIdx += i
      this
    }
    
    def referIndex = referIdxAtDecision + _afterIdx

    def toOrder: Option[Order] = {
      val time = timestamps(referIndex)
      ser.valueOf(time) match {
        case Some(quote) =>
          side match {
            case OrderSide.Buy =>
              if (_price.isNaN) {
                _price = tradingRule.buyPriceRule(quote)
              }
              if (_quantity.isNaN) {
                _quantity = tradingRule.buyQuantityRule(quote, _price, _fund)
              }
            case OrderSide.Sell =>
              if (_price.isNaN) {
                _price = tradingRule.sellPriceRule(quote)
              }
              if (_quantity.isNaN) {
                _quantity = tradingRule.sellQuantityRule(quote, _price, _fund)
              }
            case _ =>
          }

          if (_quantity > 0) {
            val order = new Order(account, sec, _quantity, _price, side)
            order.time = time
            Some(order)
          } else {
            None
          }
          
        case None => None
      }
    }
  }

}

object TradingService {
  
  def createIndicator[T <: SignalIndicator](signalClass: Class[T], factors: Array[Double]): T = {
    val ind = signalClass.newInstance.asInstanceOf[T]
    ind.factorValues = factors
    ind
  }
  
  private def init = {
    val category = "008011"
    val CSI300Code = "399300.SZ"
    val secs = securities.getSecsOfSector(category, CSI300Code)
    val referSec = Exchange.secOf("000001.SS").get
    val referSer = securities.loadSers(secs, referSec, TFreq.DAILY)
    val goodSecs = secs filter {_.serOf(TFreq.DAILY).get.size > 0}
    println("Number of good secs: " + goodSecs.length)
    (goodSecs, referSer)
  }

  /**
   * Simple test
   */
  def main(args: Array[String]) {
    import org.aiotrade.lib.indicator.basic.signal._

    case class TestParam(faster: Int, slow: Int, signal: Int) extends Param {
      override def shortDescription = List(faster, slow, signal).mkString("_")
    }
    
    val df = new SimpleDateFormat("yyyy.MM.dd")
    val fromTime = df.parse("2011.04.03").getTime
    val toTime = df.parse("2012.04.03").getTime
    
    val imageFileDir = System.getProperty("user.home") + File.separator + "backtest"
    val chartReport = new ChartReport(imageFileDir)
    
    val (secs, referSer) = init
    
    val secPicking = new SecPicking()
    secPicking ++= secs map (ValidTime(_, 0, 0))
    
    for {
      fasterPeriod <- List(5, 8, 12)
      slowPeriod <- List(26, 30, 55) if slowPeriod > fasterPeriod
      signalPeriod <- List(5, 9)
      param = TestParam(fasterPeriod, slowPeriod, signalPeriod)
    } {
      val broker = new PaperBroker("Backtest")
      val account = new Account("Backtest", 10000000.0, ShanghaiExpenseScheme(0.0008))
    
      val tradingRule = new TradingRule()
      val indTemplate = createIndicator(classOf[MACDSignal], Array(fasterPeriod, slowPeriod, signalPeriod))
    
      val tradingService = new TradingService(broker, account, param, tradingRule, referSer, secPicking, indTemplate) {
        override 
        def at(idx: Int) {
          val triggers = scanTriggers(idx)
          for (Trigger(sec, position, triggerTime, side) <- triggers) {
            side match {
              case Side.EnterLong =>
                buy (sec) after (1)
              
              case Side.ExitLong =>
                sell (sec) after (1)
              
              case Side.CutLoss => 
                sell (sec) quantity (position.quantity) after (1)
              
              case Side.TakeProfit =>
                sell (sec) quantity (position.quantity) after (1)
              
              case _ =>
            }
          }
        }
      }
    
      chartReport.roundStarted(List(param))
      tradingService.go(fromTime, toTime)
      chartReport.roundFinished
      System.gc
    }
    
    println("Done!")
  }
}
