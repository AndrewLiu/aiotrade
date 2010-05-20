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
import java.util.Calendar
import org.aiotrade.lib.math.timeseries.{TFreq, TSerEvent}
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.{QuoteSer, TickerSnapshot}
import org.aiotrade.lib.securities.model.Tickers
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.FillRecord
import org.aiotrade.lib.securities.model.FillRecords
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Secs
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.ChangeObserver
import org.aiotrade.lib.util.collection.ArrayList
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
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
    
  private val symbolToTickerSnapshot = new HashMap[String, TickerSnapshot]

  private val symbolToIntervalLastTickerPair = new HashMap[String, IntervalLastTickerPair]
  private val symbolToPrevTicker = new HashMap[String, Ticker]
  private val secToLastQuote = new HashMap[Sec, LastQuote]
  private val cal = Calendar.getInstance

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

  def tickerSnapshotOf(symbol: String): Option[TickerSnapshot] = {
    symbolToTickerSnapshot.get(symbol)
  }
    
  override def subscribe(contract: TickerContract, ser: QuoteSer, chainSers: List[QuoteSer]) {
    super.subscribe(contract, ser, chainSers)
    subscribingMutex synchronized {
      /**
       * !NOTICE
       * the symbol-tickerSnapshot pair must be immutable, other wise, if
       * the symbol is subscribed repeatly by outside, the old observers
       * of tickerSnapshot may not know there is a new tickerSnapshot for
       * this symbol, the older one won't be updated any more.
       */
      val symbol = contract.symbol
      if (!symbolToTickerSnapshot.contains(symbol)) {
        val tickerSnapshot = new TickerSnapshot
        tickerSnapshot.addObserver(this, this)
        tickerSnapshot.symbol = symbol
        symbolToTickerSnapshot(symbol) = tickerSnapshot
      }
    }
  }

  override def unSubscribe(contract: TickerContract) {
    super.unSubscribe(contract)
    subscribingMutex synchronized {
      val symbol = contract.symbol
      tickerSnapshotOf(symbol) foreach {this unObserve _}
      symbolToTickerSnapshot -= symbol
      symbolToIntervalLastTickerPair -= symbol
      symbolToPrevTicker -= symbol
    }
  }

  private val bufLoadEvents = new ArrayBuffer[TSerEvent]

  override protected def postLoadHistory {
    bufLoadEvents.clear

    for (contract <- subscribedContracts) {
      val storage = storageOf(contract).toArray
      val sec = Exchange.secOf(contract.symbol).get
      val dailyQuote = lastQuoteOf(sec).dailyOne
      assert(Quotes1d.idOf(dailyQuote) != None, "dailyQuote of " + sec.secInfo.uniSymbol + " is transient")
      storage foreach (_.quote = dailyQuote)
      Tickers.insertBatch(storage)

      composeSer(contract.symbol, serOf(contract).get, storage) match {
        case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
          source.publish(TSerEvent.FinishedLoading(source, symbol, fromTime, toTime, lastObject, callback))
          logger.info(contract.symbol + ": " + count + ", data loaded, load server finished")
        case _ =>
      }

      Tickers.evictCaches(storage)
      storageOf(contract) synchronized {storageOf(contract).clear}
    }
  }

  override protected def postRefresh {
    for (contract <- subscribedContracts) {
      val storage = storageOf(contract).toArray
      val sec = Exchange.secOf(contract.symbol).get
      val dailyQuote = lastQuoteOf(sec).dailyOne
      assert(Quotes1d.idOf(dailyQuote) != None, "dailyQuote of " + sec.secInfo.uniSymbol + " is transient")
      storage foreach (_.quote = dailyQuote)
      Tickers.insertBatch(storage)
      
      composeSer(contract.symbol, serOf(contract).get, storage) match {
        case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
          source.publish(TSerEvent.Updated(source, symbol, fromTime, toTime, lastObject, callback))
        case _ =>
      }

      Tickers.evictCaches(storage)
      storageOf(contract) synchronized {storageOf(contract).clear}
    }
  }

  override protected def postStopRefresh {
    for (tickerSnapshot <- symbolToTickerSnapshot.valuesIterator) {
      tickerSnapshot.removeObserver(this)
    }
    symbolToTickerSnapshot synchronized {
      symbolToTickerSnapshot.clear
    }
    symbolToIntervalLastTickerPair synchronized {
      symbolToIntervalLastTickerPair.clear
    }
    symbolToPrevTicker synchronized {
      symbolToPrevTicker.clear
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
  def composeSer(symbol: String, tickerSer: QuoteSer, tickers: Array[Ticker]): TSerEvent = {
    var evt: TSerEvent = TSerEvent.None

    val cal = Calendar.getInstance(Exchange.exchangeOf(toUniSymbol(symbol)).timeZone)
    var begTime = Long.MaxValue
    var endTime = Long.MinValue

    val sec = Exchange.secOf(symbol).get
    val dailyQuote = lastQuoteOf(sec).dailyOne
    val size = tickers.length
    if (size > 0) {

      val values = new Array[Ticker](size)
      tickers.copyToArray(values, 0)
            
      val shouldReverseOrder = !isAscending(values)

      val fillRecords = new Array[FillRecord](size)
      var ticker: Ticker = null // lastTicker will be stored in it
      val freq = tickerSer.freq
      var i = if (shouldReverseOrder) size - 1 else 0
      while (i >= 0 && i <= size - 1) {
        ticker = tickers(i)

        val fillRecord = new FillRecord
        fillRecords(i) = fillRecord
        fillRecord.quote = dailyQuote
        fillRecord.time = ticker.time
        
        val prevTicker = symbolToPrevTicker.get(symbol) match {
          case None =>
            fillRecord.price  = ticker.lastPrice
            fillRecord.volume = ticker.dayVolume
            fillRecord.amount = ticker.dayAmount

            val prev = new Ticker
            symbolToPrevTicker += (symbol -> prev)
            prev
          case Some(prev) =>
            fillRecord.price  = ticker.lastPrice
            fillRecord.volume = ticker.dayVolume - prev.dayVolume
            fillRecord.amount = ticker.dayAmount - prev.dayAmount

            prev
        }

        val time = freq.round(ticker.time, cal)
        val minuteQuote = minuteQuoteOf(sec, ticker.time, cal)

        symbolToIntervalLastTickerPair.get(symbol) match {
          case None =>
            /**
             * this is today's first ticker we got when begin update data server,
             * actually it should be, so maybe we should check this.
             */
            val intervalLastTickerPair = new IntervalLastTickerPair
            symbolToIntervalLastTickerPair(symbol) = intervalLastTickerPair
            intervalLastTickerPair.currIntervalOne.copyFrom(ticker)

            /**
             * As this is the first data of today:
             * 1. set OHLC = Ticker.LAST_PRICE
             * 2. to avoid too big volume that comparing to following dataSeries.
             * so give it a small 0.0001 (if give it a 0, it will won't be calculated
             * in calcMaxMin() of ChartView)
             */
            minuteQuote.open   = ticker.lastPrice
            minuteQuote.high   = ticker.lastPrice
            minuteQuote.low    = ticker.lastPrice
            minuteQuote.close  = ticker.lastPrice
            minuteQuote.volume = 0.00001F
            minuteQuote.amount = 0.00001F

          case Some(intervalLastTickerPair) =>
            /** normal process */
                        
            /** check if in new interval */
            if (freq.sameInterval(time, intervalLastTickerPair.currIntervalOne.time, cal)) {
              intervalLastTickerPair.currIntervalOne.copyFrom(ticker)

              /** still in same interval, just pick out the old data of this interval */
            } else {
              /**
               * !NOTICE
               * Here, should:
               * first:  intervalLastTicker.prevIntervalOne.copy(intervalLastTicker.currIntervalOne);
               * then:   intervalLastTicker.currIntervalOne.copy(ticker);
               */
              intervalLastTickerPair.prevIntervalOne.copyFrom(intervalLastTickerPair.currIntervalOne)
              intervalLastTickerPair.currIntervalOne.copyFrom(ticker)

              /** a new interval starts, we'll need a new data */
              minuteQuote.high = Float.MinValue
              minuteQuote.low  = Float.MaxValue
              minuteQuote.open = ticker.lastPrice
            }

            if (ticker.dayHigh > prevTicker.dayHigh) {
              /** this is a new high happened in this ticker */
              minuteQuote.high = ticker.dayHigh
            }
            minuteQuote.high = math.max(minuteQuote.high, ticker.lastPrice)

            if (prevTicker.dayLow != 0) {
              if (ticker.dayLow < prevTicker.dayLow) {
                /** this is a new low that happened in this ticker */
                minuteQuote.low = ticker.dayLow
              }
            }
            if (ticker.lastPrice != 0) {
              minuteQuote.low = math.min(minuteQuote.low, ticker.lastPrice)
            }

            minuteQuote.close = ticker.lastPrice
            val prevVolume = intervalLastTickerPair.prevIntervalOne.dayVolume
            if (prevVolume > 1) {
              minuteQuote.volume = ticker.dayVolume - intervalLastTickerPair.prevIntervalOne.dayVolume
              minuteQuote.amount = ticker.dayAmount - intervalLastTickerPair.prevIntervalOne.dayAmount
            }
        }

        prevTicker.copyFrom(ticker)

        begTime = math.min(begTime, time)
        endTime = math.max(endTime, time)

        // update 1m quoteSer with minuteQuote
        updateQuoteSer(tickerSer, minuteQuote)
        chainSersOf(tickerSer) find (_.freq == TFreq.ONE_MIN) foreach (updateQuoteSer(_, minuteQuote))

        i += (if (shouldReverseOrder) -1 else 1)
      }

      // update daily quote and ser
      if (ticker != null && ticker.dayHigh != 0 && ticker.dayLow != 0) {
        updateDailyQuote(dailyQuote, ticker)
        chainSersOf(tickerSer) find (_.freq == TFreq.DAILY) foreach (updateQuoteSer(_, dailyQuote))
      }

      FillRecords.insertBatch(fillRecords)
      FillRecords.evictCaches(fillRecords)
      val toBeClosed = minuteQuotesToBeClosed.toArray
      if (toBeClosed.length > 0) {
        Quotes1m.insertBatch(toBeClosed)
        Quotes1m.evictCaches(toBeClosed)
        minuteQuotesToBeClosed.clear
      }

      /**
       * ! ticker may be null at here ??? yes, if tickers.size == 0
       */
      evt = TSerEvent.ToBeSet(tickerSer, symbol, begTime, endTime, ticker)
    } else {

      /**
       * no new ticker got, but should consider if need to update quoteSer
       * as the quote window may be just opened.
       */
      symbolToPrevTicker.get(symbol) match {
        case Some(ticker) =>
          if (ticker != null && ticker.dayHigh != 0 && ticker.dayLow != 0) {
            updateDailyQuote(dailyQuote, ticker)
            chainSersOf(tickerSer) find (_.freq == TFreq.DAILY) foreach (updateQuoteSer(_, dailyQuote))
          }
        case None =>
      }
    }

    evt
  }

  private def updateDailyQuote(dailyQuote: Quote, ticker: Ticker) {
    dailyQuote.open   = ticker.dayOpen
    dailyQuote.high   = ticker.dayHigh
    dailyQuote.low    = ticker.dayLow
    dailyQuote.close  = ticker.lastPrice
    dailyQuote.volume = ticker.dayVolume
    dailyQuote.amount = ticker.dayAmount
  }

  /**
   * Try to update today's quote item according to quote, if it does not
   * exist, create a new one.
   */
  private def updateQuoteSer(seq: QuoteSer, quote: Quote) {
    val now = quote.time
    seq.createOrClear(now)
        
    seq.open(now)   = quote.open
    seq.high(now)   = quote.high
    seq.low(now)    = quote.low
    seq.close(now)  = quote.close
    seq.volume(now) = quote.volume

    seq.close_ori(now) = quote.close
    seq.close_adj(now) = quote.close

    /** be ware of fromTime here may not be same as ticker's event */
    seq.publish(TSerEvent.Updated(seq, "", now, now))
  }

  private class IntervalLastTickerPair {
    val currIntervalOne = new Ticker
    val prevIntervalOne = new Ticker
  }

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol

  protected case class LastQuote(var dailyOne: Quote, var minuteOne: Quote)

  /**
   * @Note when day changes, should do secToLastQuote -= sec, this can be done
   * by listening to exchange's timer event
   */
  protected def lastQuoteOf(sec: Sec): LastQuote = {
    assert(Secs.idOf(sec) != None, "Sec: " + sec + " is transient")
    secToLastQuote.get(sec) match {
      case Some(lastQuote) => lastQuote
      case None =>
        val cal = Calendar.getInstance(sec.exchange.timeZone)
        val now = TFreq.DAILY.round(System.currentTimeMillis, cal)

        val dailyQuote = (SELECT (Quotes1d.*) FROM (Quotes1d) WHERE (
            (Quotes1d.sec.field EQ Secs.idOf(sec)) AND (Quotes1d.time EQ now)
          ) unique) match {
          case Some(quote) => quote
          case None =>
            val quote = new Quote
            quote.time = now
            quote.sec = sec
            quote.unclosed_! // @todo when to close it and update to db?
            Quotes1d.save(quote)
            quote
        }

        val lastQuote = LastQuote(dailyQuote, null)
        secToLastQuote += (sec -> lastQuote)
        lastQuote
    }
  }

  /**
   * @Note when day changes, should do secToLastQuote -= sec, this can be done
   * by listening to exchange's timer event
   */
  private val minuteQuotesToBeClosed = new ArrayList[Quote]()
  protected def minuteQuoteOf(sec: Sec, time: Long, cal: Calendar): Quote = {
    val lastQuote = lastQuoteOf(sec)
    val now = TFreq.ONE_MIN.round(time, cal)
    lastQuote.minuteOne match {
      case quote: Quote if quote.time == now => quote
      case prevOne => // minute changes or null
        if (prevOne != null) {
          prevOne.closed_!
          minuteQuotesToBeClosed += prevOne
        }

        val quote = new Quote
        quote.time = now
        quote.sec = sec
        quote.unclosed_!
        lastQuote.minuteOne = quote
        quote
    }
  }

}
