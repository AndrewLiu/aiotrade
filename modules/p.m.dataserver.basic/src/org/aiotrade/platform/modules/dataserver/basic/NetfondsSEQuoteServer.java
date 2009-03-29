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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import javax.imageio.ImageIO;
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
public class NetfondsSEQuoteServer extends QuoteServer {
    private Calendar cal = Calendar.getInstance();
    
    private QuoteContract contract;
    
    protected boolean connect() {
        return true;
    }
    
    protected void request() throws Exception {
        cal.clear();
        
        contract = getCurrentContract();
        
        Date begDate = new Date();
        Date endDate = new Date();
        if (getFromTime() <= ANCIENT_TIME) {
            begDate = contract.getBegDate();
            endDate = contract.getEndDate();
        } else {
            cal.setTimeInMillis(getFromTime());
            begDate = cal.getTime();
        }
        
        cal.setTime(begDate);
        int a = cal.get(Calendar.MONTH);
        int b = cal.get(Calendar.DAY_OF_MONTH);
        int c = cal.get(Calendar.YEAR);
        
        cal.setTime(endDate);
        int d = cal.get(Calendar.MONTH);
        int e = cal.get(Calendar.DAY_OF_MONTH);
        int f = cal.get(Calendar.YEAR);
        
        StringBuffer urlStr = new StringBuffer(60);
        urlStr.append("http://www.netfonds.se/quotes/paperhistory.php").append("?paper=");
        
        urlStr.append(contract.getSymbol());
        
        URL url = new URL(urlStr.toString());
        System.out.println(url);
        
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setAllowUserInteraction(true);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        
        setInputStream(conn.getInputStream());
    }
    
    /**
     * @return readed time
     */
    protected long read() throws Exception {
        if (getInputStream() == null) {
            return 0;
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));
        
        /** skip first line */
        String s = reader.readLine();
        
        /** token index */
        int iDateTime = 0;
        int iOpen     = 3;
        int iHigh     = 4;
        int iLow      = 5;
        int iClose    = 6;
        int iVolume   = 7;
        int iAmount   = 8;
        
        long newestTime  = -Long.MAX_VALUE;
        resetCount();
        cal.clear();
        while ((s = reader.readLine()) != null) {
            String[] items;
            items = s.split("\t");
            
            if (items.length < 6) {
                break;
            }
            
            /**
             * !NOTICE
             * must catch the date parse exception, other wise, it's dangerous
             * for build a calendarTimes in MasterSer
             */
            try {
                Date date = getDateFormat().parse(items[iDateTime].trim());
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
            
            Quote quote = borrowQuote();
            
            quote.setTime(time);
            quote.setOpen(Float.parseFloat(items[iOpen].trim()));
            quote.setHigh(Float.parseFloat(items[iHigh].trim()));
            quote.setLow(Float.parseFloat(items[iLow].trim()));
            quote.setClose(Float.parseFloat(items[iClose].trim()));
            quote.setVolume(Float.parseFloat(items[iVolume].trim()) / 100f);
            quote.setAmount(Float.parseFloat(items[iAmount].trim()) / 100f);
            
            if (quote.getHigh() * quote.getLow() * quote.getClose() == 0) {
                returnQuote(quote);
                continue;
            }
            
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
        return "Netfonds.se Internet";
    }
    
    public String getDefaultDateFormatString() {
        return "yyyyMMdd";
    }
    
    public byte getSourceSerialNumber() {
        return (byte)4;
    }
    
    @Override
    public Image getIcon() {
        BufferedImage img;
        try {
            img = ImageIO.read(new File("org/aiotrade/platform/modules/dataserver/basic/resources/favicon_nefondsSE.png"));
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



