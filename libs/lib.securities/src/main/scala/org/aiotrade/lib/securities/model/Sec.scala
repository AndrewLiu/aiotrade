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

import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.datasource.SerProvider
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.QuoteSerCombiner
import org.aiotrade.lib.securities.TickerSnapshot
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.QuoteServer
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.util.ChangeObserver
import org.aiotrade.lib.util.actors.Publisher
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import ru.circumflex.orm.Table


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
}

import Sec._
class Sec extends SerProvider with Publisher with ChangeObserver {
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

  private val freqToQuoteContract = HashMap[TFreq, QuoteContract]()
  /** each freq may have a standalone quoteDataServer for easy control and thread safe */
  private val freqToQuoteServer = HashMap[TFreq, QuoteServer]()
  private val freqToSer = HashMap[TFreq, QuoteSer]()
  val tickers = ArrayBuffer[Ticker]()

  var description = ""
  var name = ""
  var defaultFreq: TFreq = _
  private var _quoteContracts: Seq[QuoteContract] = Nil
  private var _tickerContract: TickerContract = _

  def quoteContracts = _quoteContracts
  def quoteContracts_=(quoteContracts: Seq[QuoteContract]) {
    _quoteContracts = quoteContracts
    createFreqSers
  }

  /** create freq sers */
  private def createFreqSers {
    for (contract <- quoteContracts) {
      val freq = contract.freq
      if (defaultFreq == null) {
        defaultFreq = freq
      }
      freqToQuoteContract(freq) = contract
      freqToSer(freq) = new QuoteSer(freq)
    }
    
    // basic freqs:
    for (freq <- basicFreqs if !freqToQuoteContract.contains(freq)) {
      freqToSer(freq) = new QuoteSer(freq)
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
  lazy val tickerServer: TickerServer = tickerContract.serviceInstance().get

  /** create tickerSer. We'll always have a standalone tickerSer, even we have another 1-min quoteSer */
  val tickerSer = new QuoteSer(tickerContract.freq)

  val prevTicker = new Ticker

  val updater: Updater = {
    case ts: TickerSnapshot =>
      if (ts.isValueChanged(prevTicker)) {
        val ticker = new Ticker
        ticker.copyFrom(ts)
        tickers += ticker
        publish(TickerEvent(this, ticker))
        prevTicker.copyFrom(ts)
      }
  }

  reactions += {
    case TSerEvent.FinishedLoading(sourceSer, _, fromTime, endTime, _, _) =>
      // contract quoteServer of freq centernly still exists only under this type of event
      val freq = sourceSer.freq
      val contract = freqToQuoteContract(freq)
      val quoteServer = freqToQuoteServer(freq)
      sourceSer.loaded = true
      if (contract.refreshable) {
        quoteServer.startRefresh(contract.refreshInterval)
      } else {
        quoteServer.unSubscribe(contract)
        freqToQuoteServer -= freq
      }

      deafTo(sourceSer)
    case _ =>
  }

  def serOf(freq: TFreq): Option[QuoteSer] = freq match {
    case TFreq.ONE_SEC | TFreq.ONE_MIN | TFreq.DAILY => freqToSer.get(freq)
    case _ => freqToSer.get(freq) match {
        case None => createCombinedSer(freq)
        case x => x
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
        val tarSer = new QuoteSer(freq)
        val combiner = new QuoteSerCombiner(srcSer, tarSer, exchange.timeZone)
        combiner.computeFrom(0) // don't remove me, see notice above.
        putSer(tarSer)
        Some(tarSer)
      case None => None
    }
  }

  def putSer(ser: QuoteSer) {
    freqToSer.put(ser.freq, ser)
  }

  /**
   * synchronized this method to avoid conflict on variable: loadBeginning and
   * concurrent accessing to those maps.
   */
  def loadSer(freq: TFreq): Boolean = synchronized {

    /** ask contract instead of server */
    val contract = freqToQuoteContract  get(freq) getOrElse (return false)

    val quoteServer = freqToQuoteServer get(freq) getOrElse {
      contract.serviceInstance() match {
        case None => return false
        case Some(x) => freqToQuoteServer += (freq -> x); x
      }
    }

    val serToBeLoaded = serOf(freq) getOrElse {
      val x = new QuoteSer(freq)
      freqToSer(freq) = x
      x
    }

    if (!quoteServer.isContractSubsrcribed(contract)) {
      quoteServer.subscribe(contract, serToBeLoaded)
    }

    quoteServer.loadHistory
    serToBeLoaded.inLoading = true

    listenTo(serToBeLoaded)

    true
  }

  def isSerLoaded(freq:TFreq): Boolean = {
    freqToSer.get(freq) map (_.loaded) getOrElse false
  }

  def isSerInLoading(freq: TFreq): Boolean = {
    freqToSer.get(freq) map (_.inLoading) getOrElse false
  }

  def uniSymbol: String = 
    if (secInfo != null) secInfo.uniSymbol else ""
  def uniSymbol_=(uniSymbol: String) {
    if (secInfo != null) {
      secInfo.uniSymbol = uniSymbol
    }
    name = uniSymbol.replace('.', '_')
  }

  def stopAllDataServer {
    for (server <- freqToQuoteServer.valuesIterator if server.inUpdating) {
      server.stopRefresh
    }
    freqToQuoteServer.clear
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

  def dataContract: QuoteContract = 
    freqToQuoteContract(defaultFreq)
  def dataContract_=(quoteContract: QuoteContract) {
    val freq = quoteContract.freq
    freqToQuoteContract += (freq -> quoteContract)
    /** may need a new dataServer now: */
    freqToQuoteServer -= freq
  }

  def subscribeTickerServer {
    if (uniSymbol == "") return

    // always set uniSymbol, since _tickerContract may be set before secInfo.uniSymbol
    _tickerContract.symbol = uniSymbol

    if (tickerContract.serviceClassName == null) {
      freqToQuoteServer.get(defaultFreq) match {
        case Some(quoteServer) => quoteServer.classOfTickerServer match {
            case Some(clz) => tickerContract.serviceClassName = clz.getClass.getName
            case None =>
          }
        case None =>
      }
    }

    startTickerRefreshIfNecessary
  }

  private def startTickerRefreshIfNecessary {
    if (!tickerServer.isContractSubsrcribed(tickerContract)) {
      var chainSers: List[QuoteSer] = Nil
      // Only dailySer and minuteSre needs to chainly follow ticker change.
      serOf(TFreq.DAILY)   foreach {x => chainSers ::= x}
      serOf(TFreq.ONE_MIN) foreach {x => chainSers ::= x}
      tickerServer.subscribe(tickerContract, tickerSer, chainSers)
      //
      //            var break = false
      //            while (!break) {
      //                val allChainSersLoaded = (true /: chainSers) {_ && _.loaded}
      //                if (allChainSersLoaded) {
      //                    tickerServer.subscribe(tickerContract, tickerSer, chainSers)
      //                    break = true
      //                }
      //                Thread.sleep(1000)
      //            }

    }

    tickerServer.startRefresh(tickerContract.refreshInterval)
    tickerServer.tickerSnapshotOf(tickerContract.symbol) foreach {this observe _}
  }

  def unSubscribeTickerServer {
    if (tickerServer != null && tickerContract != null) {
      tickerServer.unSubscribe(tickerContract)
      tickerServer.tickerSnapshotOf(tickerContract.symbol) foreach {this unObserve _}
    }
  }

  def isTickerServerSubscribed: Boolean = {
    tickerServer != null && tickerServer.isContractSubsrcribed(tickerContract)
  }
}
