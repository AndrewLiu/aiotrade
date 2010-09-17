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
import org.aiotrade.lib.info.model.Infos1m
import org.aiotrade.lib.info.model.Infos1d
import org.aiotrade.lib.math.indicator.Indicator
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.datasource.SerProvider
import org.aiotrade.lib.securities.InfoPointSer
import org.aiotrade.lib.securities.InfoSer
import org.aiotrade.lib.securities.MoneyFlowSer
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.QuoteSerCombiner
import org.aiotrade.lib.securities.TickerSnapshot
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.QuoteInfoHisContract
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.dataserver.QuoteInfo
import org.aiotrade.lib.securities.dataserver.QuoteInfoContract
import org.aiotrade.lib.securities.dataserver.QuoteInfoDataServer
import org.aiotrade.lib.util.actors.Event
import java.util.logging.Logger
import scala.collection.mutable.HashMap
import ru.circumflex.orm.Table
import org.aiotrade.lib.info.model.GeneralInfo
import org.aiotrade.lib.info.model.GeneralInfos
import org.aiotrade.lib.info.model.GeneralInfo
import org.aiotrade.lib.info.model.InfoSecs
import ru.circumflex.orm._
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

  def tickers = inverse(Tickers.sec)
  def executions = inverse(Executions.sec)
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
}

import Sec._
class Sec extends SerProvider {
  private val log = Logger.getLogger(this.getClass.getName)

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

  type T = QuoteSer
  type C = QuoteContract

  private val freqToQuoteContract = HashMap[TFreq, QuoteContract]()
  private val freqToQuoteInfoHisContract = HashMap[TFreq, QuoteInfoHisContract]()
  private val mutex = new AnyRef
  private var _realtimeSer: QuoteSer = _
  private[securities] lazy val freqToQuoteSer = HashMap[TFreq, QuoteSer]()
  private lazy val freqToMoneyFlowSer = HashMap[TFreq, MoneyFlowSer]()
  private lazy val freqToIndicators = HashMap[TFreq, ListBuffer[Indicator]]()
  private lazy val freqToInfoSer = HashMap[TFreq, InfoSer]()
  private lazy val freqToInfoPointSer = HashMap[TFreq, InfoPointSer]()


  var description = ""
  private var _defaultFreq: TFreq = _
  private var _quoteContracts: Seq[QuoteContract] = Nil
  private var _tickerContract: TickerContract = _
  private var _quoteInfoContract : QuoteInfoContract = _
  private var _quoteInfoHisContractc : Seq[QuoteInfoHisContract] = _

  def defaultFreq = if (_defaultFreq == null) TFreq.DAILY else _defaultFreq

  def quoteContracts = _quoteContracts
  def quoteContracts_=(quoteContracts: Seq[QuoteContract]) {
    _quoteContracts = quoteContracts
    for (contract <- quoteContracts) {
      val freq = contract.freq
      if (_defaultFreq == null) {
        _defaultFreq = freq
      }

      freqToQuoteContract.put(freq, contract)
    }
  }

  def quoteInfoHisContracts = _quoteInfoHisContractc

  def quoteInfoHisContracts_= (consracts : Seq[QuoteInfoHisContract]) {
    _quoteInfoHisContractc = consracts
    for (contract <- _quoteInfoHisContractc){
      freqToQuoteInfoHisContract.put(contract.freq, contract)
    }
  }

  def quoteInfoContract = {
    if (_quoteInfoContract == null){
      _quoteInfoContract = new QuoteInfoContract()
    }
    _quoteInfoContract
  }

  def quoteInfoContract_= (contract : QuoteInfoContract) {
    _quoteInfoContract = contract
  }


  
  def realtimeSer = mutex synchronized {
    if (_realtimeSer == null) {
      _realtimeSer = new QuoteSer(this, TFreq.ONE_MIN)
      freqToQuoteSer.put(TFreq.ONE_SEC, _realtimeSer)
    }
    _realtimeSer
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
  private lazy val quoteInfoServer : QuoteInfoDataServer = quoteInfoContract.serviceInstance().get
  
  def serOf(freq: TFreq): Option[QuoteSer] = mutex synchronized {
    freq match {
      case TFreq.ONE_SEC => Some(realtimeSer)
      case _ => freqToQuoteSer.get(freq) match {
          case None => freq match {
              case TFreq.ONE_MIN | TFreq.DAILY =>
                val x = new QuoteSer(this, freq)
                freqToQuoteSer.put(freq, x)
                Some(x)
              case _ => createCombinedSer(freq)
            }
          case some => some
        }
    }
  }

  def indicatorOf(clazzName: String, freq: TFreq): Option[Indicator] = mutex synchronized {
    freqToIndicators.get(freq) match {
      case Some(inds) => inds find (_.getClass.getName == clazzName)
      case None => None
    }
  }

  def addIndicator(indicator: Indicator): Unit = mutex synchronized {
    val freq = indicator.freq
    val indicators = freqToIndicators.get(freq) match {
      case Some(xs) => xs += indicator
      case None =>
        val xs = ListBuffer[Indicator](indicator)
        freqToIndicators.put(freq, xs)
    }
  }

  def removeIndicator(indicator: Indicator): Unit = mutex synchronized  {
    val freq = indicator.freq
    freqToIndicators.get(freq) match {
      case Some(indicators) => indicators -= indicator
      case None =>
    }
  }

  def moneyFlowSerOf(freq: TFreq): Option[MoneyFlowSer] = mutex synchronized {
    freq match {
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
  }

  def infoPointSerOf(freq: TFreq): Option[InfoPointSer] = mutex synchronized {
    freq match {
      case TFreq.ONE_SEC | TFreq.ONE_MIN | TFreq.DAILY => freqToInfoPointSer.get(freq) match {
          case None => serOf(freq) match {
              case Some(quoteSer) =>
                val x = new InfoPointSer(this, freq)
                freqToInfoPointSer.put(freq, x)
                Some(x)
              case None => None
            }
          case some => some
        }
      case _ => freqToInfoPointSer.get(freq) match {
          case None => None // @todo createCombinedSer(freq)
          case some => some
        }
    }
  }

  def infoSerOf(freq: TFreq): Option[InfoSer] = mutex synchronized {
    freq match {
      case TFreq.ONE_SEC | TFreq.ONE_MIN | TFreq.DAILY => freqToInfoSer.get(freq) match {
          case None => serOf(freq) match {
              case Some(quoteSer) =>
                val x = new InfoSer(this, freq)
                freqToInfoSer.put(freq, x)
                Some(x)
              case None => None
            }
          case some => some
        }
      case _ => freqToInfoSer.get(freq) match {
          case None => None // @todo createCombinedSer(freq)
          case some => some
        }
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
        if (!srcSer.isLoaded) loadSer(srcSer)

        val tarSer = new QuoteSer(this, freq)
        val combiner = new QuoteSerCombiner(srcSer, tarSer, exchange.timeZone)
        combiner.computeFrom(0) // don't remove me, see notice above.
        freqToQuoteSer.put(tarSer.freq, tarSer)
        Some(tarSer)
      case None => None
    }
  }

  def putSer(ser: QuoteSer): Unit = mutex synchronized {
    freqToQuoteSer.put(ser.freq, ser)
  }

  /**
   * synchronized this method to avoid conflict on variable: loadBeginning and
   * concurrent accessing to varies maps.
   */
  def loadSer(ser: QuoteSer): Boolean = synchronized {
    // load from persistence
    val wantTime = loadSerFromPersistence(ser)

    // try to load from quote server
    loadFromQuoteServer(ser, wantTime)

    true
  }

  def resetSers: Unit = mutex synchronized {
    _realtimeSer = null
    freqToQuoteSer.clear
    freqToMoneyFlowSer.clear
    freqToIndicators.clear
    freqToInfoSer.clear
  }

  /**
   * @return quoteSer created and is loaded
   */
  def isSerLoaded(freq: TFreq) = {
    freqToQuoteSer.get(freq) match {
      case Some(x) => x.isLoaded
      case None => false
    }
  }

  /**
   * All quotes in persistence should have been properly rounded to 00:00 of exchange's local time
   */
  private def loadSerFromPersistence(ser: QuoteSer): Long = {
    val quotes = if (ser eq realtimeSer) {
      val dailyRoundedTime = exchange.lastDailyRoundedTradingTime match {
        case Some(x) => x
        case None => TFreq.DAILY.round(System.currentTimeMillis, Calendar.getInstance(exchange.timeZone))
      }

      val cal = Calendar.getInstance(exchange.timeZone)
      cal.setTimeInMillis(dailyRoundedTime)
      log.info("Loading realtime ser from persistence of " + cal.getTime)
      Quotes1m.mintueQuotesOf(this, dailyRoundedTime)
    } else {
      ser.freq match {
        case TFreq.ONE_MIN => Quotes1m.quotesOf(this)
        case TFreq.DAILY   => Quotes1d.quotesOf(this)
        case _ => return 0L
      }
    }

    ser ++= quotes.toArray

    val uniSymbol = secInfo.uniSymbol
    /**
     * get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!quotes.isEmpty) {
      val (first, last, isAscending) = if (quotes.head.time <= quotes.last.time)
        (quotes.head, quotes.last, true)
      else
        (quotes.last, quotes.head, false)

      ser.publish(TSerEvent.Refresh(ser, uniSymbol, first.time, last.time))

      // should load earlier quotes from data source? first.fromMe_? may means never load from data server
      val wantTime = if (first.fromMe_?) 0 else {
        // search the lastFromMe one, if exist, should re-load quotes from data source to override them
        var lastFromMe: Quote = null
        var i = if (isAscending) 0 else quotes.length - 1
        while (i < quotes.length && i >= 0 && quotes(i).fromMe_?) {
          lastFromMe = quotes(i)
          if (isAscending) i += 1 else i -= 1
        }

        if (lastFromMe != null) lastFromMe.time - 1 else last.time
      }

      log.info(uniSymbol + "(" + ser.freq + "): loaded from persistence, got quotes=" + quotes.length +
               ", loaded: time=" + last.time + ", ser size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )
      
      wantTime
    } else {
      log.info(uniSymbol + "(" + ser.freq + "): loaded from persistence, got 0 quotes" + ", ser size=" + ser.size
               + ", will try to load from data source from beginning")
      0L
    }
  }
  def loadInfoPointSer(ser : InfoPointSer) : Boolean = synchronized {
    //after resolve orm problem
    val wantTime = loadInfoPointSerFromPersistence(ser)
    loadInfoPointSerFromDataServer(ser,wantTime)
    true
  }

  private def loadInfoPointSerFromPersistence(ser: InfoPointSer): Long = {
    val id = Secs.idOf(this)
    val GI = GeneralInfos
    val IS = InfoSecs
    var time : Long = 0
    val infos = (SELECT (GI.*) FROM (GI JOIN IS) WHERE ( (GI.infoClass EQ GeneralInfo.QUOTE_INFO) AND (IS.sec.field EQ id) ) ORDER_BY (GI.publishTime DESC) list)
    infos map {
      info =>
      val quoteInfo = new QuoteInfo
      quoteInfo.time = info.publishTime
      quoteInfo.generalInfo = info
//      quoteInfo.summary = "info.summary"
//      quoteInfo.content = "info.content"
//      quoteInfo.content = info.content
      info.categories foreach ( cate => quoteInfo.categories.append(cate))
      info.secs foreach (sec => quoteInfo.secs.append(sec))
      ser.updateFrom(quoteInfo)
      time = info.publishTime
    }
    time
  }

  private def loadInfoPointSerFromDataServer(ser: InfoPointSer, fromTime: Long) : Long = {
    val freq = ser.freq
    quoteInfoHisContractOf(freq) match {
      case Some(contract) =>  contract.serviceInstance() match {
          case Some(quoteInfoHisServer) =>
            contract.freq = if (ser eq realtimeSer) TFreq.ONE_SEC else freq
            quoteInfoHisServer.subscribe(contract)

            // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
            var reaction: PartialFunction[Event, Unit] = null
            reaction = {
              case TSerEvent.Loaded(ser, uniSymbol, frTime, toTime, _, _) =>
                reactions -= reaction
                deafTo(ser)
                ser.isLoaded = true
            }
            reactions += reaction
            listenTo(ser)

            ser.isInLoading = true
            quoteInfoHisServer.loadHistory(fromTime)

          case _ => ser.isLoaded = true
        }
      case None => 
    }

    0L
  }
  /**
   * All values in persistence should have been properly rounded to 00:00 of exchange's local time
   */
  def loadMoneyFlowSerFromPersistence(ser: MoneyFlowSer): Long = {
    val mfs = ser.freq match {
      case TFreq.DAILY   => MoneyFlows1d.closedMoneyFlowOf(this)
      case TFreq.ONE_MIN => MoneyFlows1m.closedMoneyFlowOf(this)
      case _ => return 0L
    }

    ser ++= mfs.toArray
    
    /**
     * get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!mfs.isEmpty) {
      val (first, last, isAscending) = if (mfs.head.time < mfs.last.time)
        (mfs.head, mfs.last, true)
      else
        (mfs.last, mfs.head, false)

      ser.publish(TSerEvent.Refresh(ser, uniSymbol, first.time, last.time))

      // should load earlier quotes from data source?
      val wantTime = if (first.fromMe_?) 0 else {
        // search the lastFromMe one, if exist, should re-load quotes from data source to override them
        var lastFromMe: MoneyFlow = null
        var i = if (isAscending) 0 else mfs.length - 1
        while (i < mfs.length && i >= 0 && mfs(i).fromMe_?) {
          lastFromMe = mfs(i)
          if (isAscending) i += 1 else i -= 1
        }

        if (lastFromMe != null) lastFromMe.time - 1 else last.time
      }

      log.info(uniSymbol + "(" + ser.freq + "): loaded from persistence, got MoneyFlows=" + mfs.length +
               ", loaded: time=" + last.time + ", size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )

      wantTime
    } else 0L
  }

  def loadInfoSerFromPersistence(ser: InfoSer): Long = {
    val infos = ser.freq match {
      case TFreq.DAILY   => Infos1d.all()
      case TFreq.ONE_MIN => Infos1m.all()
      case _ => return 0L
    }

    ser ++= infos.toArray

    /**
     * get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!infos.isEmpty) {
      val (first, last, isAscending) = if (infos.head.time < infos.last.time)
        (infos.head, infos.last, true)
      else
        (infos.last, infos.head, false)

      ser.publish(TSerEvent.Refresh(ser, uniSymbol, first.time, last.time))

      // should load earlier quotes from data source?
      val wantTime = if (first.fromMe_?) 0 else {
        // search the lastFromMe one, if exist, should re-load quotes from data source to override them
        var lastFromMe: org.aiotrade.lib.info.model.Info = null
        var i = if (isAscending) 0 else infos.length - 1
        while (i < infos.length && i >= 0 && infos(i).fromMe_?) {
          lastFromMe = infos(i)
          if (isAscending) i += 1 else i -= 1
        }

        if (lastFromMe != null) lastFromMe.time - 1 else last.time
      }

      log.info(uniSymbol + "(" + ser.freq + "): loaded from persistence, got Infos=" + infos.length +
               ", loaded: time=" + last.time + ", size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )

      wantTime
    } else 0L
  }

  private def loadFromQuoteServer(ser: QuoteSer, fromTime: Long) {
    val freq = ser.freq
    
    quoteContractOf(freq) match {
      case Some(contract) =>
        contract.serviceInstance() match {
          case Some(quoteServer) =>
            contract.srcSymbol = quoteServer.toSrcSymbol(this.uniSymbol)
            contract.freq = if (ser eq realtimeSer) TFreq.ONE_SEC else freq
            quoteServer.subscribe(contract)

            // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
            var reaction: PartialFunction[Event, Unit] = null
            reaction = {
              case TSerEvent.Loaded(ser, uniSymbol, frTime, toTime, _, _) =>
                reactions -= reaction
                deafTo(ser)
                ser.isLoaded = true
            }
            reactions += reaction
            listenTo(ser)

            ser.isInLoading = true
            quoteServer.loadHistory(fromTime)

          case _ => ser.isLoaded = true
        }

      case _ => ser.isLoaded = true
    }
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
            x.dateFormatPattern = defaultOne.dateFormatPattern
            freqToQuoteContract.put(freq, x)
            Some(x)
          case _ => None
        }
      case some => some
    }
  }

  private def quoteInfoHisContractOf(freq: TFreq): Option[QuoteInfoHisContract] = {
    freqToQuoteInfoHisContract.get(freq)
  }

  def uniSymbol: String = if (secInfo != null) secInfo.uniSymbol else ""
  def uniSymbol_=(uniSymbol: String) {
    if (secInfo != null) {
      secInfo.uniSymbol = uniSymbol
    }
  }

  override def name: String = {
    if (secInfo != null) {
      secInfo.name
    } else uniSymbol
  }

  def stopAllDataServer {
    for ((freq, contract) <- freqToQuoteContract;
         server <- contract.serviceInstance()
    ) {
      server.stopRefresh
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

    if (tickerContract.serviceClassName == null) {
      for (quoteContract <- quoteContractOf(defaultFreq);
           quoteServer <- quoteContract.serviceInstance();
           klass <- quoteServer.classOfTickerServer
      ) {
        tickerContract.serviceClassName = klass.getName
      }
    }

    if (tickerContract.serviceClassName != null) {
      if (!startRefresh) {
        tickerServer.stopRefresh
      }
      // always set uniSymbol, since _tickerContract may be set before secInfo.uniSymbol
      //this is not always true, for DJI, src code: DJI while unisymbol is ^DJI
      tickerContract.srcSymbol = tickerServer.toSrcSymbol(uniSymbol)
      if (!tickerServer.isContractSubsrcribed(tickerContract)) {
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

  def subscribeQuoteInfoDataServer(startRefresh: Boolean = true) : Option[QuoteInfoDataServer] = {
    if (uniSymbol == "") return None

    // always set uniSymbol, since _tickerContract may be set before secInfo.uniSymbol
    quoteInfoContract.srcSymbol = uniSymbol

    if (quoteInfoContract.serviceClassName != null) {
      if (!startRefresh) {
        quoteInfoServer.stopRefresh
      }

      if (!quoteInfoServer.isContractSubsrcribed(quoteInfoContract)) {
        quoteInfoServer.subscribe(quoteInfoContract)
      }

      if (startRefresh) {
        quoteInfoServer.startRefresh(quoteInfoContract.refreshInterval)
      }
    }

    Some(quoteInfoServer)
  }

  def unSubscribeQuoteInfoDataServer {
    if(quoteInfoServer != null & quoteInfoContract != null){
      quoteInfoServer.unSubscribe(quoteInfoContract)
    }
  }

  def isQuoteInfoDataServerSubcribed : Boolean = {
    quoteInfoServer != null && quoteInfoServer.isContractSubsrcribed(quoteInfoContract)
  }

  /**
   * store latest snap info
   */
  lazy val secSnap = new SecSnap(this)

  lazy val tickerSnapshot = new TickerSnapshot
}

class SecSnap(val sec: Sec) {
  var currTicker: Ticker = _
  var prevTicker$: Ticker = _
    
  var isDayFirstTicker: Boolean = _

  var dailyQuote: Quote = _
  var minuteQuote: Quote = _

  var dailyMoneyFlow: MoneyFlow = _
  var minuteMoneyFlow: MoneyFlow = _

  def prevTicker : Ticker = prevTicker$
  
  private val timeZone = sec.exchange.timeZone

  def setByTicker(ticker: Ticker): SecSnap = {
    this.currTicker = ticker
    
    val time = ticker.time
    checkLastTickerOf(time)
    checkDailyQuoteOf(time)
    checkMinuteQuoteOf(time)
    checkDailyMoneyFlowOf(time)
    checkMinuteMoneyFlowOf(time)
    this
  }

  def checkDailyQuoteOf(time: Long): Quote = {
    assert(Secs.idOf(sec).isDefined, "Sec: " + sec + " is transient")
    val cal = Calendar.getInstance(timeZone)
    val rounded = TFreq.DAILY.round(time, cal)
    dailyQuote match {
      case one: Quote if one.time == rounded =>
        one
      case prevOneOrNull => // day changes or null
        val newone = Quotes1d.dailyQuoteOf(sec, rounded)
        dailyQuote = newone
        newone
    }
  }

  def checkDailyMoneyFlowOf(time: Long): MoneyFlow = {
    assert(Secs.idOf(sec).isDefined, "Sec: " + sec + " is transient")
    val cal = Calendar.getInstance(timeZone)
    val rounded = TFreq.DAILY.round(time, cal)
    dailyMoneyFlow match {
      case one: MoneyFlow if one.time == rounded =>
        one
      case prevOneOrNull => // day changes or null
        val newone = MoneyFlows1d.dailyMoneyFlowOf(sec, rounded)
        dailyMoneyFlow = newone
        newone
    }
  }

  def checkMinuteQuoteOf(time: Long): Quote = {
    val cal = Calendar.getInstance(timeZone)
    val rounded = TFreq.ONE_MIN.round(time, cal)
    minuteQuote match {
      case one: Quote if one.time == rounded =>
        one
      case prevOneOrNull => // minute changes or null
        val newone = Quotes1m.minuteQuoteOf(sec, rounded)
        minuteQuote = newone
        newone
    }
  }

  def checkMinuteMoneyFlowOf(time: Long): MoneyFlow = {
    val cal = Calendar.getInstance(timeZone)
    val rounded = TFreq.ONE_MIN.round(time, cal)
    minuteMoneyFlow match {
      case one: MoneyFlow if one.time == rounded =>
        one
      case prevOneOrNull => // minute changes or null
        val newone = MoneyFlows1m.minuteMoneyFlowOf(sec, rounded)
        minuteMoneyFlow = newone
        newone
    }
  }

  /**
   * @return lastTicker of this day
   */
  def checkLastTickerOf(time: Long): (Ticker, Boolean) = {
    val cal = Calendar.getInstance(timeZone)
    val rounded = TFreq.DAILY.round(time, cal)
    prevTicker match {
      case null =>
        Tickers.lastTickerOf(sec, rounded) match  {
          case None =>
            prevTicker$ = new Ticker
            isDayFirstTicker = true
          case Some(x) =>
            prevTicker$ = x
            isDayFirstTicker = false
        }
      case _ =>
        isDayFirstTicker = false
    }

    (prevTicker$, isDayFirstTicker)
  }
}

