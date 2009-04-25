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
package org.aiotrade.platform.modules.dataserver.yahoo

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import org.aiotrade.lib.securities.dataserver.{TickerContract,TickerServer}
import org.aiotrade.lib.securities.{Market,Ticker,TickerSnapshot}

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
object YahooTickerServer {
    /**
     * @NOTICE
     * If the remote datafeed keeps only one inputstream for all subscriiebed
     * symbols, one singleton instance is enough. If each symbol need a separate
     * session, you may create new data server instance for each symbol.
     */
    protected var singletonInstance :Option[YahooTickerServer] = None
    // * "http://download.finance.yahoo.com/d/quotes.csv"
    protected val BaseUrl = "http://aiotrade.com/"
    protected val UrlPath = "aiodata/yt"
}

class YahooTickerServer extends TickerServer {
    import YahooTickerServer._

    private var gzipped = false

    protected def connect :Boolean = true

    /**
     * Template:
     * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
     */
    @throws(classOf[Exception])
    protected def request :Unit = {
        sourceCalendar.clear

        val urlStr = new StringBuilder(90)
        urlStr.append(BaseUrl).append(UrlPath)
        urlStr.append("?s=")

        val contracts = subscribedContracts
        if (contracts.size == 0) {
            inputStream = None
            return
        }

        val itr = contracts.elements
        while (itr.hasNext) {
            urlStr.append(itr.next.symbol)
            if (itr.hasNext) urlStr.append("+")
        }

        urlStr.append("&d=t&f=sl1d1t1c1ohgvbap")

        /** s: symbol, n: name, x: stock exchange */
        val urlStrForName = urlStr.append("&d=t&f=snx").toString

        val url = new URL(urlStr.toString)
        println(url)

        val conn = url.openConnection.asInstanceOf[HttpURLConnection]
        conn.setRequestProperty("Accept-Encoding", "gzip")
        conn.setAllowUserInteraction(true)
        conn.setRequestMethod("GET")
        conn.setInstanceFollowRedirects(true)
        conn.connect

        val encoding = conn.getContentEncoding
        gzipped = if (encoding != null && encoding.indexOf("gzip") != -1) {
            true
        } else false
        
        inputStream = conn.getInputStream match {
            case null => None
            case is => Some(is)
        }
    }

    @throws(classOf[Exception])
    protected def read :Long = {
        val is = inputStream match {
            case None => return loadedTime
            case Some(x) => x
        }

        val reader = if (gzipped) {
            new BufferedReader(new InputStreamReader(new GZIPInputStream(is)))
        } else {
            new BufferedReader(new InputStreamReader(is))
        }

        resetCount
        sourceCalendar.clear
        val dateFormat = dateFormatOf(sourceTimeZone)
        def loop(newestTime:Long) :Long = reader.readLine match {
            case null => newestTime // break right now
            case line => line.split(",") match {
                    case Array(symbolX, lastPriceX, dateX, timeX, dayChangeX, dayOpenX, dayHighX, dayLowX, dayVolumeX, bidPriceX1, askPriceX1, prevCloseX, _*) =>
                        val symbol = symbolX.toUpperCase.replace('"', ' ').trim

                        val dateStr = dateX.replace('"', ' ').trim
                        val timeStr = timeX.replace('"', ' ').trim
                        if (dateStr.equalsIgnoreCase("N/A") || timeStr.equalsIgnoreCase("N/A")) {
                            loop(newestTime)
                        }

                        /**
                         * !NOTICE
                         * must catch the date parse exception, other wise, it's dangerous
                         * for build a calendarTimes in MasterSer
                         */
                        try {
                            val date = dateFormat.parse(dateStr + " " + timeStr)
                            sourceCalendar.clear
                            sourceCalendar.setTime(date)
                        } catch {
                            case ex:ParseException =>
                                ex.printStackTrace
                                loop(newestTime)
                        }

                        val time = sourceCalendar.getTimeInMillis
                        if (time == 0) {
                            /** for test and finding issues */
                            println("time of ticker: " + symbol + " is 0!")
                        }

                        val tickerSnapshot = tickerSnapshotOf(symbol).get
                        tickerSnapshot.time = time

                        tickerSnapshot(Ticker.PREV_CLOSE) = if (prevCloseX.equalsIgnoreCase("N/A")) 0 else prevCloseX.trim.toFloat
                        tickerSnapshot(Ticker.LAST_PRICE) = if (lastPriceX.equalsIgnoreCase("N/A")) 0 else lastPriceX.trim.toFloat
                        tickerSnapshot(Ticker.DAY_CHANGE) = if (dayChangeX.equalsIgnoreCase("N/A")) 0 else dayChangeX.trim.toFloat
                        tickerSnapshot(Ticker.DAY_OPEN)   = if (dayOpenX.equalsIgnoreCase("N/A")) 0 else dayOpenX.trim.toFloat
                        tickerSnapshot(Ticker.DAY_HIGH)   = if (dayHighX.equalsIgnoreCase("N/A")) 0 else dayHighX.trim.toFloat
                        tickerSnapshot(Ticker.DAY_LOW)    = if (dayLowX.equalsIgnoreCase("N/A")) 0 else dayLowX.trim.toFloat
                        tickerSnapshot(Ticker.DAY_VOLUME) = if (dayVolumeX.equalsIgnoreCase("N/A")) 0 else dayVolumeX.trim.toFloat / 100f
                        tickerSnapshot.setBidPrice(0, if (bidPriceX1.equalsIgnoreCase("N/A")) 0 else bidPriceX1.trim.toFloat)
                        tickerSnapshot.setAskPrice(0, if (askPriceX1.equalsIgnoreCase("N/A")) 0 else askPriceX1.trim.toFloat)

                        tickerSnapshot.fullName = symbol
                        tickerSnapshot.notifyObservers

                        countOne
                        loop(Math.max(newestTime, time))
                    case _ => loop(newestTime)
                }
        }

        val newestTime = loop(-Long.MaxValue)

        if (count > 0) {
            /**
             * Tickers may have the same time stamp even for a new one,
             * but here means there is at least one new ticker, so always return
             * afterThisTime + 1, this will cause fireDataUpdatedEvent, even only
             * one symbol's ticker renewed. But that is good, because it will
             * tell all symbols that at least one new time tick has happened.
             */
            fromTime
        } else {
            newestTime
        }
    }

    /**
     * Retrive data from Yahoo finance website
     * Template:
     * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
     *
     * @param afterThisTime from time
     */
    protected def loadFromSource(afterThisTime:Long) :Long = {
        fromTime = afterThisTime + 1

        var loadedTime1 = loadedTime
        if (!connect) {
            return loadedTime
        }
        
        try {
            request
            loadedTime1 = read
        } catch {
            case ex:Exception => ex.printStackTrace
        }

        loadedTime1
    }

    override
    def createNewInstance :Option[YahooTickerServer] = {
        if (singletonInstance == None) {
            super.createNewInstance match {
                case None =>
                case Some(x:YahooTickerServer) => x.init; singletonInstance = Some(x)
            }
        }
        singletonInstance
    }

    override
    def displayName :String = "Yahoo! Finance Internet"

    def defaultDateFormatString :String = "MM/dd/yyyy h:mma"

    def sourceSerialNumber :Byte = 1

    def sourceTimeZone = TimeZone.getTimeZone("America/New_York")

    override
    def marketOf(symbol:String) :Market = {
        return YahooQuoteServer.marketOf(symbol)
    }
}



