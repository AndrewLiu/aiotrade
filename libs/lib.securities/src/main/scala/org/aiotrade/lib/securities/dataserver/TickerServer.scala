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
import org.aiotrade.lib.math.timeseries.{TFreq, TSerEvent}
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.{QuoteSer, TickerSnapshot}
import org.aiotrade.lib.securities.model.Tickers
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.FillRecord
import org.aiotrade.lib.securities.model.FillRecordEvent
import org.aiotrade.lib.securities.model.FillRecords
import org.aiotrade.lib.securities.model.MarketDepth
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.ChangeObserver
import org.aiotrade.lib.collection.ArrayList
import ru.circumflex.orm._

/** This class will load the quote data from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
abstract class TickerServer extends DataServer[Ticker] with ChangeObserver {
  type C = TickerContract
  type T = QuoteSer

  private val logger = Logger.getLogger(this.getClass.getName)

  refreshable = true

  val updater: Updater = {
    case ts: TickerSnapshot =>
      val ticker = new Ticker
      ticker.copyFrom(ts)
      // store ticker first, will batch process when got Refreshed event
      storageOf(contractOf(ts.symbol).get) += ticker
  }

  reactions += {
    case Exchange.Opened(exchange: Exchange) =>
    case Exchange.Closed(exchange: Exchange) =>
  }

  listenTo(Exchange)

  def tickerSnapshotOf(uniSymbol: String): TickerSnapshot = {
    val sec = Exchange.secOf(uniSymbol).get
    sec.lastData.tickerSnapshot
  }

  override def subscribe(contract: TickerContract, ser: QuoteSer, chainSers: List[QuoteSer]) {
    super.subscribe(contract, ser, chainSers)
    /**
     * !NOTICE
     * the symbol-tickerSnapshot pair must be immutable, other wise, if
     * the symbol is subscribed repeatly by outside, the old observers
     * of tickerSnapshot may not know there is a new tickerSnapshot for
     * this symbol, the older one won't be updated any more.
     */
    val symbol = contract.symbol
    val sec = Exchange.secOf(contract.symbol).get
    val tickerSnapshot = sec.lastData.tickerSnapshot
    this observe tickerSnapshot
    tickerSnapshot.symbol = symbol
  }

  override def unSubscribe(contract: TickerContract) {
    super.unSubscribe(contract)
    val symbol = contract.symbol
    val sec = Exchange.secOf(contract.symbol).get
    val tickerSnapshot = sec.lastData.tickerSnapshot
    this unObserve tickerSnapshot
  }

  override protected def postLoadHistory {
    composeSer foreach {event =>
      event match {
        case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
          source.publish(TSerEvent.FinishedLoading(source, symbol, fromTime, toTime, lastObject, callback))
          logger.info(symbol + ": " + count + ", data loaded, load server finished")
        case _ =>
      }
    }
  }

  override protected def postRefresh {
    composeSer foreach {event =>
      event match {
        case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
          source.publish(TSerEvent.Updated(source, symbol, fromTime, toTime, lastObject, callback))
        case _ =>
      }
    }

  }

  override protected def postStopRefresh {
    for (contract <- subscribedContracts) {
      unSubscribe(contract)
    }
  }

  protected def loadFromPersistence: Long = {
    /** do nothing (tickers can be load from persistence? ) */
    loadedTime
  }

  /**
   * compose ser using data from TVal(s)
   * @param symbol
   * @param serToBeFilled Ser
   * @param TVal(s)
   */
  def composeSer: Seq[TSerEvent] = {
    val events = new ArrayList[TSerEvent]

    val allTickers = new ArrayList[Ticker]
    val allFillRecords = new ArrayList[FillRecord]
    val allSnapDepths = new ArrayList[SnapDepth]

    for (contract <- subscribedContracts) {
      val storage = storageOf(contract)
      val tickers = storage.toArray
      val symbol = contract.symbol
      val sec = Exchange.secOf(symbol).get
      val tickerSer = serOf(contract).get

      var frTime = Long.MaxValue
      var toTime = Long.MinValue

      val size = tickers.length
      if (size > 0) {

        val values = new Array[Ticker](size)
        tickers.copyToArray(values, 0)
            
        val shouldReverseOrder = !isAscending(values)

        var ticker: Ticker = null // to store last ticker
        var i = if (shouldReverseOrder) size - 1 else 0
        while (i >= 0 && i <= size - 1) {
          ticker = tickers(i)

          val dayQuote = sec.dailyQuoteOf(ticker.time)
          assert(Quotes1d.idOf(dayQuote) != None, "dailyQuote of " + sec.secInfo.uniSymbol + " is transient")
          ticker.quote = dayQuote
          allTickers += ticker

          val fillRecord = new FillRecord
          fillRecord.quote = dayQuote
          fillRecord.time = ticker.time
          allFillRecords += fillRecord
        
          val (prevTicker, dayFirst) = sec.lastData.prevTicker match {
            case null =>
              val prev = new Ticker
              sec.lastData.prevTicker = prev
              (prev, true)
            case prev => (prev, false)
          }

          val minQuote = sec.minuteQuoteOf(ticker.time)
          if (dayFirst) {
            dayQuote.unjustOpen_!

            /**
             * this is today's first ticker we got when begin update data server,
             * actually it should be, so maybe we should check this.
             * As this is the first data of today:
             * 1. set OHLC = Ticker.LAST_PRICE
             * 2. to avoid too big volume that comparing to following dataSeries.
             * so give it a small 0.0001 (if give it a 0, it will won't be calculated
             * in calcMaxMin() of ChartView)
             */
            fillRecord.price  = ticker.lastPrice
            fillRecord.volume = ticker.dayVolume
            fillRecord.amount = ticker.dayAmount

            minQuote.open   = ticker.lastPrice
            minQuote.high   = ticker.lastPrice
            minQuote.low    = ticker.lastPrice
            minQuote.close  = ticker.lastPrice
            minQuote.volume = 0.00001F
            minQuote.amount = 0.00001F
          
          } else {
          
            fillRecord.price  = ticker.lastPrice
            fillRecord.volume = ticker.dayVolume - prevTicker.dayVolume
            fillRecord.amount = ticker.dayAmount - prevTicker.dayAmount
            
            if (minQuote.justOpen_?) {
              minQuote.unjustOpen_!

              minQuote.open  = ticker.lastPrice
              minQuote.high  = ticker.lastPrice
              minQuote.low   = ticker.lastPrice
              minQuote.close = ticker.lastPrice
            
            } else {

              if (prevTicker.dayHigh != 0) {
                if (ticker.dayHigh > prevTicker.dayHigh) {
                  /** this is a new day high happened during this ticker */
                  minQuote.high = ticker.dayHigh
                }
              }
              if (ticker.lastPrice != 0) {
                minQuote.high = math.max(minQuote.high, ticker.lastPrice)
              }
            
              if (prevTicker.dayLow != 0) {
                if (ticker.dayLow < prevTicker.dayLow) {
                  /** this is a new day low happened during this ticker */
                  minQuote.low = ticker.dayLow
                }
              }
              if (ticker.lastPrice != 0) {
                minQuote.low = math.min(minQuote.low, ticker.lastPrice)
              }

              minQuote.close = ticker.lastPrice
              if (fillRecord.volume > 1) {
                minQuote.volume += fillRecord.volume
                minQuote.amount += fillRecord.amount
              }
            }
          }

          val prevPrice = if (dayFirst) ticker.prevClose else prevTicker.lastPrice
          val prevDepth = if (dayFirst) MarketDepth.Empty else MarketDepth(prevTicker.bidAsks, copy = true)

          frTime = math.min(frTime, ticker.time)
          toTime = math.max(toTime, ticker.time)

          // update 1m quoteSer with minuteQuote
          tickerSer.updateFrom(minQuote)
          chainSersOf(tickerSer) find (_.freq == TFreq.ONE_MIN) foreach (_.updateFrom(minQuote))

          allSnapDepths += SnapDepth(prevPrice, prevDepth, fillRecord)

          sec.lastData.prevTicker.copyFrom(ticker)

          sec.publish(FillRecordEvent(ticker.prevClose, fillRecord))

          i += (if (shouldReverseOrder) -1 else 1)
        }

        // update daily quote and ser
        if (ticker != null && ticker.dayHigh != 0 && ticker.dayLow != 0) {
          val dayQuote = sec.dailyQuoteOf(ticker.time)
          updateDailyQuote(dayQuote, ticker)
          chainSersOf(tickerSer) find (_.freq == TFreq.DAILY) foreach (_.updateFrom(dayQuote))
        }

        /**
         * ! ticker may be null at here ??? yes, if tickers.size == 0
         */
        events += TSerEvent.ToBeSet(tickerSer, symbol, frTime, toTime, ticker)
      } else {

        /**
         * no new ticker got, but should consider if need to update quoteSer
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
      }


      storage synchronized {storage.clear}
    }

    // batch save to db

    Tickers.insertBatch(allTickers.toArray)

    val fillRecords = allFillRecords.toArray
    FillRecords.insertBatch(fillRecords)

    val toClose = Sec.minuteQuotesToClose.toArray
    if (toClose.length > 0) {
      Quotes1m.insertBatch(toClose)
      Sec.minuteQuotesToClose.clear
    }

    commit

    // process events

    DataServer.publish(SnapDepthsEvent(this, allSnapDepths.toArray))
    events
  }

  private def updateDailyQuote(dailyQuote: Quote, ticker: Ticker) {
    dailyQuote.open   = ticker.dayOpen
    dailyQuote.high   = ticker.dayHigh
    dailyQuote.low    = ticker.dayLow
    dailyQuote.close  = ticker.lastPrice
    dailyQuote.volume = ticker.dayVolume
    dailyQuote.amount = ticker.dayAmount
  }

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol
}
