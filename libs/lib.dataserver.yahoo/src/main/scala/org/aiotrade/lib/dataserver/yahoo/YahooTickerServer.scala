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
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Ticker
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

/**
 * @NOTICE
 * If the remote datafeed keeps only one inputstream for all subscriiebed
 * symbols, one singleton instance is enough. If each symbol need a separate
 * session, you may create new data server instance for each symbol.
 * 
 * @author Caoyuan Deng
 */
object YahooTickerServer extends YahooTickerServer {
  override protected val isTheSingleton = true
}
class YahooTickerServer extends TickerServer {
  private val log = Logger.getLogger(this.getClass.getName)
  protected val isTheSingleton = false

  private val nSymbolsPerReq = 100

  // * "http://download.finance.yahoo.com/d/quotes.csv"
  private val BaseUrl = "http://quote.yahoo.com"
  private val UrlPath = "/download/javasoft.beans"

  /**
   * Template:
   * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
   */
  protected def request(srcSymbol: Seq[String]): Option[InputStream] = {
    if (srcSymbol.isEmpty) return None

    val cal = Calendar.getInstance(sourceTimeZone)

    val urlStr = new StringBuilder(90)
    urlStr.append(BaseUrl).append(UrlPath)
    urlStr.append("?s=")

    urlStr.append(srcSymbol mkString("+"))

    urlStr.append("&d=t&f=sl1d1t1c1ohgvbap")

    /** s: symbol, n: name, x: stock exchange */
    val urlStrForName = urlStr.append("&d=t&f=snx").toString


    try {
      val url = new URL(urlStr.toString)
      log.info(url.toString)

      val conn = url.openConnection.asInstanceOf[HttpURLConnection]
      conn.setRequestProperty("Accept-Encoding", "gzip")
      conn.setAllowUserInteraction(true)
      conn.setRequestMethod("GET")
      conn.setInstanceFollowRedirects(true)
      conn.setConnectTimeout(5000)
      conn.setReadTimeout(5000)
      conn.connect

      val encoding = conn.getContentEncoding
      val gzipped = encoding != null && encoding.indexOf("gzip") != -1

      val is = conn.getInputStream
      if (is == null) None else Some(if (gzipped) new GZIPInputStream(is) else is)
    } catch {
      case e: SocketTimeoutException => None
      case e => None
    }
  }

  @throws(classOf[Exception])
  protected def read(is: InputStream): Array[Ticker] = {
    val reader = new BufferedReader(new InputStreamReader(is))

    resetCount
    
    val tickers = new ArrayList[Ticker]

    // time in Yahoo! tickers is in Yahoo! Inc's local time instead of exchange place
    // we need to convert them to UTC time
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

            val exchange = Exchange.exchangeOf(symbol)
            
            /**
             * !NOTICE
             * must catch the date parse exception, other wise, it's dangerous
             * for build a calendarTimes in BaseTSer
             */
            try {
              val date = dateFormat.parse(dateStr + " " + timeStr)
              cal.clear
              cal.setTime(date)

              // filter tickers that have incorrect time
              val h = cal.get(Calendar.HOUR_OF_DAY)
              val m = cal.get(Calendar.MINUTE)
              val minutesOfDay = h * 60 + m
              if (minutesOfDay < exchange.firstOpen - 30 || minutesOfDay > exchange.lastClose + 30) {
                loop(newestTime)
              }
            } catch {case _: ParseException => loop(newestTime)}

            val time = cal.getTimeInMillis
            if (time == 0) {
              /** for test and finding issues */
              log.warning("time of ticker: " + symbol + " is 0!")
            }

            val tickerSnapshot = tickerSnapshotOf(symbol)
            tickerSnapshot.time = time

            tickerSnapshot.prevClose = if (prevCloseX.equalsIgnoreCase("N/A")) 0 else prevCloseX.trim.toDouble
            tickerSnapshot.lastPrice = if (lastPriceX.equalsIgnoreCase("N/A")) 0 else lastPriceX.trim.toDouble
            tickerSnapshot.dayChange = if (dayChangeX.equalsIgnoreCase("N/A")) 0 else dayChangeX.trim.toDouble
            tickerSnapshot.dayOpen   = if (dayOpenX.equalsIgnoreCase("N/A")) 0 else dayOpenX.trim.toDouble
            tickerSnapshot.dayHigh   = if (dayHighX.equalsIgnoreCase("N/A")) 0 else dayHighX.trim.toDouble
            tickerSnapshot.dayLow    = if (dayLowX.equalsIgnoreCase("N/A")) 0 else dayLowX.trim.toDouble
            tickerSnapshot.dayVolume = if (dayVolumeX.equalsIgnoreCase("N/A")) 0 else dayVolumeX.trim.toDouble
            tickerSnapshot.setBidPrice(0, if (bidPriceX1.equalsIgnoreCase("N/A")) 0 else bidPriceX1.trim.toDouble)
            tickerSnapshot.setAskPrice(0, if (askPriceX1.equalsIgnoreCase("N/A")) 0 else askPriceX1.trim.toDouble)

            if (tickerSnapshot.isChanged && this.subscribedSrcSymbols.contains(symbol)) {
              val ticker = new Ticker
              ticker.symbol = symbol
              ticker.copyFrom(tickerSnapshot)
              tickers += ticker
            }

            countOne
            loop(math.max(newestTime, time))
          case _ => loop(newestTime)
        }
    }

    val newestTime = loop(Long.MinValue)
    log.info("Got tickers: " + count)
    
    tickers.toArray
  }

  /**
   * Retrive data from Yahoo finance website
   * Template:
   * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
   *
   * @param afterThisTime from time
   */
  protected def loadFromSource(afterThisTime: Long): Array[Ticker] = {
    if (!isTheSingleton) return EmptyValues

    log.info("Loading from source ...")

    val symbols = subscribedContracts map (_.srcSymbol) toArray
    var i = 0
    while (i < symbols.length) {
      val toProcess = new ListBuffer[String]
      var j = 0
      while (j < nSymbolsPerReq && i < symbols.length) { // 1000: num of symbols per time
        toProcess += symbols(i)
        j += 1
        i += 1
      }
      if (!toProcess.isEmpty) {
        try {
          request(toProcess) match {
            case Some(is) => 
              val tickers = read(is)
              loadedTime = postRefresh(tickers)
            case None =>
          }
        } catch {case ex: Exception => log.log(Level.WARNING, ex.getMessage, ex)}
      }
    }

    log.info("Finished loading from source")

    EmptyValues
  }

  override def createNewInstance: Option[YahooTickerServer] = Some(YahooTickerServer)

  override def displayName: String = "Yahoo! Finance Internet"

  def defaultDateFormatPattern: String = "MM/dd/yyyy h:mma"

  def sourceSerialNumber = 1

  val sourceTimeZone = TimeZone.getTimeZone("America/New_York")
}



