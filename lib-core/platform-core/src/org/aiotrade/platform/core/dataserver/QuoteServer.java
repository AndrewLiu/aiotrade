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

import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.math.timeseries.QuoteItem;
import org.aiotrade.math.timeseries.SerChangeEvent;
import org.aiotrade.math.timeseries.datasource.AbstractDataServer;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.platform.core.sec.Market;
import org.aiotrade.platform.core.sec.Quote;
import org.aiotrade.platform.core.sec.QuotePool;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public abstract class QuoteServer extends AbstractDataServer<QuoteContract, Quote> {

    private final static QuotePool quotePool = PersistenceManager.getDefault().getQuotePool();

    protected final Quote borrowQuote() {
        return quotePool.borrowObject();
    }

    protected final void returnQuote(Quote quote) {
        quotePool.returnObject(quote);
    }

    protected void returnBorrowedTimeValues(Collection<Quote> quotes) {
        for (Quote quote : quotes) {
            quotePool.returnObject(quote);
        }
    }

    protected void loadFromPersistence() {
        for (QuoteContract contract : getSubscribedContracts()) {
            loadFromPersistence(contract);
        }
    }

    private void loadFromPersistence(QuoteContract contract) {
        Ser serToBeFilled = getSer(contract);

        /**
         * 1. restore data from database
         */
        Frequency freq = serToBeFilled.getFreq();
        List<Quote> storage = PersistenceManager.getDefault().restoreQuotes(contract.getSymbol(), freq);
        composeSer(contract.getSymbol(), serToBeFilled, storage);

        /**
         * 2. get the newest time which DataServer will load quotes after this time
         * if quotes is empty, means no data in db, so, let newestTime = 0, which
         * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
         */
        int size = storage.size();
        setLoadedTime(size > 0 ? storage.get(size - 1).getTime() : 0);
        serToBeFilled.fireSerChangeEvent(new SerChangeEvent(serToBeFilled, SerChangeEvent.Type.RefreshInLoading, contract.getSymbol(), 0, getLoadedTime()));

        /**
         * 3. clear quotes for following loading usage, as these quotes is borrowed
         * from pool, return them
         */
        synchronized (storage) {
            returnBorrowedTimeValues(storage);
            storage.clear();
        }
    }

    @Override
    protected void postLoad() {
        for (QuoteContract contract : getSubscribedContracts()) {
            Ser serToBeFilled = getSer(contract);

            Frequency freq = serToBeFilled.getFreq();
            List<Quote> storage = getStorage(contract);
            PersistenceManager.getDefault().saveQuotes(contract.getSymbol(), freq, storage, getSourceId());

            SerChangeEvent evt = composeSer(contract.getSymbol(), serToBeFilled, storage);

            if (evt != null) {
                evt.setType(SerChangeEvent.Type.FinishedLoading);
                //WindowManager.getDefault().setStatusText(contract.getSymbol() + ": " + getCount() + " quote data loaded, load server finished");
            } else {
                /** even though, we may have loaded data in preLoad(), so, still need fire a FinishedLoading event */
                long loadedTime = serToBeFilled.lastOccurredTime();
                evt = new SerChangeEvent(serToBeFilled, SerChangeEvent.Type.FinishedLoading, contract.getSymbol(), loadedTime, loadedTime);
            }

            serToBeFilled.fireSerChangeEvent(evt);

            synchronized (storage) {
                returnBorrowedTimeValues(storage);
                storage.clear();
            }
        }
    }

    @Override
    protected void postUpdate() {
        for (QuoteContract contract : getSubscribedContracts()) {
            List<Quote> storage = getStorage(contract);
            SerChangeEvent evt = composeSer(contract.getSymbol(), getSer(contract), storage);

            if (evt != null) {
                evt.setType(SerChangeEvent.Type.Updated);
                evt.getSource().fireSerChangeEvent(evt);
                //WindowManager.getDefault().setStatusText(contract.getSymbol() + ": update event:");
            }

            synchronized (storage) {
                returnBorrowedTimeValues(storage);
                storage.clear();
            }
        }
    }

    public SerChangeEvent composeSer(String symbol, Ser quoteSer, List<Quote> storage) {
        SerChangeEvent evt = null;

        TimeZone timeZone = getMarket(symbol).getTimeZone();
        long begTime = +Long.MAX_VALUE;
        long endTime = -Long.MAX_VALUE;

        int size = storage.size();
        if (size > 0) {
            boolean shouldReverseOrder = isAscending(storage) ? false : true;

            Frequency freq = quoteSer.getFreq();
            int i = shouldReverseOrder ? size - 1 : 0;
            while (i >= 0 && i <= size - 1) {
                Quote quote = storage.get(i);
                quote.setTime(freq.round(quote.getTime(), timeZone));

                QuoteItem item = (QuoteItem) quoteSer.createItemOrClearIt(quote.getTime());

                item.setOpen(quote.getOpen());
                item.setHigh(quote.getHigh());
                item.setLow(quote.getLow());
                item.setClose(quote.getClose());
                item.setVolume(quote.getVolume());

                item.setClose_Ori(quote.getClose());

                float adjuestedClose = quote.getClose_adj() != 0 ? quote.getClose_adj() : quote.getClose();
                item.setClose_Adj(adjuestedClose);

                if (shouldReverseOrder) {
                    /** the recent quote's index is more in quotes, thus the order in timePositions[] is opposed to quotes */
                    i--;
                } else {
                    /** the recent quote's index is less in quotes, thus the order in timePositions[] is same as quotes */
                    i++;
                }

                long itemTime = item.getTime();
                begTime = Math.min(begTime, itemTime);
                endTime = Math.max(endTime, itemTime);
            }

            evt = new SerChangeEvent(quoteSer, null, symbol, begTime, endTime);
        }

        return evt;
    }

    /**
     * Override to provide your options
     * @return supported frequency array.
     */
    public Frequency[] getSupportedFreqs() {
        return new Frequency[]{Frequency.DAILY};
    }

    public final boolean isFreqSupported(Frequency freq) {
        boolean freqSupported = false;

        Frequency[] supportedFreqs = getSupportedFreqs();
        if (supportedFreqs != null) {
            for (Frequency afreq : supportedFreqs) {
                if (afreq.equals(freq)) {
                    freqSupported = true;
                    break;
                }
            }
        } else {
            /**
             * means supporting customed freqs (such as csv etc.), should ask
             * contract if it has been set, so what ever:
             */
            freqSupported = getCurrentContract() != null ? getCurrentContract().getFreq().equals(freq) ? true : false : false;
        }

        return freqSupported;
    }

    public abstract Market getMarket(String symbol);
}



