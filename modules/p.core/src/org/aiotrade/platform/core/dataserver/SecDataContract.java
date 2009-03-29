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
import org.aiotrade.lib.math.timeseries.datasource.DataContract;
import org.aiotrade.lib.math.timeseries.datasource.DataServer;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.lib.util.serialization.JavaDocument;
import org.aiotrade.lib.util.serialization.BeansDocument;
import org.w3c.dom.Element;

/**
 * most fields' default value should be OK.
 *
 * @author Caoyuan Deng
 */
public abstract class SecDataContract<S extends DataServer> extends DataContract<S> {
    private int reqId;
    private String exchange = "SMART";
    private String primaryExchange = "SUPERSOES";
    private String currency = "USD";
    private Sec.Type secType = Sec.Type.Stock;
    
    public SecDataContract() {
        super();
        setActive(true);
        Calendar cal = Calendar.getInstance();
        setEndDate(cal.getTime());
        cal.set(1970, Calendar.JANUARY, 1);
        setBeginDate(cal.getTime());
        setUrlString("");
        setRefreshable(false);
        setRefreshInterval(60); // seconds
        setInputStream(null);
    }
    
    public final int getReqId() {
        return reqId;
    }
    
    public final void setReqId(int reqId) {
        this.reqId = reqId;
    }
    
    public final Sec.Type getSecType() {
        return secType;
    }
    
    public final void setSecType(Sec.Type secType) {
        this.secType = secType;
    }

    public final String getExchange() {
        return exchange;
    }
    
    public final void setExchange(String exchange) {
        this.exchange = exchange;
    }
    
    public final String getPrimaryExchange() {
        return primaryExchange;
    }
    
    public final void setPrimaryExchange(String primaryExchange) {
        this.primaryExchange = primaryExchange;
    }
    
    public final String getCurrency() {
        return currency;
    }
    
    public final void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Element writeToBean(BeansDocument doc) {
        final Element bean = super.writeToBean(doc);
        
        doc.valuePropertyOfBean(bean, "secType", getSecType());
        doc.valuePropertyOfBean(bean, "exchange", getExchange());
        doc.valuePropertyOfBean(bean, "primaryExchange", getPrimaryExchange());
        doc.valuePropertyOfBean(bean, "currency", getCurrency());
        
        return bean;
    }
    
    public String writeToJava(String id) {
        return super.writeToJava(id) +
                JavaDocument.set(id, "setSecType", Sec.Type.class.getName() + "." + getSecType()) +
                JavaDocument.set(id, "setExchange", "" + getExchange()) +
                JavaDocument.set(id, "setPrimaryExchange", "" + getPrimaryExchange()) +
                JavaDocument.set(id, "setCurrency", "" + getCurrency())
                ;
    }
    
}

