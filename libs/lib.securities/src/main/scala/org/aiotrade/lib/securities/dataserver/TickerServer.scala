/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.securities.dataserver

import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.TickerSnapshot
import org.aiotrade.lib.securities.model.Tickers
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.Execution
import org.aiotrade.lib.securities.model.ExecutionEvent
import org.aiotrade.lib.securities.model.Executions
import org.aiotrade.lib.securities.model.LightTicker
import org.aiotrade.lib.securities.model.MarketDepth
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.actors.Event
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.collection.ArrayList
import ru.circumflex.orm._
import scala.collection.mutable.HashMap

/** This class will load the quote data from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
case class TickerEvent(ticker: Ticker) extends Event // TickerEvent only accept Ticker
case class TickersEvent(ticker: Array[LightTicker]) extends Event // TickersEvent accect LightTicker

object TickerServer extends Publisher {
  private val log = Logger.getLogger(this.getClass.getName)

  val uniSymbolToLastTicker = new HashMap[String, LightTicker]
  // load all last tickers
  Exchange.allExchanges foreach {x =>
    val start = System.currentTimeMillis
    log.info("Loading last tickers of " + x.code)
    uniSymbolToLastTicker ++= Exchanges.uniSymbolToLastTickerOf(x)
    log.info("Loading last tickers of " + x.code + " used " + (System.currentTimeMillis - start) / 1000.0 + "s")
  }

}

abstract class TickerServer extends DataServer[Ticker] {
  type C = TickerContract

  private val log = Logger.getLogger(this.getClass.getName)

  refreshable = true

  def tickerSnapshotOf(uniSymbol: String): TickerSnapshot = {
    val sec = Exchange.secOf(uniSymbol).get
    sec.tickerSnapshot
  }

  override def subscribe(contract: TickerContract) {
    super.subscribe(contract)
    /**
     * !NOTICE
     * the symbol-tickerSnapshot pair must be immutable, other wise, if
     * the symbol is subscribed repeatly by outside, the old observers
     * of tickerSnapshot may not know there is a new tickerSnapshot for
     * this symbol, the older one won't be updated any more.
     */
    val symbol = contract.srcSymbol
    val sec = Exchange.secOf(symbol).get
    val tickerSnapshot = sec.tickerSnapshot
    tickerSnapshot.symbol = symbol
  }

  override def unSubscribe(contract: TickerContract) {
    super.unSubscribe(contract)
    val symbol = contract.srcSymbol
    val sec = Exchange.secOf(symbol).get
    val tickerSnapshot = sec.tickerSnapshot
  }

  override protected def postLoadHistory(values: Array[Ticker]): Long = {
    val events = composeSer(values)
    var lastTime = Long.MinValue
    events foreach {
      case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
        source.publish(TSerEvent.FinishedLoading(source, symbol, fromTime, toTime, lastObject, callback))
        log.info(symbol + ": " + count + ", data loaded, load server finished")
        lastTime = toTime
      case _ =>
    }
    lastTime
  }

  override protected def postRefresh(values: Array[Ticker]): Long = {
    val events = composeSer(values)
    var lastTime = Long.MinValue
    events foreach {
      case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
        source.publish(TSerEvent.Updated(source, symbol, fromTime, toTime, lastObject, callback))
        lastTime = toTime
      case _ =>
    }
    lastTime
  }

  override protected def postStopRefresh {}

  /**
   * compose ser using data from TVal(s)
   * @param symbol
   * @param serToBeFilled Ser
   * @param TVal(s)
   */
  def composeSer(values: Array[Ticker]): Iterable[TSerEvent] = {
    log.info("Composing ser from tickers: " + values.length)
    if (values.length == 0) return Nil

    val allTickers = new ArrayList[Ticker]
    val allExecutions = new ArrayList[Execution]
    val allSnapDepths = new ArrayList[SnapDepth]
    val updatedDailyQuotes = new ArrayList[Quote]

    val symbolToTickerInfo = new HashMap[String, TickerInfo]

    var i = 0
    while (i < values.length) {
      val ticker = values(i)

      val symbol = ticker.symbol
      TickerServer.uniSymbolToLastTicker.put(ticker.symbol, ticker)

      if (subscribedSrcSymbols.contains(symbol)) {
        val contract = subscribedSrcSymbols.get(symbol).get
        val sec = Exchange.secOf(symbol).get

        val minSer = contract.ser
        symbolToTickerInfo.get(symbol) match {
          case Some(x) => x.lastTicker = ticker
          case None => symbolToTickerInfo.put(symbol, new TickerInfo(ticker, minSer))
        }

        val dayQuote = sec.dailyQuoteOf(ticker.time)
        assert(Quotes1d.idOf(dayQuote).isDefined, "dailyQuote of " + sec.secInfo.uniSymbol + " is transient")
        ticker.quote = dayQuote

        val (prevTicker, dayFirst) = sec.lastTickerOf(dayQuote)
        val minQuote = sec.minuteQuoteOf(ticker.time)

        var tickerValid = false
        var execution: Execution = null
        if (dayFirst) {
          dayQuote.unjustOpen_!

          tickerValid = true

          /**
           * this is today's first ticker we got when begin update data server,
           * actually it should be, so maybe we should check this.
           * As this is the first data of today:
           * 1. set OHLC = Ticker.LAST_PRICE
           * 2. to avoid too big volume that comparing to following dataSeries.
           * so give it a small 0.0001 (if give it a 0, it will won't be calculated
           * in calcMaxMin() of ChartView)
           */
          execution = new Execution
          execution.quote = dayQuote
          execution.time = ticker.time
          execution.price  = ticker.lastPrice
          execution.volume = ticker.dayVolume
          execution.amount = ticker.dayAmount
          allExecutions += execution

          minQuote.open   = ticker.lastPrice
          minQuote.high   = ticker.lastPrice
          minQuote.low    = ticker.lastPrice
          minQuote.close  = ticker.lastPrice
          minQuote.volume = 0.00001
          minQuote.amount = 0.00001

        } else {

          if (ticker.time + 1000 > prevTicker.time) { // 1000ms, @Note: we add +1 to ticker.time later
            // some datasource only counts on seconds, but we may truly have a new ticker
            if (ticker.time == prevTicker.time) {
              ticker.time = prevTicker.time + 1
            }

            tickerValid = true

            if (ticker.dayVolume > prevTicker.dayVolume) {
              execution = new Execution
              execution.quote = dayQuote
              execution.time = ticker.time
              execution.price  = ticker.lastPrice
              execution.volume = ticker.dayVolume - prevTicker.dayVolume
              execution.amount = ticker.dayAmount - prevTicker.dayAmount
              allExecutions += execution
            }

            if (minQuote.justOpen_?) {
              minQuote.unjustOpen_!

              minQuote.open  = ticker.lastPrice
              minQuote.high  = ticker.lastPrice
              minQuote.low   = ticker.lastPrice
              minQuote.close = ticker.lastPrice

            } else {

              if (prevTicker.dayHigh != 0 && ticker.dayHigh != 0) {
                if (ticker.dayHigh > prevTicker.dayHigh) {
                  /** this is a new day high happened during this ticker */
                  minQuote.high = ticker.dayHigh
                }
              }
              if (ticker.lastPrice != 0) {
                minQuote.high = math.max(minQuote.high, ticker.lastPrice)
              }

              if (prevTicker.dayLow != 0 && ticker.dayLow != 0) {
                if (ticker.dayLow < prevTicker.dayLow) {
                  /** this is a new day low happened during this ticker */
                  minQuote.low = ticker.dayLow
                }
              }
              if (ticker.lastPrice != 0) {
                minQuote.low = math.min(minQuote.low, ticker.lastPrice)
              }

              minQuote.close = ticker.lastPrice
              if (execution != null && execution.volume > 1) {
                minQuote.volume += execution.volume
                minQuote.amount += execution.amount
              }
            }
          } else {
            log.warning("Discard ticker " + ticker.toString)
          }
        }


        // update 1m quoteSer from minuteQuote
        minSer.updateFrom(minQuote)

        if (execution != null) {
          val prevPrice = if (dayFirst) ticker.prevClose else prevTicker.lastPrice
          val prevDepth = if (dayFirst) MarketDepth.Empty else MarketDepth(prevTicker.bidAsks, copy = true)
          allSnapDepths += SnapDepth(prevPrice, prevDepth, execution)

          sec.publish(ExecutionEvent(ticker.prevClose, execution))
        }

        if (tickerValid) {
          allTickers += ticker
          prevTicker.copyFrom(ticker)
          sec.publish(TickerEvent(ticker))
        }


        // update daily quote and ser
        if (ticker.dayHigh != 0 && ticker.dayLow != 0) {
          updatedDailyQuotes += dayQuote
          updateDailyQuoteByTicker(dayQuote, ticker)
          contract.chainSers find (_.freq == TFreq.DAILY) foreach (_.updateFrom(dayQuote))
        }
      }

      i += 1
    }
    
      

    /* else {

     /**
      * no new ticker got, but should consider if it's necessary to to update quoteSer
      * as the quote window may be just opened.
      */
     sec.lastData.prevTicker match {
     case null =>
     case ticker =>
     if (ticker != null && ticker.dayHigh != 0 && ticker.dayLow != 0) {
     val dayQuote = sec.dailyQuoteOf(ticker.time)
     updateDailyQuote(dayQuote, ticker)
     chainSersOf(tickerSer) find (_.freq == TFreq.DAILY) foreach (_.updateFrom(dayQuote))
     }
     }
     } */

    // batch save to db

    var willCommit = false
    val tickers = allTickers.toArray
    if (tickers.length > 0) {
      Tickers.insertBatch(tickers)
      willCommit = true
    }

    val executions = allExecutions.toArray
    if (executions.length > 0) {
      Executions.insertBatch(executions)
      willCommit = true
    }

    val minuteQuotes = Sec.minuteQuotesToClose.toArray
    if (minuteQuotes.length > 0) {
      Quotes1m.insertBatch(minuteQuotes)
      Sec.minuteQuotesToClose.clear
      willCommit = true
    }

    val dailyQuotes = updatedDailyQuotes.toArray
    if (dailyQuotes.length > 0) {
      Quotes1d.updateBatch(dailyQuotes)
      willCommit = true
    }

    // @Note if there is no update/insert on db, do not call commit, which may cause deadlock
    if (willCommit) {
      log.info("Committing: tickers=" + tickers.length + ", executions=" + executions.length + ", minuteQuotes=" + minuteQuotes.length + ", dailyQuotes=" + dailyQuotes.length)
      commit
      log.info("Committed")
    }
    
    // publish events
    if (tickers.length > 0) {
      TickerServer.publish(TickersEvent(tickers.asInstanceOf[Array[LightTicker]]))
    }
    val snapDepths = allSnapDepths.toArray
    if (snapDepths.length > 0) {
      DataServer.publish(SnapDepthsEvent(this, snapDepths))
    }

    for ((symbol, tickerInfo) <- symbolToTickerInfo) yield {
      TSerEvent.ToBeSet(tickerInfo.minSer, symbol, tickerInfo.frTime, tickerInfo.toTime, tickerInfo.lastTicker)
    }
  }

  private def updateDailyQuoteByTicker(dailyQuote: Quote, ticker: Ticker) {
    dailyQuote.open   = ticker.dayOpen
    dailyQuote.high   = ticker.dayHigh
    dailyQuote.low    = ticker.dayLow
    dailyQuote.close  = ticker.lastPrice
    dailyQuote.volume = ticker.dayVolume
    dailyQuote.amount = ticker.dayAmount
    // In case of dailyQuote being updated, should mark it as unclosed
    dailyQuote.unclosed_!
    //dailyQuote.sec.exchange.addUnclosedDailyQuote(dailyQuote)
  }

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol

  final class TickerInfo(_lastTicker: Ticker, var minSer: QuoteSer) {
    var frTime: Long = _lastTicker.time
    var toTime: Long = _lastTicker.time

    def lastTicker = _lastTicker
    def lastTicker_=(ticker: Ticker) {
      frTime = math.min(frTime, ticker.time)
      toTime = math.max(toTime, ticker.time)
    }
  }
}
