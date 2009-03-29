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
import java.util.Calendar;
import org.aiotrade.platform.core.dataserver.TickerContract;
import org.aiotrade.platform.core.dataserver.TickerServer;
import org.aiotrade.platform.core.sec.TickerSnapshot;

/**
 *
 * @author Caoyuan Deng
 */
public class IBTickerServer extends TickerServer {
    private static IBTickerServer singletonInstance;
    
    private Calendar calendar = Calendar.getInstance();
    
    private static IBWrapper ibWrapper;
    
    protected boolean connect() {
        if (ibWrapper == null) {
            ibWrapper = IBWrapper.getInstance();
        }
        
        if (!ibWrapper.isConnected()) {
            ibWrapper.connect();
        }
        
        return ibWrapper.isConnected();
    }
    
    /**
     * Template:
     * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
     */
    protected void request() throws Exception {
        for (TickerContract contract : getSubscribedContracts()) {
            if (ibWrapper.isMktDataRequested(contract.getReqId())) {
                /** has subscribed */
                continue;
            } else {
                /** request seems lost, re-request */
            }
            
            boolean 	m_rc;
            int         m_marketDepthRows;
            Contract 	m_contract = new Contract();
            
            m_rc = false;
            try {
                
                // set contract fields
                m_contract.m_symbol = contract.getSymbol();
                m_contract.m_secType = IBWrapper.getSecType(contract.getSecType());
                m_contract.m_expiry = "";
                m_contract.m_strike = 0;
                m_contract.m_right = "";
                m_contract.m_multiplier = "";
                m_contract.m_exchange = "SMART";
                m_contract.m_primaryExch = "SUPERSOES";
                m_contract.m_currency = "USD";
                m_contract.m_localSymbol = "";
                
                // set market depth rows
                m_marketDepthRows = 20;
            } catch( Exception ex) {
                ex.printStackTrace();
                return;
            }
            m_rc = true;
            
            TickerSnapshot tickerSnapshot = getTickerSnapshot(contract.getSymbol());
            int reqId = ibWrapper.reqMktData(this, m_contract, tickerSnapshot);
            contract.setReqId(reqId);
        }
    }
    
    protected long read() throws Exception {
        long newestTime  = -Long.MAX_VALUE;
        resetCount();
        for (TickerContract contract : getSubscribedContracts()) {
            TickerSnapshot tickerSnapshot = getTickerSnapshot(contract.getSymbol());
            if (tickerSnapshot == null) {
                continue;
            }
            
            newestTime = Math.max(newestTime, tickerSnapshot.readTicker().getTime());
            countOne();
        }
        
        if (getCount() > 0) {
            /**
             * Tickers may have the same time stamp even for a new one,
             * but here means there is at least one new ticker, so always return
             * afterThisTime + 1, this will cause fireDataUpdatedEvent, even only
             * one symbol's ticker renewed. But that is good, because it will
             * tell all symbols that at least one new time tick has happened.
             */
            return getFromTime();
        } else {
            return newestTime;
        }
    }
    
    @Override
    protected void cancelRequest(TickerContract contract) {
        TickerSnapshot tickerSnapshot = getTickerSnapshot(contract.getSymbol());
        if (tickerSnapshot == null) {
            return;
        }
        
        tickerSnapshot.deleteObservers();
        ibWrapper.cancelMktDataRequest(contract.getReqId());
    }
    
    /**
     * Retrive data from Yahoo finance website
     * Template:
     * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
     *
     * @param afterThisTime from time
     */
    protected long loadFromSource(long afterThisTime) {
        setFromTime(afterThisTime + 1);
        
        long loadedTime = getLoadedTime();
        if (!connect()) {
            return loadedTime;
        }
        try {
            request();
            loadedTime = read();
        } catch (Exception ex) {
            System.out.println("Error in loading from source: " + ex.getMessage());
        }
        
        return loadedTime;
    }
    
    @Override
    public IBTickerServer createNewInstance() {
        if (singletonInstance == null) {
            singletonInstance = (IBTickerServer)super.createNewInstance();
            singletonInstance.init();
        }
        
        return singletonInstance;
    }
    
    public String getDisplayName() {
        return "IB TWS";
    }
    
    public String getDefaultDateFormatString() {
        return "yyyyMMdd HH:mm:ss";
    }
    
    public byte getSourceSerialNumber() {
        return (byte)6;
    }
}




