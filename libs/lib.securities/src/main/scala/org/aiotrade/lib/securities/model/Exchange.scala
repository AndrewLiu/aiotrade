package org.aiotrade.lib.securities.model

import java.util.logging.Logger
import java.util.{Calendar, TimeZone, ResourceBundle, Timer, TimerTask}
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.actors.Event

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object Exchanges extends Table[Exchange] {
  val log = Logger.getLogger(this.getClass.getSimpleName)

  val code = "code" VARCHAR(4)
  val name = "name" VARCHAR(10)
  val fullName = "fullName" VARCHAR(30)
  val timeZoneStr = "timeZoneStr" VARCHAR(30)
  val openCloseHMs = "openCloseHMs" SERIALIZED(classOf[Array[Int]], 100)

  def closeDates = inverse(ExchangeCloseDates.exchange)
  def secs = inverse(Secs.exchange)

  INDEX(getClass.getSimpleName + "_code_idx", code.name)

  // --- helper methods
  def secsOf(exchange: Exchange): Seq[Sec] = {
    val t0 = System.currentTimeMillis
    val exchangeId = Exchanges.idOf(exchange)
    val secs = (SELECT (Secs.*, SecInfos.*) FROM (Secs JOIN SecInfos) WHERE (Secs.exchange.field EQ exchangeId) list) map (_._1)
    log.info("Secs number of " + exchange.code + "(id=" + exchangeId + ") is " + secs.size +
             ", loaded in " + (System.currentTimeMillis - t0) + " ms")
    secs
  }

  def uniSymbolToLastTickerOf(exchange: Exchange) = {
    val symbolToTicker = new HashMap[String, LightTicker]
    for ((sec, ticker) <- Tickers.lastTickersOf(exchange) if sec != null) {
      val symbol = sec.uniSymbol
      ticker.symbol = symbol
      symbolToTicker.put(symbol, ticker)
    }

    symbolToTicker
  }
}

object Exchange extends Publisher {
  private val log = Logger.getLogger(this.getClass.getSimpleName)

  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.securities.model.Bundle")
  private val ONE_DAY = 24 * 60 * 60 * 1000

  case class Opened(exchange: Exchange) extends Event
  case class Closed(exchange: Exchange) extends Event

  /**
   * @Note should lazily call allExchanges during model running, so, don't start timer actor automatically
   */
  private val timer = new Timer("Exchange Open/Close Timer")
  private var timerStarted = false
  def startTimer {
    if (timerStarted) return

    for (exchange <- allExchanges) {
      val preOpen = exchange.open
      preOpen.add(Calendar.MINUTE, -15)
      timer.scheduleAtFixedRate(new TimerTask {
          def run {
            // @todo process vacation here
            publish(Opened(exchange))
          }
        }, preOpen.getTime, ONE_DAY)

      val postClose = exchange.close
      postClose.add(Calendar.MINUTE, +15)
      timer.schedule(new TimerTask {
          def run {
            // @todo process vacation here
            exchange.scheduleDoClosing(-1)
            publish(Closed(exchange))
          }
        }, postClose.getTime, ONE_DAY)
    }
    
    timerStarted = true
  }

  lazy val allExchanges = Exchanges.all()
  lazy val codeToExchange = allExchanges map (x => (x.code -> x)) toMap

  lazy val exchangeToSecs = {
    allExchanges map (x => (x -> Exchanges.secsOf(x))) toMap
  }

  lazy val exchangeToUniSymbols = {
    exchangeToSecs map {x =>
      val syms = ListBuffer[String]()
      x._2 foreach {sec =>
        if (sec.secInfo == null)
          log.warning("secInfo of sec " + sec + " is null")
        else
          syms += sec.secInfo.uniSymbol
      }
      log.info("Syms number of " + x._1.code + " is " + syms.size)
      x._1 -> syms.toList
    } toMap
  }

  lazy val uniSymbolToSec = {
    exchangeToSecs map (_._2) flatMap {secs =>
      secs filter (_.secInfo != null) map (x => x.secInfo.uniSymbol -> x)
    } toMap
  }

  lazy val N  = withCode("N" ).get
  lazy val SS = withCode("SS").get
  lazy val SZ = withCode("SZ").get
  lazy val L  = withCode("L" ).get

  def withCode(code: String): Option[Exchange] = codeToExchange.get(code)

  def exchangeOf(uniSymbol: String): Exchange = {
    uniSymbol.toUpperCase.split('.') match {
      case Array(symbol) => N
      case Array(symbol, "L" ) => L
      case Array(symbol, "SS") => SS
      case Array(symbol, "SZ") => SZ
      case _ => SZ
    }
  }

  def secsOf(exchange: Exchange): Seq[Sec] = exchangeToSecs.get(exchange) getOrElse Nil

  def symbolsOf(exchange: Exchange): Seq[String] = {
    exchangeToUniSymbols.get(exchange) getOrElse Nil
  }

  def secOf(uniSymbol: String): Option[Sec] =
    uniSymbolToSec.get(uniSymbol)

  def apply(code: String, timeZoneStr: String, openCloseHMs: Array[Int]) = {
    val exchange = new Exchange
    exchange.code = code
    exchange.timeZoneStr = timeZoneStr
    exchange.openCloseHMs = openCloseHMs
    exchange
  }
}

trait TradingStatus
object TradingStatus {
  case class PreOpen(time: Long) extends TradingStatus
  case class OpeningCallAcution(time: Long) extends TradingStatus
  case class Open(time: Long) extends TradingStatus
  case class Opening(time: Long) extends TradingStatus
  case class Break(time: Long) extends TradingStatus
  case class Close(time: Long) extends TradingStatus
  case class Closed(time: Long) extends TradingStatus
  case class Unknown(time: Long) extends TradingStatus
}

import Exchange._
class Exchange {

  // --- database fields
  var code: String = "SS"
  var name: String = "??"
  var fullName: String = "???????"
  var timeZoneStr: String = "Asia/Shanghai"
  var openCloseHMs: Array[Int] = Array(9, 30, 11, 30, 13, 0, 15, 0)

  var closeDates: List[ExchangeCloseDate] = Nil
  var secs: List[Sec] = Nil
  // --- end database fields

  log.info("New exchange: " + System.identityHashCode(this))

  private var _tradingStatus: TradingStatus = TradingStatus.Unknown(-1)

  lazy val longDescription:  String = BUNDLE.getString(code + "_Long")
  lazy val shortDescription: String = BUNDLE.getString(code + "_Short")
  lazy val timeZone: TimeZone = TimeZone.getTimeZone(timeZoneStr)

  private lazy val openHour  = openCloseHMs(0)
  private lazy val openMin   = openCloseHMs(1)
  private lazy val closeHour = openCloseHMs(openCloseHMs.length - 2)
  private lazy val closeMin  = openCloseHMs(openCloseHMs.length - 1)

  lazy val openingPeriods = {
    val periods = new Array[(Int, Int)](openCloseHMs.length / 4)
    
    var i = 0
    while (i < openCloseHMs.length) {
      val fr = openCloseHMs(i) * 60 + openCloseHMs(i + 1)
      i += 2

      val to = openCloseHMs(i) * 60 + openCloseHMs(i + 1)
      i += 2

      periods(i / 4 - 1) = (fr, to)
    }

    periods
  }

  lazy val firstOpen: Int = openingPeriods(0)._1
  lazy val lastClose: Int = openingPeriods(openingPeriods.length - 1)._2

  lazy val openTimeOfDay: Long = (openHour * 60 + openMin) * 60 * 1000

  lazy val nMinutes: Int = {
    val openInMillis = open.getTimeInMillis
    val closeInMills = close.getTimeInMillis
    ((closeInMills - openInMillis) / TUnit.Minute.interval).toInt + 1
  }

  private var _symbols = List[String]()

  private val dailyQuotesToClose = new ArrayList[Quote]()
  private val dailyMoneyFlowsToClose = new ArrayList[MoneyFlow]()

  private var _lastDailyRoundedTradingTime: Option[Long] = None

  def lastDailyRoundedTradingTime: Option[Long] = {
    val cal = Calendar.getInstance(timeZone)
    val dailyRoundedTimeOfToday = TFreq.DAILY.round(System.currentTimeMillis, cal)
    if (_lastDailyRoundedTradingTime.isEmpty || _lastDailyRoundedTradingTime.get != dailyRoundedTimeOfToday) {
      _lastDailyRoundedTradingTime = Tickers.lastTradingTimeOf(this)
    }
    _lastDailyRoundedTradingTime
  }

  def addNewDailyQuote(quote: Quote) = dailyQuotesToClose synchronized {
    dailyQuotesToClose += quote
  }

  def addNewDailyMoneyFlow(mf: MoneyFlow) = dailyMoneyFlowsToClose synchronized {
    dailyMoneyFlowsToClose += mf
  }

  def tradingStatus = _tradingStatus
  def tradingStatus_=(status: TradingStatus) {
    import TradingStatus._

    _tradingStatus = status
    status match {
      case Close(time)  => scheduleDoClosing(5)
      case Closed(time) => scheduleDoClosing(5)
      case _ =>
    }
  }

  def open: Calendar = {
    val cal = Calendar.getInstance(timeZone)
    cal.set(Calendar.HOUR_OF_DAY, openHour)
    cal.set(Calendar.MINUTE, openMin)
    cal
  }

  def close: Calendar = {
    val cal = Calendar.getInstance(timeZone)
    cal.set(Calendar.HOUR_OF_DAY, closeHour)
    cal.set(Calendar.MINUTE, closeMin)
    cal
  }


  def openTime(time: Long): Long = {
    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, openHour)
    cal.set(Calendar.MINUTE, openMin)
    cal.getTimeInMillis
  }

  def closeTime(time: Long): Long = {
    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, closeHour)
    cal.set(Calendar.MINUTE, closeMin)
    cal.getTimeInMillis
  }

  def symbols = _symbols
  def symbols_=(symbols: List[String]) {
    _symbols = symbols
  }

  def tradingStatusOf(time: Long): TradingStatus = {
    import TradingStatus._
    
    var status: TradingStatus = null
    if (time == 0) {
      status = Closed(time)
    } else {
      val cal = Calendar.getInstance(timeZone)
      cal.setTimeInMillis(time)
      val timeInMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

      if (timeInMinutes == firstOpen) {
        status = Open(time)
      } else if (timeInMinutes == lastClose) {
        status = Close(time)
      } else {
        var i = 0
        while (i < openingPeriods.length && status == null) {
          val openingPeriod = openingPeriods(i)
          if (timeInMinutes >= openingPeriod._1 && timeInMinutes <= openingPeriod._2) {
            status = Opening(time)
          }
          i += 1
        }

        if (status == null) {
          status = if (timeInMinutes > firstOpen && timeInMinutes < lastClose) {
            Break(time)
          } else if (timeInMinutes <  firstOpen - 15) {
            PreOpen(time)
          } else if (timeInMinutes >= firstOpen - 15 && timeInMinutes < firstOpen - 5) {
            OpeningCallAcution(time)
          } else if (timeInMinutes >= firstOpen - 5  && timeInMinutes < firstOpen) {
            Open(time)
          } else if (timeInMinutes <= lastClose + 5) {
            Close(time)
          } else {
            Closed(time)
          }
        }
      }
    }
    
    status
  }

  /**
   * Do closing in delay minutes. If quotesToClose/mfsToClose is empty, will do
   * nothing and return at once.
   */
  def scheduleDoClosing(delay: Int) {
    val quotesToClose = dailyQuotesToClose synchronized {
      val x = dailyQuotesToClose.toArray
      dailyQuotesToClose.clear
      x
    }

    val mfsToClose = dailyMoneyFlowsToClose synchronized {
      val x = dailyMoneyFlowsToClose.toArray
      dailyMoneyFlowsToClose.clear
      x
    }

    if (delay > 0) {
      log.info(this + " will do closing in " + delay + " minutes for quotes: " + quotesToClose.length + ", mfs: " + mfsToClose.length)
    } else {
      log.info(this + " do closing right now for quotes: " + quotesToClose.length + ", mfs: " + mfsToClose.length)
    }

    if (quotesToClose.length == 0 && mfsToClose.length == 0) return

    if (delay > 0) {
      (new Timer).schedule(new TimerTask {
          def run {
            doClosing(quotesToClose, mfsToClose)
          }
        }, delay * 60 * 1000)
    } else {
      doClosing(quotesToClose, mfsToClose)
    }
  }

  /**
   * Close and insert daily quotes/moneyflows
   */
  private def doClosing(quotesToClose: Array[Quote], mfsToClose: Array[MoneyFlow]) {
    var willCommit = false

    if (quotesToClose.length > 0) {
      var i = 0
      while (i < quotesToClose.length) {
        quotesToClose(i).closed_!
        i += 1
      }

      log.info(this + " closed, inserting daily quotes: " + quotesToClose.length)
      Quotes1d.insertBatch_!(quotesToClose)
      willCommit = true
    }

    if (mfsToClose.length > 0) {
      var i = 0
      while (i < mfsToClose.length) {
        mfsToClose(i).closed_!
        i += 1
      }

      log.info(this + " closed, inserting daily moneyflows=" + mfsToClose.length)
      MoneyFlows1d.insertBatch_!(mfsToClose)
      willCommit = true
    }

    if (willCommit) {
      commit
      log.info(this + " closed, committed.")
    }
  }


  override def toString: String = {
    code + "(" + timeZone.getDisplayName + ")"
  }

}

