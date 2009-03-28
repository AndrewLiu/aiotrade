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
package org.aiotrade.platform.modules.dataserver.yahoo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import org.aiotrade.platform.core.dataserver.TickerContract;
import org.aiotrade.platform.core.dataserver.TickerServer;
import org.aiotrade.platform.core.sec.Market;
import org.aiotrade.platform.core.sec.Ticker;
import org.aiotrade.platform.core.sec.TickerSnapshot;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public class YahooTickerServer extends TickerServer {

    /**
     * @NOTICE
     * If the remote datafeed keeps only one inputstream for all subscriiebed
     * symbols, one singleton instance is enough. If each symbol need a separate
     * session, you may create new data server instance for each symbol.
     */
    private static YahooTickerServer singletonInstance;
    // * "http://download.finance.yahoo.com/d/quotes.csv"
    private static String BaseUrl = "http://aiotrade.com/";
    private static String UrlPath = "aiodata/yt";
    private boolean gzipped;

    protected boolean connect() {
        return true;
    }

    /**
     * Template:
     * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
     */
    protected void request() throws Exception {
        sourceCal.clear();

        StringBuilder urlStr = new StringBuilder(90);
        urlStr.append(BaseUrl).append(UrlPath);
        urlStr.append("?s=");

        Collection<TickerContract> contracts = getSubscribedContracts();
        if (contracts.size() == 0) {
            setInputStream(null);
            setLoadedTime(getFromTime());
            return;
        }

        for (TickerContract contract : contracts) {
            urlStr.append(contract.getSymbol()).append("+");
        }
        urlStr = urlStr.deleteCharAt(urlStr.length() - 1);

        urlStr.append("&d=t&f=sl1d1t1c1ohgvbap");

        /** s: symbol, n: name, x: stock exchange */
        String urlStrForName = urlStr.append("&d=t&f=snx").toString();

        URL url = new URL(urlStr.toString());
        System.out.println(url);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.setAllowUserInteraction(true);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        conn.connect();

        String encoding = conn.getContentEncoding();
        if (encoding != null && encoding.indexOf("gzip") != -1) {
            gzipped = true;
        } else {
            gzipped = false;
        }
        setInputStream(conn.getInputStream());
    }

    protected long read() throws Exception {
        InputStream is = getInputStream();
        if (is == null) {
            return 0;
        }

        BufferedReader reader;
        if (gzipped) {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)));
        } else {
            reader = new BufferedReader(new InputStreamReader(is));
        }

        long newestTime = -Long.MAX_VALUE;
        resetCount();
        sourceCal.clear();
        boolean EOF = false;
        while (!EOF) {
            String s = reader.readLine();
            if (s == null) {
                break;
            }

            String[] items;
            items = s.split(",");

            if (items.length > 11) {
                String symbol = items[0].toUpperCase().replace('"', ' ').trim();

                String dateStr = items[2].replace('"', ' ').trim();
                String timeStr = items[3].replace('"', ' ').trim();
                if (dateStr.equalsIgnoreCase("N/A") || timeStr.equalsIgnoreCase("N/A")) {
                    continue;
                }

                /**
                 * !NOTICE
                 * must catch the date parse exception, other wise, it's dangerous
                 * for build a calendarTimes in MasterSer
                 */
                try {
                    Date date = getDateFormat().parse(dateStr + " " + timeStr);
                    sourceCal.clear();
                    sourceCal.setTime(date);
                } catch (ParseException ex) {
                    ex.printStackTrace();
                    continue;
                }

                long time = sourceCal.getTimeInMillis();
                if (time == 0) {
                    /** for test and finding issues */
                    System.out.println("time of ticker: " + symbol + " is 0!");
                }

                TickerSnapshot tickerSnapshot = getTickerSnapshot(symbol);
                tickerSnapshot.setTime(time);

                tickerSnapshot.set(Ticker.LAST_PRICE, items[1].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[1].trim()));
                tickerSnapshot.set(Ticker.DAY_CHANGE, items[4].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[4].trim()));
                tickerSnapshot.set(Ticker.DAY_OPEN, items[5].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[5].trim()));
                tickerSnapshot.set(Ticker.DAY_HIGH, items[6].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[6].trim()));
                tickerSnapshot.set(Ticker.DAY_LOW, items[7].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[7].trim()));
                tickerSnapshot.set(Ticker.DAY_VOLUME, items[8].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[8].trim()) / 100f);
                tickerSnapshot.setBidPrice(0, items[9].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[9].trim()));
                tickerSnapshot.setAskPrice(0, items[10].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[10].trim()));
                tickerSnapshot.set(Ticker.PREV_CLOSE, items[11].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[11].trim()));

                tickerSnapshot.setFullName(symbol);
                tickerSnapshot.notifyObservers();

                newestTime = Math.max(newestTime, time);
                countOne();
            }
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
    public YahooTickerServer createNewInstance() {
        if (singletonInstance == null) {
            singletonInstance = (YahooTickerServer) super.createNewInstance();
            singletonInstance.init();
        }

        return singletonInstance;
    }

    public String getDisplayName() {
        return "Yahoo! Finance Internet";
    }

    public String getDefaultDateFormatString() {
        return "MM/dd/yyyy h:mma";
    }

    public byte getSourceSerialNumber() {
        return (byte) 1;
    }

    public TimeZone getSourceTimeZone() {
        return TimeZone.getTimeZone("America/New_York");
    }

    @Override
    public Market getMarket(String symbol) {
        return YahooQuoteServer.GetMarket(symbol);
    }
}



