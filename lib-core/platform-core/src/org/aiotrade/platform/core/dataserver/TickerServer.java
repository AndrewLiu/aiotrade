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
package org.aiotrade.platform.core.dataserver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.math.timeseries.QuoteItem;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.SerChangeEvent;
import org.aiotrade.math.timeseries.datasource.AbstractDataServer;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.math.timeseries.Unit;
import org.aiotrade.platform.core.sec.Market;
import org.aiotrade.platform.core.sec.Ticker;
import org.aiotrade.platform.core.sec.TickerObserver;
import org.aiotrade.platform.core.sec.TickerPool;
import org.aiotrade.platform.core.sec.TickerSnapshot;

/** This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public abstract class TickerServer extends AbstractDataServer<TickerContract, Ticker> implements TickerObserver<TickerSnapshot> {

    private final static TickerPool tickerPool = PersistenceManager.getDefault().getTickerPool();
    private final Map<String, TickerSnapshot> symbolToTickerSnapshot = new HashMap<String, TickerSnapshot>();

    ;
    private final Map<String, IntervalLastTickerPair> symbolToIntervalLastTickerPair = new HashMap<String, IntervalLastTickerPair>();
    private final Map<String, Ticker> symbolToPreviousTicker = new HashMap<String, Ticker>();
    private Calendar cal = Calendar.getInstance();

    @Override
    protected void init() {
        super.init();
    }

    private final Ticker borrowTicker() {
        return tickerPool.borrowObject();
    }

    private final void returnTicker(Ticker ticker) {
        tickerPool.returnObject(ticker);
    }

    protected void returnBorrowedTimeValues(Collection<Ticker> tickers) {
        for (Ticker ticker : tickers) {
            tickerPool.returnObject(ticker);
        }
    }

    @Override
    public void subscribe(TickerContract contract, Ser ser) {
        this.subscribe(contract, ser, Collections.<Ser>emptyList());
    }

    @Override
    public void subscribe(TickerContract contract, Ser ser, Collection<Ser> chainSers) {
        super.subscribe(contract, ser, chainSers);
        synchronized (symbolToTickerSnapshot) {
            /**
             * !NOTICE
             * the symbol-tickerSnapshot pair must be immutable, other wise, if
             * the symbol is subscribed repeatly by outside, the old observers
             * of tickerSnapshot may not know there is a new tickerSnapshot for
             * this symbol, the older one won't be updated any more.
             */
            final String symbol = contract.getSymbol();
            if (!symbolToTickerSnapshot.containsKey(symbol)) {
                TickerSnapshot tickerSnapshot = new TickerSnapshot();
                tickerSnapshot.addObserver(this);
                tickerSnapshot.setSymbol(symbol);
                symbolToTickerSnapshot.put(symbol, tickerSnapshot);
            }
        }
    }

    @Override
    public void unSubscribe(TickerContract contract) {
        super.unSubscribe(contract);
        final String symbol = contract.getSymbol();
        TickerSnapshot tickerSnapshot = getTickerSnapshot(symbol);
        if (tickerSnapshot != null) {
            tickerSnapshot.deleteObserver(this);
        }
        synchronized (symbolToTickerSnapshot) {
            symbolToTickerSnapshot.remove(symbol);
        }
        synchronized (symbolToIntervalLastTickerPair) {
            symbolToIntervalLastTickerPair.remove(symbol);
        }
        synchronized (symbolToPreviousTicker) {
            symbolToPreviousTicker.remove(symbol);
        }
    }

    public TickerSnapshot getTickerSnapshot(String symbol) {
        return symbolToTickerSnapshot.get(symbol);
    }
    private List<SerChangeEvent> bufLoadEvents = new ArrayList();

    @Override
    protected void postLoad() {
        bufLoadEvents.clear();

        for (TickerContract contract : getSubscribedContracts()) {
            List<Ticker> storage = getStorage(contract);
            SerChangeEvent evt = composeSer(contract.getSymbol(), getSer(contract), storage);

            if (evt != null) {
                evt.setType(SerChangeEvent.Type.FinishedLoading);
                evt.getSource().fireSerChangeEvent(evt);
                System.out.println(contract.getSymbol() + ": " + getCount() + ", items loaded, load server finished");
            }

            synchronized (storage) {
                returnBorrowedTimeValues(storage);
                storage.clear();
            }
        }
    }

    @Override
    protected void postUpdate() {
        for (TickerContract contract : getSubscribedContracts()) {
            List<Ticker> storage = getStorage(contract);
            SerChangeEvent evt = composeSer(contract.getSymbol(), getSer(contract), storage);

            if (evt != null) {
                evt.setType(SerChangeEvent.Type.Updated);
                evt.getSource().fireSerChangeEvent(evt);
                //System.out.println(evt.getSymbol() + ": update event:");
            }

            synchronized (storage) {
                returnBorrowedTimeValues(storage);
                storage.clear();
            }
        }
    }

    @Override
    protected void postStopUpdateServer() {
        for (TickerSnapshot tickerSnapshot : symbolToTickerSnapshot.values()) {
            tickerSnapshot.deleteObserver(this);
        }
        synchronized (symbolToTickerSnapshot) {
            symbolToTickerSnapshot.clear();
        }
        synchronized (symbolToIntervalLastTickerPair) {
            symbolToIntervalLastTickerPair.clear();
        }
        synchronized (symbolToPreviousTicker) {
            symbolToPreviousTicker.clear();
        }
    }

    protected void loadFromPersistence() {
        /** do nothing (tickers can be load from persistence? ) */
    }

    public void update(TickerSnapshot tickerSnapshot) {
        Ticker ticker = borrowTicker();
        ticker.copy(tickerSnapshot.readTicker());
        getStorage(lookupContract(tickerSnapshot.getSymbol())).add(ticker);
    }

    public SerChangeEvent composeSer(String symbol, Ser tickerSer, List<Ticker> storage) {
        SerChangeEvent event = null;

        TimeZone timeZone = getMarket(symbol).getTimeZone();
        long begTime = +Long.MAX_VALUE;
        long endTime = -Long.MAX_VALUE;

        int size = storage.size();
        if (size > 0) {
            boolean shouldReverseOrder = isAscending(storage) ? false : true;

            Ticker ticker = null; // lastTicker will be stored in it
            Frequency freq = tickerSer.getFreq();
            int i = shouldReverseOrder ? size - 1 : 0;
            while (i >= 0 && i <= size - 1) {
                ticker = storage.get(i);
                ticker.setTime(freq.round(ticker.getTime(), timeZone));
                IntervalLastTickerPair intervalLastTickerPair = symbolToIntervalLastTickerPair.get(symbol);
                Ticker previousTicker = symbolToPreviousTicker.get(symbol);
                if (previousTicker == null) {
                    previousTicker = new Ticker();
                    symbolToPreviousTicker.put(symbol, previousTicker);
                }

                QuoteItem item;
                if (intervalLastTickerPair == null) {
                    /**
                     * this is today's first ticker we got when begin update data server,
                     * actually it should be, so maybe we should check this.
                     */
                    intervalLastTickerPair = new IntervalLastTickerPair();
                    symbolToIntervalLastTickerPair.put(symbol, intervalLastTickerPair);
                    intervalLastTickerPair.currIntervalOne.copy(ticker);

                    item = (QuoteItem) tickerSer.createItemOrClearIt(ticker.getTime());

                    /**
                     * As this is the first data of today:
                     * 1. set OHLC = Ticker.LAST_PRICE
                     * 2. to avoid too big volume that comparing to following dataSeries.
                     * so give it a small 0.0001 (if give it a 0, it will won't be calculated
                     * in calcMaxMin() of ChartView)
                     */
                    item.setOpen(ticker.get(Ticker.LAST_PRICE));
                    item.setHigh(ticker.get(Ticker.LAST_PRICE));
                    item.setLow(ticker.get(Ticker.LAST_PRICE));
                    item.setClose(ticker.get(Ticker.LAST_PRICE));
                    item.setVolume(0.00001f);

                } else {
                    /** normal process */
                    /** check if in new interval */
                    if (freq.sameInterval(ticker.getTime(), intervalLastTickerPair.currIntervalOne.getTime(), timeZone)) {
                        intervalLastTickerPair.currIntervalOne.copy(ticker);

                        /** still in same interval, just pick out the old data of this interval */
                        item = (QuoteItem) tickerSer.getItem(ticker.getTime());
                    } else {
                        /**
                         * !NOTICE
                         * Here, should:
                         * first:  intervalLastTicker.prevIntervalOne.copy(intervalLastTicker.currIntervalOne);
                         * then:   intervalLastTicker.currIntervalOne.copy(ticker);
                         */
                        intervalLastTickerPair.prevIntervalOne.copy(intervalLastTickerPair.currIntervalOne);
                        intervalLastTickerPair.currIntervalOne.copy(ticker);

                        /** a new interval starts, we'll need a new data */
                        item = (QuoteItem) tickerSer.createItemOrClearIt(ticker.getTime());

                        item.setHigh(-Float.MAX_VALUE);
                        item.setLow(+Float.MAX_VALUE);
                        item.setOpen(ticker.get(Ticker.LAST_PRICE));
                    }

                    if (ticker.get(Ticker.DAY_HIGH) > previousTicker.get(Ticker.DAY_HIGH)) {
                        /** this is a new high happened in this ticker */
                        item.setHigh(ticker.get(Ticker.DAY_HIGH));
                    }
                    item.setHigh(Math.max(item.getHigh(), ticker.get(Ticker.LAST_PRICE)));

                    if (previousTicker.get(Ticker.DAY_LOW) != 0) {
                        if (ticker.get(Ticker.DAY_LOW) < previousTicker.get(Ticker.DAY_LOW)) {
                            /** this is a new low happened in this ticker */
                            item.setLow(ticker.get(Ticker.DAY_LOW));
                        }
                    }
                    if (ticker.get(Ticker.LAST_PRICE) != 0) {
                        item.setLow(Math.min(item.getLow(), ticker.get(Ticker.LAST_PRICE)));
                    }

                    item.setClose(ticker.get(Ticker.LAST_PRICE));
                    float preVolume = intervalLastTickerPair.prevIntervalOne.get(Ticker.DAY_VOLUME);
                    if (preVolume > 1) {
                        item.setVolume(ticker.get(Ticker.DAY_VOLUME) - intervalLastTickerPair.prevIntervalOne.get(Ticker.DAY_VOLUME));
                    }
                }

                previousTicker.copy(ticker);

                if (shouldReverseOrder) {
                    i--;
                } else {
                    i++;
                }

                long itemTime = item.getTime();
                begTime = Math.min(begTime, itemTime);
                endTime = Math.max(endTime, itemTime);

                /**
                 * Now, try to update today's quoteSer with current last ticker
                 */
                for (Ser x : getChainSers(tickerSer)) {
                    QuoteSer chainSer = (QuoteSer) x;
                    if (chainSer.getFreq().equals(Frequency.DAILY)) {
                        updateDailyQuoteItem(chainSer, ticker, timeZone);
                    } else if (chainSer.getFreq().equals(Frequency.ONE_MIN)) {
                        updateMinuteQuoteItem(chainSer, ticker, (QuoteSer) tickerSer, timeZone);
                    }
                }

            }

            /**
             * ! ticker may be null at here ???
             */
            event = new SerChangeEvent(tickerSer, null, symbol, begTime, endTime, ticker);
        } else {

            /**
             * no new ticker got, but should consider if need to update quoteSer
             * as the quote window may be just opened.
             */
            Ticker ticker = symbolToPreviousTicker.get(symbol);
            if (ticker != null) {
                long today = Unit.Day.beginTimeOfUnitThatInclude(ticker.getTime(), timeZone);
                for (Ser s : getChainSers(tickerSer)) {
                    if (s.getFreq().equals(Frequency.DAILY)) {
                        if (s.getItem(today) != null) {
                            updateDailyQuoteItem((QuoteSer) s, ticker, timeZone);
                        }
                    }
                }
            }
        }

        return event;

    }

    /**
     * Try to update today's quote item according to ticker, if it does not
     * exist, create a new one.
     */
    private void updateDailyQuoteItem(QuoteSer dailySer, Ticker ticker, TimeZone timeZone) {
        long now = Unit.Day.beginTimeOfUnitThatInclude(ticker.getTime(), timeZone);
        QuoteItem itemNow = (QuoteItem) dailySer.getItem(now);
        if (itemNow == null) {
            itemNow = (QuoteItem) dailySer.createItemOrClearIt(now);
        }

        if (ticker.get(Ticker.DAY_HIGH) != 0 && ticker.get(Ticker.DAY_LOW) != 0) {
            itemNow.setOpen(ticker.get(Ticker.DAY_OPEN));
            itemNow.setHigh(ticker.get(Ticker.DAY_HIGH));
            itemNow.setLow(ticker.get(Ticker.DAY_LOW));
            itemNow.setClose(ticker.get(Ticker.LAST_PRICE));
            itemNow.setVolume(ticker.get(Ticker.DAY_VOLUME));

            itemNow.setClose_Ori(ticker.get(Ticker.LAST_PRICE));
            itemNow.setClose_Adj(ticker.get(Ticker.LAST_PRICE));

            /** be ware of fromTime here may not be same as ticker's event */
            SerChangeEvent evt = new SerChangeEvent(dailySer, SerChangeEvent.Type.Updated, "", now, now);
            dailySer.fireSerChangeEvent(evt);
        }
    }

    /**
     * Try to update today's quote item according to ticker, if it does not
     * exist, create a new one.
     */
    private void updateMinuteQuoteItem(QuoteSer minuteSer, Ticker ticker, QuoteSer tickerSer, TimeZone timeZone) {
        long now = Unit.Minute.beginTimeOfUnitThatInclude(ticker.getTime(), timeZone);
        QuoteItem tickerItem = (QuoteItem) tickerSer.getItem(now);
        QuoteItem itemNow = (QuoteItem) minuteSer.getItem(now);
        if (itemNow == null) {
            itemNow = (QuoteItem) minuteSer.createItemOrClearIt(now);
        }

        itemNow.setOpen(tickerItem.getOpen());
        itemNow.setHigh(tickerItem.getHigh());
        itemNow.setLow(tickerItem.getLow());
        itemNow.setClose(tickerItem.getClose());
        itemNow.setVolume(tickerItem.getVolume());

        /** be ware of fromTime here may not be same as ticker's event */
        SerChangeEvent evt = new SerChangeEvent(minuteSer, SerChangeEvent.Type.Updated, "", now, now);
        minuteSer.fireSerChangeEvent(evt);
    }

    private class IntervalLastTickerPair {

        Ticker currIntervalOne = new Ticker();
        Ticker prevIntervalOne = new Ticker();
    }

    public abstract Market getMarket(String symbol);
}




