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
package org.aiotrade.platform.modules.dataserver.basic;

import java.io.DataInputStream;
import java.io.EOFException;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.platform.core.dataserver.QuoteServer;
import org.aiotrade.platform.core.sec.Market;
import org.aiotrade.platform.core.sec.Quote;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public class SlonQuoteServer extends QuoteServer {
    private Calendar cal = Calendar.getInstance();
    
    private QuoteContract contract;
    
    protected boolean connect() {
        return true;
    }
    
    protected void request() throws Exception {
        cal.clear();
        
        contract = getCurrentContract();
        
        URL url = new URL(contract.getUrlString() + contract.getSymbol() + ".day");
        
        setInputStream(url.openStream());
    }
    
    protected long read() throws Exception {
        if (getInputStream() == null) {
            return 0;
        }
        
        DataInputStream reader = new DataInputStream(getInputStream());
        
        long newestTime  = -Long.MAX_VALUE;
        resetCount();
        cal.clear();
        boolean EOF = false;
        try {
            /**
             * !NOTICE
             * Reader, such as BufferedRead won't throw EOFException, but InputStream will,
             * so use catch EOFException and set EOF = true to break while circle in case of
             * InputStream.
             */
            while (!EOF) {
                int iDate = toProperFormat(reader.readInt());
                int year = iDate / 10000;
                int month = (iDate - year * 10000) / 100;
                int day = iDate - year * 10000 - month * 100;
                cal.clear();
                cal.set(year, month - 1, day);
                
                long time = cal.getTimeInMillis();
                if (time < getFromTime()) {
                    continue;
                }
                
                Quote quote = borrowQuote();
                
                quote.setTime(time);
                quote.setOpen(toProperFormat(reader.readInt()) / 1000f);
                quote.setClose(toProperFormat(reader.readInt()) / 1000f);
                quote.setHigh(toProperFormat(reader.readInt()) / 1000f);
                quote.setLow(toProperFormat(reader.readInt()) / 1000f);
                quote.setAmount(toProperFormat(reader.readInt()));
                quote.setVolume(toProperFormat(reader.readInt()));
                
                reader.skipBytes(12);
                
                getStorage(contract).add(quote);
                
                newestTime = Math.max(newestTime, time);
                countOne();
            }
        } catch(EOFException e) {
            EOF = true;
        }
        
        return newestTime;
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
    
    /**
     * Convert data from ShenLong data file to proper format.
     * @param java int type length data
     */
    private int toProperFormat(int a) {
        int c = 0x00000000;           // a =  DC 54 D0 FE
        c |= 0x00FF0000 & (a << 8);   // a -> 54 D0 FE 00
        c |= 0xFF000000 & (a << 24);  // a -> FE 00 00 00
        c |= 0x0000FF00 & (a >> 8);   // a -> 00 DC 54 D0
        c |= 0x000000FF & (a >> 24);  // a -> 00 00 00 DC
        return ~c;
    }
    
    public String getDisplayName() {
        return "Slon Data File";
    }
    
    public String getDefaultDateFormatString() {
        return "yyyyMMdd hhmmss";
    }
    
    public byte getSourceSerialNumber() {
        return (byte)5;
    }
    
    @Override
    public Frequency[] getSupportedFreqs() {
        return null;
    }


    @Override
    public Market getMarket(String symbol) {
        return Market.SHSE;
    }

    @Override
    public TimeZone getSourceTimeZone() {
        return TimeZone.getTimeZone("Asia/Shanghai");
    }


}



