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

import java.util.Calendar;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.platform.core.PersistenceManager;

/**
 *
 * most fields' default value should be OK.
 *
 * @author Caoyuan Deng
 */
public class TickerContract extends SecDataContract<TickerServer> {
    
    public TickerContract() {
        super();
        setServiceClassName("org.aiotrade.platform.modules.dataserver.basic.YahooTickerServer");
        setDateFormatString("MM/dd/yyyy h:mma");
        setFreq(Frequency.ONE_MIN);
        Calendar calendar = Calendar.getInstance();
        setEndDate(calendar.getTime());
        calendar.set(1990, Calendar.JANUARY, 1);
        setBeginDate(calendar.getTime());
        setUrlString("");
        setRefreshable(true);
        setRefreshInterval(5); // seconds
    }
    
    public String getDisplayName() {
        return "Ticker Data Contract[" + getSymbol() + "]";
    }
    
    /**
     * @param none args are needed
     */
    public TickerServer createServiceInstance(Object... args) {
        final TickerServer template = lookupServiceTemplate();
        return template == null ? null : (TickerServer)template.createNewInstance();
    }
    
    public TickerServer lookupServiceTemplate() {
        for (TickerServer server : PersistenceManager.getDefault().lookupAllRegisteredServices(TickerServer.class, getFolderName())) {
            if (server.getClass().getName().equalsIgnoreCase(getServiceClassName())) {
                return server;
            }
        }
        
        return null;
    }
    
    public static String getFolderName() {
        return "TickerServers";
    }
    
    /**
     * Ticker contract don't care about freq, so override super
     */
    @Override
    public boolean idEquals(String serviceClassName, Frequency freq) {
        if (this.getServiceClassName().equals(serviceClassName)) {
            return true;
        } else {
            return false;
        }
    }
    
}

