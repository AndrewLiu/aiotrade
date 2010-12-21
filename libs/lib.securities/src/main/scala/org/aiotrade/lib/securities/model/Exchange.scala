package org.aiotrade.lib.securities.model

import java.util.logging.Logger
import java.util.{Calendar, TimeZone, ResourceBundle, Timer, TimerTask}
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.reactors.Event

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object Exchanges extends Table[Exchange] {
  private val log = Logger.getLogger(this.getClass.getName)

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
}

object Exchange extends Publisher {
  private val log = Logger.getLogger(this.getClass.getName)

  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.securities.model.Bundle")
  private val ONE_DAY = 24 * 60 * 60 * 1000

  case class Opened(exchange: Exchange) extends Event
  case class Closed(exchange: Exchange) extends Event

  lazy val allExchanges = {
    val xs = Exchanges.all()
    xs foreach {x => log.info("Exchange: " + x)}
    xs
  }
  lazy val codeToExchange = allExchanges map (x => (x.code -> x)) toMap

  lazy val exchangeToSecs = {
    allExchanges map (x => (x -> Exchanges.secsOf(x))) toMap
  }

  lazy val exchangeToUniSymbols = {
    exchangeToSecs map {m =>
      val syms = ListBuffer[String]()
      m._2 foreach {sec =>
        if (sec.secInfo == null)
          log.warning("secInfo of sec " + sec + " is null")
        else
          syms += sec.secInfo.uniSymbol
      }
      log.info("Symbols number of " + m._1.code + " is " + syms.size)
      m._1 -> syms.toList
    } toMap
  }

  lazy val uniSymbolToSec = {
    exchangeToSecs flatMap {m =>
      m._2 filter (_.secInfo != null) map (x => x.secInfo.uniSymbol -> x)
    } toMap
  }

  lazy val N  = withCode("N" ).get
  lazy val SS = withCode("SS").get
  lazy val SZ = withCode("SZ").get
  lazy val L  = withCode("L" ).get
  lazy val HK = withCode("HK").get
  lazy val OQ = withCode("OQ").get

  def withCode(code: String): Option[Exchange] = codeToExchange.get(code)

  def exchangeOf(uniSymbol: String): Exchange = {
    uniSymbol.toUpperCase.split('.') match {
      case Array(symbol) =>
        exchangeOfIndex(symbol) match {
          case Some(exchg) => exchg
          case None => N
        }
      case Array(symbol, "L" ) => L
      case Array(symbol, "SS") => SS
      case Array(symbol, "SZ") => SZ
      case Array(symbol, "HK") => HK
      case _ => SZ
    }
  }

  def exchangeOfIndex(uniSymbol: String) : Option[Exchange] = {
    uniSymbol match {
      case "^DJI" => Some(N)
      case "^HSI" => Some(HK)
      case _=> None
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

  def apply(code: String, name: String, fullName: String, timeZoneStr: String, openCloseHMs: Array[Int]) = {
    val exchange = new Exchange
    exchange.code = code
    exchange.name = name
    exchange.fullName = fullName
    exchange.timeZoneStr = timeZoneStr
    exchange.openCloseHMs = openCloseHMs
    exchange
  }

  val CN_OPENING_CALL_AUCTION_MINUTES = 10
  val CN_PREOPEN_BREAK_MINUTES = 5
  val CLOSE_QUOTE_DELAY_MINUTES = 2
}

abstract class TradingStatus {
  def time: Long
  def timeInMinutes: Int
}
object TradingStatus {
  case class PreOpen(time: Long, timeInMinutes: Int) extends TradingStatus
  case class OpeningCallAcution(time: Long, timeInMinutes: Int) extends TradingStatus
  case class Open(time: Long, timeInMinutes: Int) extends TradingStatus
  case class Opening(time: Long, timeInMinutes: Int) extends TradingStatus
  case class Break(time: Long, timeInMinutes: Int) extends TradingStatus
  case class Close(time: Long, timeInMinutes: Int) extends TradingStatus
  case class Closed(time: Long, timeInMinutes: Int) extends TradingStatus
  case class Unknown(time: Long, timeInMinutes: Int) extends TradingStatus
}

import Exchange._
class Exchange extends Ordered[Exchange] {

  // --- database fields
  var code: String = "SS"
  var name: String = ""
  var fullName: String = ""
  var timeZoneStr: String = "Asia/Shanghai"
  var openCloseHMs: Array[Int] = Array(9, 30, 11, 30, 13, 0, 15, 0)

  var closeDates: List[ExchangeCloseDate] = Nil
  var secs: List[Sec] = Nil
  // --- end database fields

  log.info("Create exchange: " + System.identityHashCode(this))

  private var _tradingStatus: TradingStatus = TradingStatus.Unknown(-1, -1)

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

  lazy val uniSymbolToLastTicker = {
    val symbolToTicker = new HashMap[String, Ticker]

    for ((sec, ticker) <- TickersLast.lastTickersOf(this) if sec != null) {
      val symbol = sec.uniSymbol
      ticker.symbol = symbol
      symbolToTicker.put(symbol, ticker)
    }

    symbolToTicker
  }
  
  lazy val uniSymbolToLastTradingDayTicker = {
    val symbolToTicker = new HashMap[String, Ticker]

    for ((sec, ticker) <- TickersLast.lastTradingDayTickersOf(this) if sec != null) {
      val symbol = sec.uniSymbol
      ticker.symbol = symbol
      symbolToTicker.put(symbol, ticker)
    }

    symbolToTicker
  }

  private val freqToUnclosedQuotes = new HashMap[TFreq, ArrayList[Quote]]
  private val freqToUnclosedMoneyFlows = new HashMap[TFreq, ArrayList[MoneyFlow]]

  private var _lastDailyRoundedTradingTime: Option[Long] = None

  /** @Todo */
  def primaryIndex: Option[Sec] = code match {
    case "SS" => Exchange.secOf("000001.SS")
    case "SZ" => Exchange.secOf("399001.SZ")
    case "N" => Exchange.secOf("^DJI")
    case "L" => Exchange.secOf("^FTSE")
    case _ => Exchange.secOf("000001.SS")
  }

  /**
   * @return the ticker should be updated/saved to TickersLast
   */
  def gotLastTicker(ticker: Ticker): Ticker = {
    val uniSymbol = ticker.symbol

    uniSymbolToLastTicker synchronized {
      uniSymbolToLastTicker.get(uniSymbol) match {
        case Some(existOne) =>
          existOne.copyFrom(ticker)
          existOne.isTransient = TickersLast.transient_?(existOne)
          existOne
        case None =>
          val newOne = new Ticker
          newOne.isTransient = true
          newOne.copyFrom(ticker)
          uniSymbolToLastTicker.put(uniSymbol, newOne)
          uniSymbolToLastTradingDayTicker synchronized {
            uniSymbolToLastTradingDayTicker.put(uniSymbol, newOne)
          }
          newOne
      }
    }
  }

  def lastDailyRoundedTradingTime: Option[Long] = {
    val cal = Calendar.getInstance(timeZone)
    val dailyRoundedTimeOfToday = TFreq.DAILY.round(System.currentTimeMillis, cal)
    if (_lastDailyRoundedTradingTime.isEmpty || _lastDailyRoundedTradingTime.get != dailyRoundedTimeOfToday) {
      TickersLast.lastTradingTimeOf(this) match {
        case Some(x) => _lastDailyRoundedTradingTime = Some(TFreq.DAILY.round(x, cal))
        case _ =>
      }
    }
    _lastDailyRoundedTradingTime
  }

  def addNewQuote(freq: TFreq, quote: Quote) = freqToUnclosedQuotes synchronized {
    freqToUnclosedQuotes.get(freq) getOrElse {
      val xs = new ArrayList[Quote]
      freqToUnclosedQuotes.put(freq, xs)
      xs
    } += quote
  }

  def addNewMoneyFlow(freq: TFreq, mf: MoneyFlow) = freqToUnclosedMoneyFlows synchronized {
    freqToUnclosedMoneyFlows.get(freq) getOrElse {
      val xs = new ArrayList[MoneyFlow]
      freqToUnclosedMoneyFlows.put(freq, xs)
      xs
    } += mf
  }

  def tradingStatus = _tradingStatus
  def tradingStatus_=(status: TradingStatus) {
    _tradingStatus = status
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

  protected def tradingStatusCN(timeInMinutes: Int, time: Long): Option[TradingStatus]  = {
    import TradingStatus._

    if (timeInMinutes < firstOpen - CN_OPENING_CALL_AUCTION_MINUTES - CN_PREOPEN_BREAK_MINUTES) {
      Some(PreOpen(time, timeInMinutes))
    } else if (timeInMinutes >= firstOpen - CN_OPENING_CALL_AUCTION_MINUTES - CN_PREOPEN_BREAK_MINUTES &&
               timeInMinutes <= firstOpen - CN_PREOPEN_BREAK_MINUTES) {
      Some(OpeningCallAcution(time, timeInMinutes))
    } else if (timeInMinutes > firstOpen - CN_PREOPEN_BREAK_MINUTES &&
               timeInMinutes < firstOpen) {
      Some(Break(time, timeInMinutes))
    } else if (timeInMinutes > lastClose && timeInMinutes <= lastClose + CLOSE_QUOTE_DELAY_MINUTES) {
      Some(Close(time, timeInMinutes))
    } else if (timeInMinutes > lastClose + CLOSE_QUOTE_DELAY_MINUTES) {
      Some(Closed(time, timeInMinutes))
    } else {
      None
    }
  }
  
  def tradingStatusOf(time: Long): TradingStatus = {
    import TradingStatus._

    var status: TradingStatus = null

    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(time)
    val timeInMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    if (this == SZ || this == SS) {
      val cnStatus = tradingStatusCN(timeInMinutes, time)
      cnStatus match {
        case Some(s) => return s
        case None =>
      }
    }
    

    if (time == 0) {
      status = Closed(time, timeInMinutes)
    } else {
      if (timeInMinutes < firstOpen) {
        status = PreOpen(time, timeInMinutes)
      } else if (timeInMinutes == firstOpen) {
        status = Open(time, timeInMinutes)
      } else if (timeInMinutes == lastClose) {
        status = Close(time, timeInMinutes)
      } else if (timeInMinutes > lastClose) {
        status = Closed(time, timeInMinutes)
      } else {
        var i = 0
        while (i < openingPeriods.length && status == null) {
          val openingPeriod = openingPeriods(i)
          if (timeInMinutes >= openingPeriod._1 && timeInMinutes <= openingPeriod._2) {
            status = Opening(time, timeInMinutes)
          }
          i += 1
        }

        if (status == null) {
          status = Unknown(time, timeInMinutes)
        }
      }
    }
    
    status
  }

  private val emptyQuotes = ArrayList[Quote]()
  private val emptyMoneyFlows = ArrayList[MoneyFlow]()
  private val dailyCloseDelay = 5 * 60 * 1000 // 5 minutes
  private var toCloseTimeInMinutes = -1

  private def isClosed(freq: TFreq, tradingStatusTime: Long, roundedTime: Long) = {
    tradingStatusTime >= roundedTime + freq.interval
  }

  /**
   * Do closing in delay minutes. If quotesToClose/mfsToClose is empty, will do
   * nothing and return at once.
   */
  private[securities] def tryClosing(alsoSave: Boolean) {
    import TradingStatus._

    val closingTimeInMinutes = toCloseTimeInMinutes
    val statusTime = tradingStatus.time

    val freqs = tradingStatus match {
      
      case Opening(time, timeInMinutes) if toCloseTimeInMinutes <= 0 || timeInMinutes <  toCloseTimeInMinutes =>
        // When processing legacy data, 'toCloseTimeInMinutes' may be set to previous date's larger timeInMinutes,
        // so we add '|| timeInMinutes <  toCloseTimeInMinutes'
        toCloseTimeInMinutes = timeInMinutes + 1
        Nil
      case Opening(time, timeInMinutes) if toCloseTimeInMinutes >  0 && timeInMinutes >= toCloseTimeInMinutes =>
        // a new minute begins, will doClose on ONE_MIN after 1 minute
        toCloseTimeInMinutes = timeInMinutes + 1
        List(TFreq.ONE_MIN)
        
      case Close(time, timeInMinutes) => 
        toCloseTimeInMinutes = -1
        List(TFreq.ONE_MIN, TFreq.DAILY)
      case Closed(time, timeInMinutes) =>
        toCloseTimeInMinutes = -1
        List(TFreq.ONE_MIN, TFreq.DAILY)
        
      case _ => Nil
    }

    log.info("Try closing quotes of: " + freqs + ", closingTimeInMinutes=" + closingTimeInMinutes)

    for (freq <- freqs) {

      val quotesToClose = freqToUnclosedQuotes synchronized {
        freqToUnclosedQuotes.get(freq) match {
          case Some(unclosed) if freq == TFreq.DAILY =>
            freqToUnclosedQuotes.put(freq, emptyQuotes)
            unclosed
          case Some(unclosed) =>
            val (toClose, other) = unclosed.partition{x => isClosed(freq, statusTime, x.time)}
            freqToUnclosedQuotes.put(freq, other)
            toClose
          case None => emptyQuotes
        }
      }

      val mfsToClose = freqToUnclosedMoneyFlows synchronized {
        freqToUnclosedMoneyFlows.get(freq) match {
          case Some(unclosed) if freq == TFreq.DAILY =>
            freqToUnclosedMoneyFlows.put(freq, emptyMoneyFlows)
            unclosed
          case Some(unclosed) =>
            val (toClose, other) = unclosed.partition{x => isClosed(freq, statusTime, x.time)}
            freqToUnclosedMoneyFlows.put(freq, other)
            toClose
          case None => emptyMoneyFlows
        }
      }

      if (quotesToClose.length > 0 || mfsToClose.length > 0) {
        val isDailyClose = freqs.contains(TFreq.DAILY)
        if (isDailyClose) {
          log.info(this.code + " will do closing in " + (dailyCloseDelay / 60 / 1000) + " minutes for (" + freq + "), quotes=" + quotesToClose.length + ", mfs=" + mfsToClose.length)
          (new Timer).schedule(new TimerTask {
              def run {
                doClosing(freq, quotesToClose, mfsToClose, alsoSave)
              }
            }, dailyCloseDelay)
        } else {
          doClosing(freq, quotesToClose, mfsToClose, alsoSave)
        }
      }
    }
  }

  /**
   * Close and insert daily quotes/moneyflows
   */
  private def doClosing(freq: TFreq, quotesToClose: ArrayList[Quote], mfsToClose: ArrayList[MoneyFlow], alsoSave: Boolean) {
    var willCommit = false

    if (quotesToClose.length > 0) {
      willCommit = true

      var i = 0
      while (i < quotesToClose.length) {
        val quote = quotesToClose(i)
        quote.closed_!

        val sec = quote.sec
        freq match {
          case TFreq.DAILY if alsoSave || sec.isSerCreated(TFreq.DAILY) =>
            sec.serOf(TFreq.DAILY) foreach {_.updateFrom(quote)}
          case TFreq.ONE_MIN if alsoSave || sec.isSerCreated(TFreq.ONE_MIN) =>
            sec.serOf(TFreq.ONE_MIN) foreach {_.updateFrom(quote)}
          case _ =>
        }

        i += 1
      }

      if (alsoSave) {
        val (toInsert, toUpdate) = quotesToClose.partition(_.isTransient)
        freq match {
          case TFreq.DAILY =>
            log.info(this.code + " closed, inserting " + freq + " quotes: " + quotesToClose.length)
            if (toInsert.length > 0) Quotes1d.insertBatch_!(toInsert.toArray)
            if (toUpdate.length > 0) Quotes1d.updateBatch_!(toUpdate.toArray)
          case TFreq.ONE_MIN =>
            if (toInsert.length > 0) Quotes1m.insertBatch_!(toInsert.toArray)
            if (toUpdate.length > 0) Quotes1m.updateBatch_!(toUpdate.toArray)
        }

      }
    }

    if (mfsToClose.length > 0) {
      willCommit = true

      var i = 0
      while (i < mfsToClose.length) {
        val mfs = mfsToClose(i)
        mfs.closed_!

        i += 1
      }

      if (alsoSave) {
        val (toInsert, toUpdate) = mfsToClose.partition(_.isTransient)
        freq match {
          case TFreq.DAILY =>
            log.info(this.code + " closed, inserting " + freq + " moneyflows: " + mfsToClose.length)
            if (toInsert.length > 0) MoneyFlows1d.insertBatch_!(toInsert.toArray)
            if (toUpdate.length > 0) MoneyFlows1d.updateBatch_!(toUpdate.toArray)
          case TFreq.ONE_MIN =>
            if (toInsert.length > 0) MoneyFlows1m.insertBatch_!(toInsert.toArray)
            if (toUpdate.length > 0) MoneyFlows1m.updateBatch_!(toUpdate.toArray)
        }

      }
    }
    
    if (willCommit) {
      commit
      log.info(this.code + " doClosing: committed.")
    }
  }

  override def toString: String = {
    code + "(" + timeZone.getDisplayName + ")" + ", open/close: " + openCloseHMs.mkString("(", ",", ")")
  }

  override def equals(another: Any) = another match {
    case Some(x: Exchange) => this.code == x.code
    case _ => false
  }

  override def hashCode = this.code.hashCode

  def compare(another: Exchange): Int = {
    (this.code, another.code) match {
      case ("-", "-") => 0
      case ("-",  _ ) => -1
      case (_  , "-") => 1
      case (s1: String, s2: String) => s2.compare(s1) // "SZ" > "SS" > "N" > "L" > "HK"
      case _ => 0
    }
  }

}

