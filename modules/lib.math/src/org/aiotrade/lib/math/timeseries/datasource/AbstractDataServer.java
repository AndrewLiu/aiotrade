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
package org.aiotrade.lib.math.timeseries.datasource;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.SerChangeEvent;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * <K, V> data contract type, data pool type
 *
 * @author Caoyuan Deng
 */
public abstract class AbstractDataServer<K extends DataContract, V extends TimeValue> implements DataServer<K> {

    protected final static long ANCIENT_TIME = -1;
    private static Image DEFAULT_ICON;
    private static ExecutorService executorService;
    // --- Following maps should be created once here, since server may be singleton:
    private final Map<K, List<V>> contractToStorage = new HashMap<K, List<V>>();
    private final Map<K, Ser> subscribedContractToSer = new HashMap<K, Ser>();
    /** a quick seaching map */
    private final Map<String, K> subscribedSymbolToContract = new HashMap<String, K>();
    /**
     * first ser is the master one,
     * second one (if available) is that who concerns first one.
     * Example: ticker ser also will compose today's quoteSer
     */
    private final Map<Ser, Collection<Ser>> serToChainSers = new HashMap<Ser, Collection<Ser>>();
    // --- Above maps should be created once here, since server may be singleton:
    private boolean inLoading;
    private LoadServer loadServer;
    private boolean inUpdating;
    private UpdateServer updateServer;
    private Timer updateTimer;
    private int count;
    private DateFormat dateFormat;
    private InputStream inputStream;
    private long loadedTime;
    private long fromTime;
    protected final Calendar sourceCal;

    public AbstractDataServer() {
        sourceCal = Calendar.getInstance(getSourceTimeZone());
    }

    protected void init() {
    }

    protected static ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(5);
        }

        return executorService;
    }

    protected final void setInputStream(InputStream is) {
        this.inputStream = is;
    }

    protected final InputStream getInputStream() {
        return inputStream;
    }

    protected final long getFromTime() {
        return fromTime;
    }

    protected final void setFromTime(long time) {
        this.fromTime = time;
    }

    protected final DateFormat getDateFormat() {
        if (dateFormat == null) {
            String dateFormatStr = getCurrentContract().getDateFormatString();
            if (dateFormat == null) {
                dateFormatStr = getDefaultDateFormatString();
            }
            dateFormat = new SimpleDateFormat(dateFormatStr, Locale.US);
        }

        dateFormat.setTimeZone(getSourceTimeZone());
        return dateFormat;
    }

    protected final long getLoadedTime() {
        return loadedTime;
    }

    protected final void resetCount() {
        this.count = 0;
    }

    protected final void countOne() {
        this.count++;

        /*- @Reserve
         * Don't do refresh in loading any more, it may cause potential conflict
         * between connected refresh events (i.e. when processing one refresh event,
         * another event occured concurrent.)
         * if (count % 500 == 0 && System.currentTimeMillis() - startTime > 2000) {
         *     startTime = System.currentTimeMillis();
         *     preRefresh();
         *     fireDataUpdateEvent(new DataUpdatedEvent(this, DataUpdatedEvent.Type.RefreshInLoading, newestTime));
         *     System.out.println("refreshed: count " + count);
         * }
         */
    }

    protected final int getCount() {
        return count;
    }

    protected final List<V> getStorage(K contract) {
        List<V> storage = contractToStorage.get(contract);
        if (storage == null) {
            storage = new ArrayList<V>();
            contractToStorage.put(contract, storage);
        }

        return storage;
    }

    /**
     * @TODO
     * temporary method? As in some data feed, the symbol is not unique,
     * it may be same in different markets with different secType.
     */
    protected final K lookupContract(String symbol) {
        return subscribedSymbolToContract.get(symbol);
    }

    private void releaseStorage(K contract) {
        /** don't get storage via getStorage(contract), which will create a new one if none */
        List<V> storage = contractToStorage.get(contract);
        synchronized (contractToStorage) {
            contractToStorage.remove(contract);
        }
        if (storage != null) {
            returnBorrowedTimeValues(storage);
            synchronized (storage) {
                storage.clear();
            }
            storage = null;
        }
    }

    protected final boolean isAscending(List<V> storage) {
        final int size = storage.size();
        if (size <= 1) {
            return true;
        } else {
            for (int i = 0; i < size - 1; i++) {
                if (storage.get(i).getTime() < storage.get(i + 1).getTime()) {
                    return true;
                } else if (storage.get(i).getTime() > storage.get(i + 1).getTime()) {
                    return false;
                } else {
                    continue;
                }
            }
        }

        return false;
    }

    protected abstract void returnBorrowedTimeValues(Collection<V> datas);

    protected K getCurrentContract() {
        /**
         * simplely return the contract currently in the front
         * @Todo, do we need to implement a scheduler in case of multiple contract?
         * Till now, only QuoteDataServer call this method, and they all use the
         * per server per contract approach.
         */
        for (K contract : getSubscribedContracts()) {
            return contract;
        }

        return null;
    }

    public Collection<K> getSubscribedContracts() {
        return subscribedContractToSer.keySet();
    }

    protected Ser getSer(K contract) {
        return subscribedContractToSer.get(contract);
    }

    protected Collection<Ser> getChainSers(Ser ser) {
        Collection<Ser> chainSers = serToChainSers.get(ser);
        if (chainSers != null) {
            return chainSers;
        } else {
            return Collections.<Ser>emptyList();
        }
    }

    /**
     * @param symbol symbol in source
     * @param set the Ser that will be filled by this server
     */
    public void subscribe(K contract, Ser ser) {
        subscribe(contract, ser, Collections.<Ser>emptyList());
    }

    public void subscribe(K contract, Ser ser, Collection<Ser> chainSers) {
        synchronized (subscribedContractToSer) {
            subscribedContractToSer.put(contract, ser);
        }
        synchronized (subscribedSymbolToContract) {
            subscribedSymbolToContract.put(contract.getSymbol(), contract);
        }
        synchronized (serToChainSers) {
            Collection<Ser> chainSersX = serToChainSers.get(ser);
            if (chainSersX == null) {
                chainSersX = new ArrayList<Ser>();
                serToChainSers.put(ser, chainSers);
            }
            chainSersX.addAll(chainSers);
        }
    }

    public void unSubscribe(K contract) {
        cancelRequest(contract);
        synchronized (serToChainSers) {
            serToChainSers.remove(subscribedContractToSer.get(contract));
        }
        synchronized (subscribedContractToSer) {
            subscribedContractToSer.remove(contract);
        }
        synchronized (subscribedSymbolToContract) {
            subscribedSymbolToContract.remove(contract.getSymbol());
        }
        releaseStorage(contract);
    }

    protected void cancelRequest(K contract) {
    }

    public boolean isContractSubsrcribed(K contract) {
        for (K contract1 : subscribedContractToSer.keySet()) {
            if (contract1.getSymbol().equals(contract.getSymbol())) {
                return true;
            }
        }
        return false;
    }

    public void startLoadServer() {
        if (getCurrentContract() == null) {
            assert false : "dataContract not set!";
        }

        if (subscribedContractToSer.size() == 0) {
            assert false : ("none ser subscribed!");
        }

        if (loadServer == null) {
            loadServer = new LoadServer();
        }

        if (!inLoading) {
            inLoading = true;
            new Thread(loadServer).start();
            // @Note: ExecutorSrevice will cause access denied of modifyThreadGroup in Applet !!
            //getExecutorService().submit(loadServer);
        }
    }

    public void startUpdateServer(int updateInterval) {
        if (inLoading) {
            System.out.println("should start update server after loaded");
            inUpdating = false;
            return;
        }

        inUpdating = true;

        // in context of applet, a page refresh may cause timer into a unpredict status,
        // so it's always better to restart this timer, so, cancel it first.
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        updateTimer = new Timer();

        if (updateServer != null) {
            updateServer.cancel();
        }
        updateServer = new UpdateServer();

        updateTimer.schedule(updateServer, 1000, updateInterval);
    }

    public void stopUpdateServer() {
        inUpdating = false;
        updateServer = null;
        updateTimer.cancel();
        updateTimer = null;

        postStopUpdateServer();
    }

    protected void postStopUpdateServer() {
    }

    public boolean isInLoading() {
        return inLoading;
    }

    protected abstract long loadFromPersistence();

    /**
     * @param afterThisTime. when afterThisTime equals ANCIENT_TIME, you should
     *        process this condition.
     * @return loadedTime
     */
    protected abstract long loadFromSource(long afterThisTime);

    /**
     * compose ser using data from storage
     */
    public abstract SerChangeEvent composeSer(String symbol, Ser serToBeFilled, List<V> storage);

    protected class LoadServer implements Runnable {

        public void run() {
            long loadedTime1 = loadFromPersistence();
            if (loadedTime1 > loadedTime) {
                loadedTime = loadedTime1;
            }

            long loadedTime2 = loadFromSource(loadedTime);
            if (loadedTime2 > loadedTime) {
                loadedTime = loadedTime2;
            }

            inLoading = false;

            postLoad();
        }
    }

    protected void postLoad() {
    }

    public boolean isInUpdating() {
        return inUpdating;
    }

    private class UpdateServer extends TimerTask {

        public void run() {
            loadedTime = loadFromSource(loadedTime);

            postUpdate();
        }
    }

    protected void postUpdate() {
    }

    public AbstractDataServer createNewInstance() {
        try {
            AbstractDataServer instance = (AbstractDataServer) getClass().newInstance();
            instance.init();

            return instance;
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Override it to return your icon
     * @return a predifined image as the default icon
     */
    public Image getIcon() {
        if (DEFAULT_ICON == null) {
            URL url = AbstractDataServer.class.getResource("defaultIcon.gif");
            DEFAULT_ICON = url != null ? Toolkit.getDefaultToolkit().createImage(url) : null;
        }

        return DEFAULT_ICON;
    }

    /**
     * Convert source sn to source id in format of :
     * sn (0-63)       id (64 bits)
     * 0               ..,0000,0000
     * 1               ..,0000,0001
     * 2               ..,0000,0010
     * 3               ..,0000,0100
     * 4               ..,0000,1000
     * ...
     * @return source id
     */
    public final long getSourceId() {
        final byte sn = getSourceSerialNumber();
        assert sn >= 0 && sn < 63 : "source serial number should be between 0 to 63!";

        return sn == 0 ? 0 : 1 << (sn - 1);
    }

    public final int compareTo(DataServer another) {
        if (this.getDisplayName().equalsIgnoreCase(another.getDisplayName())) {
            return this.hashCode() < another.hashCode() ? -1 : (this.hashCode() == another.hashCode() ? 0 : 1);
        }
        return this.getDisplayName().compareTo(another.getDisplayName());
    }

    public abstract TimeZone getSourceTimeZone();

    @Override
    public String toString() {
        return getDisplayName();
    }
}



