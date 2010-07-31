/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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

import java.util.Calendar
import org.aiotrade.lib.math.indicator.Indicator
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.datasource.SerProvider
import org.aiotrade.lib.securities.MoneyFlowSer
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.QuoteSerCombiner
import org.aiotrade.lib.securities.TickerSnapshot
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.util.actors.Publisher
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import scala.collection.mutable.HashMap
import ru.circumflex.orm.Table
import scala.collection.mutable.ListBuffer


object Secs extends Table[Sec] {
  val exchange = "exchanges_id" REFERENCES(Exchanges)

  val validFrom = "validFrom" BIGINT 
  val validTo = "validTo" BIGINT

  val company = "companies_id" REFERENCES(Companies)
  def companyHists = inverse(Companies.sec)

  val secInfo = "secInfos_id" REFERENCES(SecInfos)
  def secInfoHists = inverse(SecInfos.sec)
  val secStatus = "secStatuses_id" REFERENCES(SecStatuses)
  def secStatusHists = inverse(SecStatuses.sec)

  val secIssue = "secIssues_id" REFERENCES(SecIssues)
  def secDividends = inverse(SecDividends.sec)

  def dailyQuotes = inverse(Quotes1d.sec)
  def dailyMoneyFlow = inverse(MoneyFlows1d.sec)

  def minuteQuotes = inverse(Quotes1m.sec)
  def minuteMoneyFlow = inverse(MoneyFlows1m.sec)
}


/**
 * Securities: Stock, Options, Futures, Index, Currency etc.
 *
 * An implement of Sec.
 * each sofic has a default quoteSer and a tickerSer which will be created in the
 * initialization. The quoteSer will be put in the freq-ser map, the tickerSer
 * won't be.
 * You may put ser from outside, to the freq-ser map, so each sofic may have multiple
 * freq sers, but only per freq pre ser is allowed.
 *
 * @param uniSymbol a globe uniSymbol, may have different source uniSymbol.

 * @author Caoyuan Deng
 */

object Sec {
  val log = Logger.getLogger(this.getClass.getName)

  trait Kind
  object Kind {
    case object Stock extends Kind
    case object Index extends Kind
    case object Option extends Kind
    case object Future extends Kind
    case object FutureOption extends Kind
    case object Currency extends Kind
    case object Bag extends Kind
    case object Bonds extends Kind
    case object Equity extends Kind

    def withName(name: String): Kind = {
      name match {
        case "Stock" => Stock
        case "Index" => Index
        case "Option" => Option
        case "Future" => Future
        case "FutureOption" => FutureOption
        case "Currency" => Currency
        case "Bag" => Bag
        case _ => null
      }
    }
  }

  val basicFreqs = List(TFreq.DAILY, TFreq.ONE_MIN)

  val dailyQuotesToClose = new ArrayList[Quote]()
  val dailyMoneyFlowsToClose = new ArrayList[MoneyFlow]()
  val minuteQuotesToClose = new ArrayList[Quote]()
  val minuteMoneyFlowsToClose = new ArrayList[MoneyFlow]()
}

import Sec._
class Sec extends SerProvider with Publisher {
  // --- database fields
  var exchange: Exchange = _

  var validFrom: Long = 0
  var validTo: Long = 0

  var company: Company = _
  var companyHists: List[Company] = Nil

  var secInfo: SecInfo = _
  var secInfoHists: List[SecInfo] = Nil
  var secStatus: SecStatus = _
  var secStatusHists: List[SecStatus] = Nil

  var secIssue: SecIssue = _
  var secDividends: List[SecDividend] = Nil

  var dailyQuotes: List[Quote] = Nil
  var dailyMoneyFlow: List[MoneyFlow] = Nil

  var minuteQuotes: List[Quote] = Nil
  var minuteMoneyFlow: List[MoneyFlow] = Nil

  // --- end of database fields

  type C = QuoteContract
  type T = QuoteSer

  val freqToQuoteSer = HashMap[TFreq, QuoteSer]()
  private val freqToQuoteContract = HashMap[TFreq, QuoteContract]()
  private lazy val freqToMoneyFlowSer = HashMap[TFreq, MoneyFlowSer]()
  private lazy val freqToIndicators = HashMap[TFreq, ListBuffer[Indicator]]()

  var description = ""
  var name = ""
  private var _defaultFreq: TFreq = _
  private var _quoteContracts: Seq[QuoteContract] = Nil
  private var _tickerContract: TickerContract = _

  def defaultFreq = if (_defaultFreq == null) TFreq.DAILY else _defaultFreq

  def quoteContracts = _quoteContracts
  def quoteContracts_=(quoteContracts: Seq[QuoteContract]) {
    _quoteContracts = quoteContracts
    createFreqSers
  }

  /** create freq sers */
  private def createFreqSers {
    for (contract <- quoteContracts) {
      val freq = contract.freq
      if (_defaultFreq == null) {
        _defaultFreq = freq
      }

      freqToQuoteContract.put(freq, contract)
      if (!freqToQuoteSer.contains(freq)) {
        freqToQuoteSer.put(freq, new QuoteSer(this, freq))
      }
    }
    
    // basic freqs:
    for (freq <- basicFreqs if !freqToQuoteSer.contains(freq)) {
      freqToQuoteSer.put(freq, new QuoteSer(this, freq))
    }
  }

  /** tickerContract will always be built according to quoteContrat ? */
  def tickerContract = {
    if (_tickerContract == null) {
      _tickerContract = new TickerContract
    }
    _tickerContract
  }
  def tickerContract_=(tickerContract: TickerContract) {
    _tickerContract = tickerContract
  }

 
  /**
   * @TODO, how about tickerServer switched?
   */
  private lazy val tickerServer: TickerServer = tickerContract.serviceInstance().get

  reactions += {
    case TSerEvent.FinishedLoading(srcSer, _, fromTime, endTime, _, _) =>
      // contract quoteServer of freq centernly still exists only under this type of event
      val freq = srcSer.freq
      for (quoteContract <- quoteContractOf(defaultFreq);
           quoteServer <- quoteContract.serviceInstance()
      ) {
        srcSer.loaded = true
        if (quoteContract.refreshable) {
          quoteServer.startRefresh(quoteContract.refreshInterval)
        } else {
          quoteServer.unSubscribe(quoteContract)
        }

        deafTo(srcSer)
      }
    case _ =>
  }

  def serOf(freq: TFreq): Option[QuoteSer] = freq match {
    case TFreq.ONE_SEC | TFreq.ONE_MIN | TFreq.DAILY => freqToQuoteSer.get(freq)
    case _ => freqToQuoteSer.get(freq) match {
        case None => createCombinedSer(freq)
        case x => x
      }
  }

  def indicatorsOf[A <: Indicator](clazz: Class[A], freq: TFreq): Seq[A] = {
    freqToIndicators.get(freq) match {
      case None => Nil
      case Some(inds) => (inds filter (clazz.isInstance(_))).asInstanceOf[Seq[A]]
    }
  }

  def addIndicator(indicator: Indicator) {
    val freq = indicator.freq
    val indicators = freqToIndicators.get(freq) getOrElse {
      val x = new ListBuffer[Indicator]
      freqToIndicators += (freq -> x)
      x
    }
    indicators += indicator
  }

  def removeIndicator(indicator: Indicator) {
    val freq = indicator.freq
    freqToIndicators.get(freq) match {
      case Some(indicators) => indicators -= indicator
      case None =>
    }
  }

  def moneyFlowSerOf(freq: TFreq): Option[MoneyFlowSer] = freq match {
    case TFreq.ONE_SEC | TFreq.ONE_MIN | TFreq.DAILY => freqToMoneyFlowSer.get(freq) match {
        case None => serOf(freq) match {
            case Some(quoteSer) => 
              val x = new MoneyFlowSer(this, freq)
              freqToMoneyFlowSer.put(freq, x)
              Some(x)
            case None => None
          }
        case some => some
      }
    case _ => freqToMoneyFlowSer.get(freq) match {
        case None => None // @todo createCombinedSer(freq)
        case some => some
      }
  }
  
  /**
   * @Note
   * here should be aware that if sec's ser has been loaded, no more
   * SerChangeEvent.Type.FinishedLoading will be fired, so if we create followed
   * viewContainers here, should make sure that the QuoteSerCombiner listen
   * to SeriesChangeEvent.FinishingCompute or SeriesChangeEvent.FinishingLoading from
   * sec's ser and computeFrom(0) at once.
   */
  private def createCombinedSer(freq: TFreq): Option[QuoteSer] = {
    val srcSer_? = freq.unit match {
      case TUnit.Day | TUnit.Week | TUnit.Month | TUnit.Year => serOf(TFreq.DAILY)
      case _ => serOf(TFreq.ONE_MIN)
    }

    srcSer_? match {
      case Some(srcSer) =>
        val tarSer = new QuoteSer(this, freq)
        val combiner = new QuoteSerCombiner(srcSer, tarSer, exchange.timeZone)
        combiner.computeFrom(0) // don't remove me, see notice above.
        putSer(tarSer)
        Some(tarSer)
      case None => None
    }
  }

  def putSer(ser: QuoteSer) {
    freqToQuoteSer.put(ser.freq, ser)
  }

  /**
   * All quotes in persistence should have been properly rounded to 00:00 of exchange's local time
   */
  def loadSerFromPersistence(freq: TFreq): Long = {
    val quotes = freq match {
      case TFreq.DAILY   => Quotes1d.closedQuotesOf(this)
      case TFreq.ONE_MIN => Quotes1m.closedQuotesOf(this)
      case _ => return 0L
    }

    val ser = serOf(freq).get
    ser ++= quotes.toArray

    val uniSymbol = secInfo.uniSymbol
    /**
     * get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!quotes.isEmpty) {
      val (first, last) = if (quotes.head.time < quotes.last.time)
        (quotes.head, quotes.last)
      else
        (quotes.last, quotes.head)

      // should load earlier quotes from data source? first.fromMe_? may means never load from data server
      val wantTime = if (first.fromMe_?) 0 else last.time
      ser.publish(TSerEvent.RefreshInLoading(ser, uniSymbol, first.time, last.time))
      log.info(uniSymbol + "(" + freq + "): loaded from persistence, got quotes=" + quotes.length +
               ", loaded: time=" + last.time + ", freq=" + ser.freq + ", size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )
      
      wantTime
    } else {
      log.info(uniSymbol + "(" + freq + "): loaded from persistence, got quotes=" + quotes.length + ", loaded: time=" + 0L + ", freq=" + ser.freq + ", size=" + ser.size)
      0L
    }
  }

  /**
   * All values in persistence should have been properly rounded to 00:00 of exchange's local time
   */
  def loadMoneyFlowSerFromPersistence(freq: TFreq): Long = {
    val mfs = freq match {
      case TFreq.DAILY   => MoneyFlows1d.closedMoneyFlowOf(this)
      case TFreq.ONE_MIN => MoneyFlows1m.closedMoneyFlowOf(this)
      case _ => return 0L
    }

    val ser = moneyFlowSerOf(freq).get
    ser ++= mfs.toArray
    
    /**
     * get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!mfs.isEmpty) {
      val (first, last) = if (mfs.head.time < mfs.last.time)
        (mfs.head, mfs.last)
      else
        (mfs.last, mfs.head)

      // should load earlier quotes from data source?
      val wantTime = if (first.fromMe_?) 0 else last.time
      ser.publish(TSerEvent.RefreshInLoading(ser, uniSymbol, first.time, last.time))
      log.info(uniSymbol + "(" + freq + "): loaded from persistence, got quotes=" + mfs.length +
               ", loaded: time=" + last.time + ", freq=" + ser.freq + ", size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )

      wantTime
    } else 0L
  }


  /**
   * synchronized this method to avoid conflict on variable: loadBeginning and
   * concurrent accessing to varies maps.
   */
  def loadSer(freq: TFreq): Boolean = synchronized {
    val ser = serOf(freq) getOrElse (return false)

    // load from persistence
    val wantTime = loadSerFromPersistence(freq)

    // try to load from quote server

    for (contract <- quoteContractOf(freq);
         quoteServer <- contract.serviceInstance()
    ) {
      contract.freq = freq
      contract.ser = ser
      quoteServer.subscribe(contract)

      ser.inLoading = true
      quoteServer.loadHistory(wantTime)

      listenTo(ser)
      return true
    }
    
    false
  }

  private def quoteContractOf(freq: TFreq): Option[QuoteContract] = {
    freqToQuoteContract.get(freq) match {
      case None => freqToQuoteContract.get(defaultFreq) match {
          case Some(defaultOne) if defaultOne.isFreqSupported(freq) =>
            val x = new QuoteContract
            x.freq = freq
            x.refreshable = false
            x.srcSymbol = defaultOne.srcSymbol
            x.serviceClassName = defaultOne.serviceClassName
            x.dateFormatPattern =(defaultOne.dateFormatPattern)
            freqToQuoteContract.put(freq, x)
            Some(x)
          case _ => None
        }
      case some => some
    }
  }

  def isSerLoaded(freq: TFreq): Boolean = {
    freqToQuoteSer.get(freq) map (_.loaded) getOrElse false
  }

  def isSerInLoading(freq: TFreq): Boolean = {
    freqToQuoteSer.get(freq) map (_.inLoading) getOrElse false
  }

  def uniSymbol: String = if (secInfo != null) secInfo.uniSymbol else ""
  def uniSymbol_=(uniSymbol: String) {
    if (secInfo != null) {
      secInfo.uniSymbol = uniSymbol
    }
    name = uniSymbol.replace('.', '_')
  }

  def stopAllDataServer {
    for ((freq, contract) <- freqToQuoteContract;
         server <- contract.serviceInstance()
    ) {
      server.stopRefresh
    }
  }

  def clearSer(freq: TFreq) {
    serOf(freq) foreach {ser =>
      ser.clear(0)
      ser.loaded = false
    }
  }

  override def toString: String = {
    "Sec(Company=" + company + ", info=" + secInfo + ")"
  }

  def dataContract: QuoteContract = freqToQuoteContract(defaultFreq)
  def dataContract_=(quoteContract: QuoteContract) {
    val freq = quoteContract.freq
    freqToQuoteContract += (freq -> quoteContract)
  }

  def subscribeTickerServer(startRefresh: Boolean = true): Option[TickerServer] = {
    if (uniSymbol == "") return None

    // always set uniSymbol, since _tickerContract may be set before secInfo.uniSymbol
    tickerContract.srcSymbol = uniSymbol

    if (tickerContract.serviceClassName == null) {
      for (quoteContract <- quoteContractOf(defaultFreq);
           quoteServer <- quoteContract.serviceInstance();
           clz <- quoteServer.classOfTickerServer
      ) {
        tickerContract.serviceClassName = clz.getName
      }
    }

    if (tickerContract.serviceClassName != null) {
      if (!startRefresh) {
        tickerServer.stopRefresh
      }

      if (!tickerServer.isContractSubsrcribed(tickerContract)) {
        val minSer = serOf(TFreq.ONE_MIN).get
        if (!isSerLoaded(TFreq.ONE_MIN)) {
          loadSerFromPersistence(TFreq.ONE_MIN)
        }
        // Only dailySer and minuteSre needs to chainly follow ticker change.
        val chainSers = List(serOf(TFreq.DAILY).get)
        tickerContract.ser = minSer
        tickerContract.chainSers = chainSers
        tickerServer.subscribe(tickerContract)
      }

      if (startRefresh) {
        tickerServer.startRefresh(tickerContract.refreshInterval)
      }
    }

    Some(tickerServer)
  }

  def unSubscribeTickerServer {
    if (tickerServer != null && tickerContract != null) {
      tickerServer.unSubscribe(tickerContract)
    }
  }

  def isTickerServerSubscribed: Boolean = {
    tickerServer != null && tickerServer.isContractSubsrcribed(tickerContract)
  }

  
// --- helper methods for realtime composing

  /**
   * store latest helper info
   */
  private lazy val lastData = new LastData

  private class LastData {
    val tickerSnapshot: TickerSnapshot = new TickerSnapshot
    var prevTicker: Ticker = _
    
    var dailyQuote: Quote = _
    var minuteQuote: Quote = _
    
    var dailyMoneyFlow: MoneyFlow = _
    var minuteMoneyFlow: MoneyFlow = _
  }

  /**
   * @Note when day changes, should do LastData.dailyQuote to null, this can be done
   * by listening to exchange's timer event
   */
  def dailyQuoteOf(time: Long): Quote = {
    assert(Secs.idOf(this).isDefined, "Sec: " + this + " is transient")
    val cal = Calendar.getInstance(exchange.timeZone)
    val rounded = TFreq.DAILY.round(time, cal)
    lastData.dailyQuote match {
      case one: Quote if one.time == rounded =>
        one
      case prevOneOrNull => // day changes or null
        val newone = Quotes1d.dailyQuoteOf(this, rounded)
        lastData.dailyQuote = newone
        newone
    }
  }

  def dailyMoneyFlowOf(time: Long): MoneyFlow = {
    assert(Secs.idOf(this).isDefined, "Sec: " + this + " is transient")
    val cal = Calendar.getInstance(exchange.timeZone)
    val rounded = TFreq.DAILY.round(time, cal)
    lastData.dailyMoneyFlow match {
      case one: MoneyFlow if one.time == rounded =>
        one
      case prevOneOrNull => // day changes or null
        val newone = MoneyFlows1d.dailyMoneyFlowOf(this, rounded)
        lastData.dailyMoneyFlow = newone
        newone
    }
  }

  def minuteQuoteOf(time: Long): Quote = {
    val cal = Calendar.getInstance(exchange.timeZone)
    val rounded = TFreq.ONE_MIN.round(time, cal)
    lastData.minuteQuote match {
      case one: Quote if one.time == rounded =>
        one
      case prevOneOrNull => // minute changes or null
        if (prevOneOrNull != null) {
          prevOneOrNull.closed_!
          minuteQuotesToClose += prevOneOrNull
        }

        val newone = new Quote
        newone.time = rounded
        newone.sec = this
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        lastData.minuteQuote = newone
        newone
    }
  }

  def minuteMoneyFlowOf(time: Long): MoneyFlow = {
    val cal = Calendar.getInstance(exchange.timeZone)
    val rounded = TFreq.ONE_MIN.round(time, cal)
    lastData.minuteMoneyFlow match {
      case one: MoneyFlow if one.time == rounded =>
        one
      case prevOneOrNull => // minute changes or null
        if (prevOneOrNull != null) {
          prevOneOrNull.closed_!
          minuteMoneyFlowsToClose += prevOneOrNull
        }

        val newone = new MoneyFlow
        newone.time = rounded
        newone.sec = this
        newone.unclosed_!
        newone.justOpen_!
        lastData.minuteMoneyFlow = newone
        newone
    }
  }

  /**
   * @return (lastTicker of day, day first?)
   */
  def lastTickerOf(dailyQuote: Quote): (Ticker, Boolean) = {
    lastData.prevTicker match {
      case null =>
        Tickers.lastTickerOf(dailyQuote) match  {
          case None =>
            val x = new Ticker
            lastData.prevTicker = x
            (x, true)
          case Some(x) =>
            lastData.prevTicker = x
            (x, false)
        }
      case x => (x, false)
    }
  }

  def tickerSnapshot: TickerSnapshot = lastData.tickerSnapshot
}
