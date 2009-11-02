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

import java.util.{Calendar}
import org.aiotrade.lib.math.timeseries.{TFreq, TSer, SerChangeEvent, TUnit}
import org.aiotrade.lib.math.timeseries.datasource.AbstractDataServer
import org.aiotrade.lib.securities.{Market, QuoteItem, QuoteSer, Ticker, TickerObserver, TickerPool, TickerSnapshot}
import org.aiotrade.lib.util.Observable
import org.aiotrade.lib.util.collection.ArrayList
import scala.collection.mutable.{HashMap}

/** This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
object TickerServer {
  val tickerPool = new TickerPool
}

abstract class TickerServer extends AbstractDataServer[TickerContract, Ticker] with TickerObserver {
  import TickerServer._
    
  private val symbolToTickerSnapshot = new HashMap[String, TickerSnapshot]

  private val symbolToIntervalLastTickerPair = new HashMap[String, IntervalLastTickerPair]
  private val symbolToPreviousTicker = new HashMap[String, Ticker]
  private val cal = Calendar.getInstance

  override protected def init: Unit = {
    super.init
  }

  private def borrowTicker: Ticker = {
    tickerPool.borrowObject
  }

  private def returnTicker(ticker:Ticker): Unit = {
    tickerPool.returnObject(ticker)
  }

  override protected def returnBorrowedTimeValues(tickers: ArrayList[Ticker]): Unit = {
    tickers foreach (tickerPool.returnObject(_))
  }

  def tickerSnapshotOf(symbol: String): Option[TickerSnapshot] = {
    symbolToTickerSnapshot.get(symbol)
  }
    
  override def subscribe(contract: TickerContract, ser: TSer, chainSers: Seq[TSer] ): Unit = {
    super.subscribe(contract, ser, chainSers)
    symbolToTickerSnapshot synchronized {
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
        tickerSnapshot.addObserver(this)
        tickerSnapshot.symbol = symbol
        symbolToTickerSnapshot.put(symbol, tickerSnapshot)
      }
    }
  }

  override def unSubscribe(contract: TickerContract): Unit = {
    super.unSubscribe(contract)
    val symbol = contract.symbol
    tickerSnapshotOf(symbol).foreach{x => x.deleteObserver(this)}
    symbolToTickerSnapshot synchronized {
      symbolToTickerSnapshot -= symbol
    }
    symbolToIntervalLastTickerPair synchronized {
      symbolToIntervalLastTickerPair -= symbol
    }
    symbolToPreviousTicker synchronized {
      symbolToPreviousTicker -= symbol
    }
  }

  private val bufLoadEvents = new ArrayList[SerChangeEvent]

  override protected def postLoad: Unit = {
    bufLoadEvents.clear

    for (contract <- subscribedContracts) {
      val storage = storageOf(contract)
      val evt = composeSer(contract.symbol, serOf(contract).get, storage)

      if (evt != null) {
        evt.tpe = SerChangeEvent.Type.FinishedLoading
        evt.getSource.fireSerChangeEvent(evt)
        println(contract.symbol + ": " + count + ", items loaded, load server finished")
      }

      storage synchronized {
        returnBorrowedTimeValues(storage)
        storage.clear
      }
    }
  }

  override protected def postUpdate: Unit = {
    for (contract <- subscribedContracts) {
      val storage = storageOf(contract)
      val evt = composeSer(contract.symbol, serOf(contract).get, storage)

      if (evt != null) {
        evt.tpe = SerChangeEvent.Type.Updated
        evt.getSource.fireSerChangeEvent(evt)
        //println(evt.symbol + ": update event:")
      }

      storage synchronized {
        returnBorrowedTimeValues(storage)
        storage.clear
      }
    }
  }

  override protected def postStopUpdateServer: Unit = {
    for (tickerSnapshot <- symbolToTickerSnapshot.values) {
      tickerSnapshot.deleteObserver(this)
    }
    symbolToTickerSnapshot synchronized {
      symbolToTickerSnapshot.clear
    }
    symbolToIntervalLastTickerPair synchronized {
      symbolToIntervalLastTickerPair.clear
    }
    symbolToPreviousTicker synchronized {
      symbolToPreviousTicker.clear
    }
  }

  protected def loadFromPersistence: Long = {
    /** do nothing (tickers can be load from persistence? ) */
    loadedTime
  }

  def update(tickerSnapshot: Observable): Unit = {
    val ts = tickerSnapshot.asInstanceOf[TickerSnapshot]
    val ticker = borrowTicker
    ticker.copy(ts.ticker)
    storageOf(lookupContract(ts.symbol).get) += ticker
  }

  def composeSer(symbol: String, tickerSer: TSer, storage: ArrayList[Ticker]): SerChangeEvent = {
    var evt: SerChangeEvent = null

    val cal = Calendar.getInstance(marketOf(symbol).timeZone)
    var begTime = +Long.MaxValue
    var endTime = -Long.MaxValue

    val size = storage.size
    if (size > 0) {
      val values = new Array[Ticker](size)
      storage.copyToArray(values, 0)
            
      val shouldReverseOrder = !isAscending(values)

      var ticker: Ticker = null // lastTicker will be stored in it
      val freq = tickerSer.freq
      var i = if (shouldReverseOrder) size - 1 else 0
      while (i >= 0 && i <= size - 1) {
        ticker = storage(i)
        ticker.time = (freq.round(ticker.time, cal))
        val prevTicker = symbolToPreviousTicker.get(symbol) match {
          case None =>
            val x = new Ticker
            symbolToPreviousTicker.put(symbol, x)
            x
          case Some(x) => x
        }

        val item = symbolToIntervalLastTickerPair.get(symbol) match {
          case None =>
            /**
             * this is today's first ticker we got when begin update data server,
             * actually it should be, so maybe we should check this.
             */
            val intervalLastTickerPair = new IntervalLastTickerPair
            symbolToIntervalLastTickerPair.put(symbol, intervalLastTickerPair)
            intervalLastTickerPair.currIntervalOne.copy(ticker)

            val item1 = tickerSer.createItemOrClearIt(ticker.time).asInstanceOf[QuoteItem]

            /**
             * As this is the first data of today:
             * 1. set OHLC = Ticker.LAST_PRICE
             * 2. to avoid too big volume that comparing to following dataSeries.
             * so give it a small 0.0001 (if give it a 0, it will won't be calculated
             * in calcMaxMin() of ChartView)
             */
            item1.open   = ticker(Ticker.LAST_PRICE)
            item1.high   = ticker(Ticker.LAST_PRICE)
            item1.low    = ticker(Ticker.LAST_PRICE)
            item1.close  = ticker(Ticker.LAST_PRICE)
            item1.volume = 0.00001f
            item1

          case Some(intervalLastTickerPair) =>
            /** normal process */
                        
            /** check if in new interval */
            freq.sameInterval(ticker.time, intervalLastTickerPair.currIntervalOne.time, cal)
            val item1 = if (freq.sameInterval(ticker.time, intervalLastTickerPair.currIntervalOne.time, cal)) {
              intervalLastTickerPair.currIntervalOne.copy(ticker)

              /** still in same interval, just pick out the old data of this interval */
              tickerSer.getItem(ticker.time).asInstanceOf[QuoteItem]
            } else {
              /**
               * !NOTICE
               * Here, should:
               * first:  intervalLastTicker.prevIntervalOne.copy(intervalLastTicker.currIntervalOne);
               * then:   intervalLastTicker.currIntervalOne.copy(ticker);
               */
              intervalLastTickerPair.prevIntervalOne.copy(intervalLastTickerPair.currIntervalOne)
              intervalLastTickerPair.currIntervalOne.copy(ticker)

              /** a new interval starts, we'll need a new data */
              val item2 = tickerSer.createItemOrClearIt(ticker.time).asInstanceOf[QuoteItem]

              item2.high = -Float.MaxValue
              item2.low  = +Float.MaxValue
              item2.open = ticker(Ticker.LAST_PRICE)
              item2
            }

            if (ticker(Ticker.DAY_HIGH) > prevTicker(Ticker.DAY_HIGH)) {
              /** this is a new high happened in this ticker */
              item1.high = ticker(Ticker.DAY_HIGH)
            }
            item1.high = Math.max(item1.high, ticker(Ticker.LAST_PRICE))

            if (prevTicker(Ticker.DAY_LOW) != 0) {
              if (ticker(Ticker.DAY_LOW) < prevTicker(Ticker.DAY_LOW)) {
                /** this is a new low happened in this ticker */
                item1.low = ticker(Ticker.DAY_LOW)
              }
            }
            if (ticker(Ticker.LAST_PRICE) != 0) {
              item1.low = Math.min(item1.low, ticker(Ticker.LAST_PRICE))
            }

            item1.close = ticker(Ticker.LAST_PRICE)
            val preVolume = intervalLastTickerPair.prevIntervalOne(Ticker.DAY_VOLUME)
            if (preVolume > 1) {
              item1.volume = ticker(Ticker.DAY_VOLUME) - intervalLastTickerPair.prevIntervalOne(Ticker.DAY_VOLUME)
            }
            item1
        }

        prevTicker.copy(ticker)

        if (shouldReverseOrder) {
          i -= 1
        } else {
          i += 1
        }

        val itemTime = item.time
        begTime = Math.min(begTime, itemTime)
        endTime = Math.max(endTime, itemTime)

        /**
         * Now, try to update today's quoteSer with current last ticker
         */
        for (chainSer <- chainSersOf(tickerSer)) {
          if (chainSer.freq.equals(TFreq.DAILY)) {
            updateDailyQuoteItem(chainSer.asInstanceOf[QuoteSer], ticker, cal)
          } else if (chainSer.freq.equals(TFreq.ONE_MIN)) {
            updateMinuteQuoteItem(chainSer.asInstanceOf[QuoteSer], ticker, tickerSer.asInstanceOf[QuoteSer], cal)
          }
        }

      }

      /**
       * ! ticker may be null at here ???
       */
      evt = new SerChangeEvent(tickerSer, SerChangeEvent.Type.None, symbol, begTime, endTime, ticker)
    } else {

      /**
       * no new ticker got, but should consider if need to update quoteSer
       * as the quote window may be just opened.
       */
      symbolToPreviousTicker.get(symbol) match {
        case None =>
        case Some(ticker) =>
          val today = TUnit.Day.beginTimeOfUnitThatInclude(ticker.time, cal)
          for (ser <- chainSersOf(tickerSer)) {
            if (ser.freq.equals(TFreq.DAILY)) {
              if (ser.getItem(today) != null) {
                updateDailyQuoteItem(ser.asInstanceOf[QuoteSer], ticker, cal)
              }
            }
          }

      }
    }

    evt
  }

  /**
   * Try to update today's quote item according to ticker, if it does not
   * exist, create a new one.
   */
  private def updateDailyQuoteItem(dailySer: QuoteSer, ticker: Ticker, cal: Calendar): Unit = {
    val now = TUnit.Day.beginTimeOfUnitThatInclude(ticker.time, cal)
    val itemNow = dailySer.createItemOrClearIt(now).asInstanceOf[QuoteItem]
        
    if (ticker(Ticker.DAY_HIGH) != 0 && ticker(Ticker.DAY_LOW) != 0) {
      itemNow.open   = ticker(Ticker.DAY_OPEN)
      itemNow.high   = ticker(Ticker.DAY_HIGH)
      itemNow.low    = ticker(Ticker.DAY_LOW)
      itemNow.close  = ticker(Ticker.LAST_PRICE)
      itemNow.volume = ticker(Ticker.DAY_VOLUME)

      itemNow.close_ori = ticker(Ticker.LAST_PRICE)
      itemNow.close_adj = ticker(Ticker.LAST_PRICE)

      /** be ware of fromTime here may not be same as ticker's event */
      val evt = new SerChangeEvent(dailySer, SerChangeEvent.Type.Updated, "", now, now)
      dailySer.fireSerChangeEvent(evt)
    }
  }

  /**
   * Try to update today's quote item according to ticker, if it does not
   * exist, create a new one.
   */
  private def updateMinuteQuoteItem(minuteSer: QuoteSer, ticker: Ticker, tickerSer: QuoteSer, cal: Calendar): Unit = {
    val now = TUnit.Minute.beginTimeOfUnitThatInclude(ticker.time, cal)
    val itemNow = minuteSer.createItemOrClearIt(now).asInstanceOf[QuoteItem]

    itemNow.open   = ticker(Ticker.DAY_OPEN)
    itemNow.high   = ticker(Ticker.DAY_HIGH)
    itemNow.low    = ticker(Ticker.DAY_LOW)
    itemNow.close  = ticker(Ticker.LAST_PRICE)
    itemNow.volume = ticker(Ticker.DAY_VOLUME)

    /** be ware of fromTime here may not be same as ticker's event */
    val evt = new SerChangeEvent(minuteSer, SerChangeEvent.Type.Updated, "", now, now)
    minuteSer.fireSerChangeEvent(evt)
  }

  def marketOf(symbol: String): Market

  private class IntervalLastTickerPair {
    val currIntervalOne = new Ticker
    val prevIntervalOne = new Ticker
  }

}
