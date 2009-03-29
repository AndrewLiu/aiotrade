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

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisDescriptor;
import org.aiotrade.lib.util.serialization.BeansDocument;
import org.aiotrade.lib.util.serialization.JavaDocument;
import org.w3c.dom.Element;

/**
 * Securities' data source request contract. It know how to find and invoke
 * server by call createBindInstance().
 *
 * We simplely inherit AnalysisDescriptor, we may think the bindClass provides
 * service for descriptor.
 *
 * most fields' default value should be OK.
 *
 * @author Caoyuan Deng
 */
public abstract class DataContract<T extends DataServer> extends AnalysisDescriptor<T> {

    private String symbol; // symbol in source
    private String category;
    private String shortName;
    private String longName;
    private String dateFormatString;
    private Date begDate = new GregorianCalendar(1990, Calendar.JANUARY, 1).getTime();
    private Date endDate = new GregorianCalendar().getTime();
    private String urlString = "";
    private boolean refreshable = false;
    private int refreshInterval = 5; // seconds
    private InputStream inputStream;

    public DataContract() {
    }

    /**
     * @return symbol in source side
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @param symbol symbol in source side
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public final String getCategory() {
        return category;
    }

    public final void setCategory(String category) {
        this.category = category;
    }

    public final String getShortName() {
        return shortName;
    }

    public final void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public final String getLongName() {
        return longName;
    }

    public final void setLongName(String longName) {
        this.longName = longName;
    }

    public String getDateFormatString() {
        return dateFormatString;
    }

    public void setDateFormatString(String dateFormatString) {
        this.dateFormatString = dateFormatString;
    }

    public Date getBegDate() {
        return begDate;
    }

    public void setBeginDate(Date date) {
        this.begDate = date;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date date) {
        this.endDate = date;
    }

    public String getUrlString() {
        return urlString;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public boolean isRefreshable() {
        return refreshable;
    }

    public void setRefreshable(boolean b) {
        this.refreshable = b;
    }

    public int getRefereshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(int refreshIntreval) {
        this.refreshInterval = refreshInterval;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public Element writeToBean(BeansDocument doc) {
        final Element bean = super.writeToBean(doc);

        doc.valuePropertyOfBean(bean, "symbol", getSymbol());
        doc.valuePropertyOfBean(bean, "dateFormatString", getDateFormatString());

        final Element begDateBean = doc.createBean(getBegDate());
        doc.innerPropertyOfBean(bean, "begDate", begDateBean);
        doc.valueConstructorArgOfBean(begDateBean, 0, getBegDate().getTime());

        final Element endDateBean = doc.createBean(getEndDate());
        doc.innerPropertyOfBean(bean, "endDate", endDateBean);
        doc.valueConstructorArgOfBean(endDateBean, 0, getEndDate().getTime());

        doc.valuePropertyOfBean(bean, "urlString", getUrlString());
        doc.valuePropertyOfBean(bean, "refreshable", isRefreshable());
        doc.valuePropertyOfBean(bean, "refreshInterval", getRefereshInterval());

        return bean;
    }

    public String writeToJava(String id) {
        return super.writeToJava(id) +
                JavaDocument.set(id, "setSymbol", "" + getSymbol()) +
                JavaDocument.set(id, "setDateFormatString", "" + getDateFormatString()) +
                JavaDocument.create("begDate", Date.class, getBegDate().getTime()) +
                JavaDocument.set(id, "setBegDate", "begDate") +
                JavaDocument.create("endDate", Date.class, getEndDate().getTime()) +
                JavaDocument.set(id, "setEndDate", "endDate") +
                JavaDocument.set(id, "setUrlString", getUrlString()) +
                JavaDocument.set(id, "setRefreshable", isRefreshable()) +
                JavaDocument.set(id, "setRefreshInterval", getRefereshInterval());
    }
}

