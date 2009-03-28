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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
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
public class YahooQuoteServer extends QuoteServer {

    // * "http://table.finance.yahoo.com/table.csv"
    private static String BaseUrl = "http://aiotrade.com/";
    private static String UrlPath = "aiodata/yq";
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private DateFormat dateFormat_old = new SimpleDateFormat("dd-MMM-yy", Locale.US);
    private QuoteContract contract;
    private boolean gzipped;

    protected boolean connect() {
        return true;
    }

    /**
     * Template:
     * http://table.finance.yahoo.com/table.csv?s=^HSI&a=01&b=20&c=1990&d=07&e=18&f=2005&g=d&ignore=.csv
     */
    protected void request() throws Exception {
        sourceCal.clear();

        contract = getCurrentContract();

        Date begDate = new Date();
        Date endDate = new Date();
        if (getFromTime() <= ANCIENT_TIME /* @todo */) {
            begDate = contract.getBegDate();
            endDate = contract.getEndDate();
        } else {
            sourceCal.setTimeInMillis(getFromTime());
            begDate = sourceCal.getTime();
        }

        sourceCal.setTime(begDate);
        int a = sourceCal.get(Calendar.MONTH);
        int b = sourceCal.get(Calendar.DAY_OF_MONTH);
        int c = sourceCal.get(Calendar.YEAR);

        sourceCal.setTime(endDate);
        int d = sourceCal.get(Calendar.MONTH);
        int e = sourceCal.get(Calendar.DAY_OF_MONTH);
        int f = sourceCal.get(Calendar.YEAR);

        StringBuilder urlStr = new StringBuilder(50);
        urlStr.append(BaseUrl).append(UrlPath);
        urlStr.append("?s=").append(contract.getSymbol());

        /** a, d is month, which from 0 to 11 */
        urlStr.append(
                "&a=" + a + "&b=" + b + "&c=" + c +
                "&d=" + d + "&e=" + e + "&f=" + f);

        urlStr.append("&g=d&ignore=.csv");

        URL url = new URL(urlStr.toString());

        System.out.println(url);

        if (url != null) {
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
    }

    /**
     * @return readed time
     */
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

        /** skip first line */
        String s = reader.readLine();

        /** token index */
        int iDateTime = 0;
        int iOpen = 1;
        int iHigh = 2;
        int iLow = 3;
        int iClose = 4;
        int iVolume = 5;
        int iAdjClose = 6;

        long newestTime = -Long.MAX_VALUE;
        resetCount();
        sourceCal.clear();
        while ((s = reader.readLine()) != null) {
            String[] items;
            items = s.split(",");

            if (items.length < 6) {
                break;
            }

            /**
             * !NOTICE
             * must catch the date parse exception, other wise, it's dangerous
             * for build a calendarTimes in MasterSer
             */
            Date date = null;
            try {
                date = dateFormat.parse(items[iDateTime].trim());
            } catch (ParseException ex1) {
                try {
                    date = dateFormat_old.parse(items[iDateTime].trim());
                } catch (ParseException ex2) {
                    continue;
                }
            }
            sourceCal.clear();
            sourceCal.setTime(date);

            long time = sourceCal.getTimeInMillis();
            if (time < getFromTime()) {
                continue;
            }

            // quote time is rounded to 00:00, we should adjust it to open time
            String symbol = contract.getSymbol();
            time += getMarket(symbol).getOpenTimeOfDay();

            Quote quote = borrowQuote();

            quote.setTime(time);
            quote.setOpen(Float.parseFloat(items[iOpen].trim()));
            quote.setHigh(Float.parseFloat(items[iHigh].trim()));
            quote.setLow(Float.parseFloat(items[iLow].trim()));
            quote.setClose(Float.parseFloat(items[iClose].trim()));
            quote.setVolume(Float.parseFloat(items[iVolume].trim()) / 100f);
            quote.setAmount(-1);
            quote.setClose_adj(Float.parseFloat(items[iAdjClose].trim()));

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
        return "Yahoo! Finance Internet";
    }

    public String getDefaultDateFormatString() {
        return "yyyy-mm-dd";
    }

    public byte getSourceSerialNumber() {
        return (byte) 1;
    }

    @Override
    public Frequency[] getSupportedFreqs() {
        return new Frequency[]{Frequency.DAILY};
    }

    @Override
    public Image getIcon() {
        BufferedImage img;
        try {
            img = ImageIO.read(new File("org/aiotrade/platform/modules/dataserver/basic/netbeans/resources/favicon_yahoo.png"));
        } catch (IOException e) {
            img = null;
        }
        return img;
    }

    @Override
    public TimeZone getSourceTimeZone() {
        return TimeZone.getTimeZone("America/New_York");
    }

    public Market getMarket(String symbol) {
        return GetMarket(symbol);
    }

    public static Market GetMarket(String symbol) {
        String[] tokens = symbol.split("\\.");
        if (tokens.length >= 2) {
            String marketSym = tokens[tokens.length - 1];
            if (marketSym.equalsIgnoreCase("L")) {
                return Market.LDSE;
            } else if (marketSym.equalsIgnoreCase("SS")) {
                return Market.SHSE;
            } else if (marketSym.equalsIgnoreCase("SZ")) {
                return Market.SZSE;
            }
        }

        return Market.NYSE;
    }
}


