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

import java.util.{Calendar,TimeZone}
import org.aiotrade.lib.math.timeseries.{Frequency,QuoteItem,QuoteSer,Ser,SerChangeEvent,Unit}
import org.aiotrade.lib.math.timeseries.datasource.AbstractDataServer
import org.aiotrade.lib.securities.{Market,Ticker,TickerObserver,TickerPool,TickerSnapshot}
import scala.collection.mutable.{ArrayBuffer,HashMap}

/** This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
object TickerServer {
    val tickerPool = new TickerPool
}
abstract class TickerServer extends AbstractDataServer[TickerContract, Ticker] with TickerObserver[TickerSnapshot] {
    import TickerServer._
    
    private val symbolToTickerSnapshot = new HashMap[String, TickerSnapshot]

    private val symbolToIntervalLastTickerPair = new HashMap[String, IntervalLastTickerPair]
    private val symbolToPreviousTicker = new HashMap[String, Ticker]
    private val cal = Calendar.getInstance

    override
    protected def init :Unit = {
        super.init
    }

    private def borrowTicker :Ticker = {
        tickerPool.borrowObject
    }

    private def returnTicker(ticker:Ticker) :Unit = {
        tickerPool.returnObject(ticker)
    }

    override
    protected def returnBorrowedTimeValues(tickers:ArrayBuffer[Ticker]) :Unit = {
        tickers.foreach{tickerPool.returnObject(_)}
    }

    override
    def subscribe(contract:TickerContract, ser:Ser) :Unit = {
        this.subscribe(contract, ser, Nil)
    }

    def tickerSnapshotOf(symbol:String) :Option[TickerSnapshot] = {
        symbolToTickerSnapshot.get(symbol)
    }
    
    override
    def subscribe(contract:TickerContract, ser:Ser, chainSers:Seq[Ser] ) :Unit = {
        super.subscribe(contract, ser, chainSers)
        symbolToTickerSnapshot.synchronized {
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

    override
    def unSubscribe(contract:TickerContract) :Unit = {
        super.unSubscribe(contract)
        val symbol = contract.symbol
        for (x <- tickerSnapshotOf(symbol)) {
            x.deleteObserver(this)
        }
        symbolToTickerSnapshot.synchronized {
            symbolToTickerSnapshot.removeKey(symbol)
        }
        symbolToIntervalLastTickerPair.synchronized {
            symbolToIntervalLastTickerPair.removeKey(symbol)
        }
        symbolToPreviousTicker.synchronized {
            symbolToPreviousTicker.removeKey(symbol)
        }
    }

    private val bufLoadEvents = new ArrayBuffer[SerChangeEvent]

    override
    protected def postLoad :Unit = {
        bufLoadEvents.clear

        for (contract <- subscribedContracts) {
            val storage = storageOf(contract)
            val evt = composeSer(contract.symbol, serOf(contract).get, storage)

            if (evt != null) {
                evt.tpe = SerChangeEvent.Type.FinishedLoading
                evt.getSource.fireSerChangeEvent(evt)
                System.out.println(contract.symbol + ": " + count + ", items loaded, load server finished")
            }

            storage.synchronized {
                returnBorrowedTimeValues(storage)
                storage.clear
            }
        }
    }

    override
    protected def postUpdate :Unit = {
        for (contract <- subscribedContracts) {
            val storage = storageOf(contract)
            val evt = composeSer(contract.symbol, serOf(contract).get, storage)

            if (evt != null) {
                evt.tpe = SerChangeEvent.Type.Updated
                evt.getSource.fireSerChangeEvent(evt)
                //System.out.println(evt.getSymbol() + ": update event:");
            }

            storage.synchronized {
                returnBorrowedTimeValues(storage)
                storage.clear
            }
        }
    }

    override
    protected def postStopUpdateServer :Unit = {
        for (tickerSnapshot <- symbolToTickerSnapshot.values) {
            tickerSnapshot.deleteObserver(this)
        }
        symbolToTickerSnapshot.synchronized {
            symbolToTickerSnapshot.clear
        }
        symbolToIntervalLastTickerPair.synchronized {
            symbolToIntervalLastTickerPair.clear
        }
        symbolToPreviousTicker.synchronized {
            symbolToPreviousTicker.clear
        }
    }

    protected def loadFromPersistence :Long = {
        /** do nothing (tickers can be load from persistence? ) */
        loadedTime
    }

    def update(tickerSnapshot:TickerSnapshot) :Unit = {
        val ticker = borrowTicker
        ticker.copy(tickerSnapshot.ticker)
        storageOf(lookupContract(tickerSnapshot.symbol).get) += (ticker)
    }

    def composeSer(symbol:String, tickerSer:Ser, storage:ArrayBuffer[Ticker]) :SerChangeEvent = {
        var evt:SerChangeEvent = null

        val timeZone = marketOf(symbol).timeZone
        var begTime = +Long.MaxValue
        var endTime = -Long.MaxValue

        val size = storage.size
        if (size > 0) {
            val shouldReverseOrder = !isAscending(storage)

            var ticker :Ticker = null; // lastTicker will be stored in it
            val freq = tickerSer.freq
            var i = if (shouldReverseOrder) size - 1 else 0
            while (i >= 0 && i <= size - 1) {
                ticker = storage(i)
                ticker.time = (freq.round(ticker.time, timeZone))
                val prevTicker = symbolToPreviousTicker.get(symbol) match {
                    case None =>
                        val x = new Ticker
                        symbolToPreviousTicker.put(symbol, x)
                        x
                    case Some(x) => x
                }

                var item :QuoteItem = null
                symbolToIntervalLastTickerPair.get(symbol) match {
                    case None =>
                        /**
                         * this is today's first ticker we got when begin update data server,
                         * actually it should be, so maybe we should check this.
                         */
                        val intervalLastTickerPair = new IntervalLastTickerPair
                        symbolToIntervalLastTickerPair.put(symbol, intervalLastTickerPair)
                        intervalLastTickerPair.currIntervalOne.copy(ticker)

                        item = tickerSer.createItemOrClearIt(ticker.time).asInstanceOf[QuoteItem]

                        /**
                         * As this is the first data of today:
                         * 1. set OHLC = Ticker.LAST_PRICE
                         * 2. to avoid too big volume that comparing to following dataSeries.
                         * so give it a small 0.0001 (if give it a 0, it will won't be calculated
                         * in calcMaxMin() of ChartView)
                         */
                        item.open   = ticker(Ticker.LAST_PRICE)
                        item.high   = ticker(Ticker.LAST_PRICE)
                        item.low    = ticker(Ticker.LAST_PRICE)
                        item.close  = ticker(Ticker.LAST_PRICE)
                        item.volume = 0.00001f

                    case Some(intervalLastTickerPair) =>
                        /** normal process */
                        
                        /** check if in new interval */
                        freq.sameInterval(ticker.time, intervalLastTickerPair.currIntervalOne.time, timeZone)
                        if (freq.sameInterval(ticker.time, intervalLastTickerPair.currIntervalOne.time, timeZone)) {
                            intervalLastTickerPair.currIntervalOne.copy(ticker)

                            /** still in same interval, just pick out the old data of this interval */
                            item = tickerSer.getItem(ticker.time).asInstanceOf[QuoteItem]
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
                            item = tickerSer.createItemOrClearIt(ticker.time).asInstanceOf[QuoteItem]

                            item.high = -Float.MaxValue
                            item.low  = +Float.MaxValue
                            item.open = ticker(Ticker.LAST_PRICE)
                        }

                        if (ticker(Ticker.DAY_HIGH) > prevTicker(Ticker.DAY_HIGH)) {
                            /** this is a new high happened in this ticker */
                            item.high = ticker(Ticker.DAY_HIGH)
                        }
                        item.high = Math.max(item.high, ticker(Ticker.LAST_PRICE))

                        if (prevTicker(Ticker.DAY_LOW) != 0) {
                            if (ticker(Ticker.DAY_LOW) < prevTicker(Ticker.DAY_LOW)) {
                                /** this is a new low happened in this ticker */
                                item.low = (ticker(Ticker.DAY_LOW));
                            }
                        }
                        if (ticker(Ticker.LAST_PRICE) != 0) {
                            item.low = Math.min(item.low, ticker(Ticker.LAST_PRICE))
                        }

                        item.close = ticker(Ticker.LAST_PRICE)
                        val preVolume = intervalLastTickerPair.prevIntervalOne(Ticker.DAY_VOLUME)
                        if (preVolume > 1) {
                            item.volume = ticker(Ticker.DAY_VOLUME) - intervalLastTickerPair.prevIntervalOne(Ticker.DAY_VOLUME)
                        }
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
                    if (chainSer.freq.equals(Frequency.DAILY)) {
                        updateDailyQuoteItem(chainSer.asInstanceOf[QuoteSer], ticker, timeZone);
                    } else if (chainSer.freq.equals(Frequency.ONE_MIN)) {
                        updateMinuteQuoteItem(chainSer.asInstanceOf[QuoteSer], ticker, tickerSer.asInstanceOf[QuoteSer], timeZone);
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
                    val today = Unit.Day.beginTimeOfUnitThatInclude(ticker.time, timeZone)
                    for (ser <- chainSersOf(tickerSer)) {
                        if (ser.freq.equals(Frequency.DAILY)) {
                            if (ser.getItem(today) != null) {
                                updateDailyQuoteItem(ser.asInstanceOf[QuoteSer], ticker, timeZone)
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
    private def updateDailyQuoteItem(dailySer:QuoteSer, ticker:Ticker, timeZone:TimeZone) :Unit = {
        val now = Unit.Day.beginTimeOfUnitThatInclude(ticker.time, timeZone)
        var itemNow =  dailySer.getItem(now).asInstanceOf[QuoteItem]
        if (itemNow == null) {
            itemNow = dailySer.createItemOrClearIt(now).asInstanceOf[QuoteItem]
        }

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
    private def updateMinuteQuoteItem(minuteSer:QuoteSer, ticker:Ticker, tickerSer:QuoteSer, timeZone:TimeZone) :Unit = {
        val now = Unit.Minute.beginTimeOfUnitThatInclude(ticker.time, timeZone)
        val tickerItem = tickerSer.getItem(now).asInstanceOf[QuoteItem]
        var itemNow = minuteSer.getItem(now).asInstanceOf[QuoteItem]
        if (itemNow == null) {
            itemNow = minuteSer.createItemOrClearIt(now).asInstanceOf[QuoteItem]
        }

        itemNow.open   = ticker(Ticker.DAY_OPEN)
        itemNow.high   = ticker(Ticker.DAY_HIGH)
        itemNow.low    = ticker(Ticker.DAY_LOW)
        itemNow.close  = ticker(Ticker.LAST_PRICE)
        itemNow.volume = ticker(Ticker.DAY_VOLUME)

        /** be ware of fromTime here may not be same as ticker's event */
        val evt = new SerChangeEvent(minuteSer, SerChangeEvent.Type.Updated, "", now, now)
        minuteSer.fireSerChangeEvent(evt)
    }

    def marketOf(symbol:String) :Market

    private class IntervalLastTickerPair {
        val currIntervalOne = new Ticker
        val prevIntervalOne = new Ticker
    }

}
