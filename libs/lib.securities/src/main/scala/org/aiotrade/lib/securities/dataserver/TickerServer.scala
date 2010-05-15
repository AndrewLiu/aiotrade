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
import org.aiotrade.lib.math.timeseries.{TFreq, TSerEvent, TUnit}
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.{QuoteSer, TickerSnapshot, PersistenceManager}
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.ChangeObserver
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.swing.Reactor

/** This class will load the quote data from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
abstract class TickerServer extends DataServer[Ticker] with ChangeObserver with Reactor {
  type C = TickerContract
  type T = QuoteSer

  private val logger = Logger.getLogger(this.getClass.getName)
    
  private val symbolToTickerSnapshot = new HashMap[String, TickerSnapshot]

  private val symbolToIntervalLastTickerPair = new HashMap[String, IntervalLastTickerPair]
  private val symbolToPrevTicker = new HashMap[String, Ticker]
  private val cal = Calendar.getInstance

  val updater: Updater = {
    case ts: TickerSnapshot =>
      val ticker = new Ticker
      ticker.copyFrom(ts)
      storageOf(contractOf(ts.symbol).get) += ticker
  }

  actorActions += {
    case Loaded(loadedTime) =>
      postLoad
    case Refreshed(loadedTime) =>
      postRefresh
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

  protected def postLoad {
    bufLoadEvents.clear

    for (contract <- subscribedContracts) {
      val storage = storageOf(contract).toArray
      PersistenceManager().saveRealTimeTickers(storage, sourceId)

      composeSer(contract.symbol, serOf(contract).get, storage) match {
        case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
          source.publish(TSerEvent.FinishedLoading(source, symbol, fromTime, toTime, lastObject, callback))
          logger.info(contract.symbol + ": " + count + ", data loaded, load server finished")
        case _ =>
      }

      storage synchronized {storageOf(contract).clear}
    }
  }

  protected def postRefresh {
    for (contract <- subscribedContracts) {
      val storage = storageOf(contract).toArray
      PersistenceManager().saveRealTimeTickers(storage, sourceId)
      
      composeSer(contract.symbol, serOf(contract).get, storage) match {
        case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
          source.publish(TSerEvent.Updated(source, symbol, fromTime, toTime, lastObject, callback))
        case _ =>
      }

      storageOf(contract) synchronized {storageOf(contract).clear}
    }
  }

  override protected def postStopRefreshServer {
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

    val size = tickers.length
    if (size > 0) {
      val values = new Array[Ticker](size)
      tickers.copyToArray(values, 0)
            
      val shouldReverseOrder = !isAscending(values)

      var ticker: Ticker = null // lastTicker will be stored in it
      val freq = tickerSer.freq
      var i = if (shouldReverseOrder) size - 1 else 0
      while (i >= 0 && i <= size - 1) {
        ticker = tickers(i)
        //ticker.time = (freq.round(ticker.time, cal))
        val prevTicker = symbolToPrevTicker.get(symbol) getOrElse {
          val x = new Ticker
          symbolToPrevTicker(symbol) = x
          x
        }

        val time = freq.round(ticker.time, cal)
        symbolToIntervalLastTickerPair.get(symbol) match {
          case None =>
            /**
             * this is today's first ticker we got when begin update data server,
             * actually it should be, so maybe we should check this.
             */
            val intervalLastTickerPair = new IntervalLastTickerPair
            symbolToIntervalLastTickerPair(symbol) = intervalLastTickerPair
            intervalLastTickerPair.currIntervalOne.copyFrom(ticker)

            tickerSer.createOrClear(time)

            /**
             * As this is the first data of today:
             * 1. set OHLC = Ticker.LAST_PRICE
             * 2. to avoid too big volume that comparing to following dataSeries.
             * so give it a small 0.0001 (if give it a 0, it will won't be calculated
             * in calcMaxMin() of ChartView)
             */
            tickerSer.open(time)   = ticker.lastPrice
            tickerSer.high(time)   = ticker.lastPrice
            tickerSer.low(time)    = ticker.lastPrice
            tickerSer.close(time)  = ticker.lastPrice
            tickerSer.volume(time) = 0.00001F

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
              tickerSer.createOrClear(time)
              tickerSer.high(time) = Float.MinValue
              tickerSer.low(time)  = Float.MaxValue
              tickerSer.open(time) = ticker.lastPrice
            }

            if (ticker.dayHigh > prevTicker.dayHigh) {
              /** this is a new high happened in this ticker */
              tickerSer.high(time) = ticker.dayHigh
            }
            tickerSer.high(time) = math.max(tickerSer.high(time), ticker.lastPrice)

            if (prevTicker.dayLow != 0) {
              if (ticker.dayLow < prevTicker.dayLow) {
                /** this is a new low that happened in this ticker */
                tickerSer.low(time) = ticker.dayLow
              }
            }
            if (ticker.lastPrice != 0) {
              tickerSer.low(time) = math.min(tickerSer.low(time), ticker.lastPrice)
            }

            tickerSer.close(time) = ticker.lastPrice
            val preVolume = intervalLastTickerPair.prevIntervalOne.dayVolume
            if (preVolume > 1) {
              tickerSer.volume(time) = ticker.dayVolume - intervalLastTickerPair.prevIntervalOne.dayVolume
            }
        }

        prevTicker.copyFrom(ticker)

        if (shouldReverseOrder) {
          i -= 1
        } else {
          i += 1
        }

        begTime = math.min(begTime, time)
        endTime = math.max(endTime, time)

        /**
         * Now, try to update today's quoteSer with current last ticker
         */
        for (chainSer <- chainSersOf(tickerSer)) {
          chainSer.freq match {
            case TFreq.DAILY =>
              updateDailyQuote(chainSer, ticker, cal)
            case  TFreq.ONE_MIN =>
              updateMinuteQuote(chainSer, ticker, tickerSer, cal)
          }
        }

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
      symbolToPrevTicker.get(symbol) foreach {ticker =>
        val today = TUnit.Day.beginTimeOfUnitThatInclude(ticker.time, cal)
        for (ser <- chainSersOf(tickerSer) if ser.freq == TFreq.DAILY && ser.exists(today)) {
          updateDailyQuote(ser, ticker, cal)
        }
      }
    }

    evt
  }

  /**
   * Try to update today's quote item according to ticker, if it does not
   * exist, create a new one.
   */
  private def updateDailyQuote(dailySer: QuoteSer, ticker: Ticker, cal: Calendar) {
    val now = TUnit.Day.beginTimeOfUnitThatInclude(ticker.time, cal)
    dailySer.createOrClear(now)
        
    if (ticker.dayHigh != 0 && ticker.dayLow != 0) {
      dailySer.open(now)   = ticker.dayOpen
      dailySer.high(now)   = ticker.dayHigh
      dailySer.low(now)    = ticker.dayLow
      dailySer.close(now)  = ticker.lastPrice
      dailySer.volume(now) = ticker.dayVolume

      dailySer.close_ori(now) = ticker.lastPrice
      dailySer.close_adj(now) = ticker.lastPrice

      /** be ware of fromTime here may not be same as ticker's event */
      dailySer.publish(TSerEvent.Updated(dailySer, "", now, now))
    }
  }

  /**
   * Try to update today's quote according to ticker, if it does not
   * exist, create a new one.
   */
  private def updateMinuteQuote(minuteSer: QuoteSer, ticker: Ticker, tickerSer: QuoteSer, cal: Calendar) {
    val now = TUnit.Minute.beginTimeOfUnitThatInclude(ticker.time, cal)
    minuteSer.createOrClear(now)

    minuteSer.open(now)   = tickerSer.open(now)
    minuteSer.high(now)   = tickerSer.high(now)
    minuteSer.low(now)    = tickerSer.low(now)
    minuteSer.close(now)  = tickerSer.close(now)
    minuteSer.volume(now) = tickerSer.volume(now)

    /** be ware of fromTime here may not be same as ticker's event */
    minuteSer.publish(TSerEvent.Updated(minuteSer, "", now, now))
  }

  private class IntervalLastTickerPair {
    val currIntervalOne = new Ticker
    val prevIntervalOne = new Ticker
  }

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol
}
