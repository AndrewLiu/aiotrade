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
package org.aiotrade.lib.dataserver.ib;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.TickType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.Quote
import org.aiotrade.lib.securities.QuotePool
import org.aiotrade.lib.securities.Security
import org.aiotrade.lib.securities.Ticker
import org.aiotrade.lib.securities.TickerPool
import org.aiotrade.lib.securities.TickerSnapshot
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer


/**
 *
 * @author Caoyuan Deng
 */
class IBWrapper extends EWrapperAdapter

object IBWrapper extends IBWrapper {
  private val quotePool = new QuotePool
  private val tickerPool = new TickerPool

  private val TWS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss")
  private val HISTORICAL_DATA_END = "finished"
  private val HIS_REQ_PROC_SPEED_THROTTLE = 1000 * 20 // 20 seconds

  // TreeMap
  private val freqToBarSize = TreeMap(
    TFreq.ONE_SEC      ->  1,
    TFreq.FIVE_SECS    ->  2,
    TFreq.FIFTEEN_SECS ->  3,
    TFreq.THREE_SECS   ->  4,
    TFreq.ONE_MIN      ->  5,
    TFreq.TWO_MINS     ->  6,
    TFreq.THREE_MINS   -> 16,
    TFreq.FIVE_MINS    ->  7,
    TFreq.FIFTEEN_MINS ->  8,
    TFreq.THIRTY_MINS  ->  9,
    TFreq.ONE_HOUR     -> 10,
    TFreq.DAILY        -> 11,
    TFreq.WEEKLY       -> 12,
    TFreq.MONTHLY      -> 13,
    TFreq.THREE_MONTHS -> 14,
    TFreq.ONE_YEAR     -> 15)

  private val secTypesToName: Map[Security.Type, String] = Map(
    Security.Type.Stock        -> "STK",
    Security.Type.Stock        -> "STK",
    Security.Type.Option       -> "OPT",
    Security.Type.Future       -> "FUT",
    Security.Type.Index        -> "IND",
    Security.Type.FutureOption -> "FOP",
    Security.Type.Currency     -> "CASH",
    Security.Type.Bag          -> "BAG")

  private var singletonInstance: IBWrapper = this
  private var eclient: EClientSocket = new EClientSocket(this)

  private lazy val hisRequestServer = new HisRequestServer

  private val host = ""
  private val port = 7496
  private val clientId = 0
    
  /** in IB, the next valid id after connected should be 1 */
  private var nextReqId = 1
  private var reqIdToHisDataReq = new TreeMap[Int, HistoricalDataRequest]
  private var reqIdToMktDataReq = new TreeMap[Int, MarketDataRequest]
    
  private var serverVersion: Int = _
    
  def getBarSize(freq: TFreq) = {
    for (afreq <- freqToBarSize.keySet) {
      if (afreq.equals(freq)) {
        freqToBarSize.get(afreq)
      }
    }
        
    1;
  }
    
  def getSupportedFreqs: Array[TFreq] = {
    freqToBarSize.keySet.toArray
  }
    
  def getSecType(tpe: Security.Type) = {
    secTypesToName.get(tpe)
  }
    
  private def askReqId: Int = synchronized {
    val reqId = nextReqId
    nextReqId += 1
        
    reqId
  }
    
  private def getQuoteStorage(reqId: Int): ArrayBuffer[Quote] = {
    reqIdToHisDataReq.get(reqId) match {
      case None => null
      case Some(hisReq) => hisReq.storage
    }
  }
    
  private def getHisDataRequestor(reqId: Int): DataServer[_] = {
    reqIdToHisDataReq.get(reqId) match {
      case None => null
      case Some(hisReq) => hisReq.requestor
    }
  }
    
  def isHisDataReqPending(reqId: Int): Boolean = {
    reqIdToHisDataReq.contains(reqId)
  }
    
  def connect: Unit = synchronized {
    if (isConnected) {
      return
    }
        
    eclient.eConnect(host, port, clientId)
    var timeout = false
    var break = false
    while (!isConnected && !timeout && !break) {
      try {
        wait(TUnit.Second.interval * 5)
        timeout = true // whatever
      } catch {case ex: InterruptedException => break = true}
    }
        
    if (isConnected) {
      /**
       * IB Log levels: 1 = SYSTEM 2 = ERROR 3 = WARNING 4 = INFORMATION 5 = DETAIL
       */
      eclient.setServerLogLevel(2)
      eclient.reqNewsBulletins(true)
      serverVersion = eclient.serverVersion
            
      //WindowManager.getDefault.setStatusText("TWS connected. Server version: " + serverVersion)
    } else {
      //WindowManager.getDefault().setStatusText("Could not connect to TWS.")
    }
  }
    
  def isConnected: Boolean = {
    eclient.isConnected
  }
    
  def getTwsDateFormart: DateFormat = {
    TWS_DATE_FORMAT
  }
    
  def reqHistoricalData(requestor: DataServer[_ <: DataContract[_]], storage: ArrayBuffer[Quote],
                        contract: Contract, endDateTime: String, durationStr: String,
                        barSizeSetting: Int, whatToShow: String, useRTH: Int, formatDate: Int): Int = {
        
    val reqId = askReqId
        
    val hisReq = HistoricalDataRequest(
      requestor,
      storage,
      contract,
      endDateTime,
      durationStr,
      barSizeSetting,
      whatToShow,
      useRTH,
      formatDate,
      reqId
    )
        
    reqIdToHisDataReq synchronized {
      reqIdToHisDataReq += (reqId -> hisReq)
    }
        
    if (!hisRequestServer.isInRunning) {
      new Thread(hisRequestServer).start
    }
        
    return reqId
  }
    
  def reqMktData(requestor: DataServer[_], contract: Contract, tickerSnapshot: TickerSnapshot): Int = {
    val reqId = askReqId
        
    val mktReq = MarketDataRequest(
      contract,
      tickerSnapshot,
      reqId
    )
        
    reqIdToMktDataReq synchronized {
      reqIdToMktDataReq += (reqId -> mktReq)
    }
        
    eclient.reqMktData(reqId, contract)
        
    reqId
  }
    
  def cancelHisDataRequest(reqId: Int) {
    eclient.cancelHistoricalData(reqId);
    clearHisDataRequest(reqId);
  }
    
  def cancelMktDataRequest(reqId: Int) {
    eclient.cancelMktData(reqId);
    reqIdToMktDataReq synchronized {
      reqIdToMktDataReq -= reqId
    }
  }
    
  def isMktDataRequested(reqId: Int): Boolean = {
    reqIdToMktDataReq.contains(reqId)
  }
    
  private def getTickerSnapshot(reqId: Int): TickerSnapshot = {
    reqIdToMktDataReq.get(reqId) match {
      case None => null
      case Some(mktReq) => mktReq.snapshotTicker
    }
  }
    
  private def clearHisDataRequest(hisReqId: Int) {
    reqIdToHisDataReq synchronized {
      reqIdToHisDataReq -= hisReqId
    }
  }
    
  def getServerVersion: Int = {
    serverVersion
  }
    
  def disconnect {
    if (eclient != null && eclient.isConnected) {
      eclient.cancelNewsBulletins
      eclient.eDisconnect
    }
  }
    
  override def nextValidId(orderId: Int) {
    /**
     * this seems only called one when connected or re-connected. As we use
     * auto-increase id, we can just ignore it?
     */
  }
    
  /** A historical data arrived */
  override def historicalData(reqId: Int, date: String,
                              open: Double, high: Double, low: Double, close: Double, volume: Int, WAP: Double, hasGaps: Boolean) {
        
    val storage = getQuoteStorage(reqId)
    if (storage == null) {
      return
    }
        
    /** we only need lock storage here */
    storage synchronized {
      try {
        if (date.startsWith(HISTORICAL_DATA_END)) {
          val requstor = getHisDataRequestor(reqId)
          if (requstor != null) {
            requstor synchronized {
              requstor.notifyAll
              System.out.println("requstor nofity all: finished")
            }
          }
          clearHisDataRequest(reqId)
        } else {
          val time = try {
            date.toLong * 1000
          } catch {case ex: NumberFormatException => return}
                    
          val quote = quotePool.borrowObject
                    
          quote.time = time
          quote.open = open.toFloat
          quote.high = high.toFloat
          quote.low = low.toFloat
          quote.close = close.toFloat
          quote.volume = volume
                    
          quote.wap = WAP.toFloat
          quote.hasGaps = hasGaps
                    
          storage += quote
                    
          /** quote is still pending for process, don't return it */
        }
      } catch {case x: Throwable =>
          /**
           * Catch any Throwable to prevent them back to the eclient (will cause disconnect).
           * We don't need cancel this historical requset as the next data may be good.
           */
      }
    }
  }
    
  override def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) {
    // received price tick
    val tickerSnapshot = getTickerSnapshot(tickerId)
    if (tickerSnapshot == null) {
      return;
    }
        
    tickerSnapshot synchronized {
      val value = price.toFloat
      tickerSnapshot.time = System.currentTimeMillis
      field match {
        case TickType.ASK =>
          tickerSnapshot.setAskPrice(0, value)
        case TickType.ASK_SIZE =>
          tickerSnapshot.setAskSize(0, value)
        case TickType.BID =>
          tickerSnapshot.setBidPrice(0, value)
        case TickType.BID_SIZE =>
          tickerSnapshot.setBidSize(0, value)
        case TickType.CLOSE =>
          tickerSnapshot(Ticker.PREV_CLOSE) = value
        case TickType.HIGH =>
          tickerSnapshot(Ticker.DAY_HIGH) = value
        case TickType.LAST =>
          tickerSnapshot(Ticker.LAST_PRICE) = value
        case TickType.LAST_SIZE =>
        case TickType.LOW =>
          tickerSnapshot(Ticker.DAY_LOW) = value
        case TickType.VOLUME =>
          tickerSnapshot(Ticker.DAY_VOLUME) = value
        case _ =>
      }
    }
        
    tickerSnapshot.notifyObservers
        
    //System.out.println("id=" + tickerId + "  " + TickType.getField( field) + "=" + price + " " +
    //(canAutoExecute != 0 ? " canAutoExecute" : " noAutoExecute"));
  }
    
  override def tickSize(tickerId: Int, field: Int, size: Int) {
    // received size tick
    tickPrice(tickerId, field, size, 0);
  }
    
  override def tickOptionComputation(tickerId: Int, field: Int, impliedVol: Double, delta: Double) {
    // received price tick
    //        System.out.println( "id=" + tickerId + "  " + TickType.getField( field) + ": vol = " +
    //                ((impliedVol >= 0 && impliedVol != Double.MAX_VALUE) ? Double.toString(impliedVol) : "N/A") + " delta = " +
    //                ((Math.abs(delta) <= 1) ? Double.toString(delta) : "N/A") );
  }
    
  override def error(error: String) {
    //WindowManager.getDefault().setStatusText(error);
  }
    
  private val msg = new StringBuilder(40)
  override def error(id: Int, errorCode: Int, errorMsg: String) {
    msg.delete(0, msg.length);
    if (id < 0) {
      /** connected or not connected msg, notify connect() waiting */
      this synchronized {
        notifyAll();
      }
    } else {
      msg.append("Error: reqId = ")
    }
    msg.append(id).append(" | ").append(errorCode).append(" : ").append(errorMsg).toString
        
    System.out.println(msg.toString)
    //WindowManager.getDefault().setStatusText(msg.toString)
        
    /** process error concerns with hisReq */
    val shouldResetAllHisReqs = (
      (errorCode == 1102) ||
      (errorCode == 165 && msg.toString.contains("HMDS connection attempt failed")) ||
      (errorCode == 165 && msg.toString.contains("HMDS server disconnect occurred")))
        
    if (shouldResetAllHisReqs) {
      for (reqId <- reqIdToHisDataReq.keySet) {
        resetHisReq(reqId)
      }
    } else {
      if (reqIdToHisDataReq.contains(id)) {
        resetHisReq(id)
      }
    }
        
  }
    
  private def resetHisReq(reqId: Int) {
    val requstor = getHisDataRequestor(reqId)
    if (requstor != null) {
      requstor synchronized {
        requstor.notifyAll
        //System.out.println("requstor nofity all on error");
      }
      /** Don't do this before requstor has been fetched from map ! */
      cancelHisDataRequest(reqId)
    }
  }
    
  override def connectionClosed {
  }
        
  private class HisRequestServer extends Runnable {
        
    private var inRunning: Boolean = false
        
    def isInRunning = {
      inRunning;
    }
        
    def run {
      inRunning = true
            
      var inRoundProcessing = false
      while (!inRoundProcessing) {
        try {
          Thread.sleep(HIS_REQ_PROC_SPEED_THROTTLE)
        } catch {
          case ex: InterruptedException =>
            ex.printStackTrace
            inRunning = false
            return
        }
                
        inRoundProcessing = true

        /** just fetch the first one to process */
        val hisReq = reqIdToHisDataReq(reqIdToHisDataReq.firstKey)
        eclient.reqHistoricalData(
          hisReq.reqId,
          hisReq.contract,
          hisReq.endDateTime,
          hisReq.durationStr,
          hisReq.barSizeSetting,
          hisReq.whatToShow,
          hisReq.useRTH,
          hisReq.formatDate
        )
                    
        inRoundProcessing = false
      }
    }
  }

  private case class MarketDataRequest(
    contract: Contract,
    snapshotTicker: TickerSnapshot,
    reqId: Int
  )

  private case class HistoricalDataRequest(
    requestor: DataServer[_ <: DataContract[_]],
    storage: ArrayBuffer[Quote],
    contract: Contract,
    endDateTime: String,
    durationStr: String,
    barSizeSetting: Int,
    whatToShow: String,
    useRTH: Int,
    formatDate: Int,
    reqId: Int
  )

}

