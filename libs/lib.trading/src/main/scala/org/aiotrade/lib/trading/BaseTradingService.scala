package org.aiotrade.lib.trading

import java.util.Date
import java.util.logging.Logger
import org.aiotrade.lib.math.indicator.SignalIndicator
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.signal.SignalEvent
import org.aiotrade.lib.securities
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable

/**
 * 
 * @author Caoyuan Deng
 */
class BaseTradingService(val broker: Broker, val accounts: List[Account], val param: Param,
                         protected val referSer: QuoteSer, protected val secPicking: SecPicking, protected val signalIndTemplates: SignalIndicator*
) extends TradingService {
  protected val log = Logger.getLogger(this.getClass.getName)
  
  val tradableAccounts = (accounts filter (_.isInstanceOf[TradableAccount])).asInstanceOf[List[TradableAccount]]
  val cashAccounts = (accounts filter (_.isInstanceOf[CashAccount])).asInstanceOf[List[CashAccount]]
  
  val benchmark = new Benchmark(this)
  
  protected val timestamps = referSer.timestamps.clone
  protected val freq = referSer.freq

  protected val signalIndicators = new mutable.HashSet[SignalIndicator]()
  protected val triggers = new mutable.HashSet[Trigger]()
  protected val openingOrders = new mutable.HashMap[Account, List[Order]]() // orders to open position
  protected val closingOrders = new mutable.HashMap[Account, List[Order]]() // orders to close position
  protected val pendingOrders = new mutable.HashSet[OrderCompose]()
  
  /** current closed refer idx */
  protected var closeReferIdx = 0
  /** current closed refer time */
  protected def closeTime = timestamps(closeReferIdx)

  protected var tradeStartIdx: Int = -1
  protected def isTradeStarted: Boolean = tradeStartIdx >= 0

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
      
    case _ =>
  }

  protected def initSignalIndicators {
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
    tradableAccounts find (_.positions.contains(sec)) map (_.positions(sec))
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
   * The actions at referIdx time. Main entrance of trading server
   * It could be trigged by a closed/opened event
   * @Todo more details and conditions for real trading. 
   */
  protected def go(referIdx: Int) {
    closeReferIdx = referIdx
    executeOrders
    updatePositionsPrice
      
    checkOrderStatus

    if (isTradeStarted) {
      report(referIdx)
    }

    // today's orders processed, now begin to check new conditions and 
    // prepare new orders according to today's close status.
      
    secPicking.go(closeTime)
    checkStopCondition
    
    at(referIdx)
    
    processPendingOrders
  }
  
  protected def executeOrders {
    val allOpeningOrders = openingOrders flatMap (_._2)
    val allClosingOrders = closingOrders flatMap (_._2)
    
    if (!isTradeStarted && (allOpeningOrders.nonEmpty || allClosingOrders.nonEmpty)) {
      tradeStartIdx = closeReferIdx
    }
    
    // sell first?. If so, how about the returning funds?
    allOpeningOrders foreach {order => 
      broker.submit(order)
      //@Todo this should be re-considered for real trading, i.e. trigged by returning order executed event 
      broker.processTrade(order.sec, closeTime, order.price, order.quantity)
    }

    allClosingOrders foreach {order => 
      broker.submit(order)
      //@Todo this should be re-considered for real trading, i.e. trigged by returning order executed event 
      broker.processTrade(order.sec, closeTime, order.price, order.quantity)
    }    
  }
  
  protected def updatePositionsPrice {
    for {
      account <- tradableAccounts
      (sec, position) <- account.positions
      ser <- sec.serOf(freq)
      idx = ser.indexOfOccurredTime(closeTime) if idx >= 0
    } {
      position.update(ser.close(idx))
    }
  }

  protected def checkOrderStatus {
    for {
      accountToOrders <- List(openingOrders, closingOrders)
      (account, orders) <- accountToOrders
      order <- orders
    } {
      order.status match {
        case (OrderStatus.New | OrderStatus.PendingNew | OrderStatus.Partial) => 
          log.info("Unfinished order (will retry): " + order)
          val retry = new OrderCompose(order.sec, order.side, closeReferIdx) quantity (order.remainQuantity) after (1) using(order.account)
          println("Retry order due to %s: %s".format(order.status, retry))
          addPendingOrder(retry)
        case _ =>
      }
    }
  }
  
  protected def report(idx: Int) {
    val numAccounts = accounts.size
    val (equity, initialEquity) = accounts.foldLeft((0.0, 0.0)){(s, x) => (s._1 + x.equity, s._2 + x.initialEquity)}
    param.publish(ReportData("Total", 0, closeTime, equity / initialEquity))
    param.publish(ReportData("Refer", 0, closeTime, referSer.close(idx) / referSer.open(tradeStartIdx) - 1))
    
    benchmark.at(closeTime, equity, referSer.close(idx))

    accounts foreach {account =>
      if (numAccounts > 1) {
        param.publish(ReportData(account.description, 0, closeTime, account.equity / initialEquity))
      }

      log.info("%1$tY.%1$tm.%1$td: %2$s, opening=%3$s, closing=%4$s，pending=%5$s".format(
          new Date(closeTime), account, openingOrders.getOrElse(account, Nil).size, closingOrders.getOrElse(account, Nil).size, pendingOrders.filter(_.account eq account).size)
      )
    }
  }
  
  protected def checkStopCondition {
    for {
      account <- tradableAccounts
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
  protected def at(idx: Int) {
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
  
  protected def processPendingOrders {
    val orderSubmitReferIdx = closeReferIdx + 1 // next trading day
    if (orderSubmitReferIdx < timestamps.length) {
      val orderSubmitReferTime = timestamps(orderSubmitReferIdx)

      val pendingOrdersToRemove = new mutable.HashSet[OrderCompose]()
      // we should group pending orders here, since orderCompose.order may be set after created
      val newOpenCloseOrders = pendingOrders groupBy (_.account) map {case (account, orders) =>
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
        
          account -> (openingOrdersx, closingOrdersx)
      }
      
      // We should iterate through each account of accounts instead of account in newOpenCloseOrders 
      // to make sure orders of each account are updated. 
      // @Note newOpenCloseOrders may be empty
      for (account <- tradableAccounts) {
        val (openingOrdersx, closingOrdersx) = newOpenCloseOrders.getOrElse(account, (Nil, Nil))
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
  protected def adjustOpeningOrders(account: TradableAccount, openingOrders: List[Order]) {
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
  
  protected def calcTotalFundsToOpen(account: TradableAccount, orders: List[Order]) = {
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
    val ser = sec.serOf(freq).get
    private var _account = tradableAccounts.head // default account
    private var _price = Double.NaN
    private var _funds = Double.NaN
    private var _quantity = Double.NaN
    private var _afterIdx = 0

    def account = _account
    def using(account: TradableAccount) = {
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
