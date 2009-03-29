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
package org.aiotrade.platform.modules.dataserver.ib;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.TickType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.math.timeseries.Unit;
import org.aiotrade.lib.math.timeseries.datasource.DataServer;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.platform.core.WindowManager;
import org.aiotrade.platform.core.sec.Quote;
import org.aiotrade.platform.core.sec.QuotePool;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.platform.core.sec.Ticker;
import org.aiotrade.platform.core.sec.TickerPool;
import org.aiotrade.platform.core.sec.TickerSnapshot;

/**
 *
 * @author Caoyuan Deng
 */
public class IBWrapper extends EWrapperAdapter {
    private static QuotePool quotePool = PersistenceManager.getDefault().getQuotePool();
    private static TickerPool tickerPool = PersistenceManager.getDefault().getTickerPool();
    
    private final static DateFormat TWS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    private final static String HISTORICAL_DATA_END = "finished";
    private final static int HIS_REQ_PROC_SPEED_THROTTLE = 1000 * 20; // 20 seconds
    
    private static SortedMap<Frequency, Integer> barSizeMap;
    static {
        barSizeMap = new TreeMap<Frequency, Integer>();
        barSizeMap.put(Frequency.ONE_SEC,      1);
        barSizeMap.put(Frequency.FIVE_SECS,    2);
        barSizeMap.put(Frequency.FIFTEEN_SECS, 3);
        barSizeMap.put(Frequency.THREE_SECS,   4);
        barSizeMap.put(Frequency.ONE_MIN,      5);
        barSizeMap.put(Frequency.TWO_MINS,     6);
        barSizeMap.put(Frequency.THREE_MINS,   16);
        barSizeMap.put(Frequency.FIVE_MINS,    7);
        barSizeMap.put(Frequency.FIFTEEN_MINS, 8);
        barSizeMap.put(Frequency.THIRTY_MINS,  9);
        barSizeMap.put(Frequency.ONE_HOUR,     10);
        barSizeMap.put(Frequency.DAILY,        11);
        barSizeMap.put(Frequency.WEEKLY,       12);
        barSizeMap.put(Frequency.MONTHLY,      13);
        barSizeMap.put(Frequency.THREE_MONTHS, 14);
        barSizeMap.put(Frequency.ONE_YEAR,     15);
    }
    
    private static Map<Sec.Type, String> secTypesWithNameMap;
    static {
        secTypesWithNameMap = new HashMap<Sec.Type, String>();
        secTypesWithNameMap.put(Sec.Type.Stock,        "STK");
        secTypesWithNameMap.put(Sec.Type.Option,       "OPT");
        secTypesWithNameMap.put(Sec.Type.Future,       "FUT");
        secTypesWithNameMap.put(Sec.Type.Index,        "IND");
        secTypesWithNameMap.put(Sec.Type.FutureOption, "FOP");
        secTypesWithNameMap.put(Sec.Type.Currency,     "CASH");
        secTypesWithNameMap.put(Sec.Type.Bag,          "BAG");
    }
    
    private static IBWrapper singletonInstance;
    private static EClientSocket eclient;
    
    private static HisRequestServer hisRequestServer;
    
    private String host = "";
    private int port = 7496;
    private int clientId = 0;
    
    /** in IB, the next valid id after connected should be 1 */
    private int nextReqId = 1;
    private final SortedMap<Integer, HistoricalDataRequest> hisDataRequestMap = new TreeMap<Integer, HistoricalDataRequest>();
    private final SortedMap<Integer, MarketDataRequest> mktDataRequestMap = new TreeMap<Integer, MarketDataRequest>();
    
    private int serverVersion;
    
    private IBWrapper() {
        if (eclient == null) {
            eclient = new EClientSocket(this);
        }
    }
    
    public static IBWrapper getInstance() {
        return singletonInstance == null ?
            singletonInstance = new IBWrapper() :
            singletonInstance;
    }
    
    public static int getBarSize(Frequency freq) {
        for (Frequency afreq : barSizeMap.keySet()) {
            if (afreq.equals(freq)) {
                return barSizeMap.get(afreq);
            }
        }
        
        return 1;
    }
    
    public static Frequency[] getSupportedFreqs() {
        return barSizeMap.keySet().toArray(new Frequency[barSizeMap.size()]);
    }
    
    public static String getSecType(Sec.Type type) {
        return secTypesWithNameMap.get(type);
    }
    
    private synchronized final int askReqId() {
        final int reqId = nextReqId;
        nextReqId++;
        
        return reqId;
    }
    
    private List<Quote> getQuoteStorage(int reqId) {
        HistoricalDataRequest hisReq = hisDataRequestMap.get(reqId);
        return hisReq != null ? hisReq.storage : null;
    }
    
    private DataServer getHisDataRequestor(int reqId) {
        HistoricalDataRequest hisReq = hisDataRequestMap.get(reqId);
        return hisReq != null ? hisReq.requestor : null;
    }
    
    public boolean isHisDataReqPending(int reqId) {
        return hisDataRequestMap.containsKey(reqId);
    }
    
    public synchronized void connect() {
        if (isConnected()) {
            return;
        }
        
        eclient.eConnect(host, port, clientId);
        boolean timeout = false;
        while (!isConnected() && !timeout) {
            try {
                wait(Unit.Second.getInterval() * 5);
                timeout = true; // whatever
            } catch (InterruptedException ex) {
                break;
            }
        }
        
        if (isConnected()) {
            /**
             * IB Log levels: 1 = SYSTEM 2 = ERROR 3 = WARNING 4 = INFORMATION 5 = DETAIL
             */
            eclient.setServerLogLevel(2);
            eclient.reqNewsBulletins(true);
            serverVersion = eclient.serverVersion();
            
            WindowManager.getDefault().setStatusText("TWS connected. Server version: " + serverVersion);
        } else {
            WindowManager.getDefault().setStatusText("Could not connect to TWS.");
        }
    }
    
    public boolean isConnected() {
        return eclient.isConnected();
    }
    
    public DateFormat getTwsDateFormart() {
        return TWS_DATE_FORMAT;
    }
    
    public int reqHistoricalData(DataServer requestor, List<Quote> storage,
            Contract contract, String endDateTime, String durationStr,
            int barSizeSetting, String whatToShow, int useRTH, int formatDate) {
        
        int reqId = askReqId();
        
        HistoricalDataRequest hisReq = new HistoricalDataRequest();
        hisReq.requestor = requestor;
        hisReq.storage = storage;
        hisReq.contract = contract;
        hisReq.endDateTime = endDateTime;
        hisReq.durationStr = durationStr;
        hisReq.barSizeSetting = barSizeSetting;
        hisReq.whatToShow = whatToShow;
        hisReq.useRTH = useRTH;
        hisReq.formatDate = formatDate;
        hisReq.reqId = reqId;
        
        synchronized (hisDataRequestMap) {
            hisDataRequestMap.put(new Integer(reqId), hisReq);
        }
        
        if (hisRequestServer == null) {
            hisRequestServer = new HisRequestServer();
        }
        
        if (!hisRequestServer.isInRunning()) {
            new Thread(hisRequestServer).start();
        }
        
        return reqId;
    }
    
    public int reqMktData(DataServer requestor, Contract contract, TickerSnapshot tickerSnapshot) {
        int reqId = askReqId();
        
        MarketDataRequest mktReq = new MarketDataRequest();
        mktReq.contract = contract;
        mktReq.snapshotTicker = tickerSnapshot;
        mktReq.reqId = reqId;
        
        synchronized (mktDataRequestMap) {
            mktDataRequestMap.put(new Integer(reqId), mktReq);
        }
        
        eclient.reqMktData(reqId, contract);
        
        return reqId;
    }
    
    public void cancelHisDataRequest(int reqId) {
        eclient.cancelHistoricalData(reqId);
        clearHisDataRequest(reqId);
    }
    
    public void cancelMktDataRequest(int reqId) {
        eclient.cancelMktData(reqId);
        synchronized (mktDataRequestMap) {
            mktDataRequestMap.remove(reqId);
        }
    }
    
    public boolean isMktDataRequested(int reqId) {
        return mktDataRequestMap.containsKey(reqId);
    }
    
    private TickerSnapshot getTickerSnapshot(int reqId) {
        MarketDataRequest mktReq = mktDataRequestMap.get(reqId);
        return mktReq != null ? mktReq.snapshotTicker : null;
    }
    
    private void clearHisDataRequest(int hisReqId) {
        synchronized (hisDataRequestMap) {
            hisDataRequestMap.remove(hisReqId);
        }
    }
    
    public int getServerVersion() {
        return serverVersion;
    }
    
    public void disconnect() {
        if (eclient != null && eclient.isConnected()) {
            eclient.cancelNewsBulletins();
            eclient.eDisconnect();
        }
    }
    
    @Override
    public void nextValidId( int orderId) {
        /** 
         * this seems only called one when connected or re-connected. As we use
         * auto-increase id, we can just ignore it?
         */
    }
    
    /** A historical data arrived */
    @Override
    public void historicalData(int reqId, String date,
            double open, double high, double low, double close, int volume, double WAP, boolean hasGaps) {
        
        List<Quote> storage = getQuoteStorage(reqId);
        if (storage == null) {
            return;
        }
        
        /** we only need lock storage here */
        synchronized (storage) {
            try {
                if (date.startsWith(HISTORICAL_DATA_END)) {
                    DataServer requstor = getHisDataRequestor(reqId);
                    if (requstor != null) {
                        synchronized (requstor) {
                            requstor.notifyAll();
                            System.out.println("requstor nofity all: finished");
                        }
                    }
                    clearHisDataRequest(reqId);
                } else {
                    long time = 0;
                    try {
                        time = Long.parseLong(date) * 1000;
                    } catch (NumberFormatException ex) {
                        return;
                    }
                    
                    Quote quote = quotePool.borrowObject();
                    
                    quote.setTime(time);
                    quote.setOpen((float)open);
                    quote.setHigh((float)high);
                    quote.setLow((float)low);
                    quote.setClose((float)close);
                    quote.setVolume(volume);
                    
                    quote.setWAP((float)WAP);
                    quote.setHasGaps(hasGaps);
                    
                    storage.add(quote);
                    
                    /** quote is still pending for process, don't return it */
                }
            } catch (Throwable t) {
                /**
                 * Catch any Throwable to prevent them back to the eclient (will cause disconnect).
                 * We don't need cancel this historical requset as the next data may be good.
                 */
            }
        }
    }
    
    @Override
    public void tickPrice( int tickerId, int field, double price, int canAutoExecute) {
        // received price tick
        TickerSnapshot tickerSnapshot = getTickerSnapshot(tickerId);
        if (tickerSnapshot == null) {
            return;
        }
        
        synchronized (tickerSnapshot) {
            final float value = (float)price;
            tickerSnapshot.setTime(System.currentTimeMillis());
            switch (field) {
                case TickType.ASK:
                    tickerSnapshot.setAskPrice(0, value);
                    break;
                case TickType.ASK_SIZE:
                    tickerSnapshot.setAskSize(0, value);
                    break;
                case TickType.BID:
                    tickerSnapshot.setBidPrice(0, value);
                    break;
                case TickType.BID_SIZE:
                    tickerSnapshot.setBidSize(0, value);
                    break;
                case TickType.CLOSE:
                    tickerSnapshot.set(Ticker.PREV_CLOSE, value);
                    break;
                case TickType.HIGH:
                    tickerSnapshot.set(Ticker.DAY_HIGH, value);
                    break;
                case TickType.LAST:
                    tickerSnapshot.set(Ticker.LAST_PRICE, value);
                    break;
                case TickType.LAST_SIZE:
                    break;
                case TickType.LOW:
                    tickerSnapshot.set(Ticker.DAY_LOW, value);
                    break;
                case TickType.VOLUME:
                    tickerSnapshot.set(Ticker.DAY_VOLUME, value);
                    break;
                default:
            }
        }
        
        tickerSnapshot.notifyObservers();
        
        //System.out.println("id=" + tickerId + "  " + TickType.getField( field) + "=" + price + " " +
        //(canAutoExecute != 0 ? " canAutoExecute" : " noAutoExecute"));
    }
    
    @Override
    public void tickSize( int tickerId, int field, int size) {
        // received size tick
        tickPrice(tickerId, field, size, 0);
    }
    
    @Override
    public void tickOptionComputation( int tickerId, int field, double impliedVol, double delta) {
        // received price tick
        //        System.out.println( "id=" + tickerId + "  " + TickType.getField( field) + ": vol = " +
        //                ((impliedVol >= 0 && impliedVol != Double.MAX_VALUE) ? Double.toString(impliedVol) : "N/A") + " delta = " +
        //                ((Math.abs(delta) <= 1) ? Double.toString(delta) : "N/A") );
    }
    
    
    @Override
    public void error(String error) {
        WindowManager.getDefault().setStatusText(error);
    }
    
    private StringBuilder msg = new StringBuilder(40);;
    @Override
    public void error(int id, int errorCode, String errorMsg) {
        msg.delete(0, msg.length());
        if (id < 0) {
            /** connected or not connected msg, notify connect() waiting */
            synchronized (this) {
                notifyAll();
            }
        } else {
            msg.append("Error: reqId = ");
        }
        msg.append(id).append(" | ")
                .append(errorCode).append(" : ")
                .append(errorMsg).toString();
        
        System.out.println(msg.toString());
        WindowManager.getDefault().setStatusText(msg.toString());
        
        /** process error concerns with hisReq */
        boolean shouldResetAllHisReqs = (errorCode == 1102) ||
                (errorCode == 165 && msg.toString().contains("HMDS connection attempt failed")) ||
                (errorCode == 165 && msg.toString().contains("HMDS server disconnect occurred"));
        
        if (shouldResetAllHisReqs) {
            for (Integer reqId : hisDataRequestMap.keySet()) {
                resetHisReq(reqId);
            }
        } else {
            if (hisDataRequestMap.containsKey(id)) {
                resetHisReq(id);
            }
        }
        
    }
    
    private void resetHisReq(int reqId) {
        DataServer requstor = getHisDataRequestor(reqId);
        if (requstor != null) {
            synchronized (requstor) {
                requstor.notifyAll();
                //System.out.println("requstor nofity all on error");
            }
            /** Don't do this before requstor has been fetched from map ! */
            cancelHisDataRequest(reqId);
        }
    }
    
    @Override
    public void connectionClosed() {
    }
    
    private class MarketDataRequest {
        Contract contract;
        TickerSnapshot snapshotTicker;
        int reqId;
    }
    
    private class HistoricalDataRequest {
        DataServer requestor;
        List<Quote> storage;
        Contract contract;
        String endDateTime;
        String durationStr;
        int barSizeSetting;
        String whatToShow;
        int useRTH;
        int formatDate;
        int reqId;
    }
    
    private class HisRequestServer implements Runnable {
        
        private boolean inRunning;
        
        private boolean isInRunning() {
            return inRunning;
        }
        
        public void run() {
            inRunning = true;
            
            boolean inRoundProcessing = false;
            while (!inRoundProcessing) {
                try {
                    Thread.currentThread().sleep(HIS_REQ_PROC_SPEED_THROTTLE);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    inRunning = false;
                    return;
                }
                
                inRoundProcessing = true;
                
                for (Integer reqId : hisDataRequestMap.keySet()) {
                    HistoricalDataRequest hisReq = hisDataRequestMap.get(reqId);
                    eclient.reqHistoricalData(
                            hisReq.reqId,
                            hisReq.contract,
                            hisReq.endDateTime,
                            hisReq.durationStr,
                            hisReq.barSizeSetting,
                            hisReq.whatToShow,
                            hisReq.useRTH,
                            hisReq.formatDate);
                    
                    /** just fetch the first one to process, so, break at once */
                    break;
                }
                
                inRoundProcessing = false;
            }
        }
    }
    
}

