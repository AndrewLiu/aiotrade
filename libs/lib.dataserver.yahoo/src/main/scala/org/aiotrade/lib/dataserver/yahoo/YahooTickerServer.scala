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
package org.aiotrade.lib.dataserver.yahoo

import java.io.{BufferedReader, InputStreamReader, InputStream}
import java.net.{HttpURLConnection, URL, SocketTimeoutException}
import java.text.ParseException
import java.util.{Calendar, TimeZone}
import java.util.zip.GZIPInputStream
import org.aiotrade.lib.securities.dataserver.TickerServer
import scala.annotation.tailrec

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
  protected var singletonInstance: Option[YahooTickerServer] = None
  // * "http://download.finance.yahoo.com/d/quotes.csv"
  protected val BaseUrl = "http://quote.yahoo.com"
  protected val UrlPath = "/download/javasoft.beans"
}

import YahooTickerServer._
class YahooTickerServer extends TickerServer {

  private var gzipped = false

  protected def connect: Boolean = true

  /**
   * Template:
   * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
   */
  protected def request: Option[InputStream] = {
    val cal = Calendar.getInstance(sourceTimeZone)

    val urlStr = new StringBuilder(90)
    urlStr.append(BaseUrl).append(UrlPath)
    urlStr.append("?s=")

    val contracts = subscribedContracts
    if (!contracts.hasNext) {
      return None
    }

    while (contracts.hasNext) {
      urlStr.append(contracts.next.symbol)
      if (contracts.hasNext) urlStr.append("+")
    }

    urlStr.append("&d=t&f=sl1d1t1c1ohgvbap")

    /** s: symbol, n: name, x: stock exchange */
    val urlStrForName = urlStr.append("&d=t&f=snx").toString


    try {
      val url = new URL(urlStr.toString)
      println(url)

      val conn = url.openConnection.asInstanceOf[HttpURLConnection]
      conn.setRequestProperty("Accept-Encoding", "gzip")
      conn.setAllowUserInteraction(true)
      conn.setRequestMethod("GET")
      conn.setInstanceFollowRedirects(true)
      conn.setConnectTimeout(5000)
      conn.connect

      val encoding = conn.getContentEncoding
      gzipped = if (encoding != null && encoding.indexOf("gzip") != -1) {
        true
      } else false
        
      Option(conn.getInputStream)
    } catch {
      case e: SocketTimeoutException => None
      case e => None
    }
  }

  @throws(classOf[Exception])
  protected def read(is: InputStream): Long = {
    val reader = new BufferedReader(new InputStreamReader(if (gzipped) new GZIPInputStream(is) else is))

    resetCount
    // time in Yahoo! tickers is in Yahoo! Inc's local time instead of exchange place, we need to convert to UTC time
    val cal = Calendar.getInstance(sourceTimeZone)
    val dateFormat = dateFormatOf(sourceTimeZone)

    @tailrec
    def loop(newestTime: Long): Long = reader.readLine match {
      case null => newestTime // break right now
      case line => line.split(",") match {
          case Array(symbolX, lastPriceX, dateX, timeX, dayChangeX, dayOpenX, dayHighX, dayLowX, dayVolumeX, bidPriceX1, askPriceX1, prevCloseX, _, _, _, nameX, marketX, _*)
            if !dateX.toUpperCase.contains("N/A") && !timeX.toUpperCase.contains("N/A") =>

            val symbol  = symbolX.toUpperCase.replace('"', ' ').trim
            val dateStr = dateX.replace('"', ' ').trim
            val timeStr = timeX.replace('"', ' ').trim

            /**
             * !NOTICE
             * must catch the date parse exception, other wise, it's dangerous
             * for build a calendarTimes in BaseTSer
             */
            try {
              val date = dateFormat.parse(dateStr + " " + timeStr)
              cal.clear
              cal.setTime(date)
            } catch {case _: ParseException => loop(newestTime)}

            val time = cal.getTimeInMillis
            if (time == 0) {
              /** for test and finding issues */
              println("time of ticker: " + symbol + " is 0!")
            }

            val tickerSnapshot = tickerSnapshotOf(symbol)
            tickerSnapshot.time = time

            tickerSnapshot.prevClose = if (prevCloseX.equalsIgnoreCase("N/A")) 0 else prevCloseX.trim.toFloat
            tickerSnapshot.lastPrice = if (lastPriceX.equalsIgnoreCase("N/A")) 0 else lastPriceX.trim.toFloat
            tickerSnapshot.dayChange = if (dayChangeX.equalsIgnoreCase("N/A")) 0 else dayChangeX.trim.toFloat
            tickerSnapshot.dayOpen   = if (dayOpenX.equalsIgnoreCase("N/A")) 0 else dayOpenX.trim.toFloat
            tickerSnapshot.dayHigh   = if (dayHighX.equalsIgnoreCase("N/A")) 0 else dayHighX.trim.toFloat
            tickerSnapshot.dayLow    = if (dayLowX.equalsIgnoreCase("N/A")) 0 else dayLowX.trim.toFloat
            tickerSnapshot.dayVolume = if (dayVolumeX.equalsIgnoreCase("N/A")) 0 else dayVolumeX.trim.toFloat / 100f
            tickerSnapshot.setBidPrice(0, if (bidPriceX1.equalsIgnoreCase("N/A")) 0 else bidPriceX1.trim.toFloat)
            tickerSnapshot.setAskPrice(0, if (askPriceX1.equalsIgnoreCase("N/A")) 0 else askPriceX1.trim.toFloat)

            tickerSnapshot.fullName = nameX
            tickerSnapshot.notifyChanged

            countOne
            loop(math.max(newestTime, time))
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
  protected def loadFromSource(afterThisTime: Long): Long = {
    fromTime = afterThisTime + 1

    var loadedTime1 = loadedTime
    if (!connect) {
      return loadedTime
    }
        
    try {
      request match {
        case Some(is) => loadedTime1 = read(is)
        case None => loadedTime1 = loadedTime
      }
    } catch {case ex: Exception => ex.printStackTrace}

    loadedTime1
  }

  override def createNewInstance: Option[YahooTickerServer] = {
    if (singletonInstance == None) {
      super.createNewInstance match {
        case None =>
        case Some(x: YahooTickerServer) => x.init; singletonInstance = Some(x)
      }
    }
    singletonInstance
  }

  override def displayName: String = "Yahoo! Finance Internet"

  def defaultDateFormatPattern: String = "MM/dd/yyyy h:mma"

  def sourceSerialNumber = 1

  val sourceTimeZone = TimeZone.getTimeZone("America/New_York")
}



