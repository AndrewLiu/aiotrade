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
import com.ib.client.Order;
import java.awt.Image;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.math.timeseries.Unit;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.platform.core.dataserver.QuoteServer;
import org.aiotrade.platform.core.sec.Quote;
import org.openide.util.Utilities;

/**
 * TWS demo user/password
 * For Individual Demo
 *   o User Name: edemo
 *     Password: demouser
 * For Advisor Demo
 *   o User Name: fdemo
 *     Password: demouser
 *
 * @author Caoyuan Deng
 */
public class IBQuoteServer extends QuoteServer {
    private Calendar cal = Calendar.getInstance();
    
    private static IBWrapper ibWrapper;
    
    private int maxDurationInSeconds = 86400; // 24 hours
    private int maxDurationInDays = 365;
    private int maxDurationInWeeks = 54;
    private int maxDurationInYears = 1;
    
    private QuoteContract contract;
    
    protected boolean connect() {
        if (ibWrapper == null) {
            ibWrapper = IBWrapper.getInstance();
        }
        
        if (!ibWrapper.isConnected()) {
            ibWrapper.connect();
        }
        
        return ibWrapper.isConnected();
    }
    
    protected void request() throws Exception {
        cal.clear();
        
        contract = getCurrentContract();
        List<Quote> storage = getStorage(contract);
        
        Date begDate = new Date();
        Date endDate = new Date();
        if (getFromTime() <= ANCIENT_TIME /* @todo */) {
            begDate = contract.getBegDate();
            endDate = contract.getEndDate();
        } else {
            cal.setTimeInMillis(getFromTime());
            begDate = cal.getTime();
        }
        
        
        boolean 	m_rc;
        String      m_backfillEndTime;
        String		m_backfillDuration;
        int         m_barSizeSetting;
        int         m_useRTH;
        int         m_formatDate;
        int         m_marketDepthRows;
        String      m_whatToShow;
        Contract 	m_contract = new Contract();
        Order 		m_order = new Order();
        int         m_exerciseAction;
        int         m_exerciseQuantity;
        int         m_override;
        
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
            
            // set order fields
            m_order.m_action = "BUY";
            m_order.m_totalQuantity = 10;
            m_order.m_orderType = "LMT";
            m_order.m_lmtPrice = 40;
            m_order.m_auxPrice = 0;
            m_order.m_sharesAllocation = "FA Allocation Info...";
            m_order.m_goodAfterTime = "";
            m_order.m_goodTillDate = "";
            
            /** set historical data fields: */
            
            m_backfillEndTime = ibWrapper.getTwsDateFormart().format(endDate);
            
            Frequency freq = getSer(contract).getFreq();
            
            /**
             * An integer followed by a space, followed by one of these units:
             * S (seconds), D (days), W (weeks), and Y (years)
             */
            int durationInt = 300;
            String durationStr = "D";
            switch (freq.unit) {
                case Second:
                    durationInt = Math.min(durationInt, maxDurationInSeconds);
                    durationStr = "S";
                    break;
                case Minute:
                    durationInt *= 60;
                    durationInt = Math.min(durationInt, maxDurationInSeconds);
                    durationStr = "S";
                    break;
                case Hour:
                    durationInt *= 60 * 24;
                    durationInt = Math.min(durationInt, maxDurationInSeconds);
                    durationStr = "S";
                    break;
                case Day:
                    durationInt = Math.min(durationInt, maxDurationInDays);
                    durationStr = "D";
                    break;
                case Week:
                    durationInt = Math.min(durationInt, maxDurationInWeeks);
                    durationStr = "W";
                    break;
                case Month:
                    durationInt *= 30;
                    durationInt = Math.min(durationInt, maxDurationInDays);
                    durationStr = "D";
                case Year:
                    durationInt = Math.min(durationInt, maxDurationInYears);
                    durationStr = "Y";
                    break;
                default:
            }
            m_backfillDuration = new StringBuilder(7)
                    .append(durationInt).append(" ").append(durationStr)
                    .toString();
            
            m_barSizeSetting = IBWrapper.getBarSize(freq);
            
            m_useRTH = 1;
            m_whatToShow = "MIDPOINT";
            
            /**
             * formatDate = 1, dates applying to bars are returned in a format ¡°yyyymmdd{space}{space}hh:mm:dd¡±
             *   - the same format already used when reporting executions.
             * formatDate = 2, dates are returned as a integer specifying the number of seconds since 1/1/1970 GMT.
             */
            m_formatDate = 2;
            m_exerciseAction = 1;
            m_exerciseQuantity = 1;
            m_override = 0;
            
            // set market depth rows
            m_marketDepthRows = 20;
        } catch( Exception ex) {
            ex.printStackTrace();
            return;
        }
        m_rc = true;
        
        int reqId = ibWrapper.reqHistoricalData(
                this,
                storage,
                m_contract,
                m_backfillEndTime,
                m_backfillDuration,
                m_barSizeSetting,
                m_whatToShow,
                m_useRTH,
                m_formatDate);
        contract.setReqId(reqId);
    }
    
    protected long read() throws Exception {
        /**
         * Don't try <code>synchronized (storage) {}<code>, synchronized (this)
         * instead. Otherwise, the ibWrapper can not process storage during the
         * storage waiting period which will be whthin the synchronized block.
         */
        synchronized (this) {
            boolean timeout = false;
            while (ibWrapper.isHisDataReqPending(contract.getReqId()) && !timeout) {
                try {
                    //System.out.println("dataserver is waiting: " + getSer(contract).getFreq());
                    wait(Unit.Minute.getInterval() * 1);
                    //System.out.println("dataserver is woke up: " + getSer(contract).getFreq());
                    timeout = true; // whatever
                } catch (InterruptedException ex) {
                    if (ibWrapper.isHisDataReqPending(contract.getReqId())) {
                        ibWrapper.cancelHisDataRequest(contract.getReqId());
                    }
                    return getLoadedTime();
                }
            }
        }
        
        long newestTime = -Long.MAX_VALUE;
        resetCount();
        List<Quote> storage = getStorage(contract);
        synchronized (storage) {
            for (Quote quote : storage) {
                newestTime = Math.max(newestTime, quote.getTime());
                countOne();
            }
        }
        
        return newestTime;
    }
    
    @Override
    protected void cancelRequest(QuoteContract contract) {
        ibWrapper.cancelHisDataRequest(contract.getReqId());
    }
    
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
    
    public String getDisplayName() {
        return "IB TWS";
    }
    
    public String getDefaultDateFormatString() {
        return "yyyyMMdd HH:mm:ss";
    }
    
    public byte getSourceSerialNumber() {
        return (byte)6;
    }
    
    @Override
    public Frequency[] getSupportedFreqs() {
        return IBWrapper.getSupportedFreqs();
    }
    
    @Override
    public Image getIcon() {
        return Utilities.loadImage("org/aiotrade/platform/modules/dataserver/ib/netbeans/resources/favicon_ib.png");
    }
    
    /**
     * 1 1sec "<30;2000> S"
     * 2 5sec "<30;10000> S"
     * 3 15sec "<30;30000> S" (returns 2 days of data for "30000 S"!)
     * 4 30sec "<30;86400> S", "1 D" (returns 4 days of data for "86400 S"!)
     * 5 1min "<30;86400> S", "<1;6> D" (returns 4 days of data for "86400 S"!)
     * 6 2min "<30;86400> S", "<1;6> D" (returns 4 days of data for "86400 S"!)
     * 7 5min "<30;86400> S", "<1;6> D" (returns 4 days of data for "86400 S"!)
     * 8 15min "<30;86400> S", "<1;20> D", "<1,2> W" (returns 4 days of data for "86400 S"!)
     * 9 30min "<30;86400> S", "<1;34> D", "<1,4> W" (returns 4 days of data for "86400 S"!)
     * 10 1h "<30;86400> S", "<1;34> D", "<1,4> W" (returns 4 days of data for "86400 S"!)
     * 11 1d "<30;86400> S", "<1;60> D", "<1,52> W" (returns 4 days of data for "86400 S"!)
     */
}




