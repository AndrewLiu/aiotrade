package org.aiotrade.lib.trading.backtest

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.indicator.SignalIndicator
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.signal.SignalEvent
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.trading.Account
import org.aiotrade.lib.trading.Benchmark
import org.aiotrade.lib.trading.Broker
import org.aiotrade.lib.trading.Order
import org.aiotrade.lib.trading.OrderSide
import org.aiotrade.lib.trading.OrderStatus
import org.aiotrade.lib.trading.PaperBroker
import org.aiotrade.lib.trading.Param
import org.aiotrade.lib.trading.Position
import org.aiotrade.lib.trading.SecPicking
import org.aiotrade.lib.trading.SecPickingEvent
import org.aiotrade.lib.trading.StockAccount
import org.aiotrade.lib.trading.TradingRule
import org.aiotrade.lib.util.ValidTime
import scala.collection.mutable
import scala.concurrent.SyncVar

case class Trigger(sec: Sec, position: Position, time: Long, side: Side)

/**
 * 
 * @author Caoyuan Deng
 */
class TradingService(val broker: Broker, val accounts: List[Account], val param: Param, referSer: QuoteSer, 
                     secPicking: SecPicking, signalIndTemplates: SignalIndicator*
) extends org.aiotrade.lib.trading.TradingService {
  protected val log = Logger.getLogger(this.getClass.getName)
  
  val benchmark = new Benchmark(this)
  
  private case class Go(fromTime: Long, toTime: Long)
  private val done = new SyncVar[Boolean]()
  
  protected val timestamps = referSer.timestamps.clone
  protected val freq = referSer.freq

  protected val signalIndicators = new mutable.HashSet[SignalIndicator]()
  protected val triggers = new mutable.HashSet[Trigger]()
  protected val openingOrders = new mutable.HashMap[Account, List[Order]]() // orders to open position
  protected val closingOrders = new mutable.HashMap[Account, List[Order]]() // orders to close position
  protected val pendingOrders = new mutable.HashSet[OrderCompose]()
  
  protected var fromTime: Long = _
  protected var toTime: Long = _
  protected var fromIdx: Int = _
  protected var toIdx: Int = _
  protected var initialReferPrice: Double = _
  protected var isTradeStarted: Boolean = _

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
  
  protected def positionOf(sec: Sec): Option[Position] = {
    accounts find (_.positions.contains(sec)) map (_.positions(sec))
  }
  
  protected def buy(sec: Sec): OrderCompose = {
    addPendingOrder(new OrderCompose(sec, OrderSide.Buy, closeReferIdx))
  }

  protected def sell(sec: Sec): OrderCompose = {
    addPendingOrder(new OrderCompose(sec, OrderSide.Sell, closeReferIdx))
  }
  
  protected def sellShort(sec: Sec): OrderCompose = {
    addPendingOrder(new OrderCompose(sec, OrderSide.SellShort, closeReferIdx))
  }

  protected def buyCover(sec: Sec): OrderCompose = {
    addPendingOrder(new OrderCompose(sec, OrderSide.BuyCover, closeReferIdx))
  }
  
  private def addPendingOrder(order: OrderCompose) = {
    pendingOrders += order
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
      
      checkOrderStatus

      report(i)

      // -- todays ordered processed, now begin to check new conditions and 
      // -- prepare new orders according to today's close status.
      
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
  
  private def executeOrders {
    // sell first?. If so, how about the returning funds?
    openingOrders flatMap (_._2) foreach {order => 
      broker.submit(order)
      pseudoProcessTrade(order)
    }

    closingOrders flatMap (_._2) foreach {order => 
      broker.submit(order)
      pseudoProcessTrade(order)
    }    
  }
  
  private def pseudoProcessTrade(order: Order) {
    broker.processTrade(order.sec, closeTime, order.price, order.quantity)
  }
 
  private def updatePositionsPrice {
    for {
      account <- accounts
      (sec, position) <- account.positions
      ser <- sec.serOf(freq)
      idx = ser.indexOfOccurredTime(closeTime) if idx >= 0
    } {
      position.update(ser.close(idx))
    }
  }

  private def checkOrderStatus {
    for {
      accountToOrders <- List(openingOrders, closingOrders)
      (account, orders) <- accountToOrders
      order <- orders
    } {
      order.status match {
        case (OrderStatus.New | OrderStatus.PendingNew | OrderStatus.Partial) if order.remainQuantity != 0 => 
          log.info("Unfinished order: " + order)
          val retry = new OrderCompose(order.sec, order.side, closeReferIdx) quantity (order.remainQuantity) after (1) using(order.account)
          println("Retry order due to %s: %s".format(order.status, retry))
          addPendingOrder(retry)
        case _ =>
      }
    }
  }
  
  def report(idx: Int) {
    val numAccounts = accounts.size
    val (equity, initialEquity) = accounts.foldLeft((0.0, 0.0)){(s, x) => (s._1 + x.equity, s._2 + x.initialEquity)}
    param.publish(ChartData("Total", 0, closeTime, equity / initialEquity))
    param.publish(ChartData("Refer", 0, closeTime, referSer.close(idx) / initialReferPrice - 1))
    
    if (isTradeStarted) {
      benchmark.at(closeTime, equity)
    }

    accounts foreach {account =>
      if (numAccounts > 1) {
        param.publish(ChartData(account.description, 0, closeTime, account.equity / initialEquity))
      }

      log.info("%1$tY.%1$tm.%1$td: %2$s, opening=%3$s, closing=%4$s，pending=%5$s".format(
          new Date(closeTime), account, openingOrders.getOrElse(account, Nil).size, closingOrders.getOrElse(account, Nil).size, pendingOrders.filter(_.account eq account).size)
      )
    }
  }
  
  protected def checkStopCondition {
    for {
      account <- accounts
      (sec, position) <- account.positions
    } {
      if (account.tradingRule.cutLossRule(position)) {
        triggers += Trigger(sec, position, closeTime, Side.CutLoss)
      }
      if (account.tradingRule.takeProfitRule(position)) {
        triggers += Trigger(sec, position, closeTime, Side.TakeProfit)
      }
    }
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
  
  private def processPendingOrders {
    val orderSubmitReferIdx = closeReferIdx + 1 // next trading day
    if (orderSubmitReferIdx < timestamps.length) {
      val orderSubmitReferTime = timestamps(orderSubmitReferIdx)

      val pendingOrdersToRemove = new mutable.HashSet[OrderCompose]()
      // we should group pending orders here, since orderCompose.order may be set after created
      pendingOrders groupBy (_.account) map {case (account, orders) =>
          val expired = new mutable.HashSet[OrderCompose]()
          val opening = new mutable.HashMap[Sec, OrderCompose]()
          val closing = new mutable.HashMap[Sec, OrderCompose]()
          for (order <- orders) {
            if (order.referIndex < orderSubmitReferIdx) {
              expired += order
            } else if (order.referIndex == orderSubmitReferIdx) { 
              if (order.ser.exists(orderSubmitReferTime)) {
                order.side match {
                  case OrderSide.Buy | OrderSide.SellShort => opening(order.sec) = order
                  case OrderSide.Sell | OrderSide.BuyCover => closing(order.sec) = order
                  case _ =>
                }
              } else {
                order.side match {
                  // @Note if we want to pend buying order, we should count it in opening, 
                  // otherwise the funds may has used out during following steps.
                  case OrderSide.Buy | OrderSide.SellShort => expired += order // drop this orderCompose
                  case OrderSide.Sell | OrderSide.BuyCover => order after (1)   // pend 1 day
                  case _ =>
                }
              }
            }
          }

          if (account.availableFunds <= 0) {
            opening == Nil
          }
          
          val conflicts = Nil//opening.keysIterator filter (closing.contains(_))
          val openingx = (opening -- conflicts).values.toList
          val closingx = (closing -- conflicts).values.toList

          // opening
          val (noFunds, withFunds) = openingx partition (_.funds.isNaN)
          val assignedFunds = withFunds.foldLeft(0.0){(s, x) => s + x.funds}
          val estimateFundsPerSec = (account.availableFunds - assignedFunds) / noFunds.size
          val openingOrdersx = (withFunds ::: (noFunds map {_ funds (estimateFundsPerSec)})) flatMap (_.toOrder)
          adjustOpeningOrders(account, openingOrdersx)
        
          // closing
          val closingOrdersx = closingx flatMap {_ toOrder}
        
          // pending to remove
          pendingOrdersToRemove ++= expired 
          pendingOrdersToRemove ++= openingx
          pendingOrdersToRemove ++= closingx
        
          (account, openingOrdersx, closingOrdersx)
      } foreach {case (account, openingOrdersx, closingOrdersx) =>
          openingOrders(account) = openingOrdersx
          closingOrders(account) = closingOrdersx
      }
      
      pendingOrders --= pendingOrdersToRemove
    } // end if
  }
  
  /** 
   * Adjust orders for expenses etc, by reducing quantities (or number of orders @todo)
   * @Note Iterable has no method of sortBy, that why use List here instead of Set etc
   */
  private def adjustOpeningOrders(account: Account, openingOrders: List[Order]) {
    var orders = openingOrders.sortBy(_.price) 
    var amount = 0.0
    while ({amount = calcTotalFundsToOpen(account, openingOrders); amount > account.availableFunds}) {
      orders match {
        case order :: tail =>
          order.quantity -= account.tradingRule.quantityPerLot
          orders = tail
        case Nil => 
          orders = openingOrders // cycle again
      }
    }
  }
  
  private def calcTotalFundsToOpen(account: Account, orders: List[Order]) = {
    orders.foldLeft(0.0){(s, x) => s + account.calcFundsToOpen(x.quantity, x.price)}
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
    if (!isTradeStarted) {
      isTradeStarted = true
    }
    
    val ser = sec.serOf(freq).get
    private var _account = accounts.head // default account
    private var _price = Double.NaN
    private var _funds = Double.NaN
    private var _quantity = Double.NaN
    private var _afterIdx = 0

    def account = _account
    def using(account: Account) = {
      _account = account
      this
    }
    
    def price = _price
    def price(price: Double) = {
      _price = price
      this
    }

    def funds = _funds
    def funds(funds: Double) = {
      _funds = funds
      this
    }
    
    def quantity = _quantity
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
            case OrderSide.Buy | OrderSide.SellShort =>
              if (_price.isNaN) {
                _price = _account.tradingRule.buyPriceRule(quote)
              }
              if (_quantity.isNaN) {
                _quantity = _account.tradingRule.buyQuantityRule(quote, _price, _funds)
              }
            case OrderSide.Sell | OrderSide.BuyCover =>
              if (_price.isNaN) {
                _price = _account.tradingRule.sellPriceRule(quote)
              }
              if (_quantity.isNaN) {
                _quantity = positionOf(sec) match {
                  case Some(position) => 
                    // @Note quantity of position may be negative because of sellShort etc.
                    _account.tradingRule.sellQuantityRule(quote, _price, math.abs(position.quantity))
                  case None => 0
                }
              }
            case _ =>
          }
          
          _quantity = math.abs(_quantity)
          if (_quantity > 0) {
            val order = new Order(_account, sec, _quantity, _price, side)
            order.time = time
            println("Some order: %s".format(this))
            Some(order)
          } else {
            println("None order: %s. Quote: volume=%5.2f, average=%5.2f, cost=%5.2f".format(this, quote.volume, quote.average, quote.average * _account.tradingRule.multiplier * _account.tradingRule.marginRate))
            None
          }
          
        case None => None
      }
    }
    
    override 
    def toString = {
      "OrderCompose(%1$s, %2$tY.%2$tm.%2$td, %3$s, %4$s, %5$10.2f, %6$d, %7$5.2f)".format(_account.description, new Date(timestamps(referIndex)), sec.uniSymbol, side, _funds, _quantity.toInt, _price)
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
      val tradingRule = new TradingRule()
      val account = new StockAccount("Backtest", 10000000.0, tradingRule)
    
      val indTemplate = createIndicator(classOf[MACDSignal], Array(fasterPeriod, slowPeriod, signalPeriod))
    
      val tradingService = new TradingService(broker, List(account), param, referSer, secPicking, indTemplate) {
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
