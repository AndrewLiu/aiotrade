/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of AIOTrade Computing Co. nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.securities.model

import java.util.logging.Level
import java.util.logging.Logger
import java.util.{Calendar, TimeZone, ResourceBundle, Timer, TimerTask}
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.util.actors.Publisher

import org.aiotrade.lib.util.pinyin.PinYin
import ru.circumflex.orm._
import scala.collection.mutable

case class SecsAddedToDb(secs: Array[Sec])
case class SecInfosAddedToDb(secInfo: Array[SecInfo])
case class SecAdded(sec: Sec)
case class SecInfoAdded(secInfo: SecInfo)

trait TradingStatus {
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


class Exchange extends CRCLongId with Ordered[Exchange] {
  import Exchange._

  private val log = Logger.getLogger(this.getClass.getName)

  // --- database fields
  var code: String = "SS"
  var name: String = ""
  var fullName: String = ""
  var timeZoneStr: String = "Asia/Shanghai"
  var openCloseHMs: Array[Int] = Array(9, 30, 11, 30, 13, 0, 15, 0)

  var closeDates: List[ExchangeCloseDate] = Nil
  var secs: List[Sec] = Nil
  // --- end database fields

  log.info("Create exchange: identityHashCode=" + System.identityHashCode(this))

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

  private lazy val _uniSymbolToLastTicker = {
    val symbolToTicker = mutable.Map[String, Ticker]()

    for ((sec, ticker) <- TickersLast.lastTickersOf(this) if sec != null) {
      val symbol = sec.uniSymbol
      ticker.uniSymbol = symbol
      symbolToTicker.put(symbol, ticker)
    }

    symbolToTicker
  }
  
  private lazy val _uniSymbolToLastTradingDayTicker = {
    val symbolToTicker = mutable.Map[String, Ticker]()

    for ((sec, ticker) <- TickersLast.lastTradingDayTickersOf(this) if sec != null) {
      val symbol = sec.uniSymbol
      ticker.uniSymbol = symbol
      symbolToTicker.put(symbol, ticker)
    }

    symbolToTicker
  }

  private val freqToUnclosedQuotes = mutable.Map[TFreq, ArrayList[Quote]]()
  private val freqToUnclosedMoneyFlows = mutable.Map[TFreq, ArrayList[MoneyFlow]]()
  private val freqToUnclosedPriceDistributions = mutable.Map[TFreq, ArrayList[PriceCollection]]()

  private var _lastDailyRoundedTradingTime: Option[Long] = None

  def uniSymbolToLastTicker: collection.Map[String, Ticker] = _uniSymbolToLastTicker synchronized {_uniSymbolToLastTicker}
  def uniSymbolToLastTradingDayTicker: collection.Map[String, Ticker] = _uniSymbolToLastTradingDayTicker synchronized {_uniSymbolToLastTradingDayTicker}
  def uniSymbols: collection.Set[String] = Exchange.symbolsOf(this)
  
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
    val uniSymbol = ticker.uniSymbol

    _uniSymbolToLastTicker synchronized {
      _uniSymbolToLastTicker.get(uniSymbol) match {
        case Some(existOne) =>
          existOne.copyFrom(ticker)
          existOne.isTransient = TickersLast.transient_?(existOne)
          existOne
        case None =>
          val newOne = new Ticker
          newOne.isTransient = true
          newOne.copyFrom(ticker)
          _uniSymbolToLastTicker.put(uniSymbol, newOne)
          _uniSymbolToLastTradingDayTicker synchronized {
            _uniSymbolToLastTradingDayTicker.put(uniSymbol, newOne)
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
    (freqToUnclosedQuotes.get(freq) getOrElse {
        val xs = new ArrayList[Quote]
        freqToUnclosedQuotes.put(freq, xs)
        xs
      }
    ) += quote
  }

  def addNewMoneyFlow(freq: TFreq, mf: MoneyFlow) = freqToUnclosedMoneyFlows synchronized {
    (freqToUnclosedMoneyFlows.get(freq) getOrElse {
        val xs = new ArrayList[MoneyFlow]
        freqToUnclosedMoneyFlows.put(freq, xs)
        xs
      }
    ) += mf
  }

  def addNewPriceDistribution(freq: TFreq, pd: PriceCollection) = freqToUnclosedPriceDistributions synchronized {
    (freqToUnclosedPriceDistributions.get(freq) getOrElse {
        val xs = new ArrayList[PriceCollection]
        freqToUnclosedPriceDistributions.put(freq, xs)
        xs
      }
    ) += pd
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

    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(time)
    val timeInMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    
    if (this == SZ || this == SS) {
      tradingStatusCN(timeInMinutes, time) match {
        case Some(status) => return status
        case None =>
      }
    }
    
    var status: TradingStatus = null
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
        var i = -1
        while ({i += 1; i < openingPeriods.length && status == null}) {
          val openingPeriod = openingPeriods(i)
          if (timeInMinutes >= openingPeriod._1 && timeInMinutes <= openingPeriod._2) {
            status = Opening(time, timeInMinutes)
          }
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
  private val emptyPriceDistributions = ArrayList[PriceCollection]()
  private val dailyCloseDelay = 5 * 60 * 1000 // 5 minutes
  private var timeInMinutesToClose = -1
  private val closingTimer = new Timer("ExchangeClosingTimer")
  
  private def isClosed(freq: TFreq, tradingStatusTime: Long, roundedTime: Long) = {
    tradingStatusTime >= roundedTime + freq.interval
  }

  /**
   * Do closing in delay minutes. If quotesToClose/mfsToClose is empty, will do
   * nothing and return at once.
   */
  private[securities] def tryClosing(alsoSave: Boolean) {
    import TradingStatus._

    val closeTimeInMinutes = timeInMinutesToClose
    val statusTime = tradingStatus.time

    val freqs = tradingStatus match {
      
      case Opening(time, timeInMinutes) if timeInMinutesToClose <= 0 || timeInMinutes <  timeInMinutesToClose =>
        // When processing legacy data, 'timeInMinutesToClose' may be set to previous date's larger timeInMinutes,
        // so we add '|| timeInMinutes <  timeInMinutesToClose'
        timeInMinutesToClose = timeInMinutes + 1
        Nil
      case Opening(time, timeInMinutes) if timeInMinutesToClose >  0 && timeInMinutes >= timeInMinutesToClose =>
        // a new minute begins, will doClose on ONE_MIN after 1 minute
        timeInMinutesToClose = timeInMinutes + 1
        List(TFreq.ONE_MIN)
        
      case Close(time, timeInMinutes) => 
        timeInMinutesToClose = -1
        List(TFreq.ONE_MIN, TFreq.DAILY)
      case Closed(time, timeInMinutes) =>
        timeInMinutesToClose = -1
        List(TFreq.ONE_MIN, TFreq.DAILY)
        
      case _ => Nil
    }

    log.info("Try closing quotes of freqs: " + freqs + ", closingTimeInMinutes=" + closeTimeInMinutes)

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

      val pdsToClose = freqToUnclosedPriceDistributions synchronized {
        freqToUnclosedPriceDistributions.get(freq) match {
          case Some(unclosed) if freq == TFreq.DAILY =>
            freqToUnclosedPriceDistributions.put(freq, emptyPriceDistributions)
            log.info("price distribution unclosed," + TFreq.DAILY.name)
            unclosed
          case Some(unclosed) =>
            val (toClose, other) = unclosed.partition{x => isClosed(freq, statusTime, x.time)}
            freqToUnclosedPriceDistributions.put(freq, other)
            log.info("price distribution unclosed, Other freq")
            toClose
          case None => emptyPriceDistributions
        }
      }
      log.info("price distribution collection length: " + pdsToClose.length )

      if (quotesToClose.length > 0 || mfsToClose.length > 0 || pdsToClose.length > 0) {
        val isDailyClose = freqs.contains(TFreq.DAILY)
        if (isDailyClose) {
          log.info(this.code + " will do closing in " + (dailyCloseDelay / 60 / 1000) + " minutes for (" + freq + "), quotes=" + quotesToClose.length + ", mfs=" + mfsToClose.length + ", pds=" + pdsToClose.length)
         closingTimer.schedule(new TimerTask {
              def run {
                doClosing(freq, quotesToClose, mfsToClose, pdsToClose, alsoSave)
              }
            }, dailyCloseDelay)
        } else {
          doClosing(freq, quotesToClose, mfsToClose, pdsToClose, alsoSave)
        }
      }
    }
  }

  /**
   * Close and insert daily quotes/moneyflows
   */
  private def doClosing(freq: TFreq, quotesToClose: ArrayList[Quote], mfsToClose: ArrayList[MoneyFlow], pdsToClose: ArrayList[PriceCollection], alsoSave: Boolean) {
    var willCommit = false

    if (quotesToClose.length > 0) {
      willCommit = true

      var i = -1
      while ({i += 1; i < quotesToClose.length}) {
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

      var i = -1
      while ({i += 1; i < mfsToClose.length}) {
        val mfs = mfsToClose(i)
        mfs.closed_!
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

    if (pdsToClose.length > 0) {
      willCommit = true

      var i = -1
      while ({i += 1; i < pdsToClose.length}) {
        val pd = pdsToClose(i)
        pd.closed_!
      }

      if (alsoSave) {
        val (toInsert, toUpdate) = pdsToClose.partition(_.isTransient)

        var inserts = new ArrayList[PriceDistribution]()
        toInsert.foreach{pc =>
          pc.values.foreach{pd =>
            inserts += pd
          }
        }

        var updates = new ArrayList[PriceDistribution]()
        toUpdate.foreach{pc =>
          pc.values.foreach{pd =>
            updates += pd
          }
        }

        freq match {
          case TFreq.DAILY =>
            log.info(this.code + " closed, inserting " + freq + " price distributions: " + pdsToClose.length)
            if (inserts.length > 0) PriceDistributions.insertBatch_!(inserts.toArray)
            if (updates.length > 0) PriceDistributions.updateBatch_!(updates.toArray)
        }

      }
    }

    if (willCommit) {
      COMMIT
      log.info(this.code + " doClosing: committed.")
    }
  }

  override def toString: String = {
    code + "(" + timeZone.getDisplayName + ")" + ", open/close=" + openCloseHMs.mkString("(", ",", ")") + ", identityHashCode=" + System.identityHashCode(this)
  }

  override def equals(that: Any) = that match {
    case x: Exchange => this.id == x.id
    case _ => false
  }

  def compare(that: Exchange): Int = {
    (this.code, that.code) match {
      case ("-", "-") => 0
      case ("-",  _ ) => -1
      case (_  , "-") => 1
      case (s1: String, s2: String) => s2.compare(s1) // "SZ" > "SS" > "N" > "L" > "HK"
      case _ => 0
    }
  }

}

object Exchange extends Publisher {
  private val log = Logger.getLogger(this.getClass.getName)

  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.securities.model.Bundle")
  private val ONE_DAY = 24 * 60 * 60 * 1000
  private val config = org.aiotrade.lib.util.config.Config()

  case class Opened(exchange: Exchange)
  case class Closed(exchange: Exchange)

  // ----- search tables, always use immutable collections to avoid sync issue
  private var _allExchanges: Seq[Exchange] = Nil
  private var _activeExchanges: Seq[Exchange] = Nil
  private var _codeToExchange = Map[String, Exchange]()
  private var _exchangeToSecs = Map[Exchange, Seq[Sec]]()
  private var _exchangeToUniSymbols = Map[Exchange, Set[String]]()
  private var _uniSymbolToSec = Map[String, Sec]()
  private var _searchTextToSecs = Map[String, Seq[Sec]]()

  
  // Init all searching tables
  allExchanges = Exchanges.all()

  // Init active exchanges
  activeExchanges = config.getList("market.exchanges") match {
    case Seq() => allExchanges
    case xs => xs flatMap {x => Exchange.withCode(x)}
  }

  lazy val N  = withCode("N" ).get
  lazy val SS = withCode("SS").get
  lazy val SZ = withCode("SZ").get
  lazy val L  = withCode("L" ).get
  lazy val HK = withCode("HK").get
  lazy val OQ = withCode("OQ").get

  def allExchanges = _allExchanges
  def allExchanges_=(allExchanges: Seq[Exchange]) {
    _allExchanges = allExchanges
    resetSearchTables
  }
  
  def resetSearchTables() {
    try {
      val t0 = System.currentTimeMillis
    
      _codeToExchange = Map() ++ (_allExchanges map {x => (x.code, x)})

      _exchangeToSecs = Exchanges.exchangeToSec()

      val exchgToSecInfos = Exchanges.exchangeToSecInfo()
      
      _exchangeToUniSymbols = exchgToSecInfos map {case (exchg, secInfos) => (exchg, secInfos map (_.uniSymbol.toUpperCase) toSet)}
    
      _uniSymbolToSec = exchgToSecInfos flatMap {case (exchg, secInfos) => secInfos map (x => (x.uniSymbol.toUpperCase, x.sec))}
    
      // deal with the worst case: a sec has been in db (with crckey) but lacks secInfo (wrongly deleted or other cases)
      // we should add these crckey as unisymbol, to avoid a new sec being created on this crckey(unisymol)
      for ((exchg, secs) <- _exchangeToSecs) {
        _exchangeToUniSymbols += (exchg -> (_exchangeToUniSymbols.getOrElse(exchg, Set()) ++ (secs map (_.crckey.toUpperCase))))
        _uniSymbolToSec ++= (secs map (sec => sec.crckey.toUpperCase -> sec))
      }
      
      // _searchTextToSecs
      for ((symbol, sec) <- _uniSymbolToSec) {
        _searchTextToSecs ++= Map(symbol.toUpperCase -> List(sec)) ++ (
          PinYin.getFirstSpells(sec.name) map {spell => (spell, _searchTextToSecs.getOrElse(spell, Nil) :+ sec)}
        )
      }
      
      log.info("Reset search table in " + (System.currentTimeMillis - t0) + "ms.")
    } catch {
      case ex => log.severe(ex.getMessage)
    }
  }

  def activeExchanges = _activeExchanges
  def activeExchanges_=(activeExchanges: Seq[Exchange]) {
    _activeExchanges = activeExchanges
  }

  def codeToExchange: Map[String, Exchange] = _codeToExchange
  def exchangeToSecs: Map[Exchange, Seq[Sec]] = _exchangeToSecs
  def exchangeToUniSymbols: Map[Exchange, Set[String]] = _exchangeToUniSymbols
  def uniSymbolToSec: Map[String, Sec] = _uniSymbolToSec
  def searchTextToSecs: Map[String, Seq[Sec]] = _searchTextToSecs

  // ----- Major methods

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

  def exchangeOfIndex(uniSymbol: String): Option[Exchange] = {
    uniSymbol match {
      case "^DJI" => Some(N)
      case "^HSI" => Some(HK)
      case "000001.SS" => Some(SS)
      case "399001.SZ" => Some(SZ)
      case _=> None
    }
  }

  def secsOf(exchange: Exchange): Seq[Sec] = {
    exchangeToSecs.getOrElse(exchange, Nil)
  }

  def symbolsOf(exchange: Exchange): Set[String] = {
    exchangeToUniSymbols.getOrElse(exchange, Set())
  }

  def secOf(uniSymbol: String): Option[Sec] = {
    uniSymbolToSec.get(uniSymbol.toUpperCase)
  }

  def checkIfSomethingNew(tickers: Array[Ticker]) {
    val symbol_name_xs = new ArrayList[(String, String)]
    val sec_symbol_name_xs = new ArrayList[(Sec, String, String)]
    
    var i = -1
    while ({i += 1; i < tickers.length}) {
      val ticker = tickers(i)
      val uniSymbol = ticker.uniSymbol.toUpperCase
      val name = ticker.name
      if (name != null && name.trim != "") {
        uniSymbolToSec.get(uniSymbol) match {
          case Some(sec) =>
            val secInfo = sec.secInfo
            if (secInfo == null || secInfo.name != name) {
              log.info("Found new name of symbol: " + uniSymbol + ", new name: " + name + (if (secInfo != null) ", old name: " + secInfo.name else ", no secInfo yet"))
              sec_symbol_name_xs += ((sec, uniSymbol, name))
            }
          case None =>
            log.info("Found new symbol: " + uniSymbol + ", name: " + name)
            symbol_name_xs += ((uniSymbol, name))
        }
      }
    }
    
    val secs = Exchanges.createSimpleSecs(symbol_name_xs.toArray, true)
    val secInfos = Exchanges.createSimpleSecInfos(sec_symbol_name_xs.toArray, true)
    
    if (secs.length > 0) {
      publish(SecsAddedToDb(secs))
      var i = -1
      while ({i += 1; i < secs.length}) {
        secAdded(secs(i))
      }
    }
    
    if (secInfos.length > 0) {
      publish(SecInfosAddedToDb(secInfos))
    }
  }

  def secAdded(uniSymbol: String): Sec = {
    // check database if sec has been here, if not, something was wrong
    Exchanges.secOf(uniSymbol) match {
      case Some(sec) => secAdded(sec)
      case None => log.severe("Sec of " + uniSymbol + " has not been created in database"); null
    }
  }

  private def secAdded(sec: Sec): Sec = {
    val exchg = sec.exchange
    val symbol= sec.uniSymbol

    _exchangeToUniSymbols += (exchg -> (_exchangeToUniSymbols.getOrElse(exchg, Set()) + symbol))
    _exchangeToSecs += (exchg -> (_exchangeToSecs.getOrElse(exchg, Nil) :+ sec))
    _uniSymbolToSec += (symbol -> sec)

    publish(SecAdded(sec))
    sec
  }
  
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

// --- table
object Exchanges extends CRCLongPKTable[Exchange] {
  private val log = Logger.getLogger(this.getClass.getName)
  private val config = org.aiotrade.lib.util.config.Config()
  private val isServer = !config.getBool("dataserver.client", false)

  val code = "code" VARCHAR(4)
  val name = "name" VARCHAR(10)
  val fullName = "fullName" VARCHAR(30)
  val timeZoneStr = "timeZoneStr" VARCHAR(30)
  val openCloseHMs = "openCloseHMs" SERIALIZED(classOf[Array[Int]], 100)

  def closeDates = inverse(ExchangeCloseDates.exchange)
  def secs = inverse(Secs.exchange)

  val codeIdx = getClass.getSimpleName + "_code_idx" INDEX(code.name)

  // --- helper methods
  def secsOf(exchange: Exchange): Seq[Sec] = {
    val exchangeId = Exchanges.idOf(exchange)
    val t0 = System.currentTimeMillis
    
    val secs = if (isServer) {
      SELECT (Secs.*, SecInfos.*) FROM (Secs JOIN SecInfos) WHERE (Secs.exchange.field EQ exchangeId) list() map (_._1)
    } else {
      val secInfos = SELECT (SecInfos.*) FROM (AVRO(SecInfos)) list()
      SELECT (Secs.*) FROM (AVRO(Secs)) list() filter (sec => sec.exchange.code == exchange.code)
    }
    
    log.info("Secs number of " + exchange.code + "(id=" + exchangeId + ") is " + secs.size + ", loaded in " + (System.currentTimeMillis - t0) + " ms")
    
    secs
  }

  def exchangeToSec(): Map[Exchange, Seq[Sec]] = {
    val t0 = System.currentTimeMillis
    
    val secs = if (isServer) {
      SELECT (Secs.*, SecInfos.*) FROM (Secs JOIN SecInfos) list() map (_._1)
    } else {
      val secInfos = SELECT (SecInfos.*) FROM (AVRO(SecInfos)) list()
      SELECT (Secs.*) FROM (AVRO(Secs)) list()
    }
    
    log.info("Secs number is "  + secs.size + ", loaded in " + (System.currentTimeMillis - t0) + " ms")
    
    secs.groupBy(x => x.exchange)
  }

  def exchangeToSecInfo(): Map[Exchange, Seq[SecInfo]] = {
    val t0 = System.currentTimeMillis
    
    val secInfos = if (isServer) {
      SELECT (SecInfos.*, Secs.*) FROM (SecInfos JOIN Secs) list() map (_._1)
    } else {
      val secs = SELECT (Secs.*) FROM (AVRO(Secs)) list()
      SELECT (SecInfos.*) FROM (AVRO(SecInfos)) list()
    }
    
    log.info("SecInfos number " +  " is " + secInfos.size + ", loaded in " + (System.currentTimeMillis - t0) + " ms")

    secInfos.groupBy(x => x.sec.exchange)
  }

  /**
   * @Note expensive method.
   * private package method to avoid to be used no-aware expensive
   */
  private[model] def secOf(uniSymbol: String): Option[Sec] = {
    (SELECT (Secs.*, SecInfos.*) FROM (Secs JOIN SecInfos) WHERE (SecInfos.uniSymbol EQ uniSymbol) unique) map (_._1)
  }
  
  def dividendsOf(sec: Sec): Seq[SecDividend] = {
    val divs = if (TickerServer.isServer) {
      val secId = Secs.idOf(sec)
      SELECT (SecDividends.*) FROM (SecDividends) WHERE (SecDividends.sec.field EQ secId) list()
    } else {
      SELECT (SecDividends.*) FROM (AVRO(SecDividends)) list() filter (div => div.sec eq sec)
    }

    val cal = Calendar.getInstance(sec.exchange.timeZone)
    divs foreach {div => div.dividendDate = TFreq.DAILY.round(div.dividendDate, cal)}
    divs.sortWith((a, b) => a.dividendDate < b.dividendDate)
  }

  def createSimpleSec(uniSymbol: String, name: String, willCommit: Boolean = false) = {
    val symbol = uniSymbol.toUpperCase
    val exchange = Exchange.exchangeOf(symbol)
    val sec = new Sec
    sec.crckey = symbol
    sec.exchange = exchange
    Secs.save_!(sec)

    val secInfo = new SecInfo
    secInfo.sec = sec
    secInfo.uniSymbol = symbol
    secInfo.name = name
    SecInfos.save_!(secInfo)

    sec.secInfo = secInfo
    Secs.update_!(sec, Secs.secInfo)

    if (willCommit) {
      COMMIT
      log.info("Committed: sec_infos" + name)
    }

    sec
  }

  def createSimpleSecInfo(sec: Sec, name: String, isCurrent: Boolean = true): SecInfo = {
    val secInfo = new SecInfo
    secInfo.sec = sec
    secInfo.uniSymbol = sec.uniSymbol
    secInfo.name = name
    SecInfos.save_!(secInfo)

    if (isCurrent) {
      sec.secInfo = secInfo
      Secs.update_!(sec, Secs.secInfo)
    }

    COMMIT
    log.info("Committed: sec_infos" + name)

    secInfo
  }

  def createSimpleSecs(symbol_name_xs: Array[(String, String)], willCommit: Boolean = false): Array[Sec] = {
    val secInfos = new ArrayList[SecInfo]
    val secs = new ArrayList[Sec]
    
    for ((uniSymbol, name) <- symbol_name_xs) {
      val sec = new Sec
      sec.crckey = uniSymbol
      sec.exchange = Exchange.exchangeOf(uniSymbol)
      secs += sec

      val secInfo = new SecInfo
      secInfo.sec = sec
      secInfo.uniSymbol = uniSymbol
      secInfo.name = name
      secInfos += secInfo

      sec.secInfo = secInfo
    }
    
    val secInfosA = secInfos.toArray
    val secsA = secs.toArray
    try {
      Secs.insertBatch_!(secsA, false) // do not use auto increamnent id
      SecInfos.insertBatch_!(secInfosA)
      Secs.updateBatch_!(secsA, Secs.secInfo)
    
      if (willCommit && (secsA.length > 0 | secInfosA.length > 0)) {
        COMMIT
        log.info("Committed secs: " + secsA.length + ", sec_infos: " + secInfosA.length)
      }
      
      secsA
    } catch {
      case ex => log.log(Level.WARNING, ex.getMessage, ex); Array()
    }
    
  }
  
  def createSimpleSecInfos(sec_symbol_name_xs: Array[(Sec, String, String)], isCurrent: Boolean = true): Array[SecInfo] = {
    val secInfos = new ArrayList[SecInfo]
    val secs = new ArrayList[Sec]
    
    for ((sec, uniSymbol, name) <- sec_symbol_name_xs) {
      val secInfo = new SecInfo
      secInfo.sec = sec
      secInfo.uniSymbol = uniSymbol // we cannot trust sec.uniSymbol here, since it may lack secInfo 
      secInfo.name = name
      secInfos += secInfo

      if (isCurrent) {
        sec.secInfo = secInfo
        secs += sec
      }
    }
    
    val secInfosA = secInfos.toArray
    val secsA = secs.toArray
    try {
      SecInfos.insertBatch_!(secInfosA)
      Secs.updateBatch_!(secsA, Secs.secInfo)
    
      if (secsA.length > 0 | secInfosA.length > 0) {
        COMMIT
        log.info("Committed secs: " + secsA.length + ", sec_infos: " + secInfosA.length)
      }
    
      secInfosA
    } catch {
      case ex => log.log(Level.WARNING, ex.getMessage, ex); Array()
    }
  }
  
}


