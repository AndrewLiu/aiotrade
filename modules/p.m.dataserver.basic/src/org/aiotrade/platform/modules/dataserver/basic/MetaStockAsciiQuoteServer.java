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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import javax.imageio.ImageIO;
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
public class MetaStockAsciiQuoteServer extends QuoteServer {
    private Calendar cal = Calendar.getInstance();
    
    private QuoteContract contract;
    
    protected boolean connect() {
        return true;
    }
    
    protected void request() throws Exception {
        cal.clear();
        
        contract = getCurrentContract();
        
        URL url = new URL(contract.getUrlString());
        
        setInputStream(url.openStream());
    }
    
    protected long read() throws Exception {
        if (getInputStream() == null) {
            return 0;
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));
        
        /** token index */
        int iCode = 0;
        int iDate = 0;
        int iTime = 0;
        int iOpen = 0;
        int iHigh = 0;
        int iLow = 0;
        int iClose = 0;
        int iVolume = 0;
        
        /** parse first line to get the position of quote */
        String s = reader.readLine();
        StringTokenizer tTilte = new StringTokenizer(s, ",");
        String token = null;
        
        /** countTokens() will change in cycle, so, must get it first */
        int tokenCount = tTilte.countTokens();
        for (int i = 0; i < tokenCount; i++) {
            token = tTilte.nextToken().trim().toUpperCase();
            if (token.contains("TICKER")) {
                iCode = i;
            } else if (token.contains("YYMMDD")) {
                iDate = i;
            } else if (token.contains("TIME")) {
                iTime = i;
            } else if (token.contains("OPEN")) {
                iOpen = i;
            } else if (token.contains("HIGH")) {
                iHigh = i;
            } else if (token.contains("LOW")) {
                iLow = i;
            } else if (token.contains("CLOSE")) {
                iClose = i;
            } else if (token.contains("VOLUME")) {
                iVolume = i;
            }
        }
        
        long newestTime  = -Long.MAX_VALUE;
        resetCount();
        cal.clear();
        while ((s = reader.readLine()) != null) {
            String[] items;
            items = s.split(",");
            
            if (items.length < 5 || contract.getSymbol().contains(items[iCode]) == false) {
                break;
            }
            
            String dateStr = items[iDate] + " " + items[iTime];
            
            /**
             * !NOTICE
             * must catch the date parse exception, other wise, it's dangerous
             * for build a calendarTimes in MasterSer
             */
            try {
                Date date = getDateFormat().parse(dateStr.trim());
                cal.clear();
                cal.setTime(date);
            } catch(ParseException ex) {
                ex.printStackTrace();
                continue;
            }
            
            long time = cal.getTimeInMillis();
            if (time < getFromTime()) {
                continue;
            }
            
            float open   = Float.parseFloat(items[iOpen].trim());
            float high   = Float.parseFloat(items[iHigh].trim());
            float low    = Float.parseFloat(items[iLow].trim());
            float close  = Float.parseFloat(items[iClose].trim());
            float volume = Float.parseFloat(items[iVolume].trim()) / 100f;
            
            Quote quote = borrowQuote();
            
            quote.setTime(time);
            quote.setOpen(open);
            quote.setHigh(high);
            quote.setLow(low);
            quote.setClose(close);
            quote.setVolume(volume);
            
            quote.setAmount(-1);
            
            getStorage(contract).add(quote);
            
            newestTime = Math.max(newestTime, time);
            countOne();
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
    
    public String getDisplayName() {
        return "MetaStock ASCII File";
    }
    
    public String getDefaultDateFormatString() {
        /**
         * @NOTICE
         * "yyyyMMdd hhmmss" is wrong, "hh" means Hour in am/pm (1-12)
         */
        return "yyyyMMdd HHmmss";
    }
    
    public byte getSourceSerialNumber() {
        return (byte)3;
    }
    
    @Override
    public Frequency[] getSupportedFreqs() {
        return null;
    }
    
    @Override
    public Image getIcon() {
        BufferedImage img;
        try {
            img = ImageIO.read(new File("org/aiotrade/platform/modules/dataserver/basic/resources/favicon_metastock.png"));
        } catch (IOException e) {
            img = null;
        }
        return img;
    }

    @Override
    public Market getMarket(String symbol) {
        return Market.NYSE;
    }

    @Override
    public TimeZone getSourceTimeZone() {
        return TimeZone.getTimeZone("America/New_York");
    }
    
}



