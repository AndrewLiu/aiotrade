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
package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import org.aiotrade.lib.securities.model.Sec
import ru.circumflex.orm._

object QuoteServer {

  /**
   * All quotes in storage should have been properly rounded to 00:00 of exchange's local time
   */
  def loadFromPersistence(sec: Sec, serToLoad: QuoteSer): Long = {
    val uniSymbol = sec.secInfo.uniSymbol
    val freq = serToLoad.freq
    
    /**
     * 1. restore data from database
     */
    val quotes = if (freq == TFreq.DAILY) {
      Quotes1d.closedQuotesOf(sec)
    } else if (freq == TFreq.ONE_MIN) {
      Quotes1m.closedQuotesOf(sec)
    } else Nil
    
    composeSer(uniSymbol, serToLoad, quotes)

    /**
     * 2. get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    val size = quotes.length
    val loadedTime1 = if (size > 0) quotes(size - 1).time else 0L
    serToLoad.publish(TSerEvent.RefreshInLoading(serToLoad,
                                                 uniSymbol,
                                                 0,
                                                 loadedTime1))

    loadedTime1
  }


  /**
   * compose ser using data from quote(s)
   *
   * All quotes in storage should have been properly rounded to 00:00 of exchange's local time
   * @param symbol
   * @param serToBeFilled Ser
   * @param quotes
   */
  protected def composeSer(uniSymbol: String, quoteSer: QuoteSer, quotes: Seq[Quote]): TSerEvent = {
    var evt: TSerEvent = null

    if (!quotes.isEmpty) {
      // copy to a new array and don't change it anymore, so we can ! it as message
      quoteSer ++= quotes.toArray
    }

    evt
  }

}

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
import QuoteServer._
abstract class QuoteServer extends DataServer[Quote] {
  type C = QuoteContract
  type T = QuoteSer

  reactions += {
    case Exchange.Opened(exchange: Exchange) =>
    case Exchange.Closed(exchange: Exchange) =>
  }

  listenTo(Exchange)

  /**
   * All quotes in storage should have been properly rounded to 00:00 of exchange's local time
   */
  protected def loadFromPersistence: Long = {
    var loadedTime1 = loadedTime
    for (contract <- subscribedContracts) {
      val uniSymbol = toUniSymbol(contract.symbol)
      val sec = Exchange.secOf(uniSymbol).get
      val ser = sec.serOf(contract.freq).get
      loadedTime1 = QuoteServer.loadFromPersistence(sec, ser)
    }
    loadedTime1
  }


  /**
   * All quotes in storage should have been properly rounded to 00:00 of exchange's local time
   */
  override protected def postLoadHistory {
    for (contract <- subscribedContracts) {
      val serToFill = serOf(contract).get

      val freq = serToFill.freq
      val storage = storageOf(contract)
      val sec = Exchange.secOf(contract.symbol).get
      storage foreach {_.sec = sec}
      if (freq == TFreq.DAILY) {
        //Quotes1d.insertBatch(storage.toArray)
        Quotes1d.saveBatch(sec, storage)
        commit
      } else if (freq == TFreq.ONE_MIN) {
        //Quotes1m.insertBatch(storage.toArray)
        Quotes1m.saveBatch(sec, storage)
        commit
      }

      var evt = composeSer(toUniSymbol(contract.symbol), serToFill, storage)
      //            if (evt != null) {
      //                evt.tpe = TSerEvent.Type.FinishedLoading
      //                //WindowManager.getDefault().setStatusText(contract.getSymbol() + ": " + getCount() + " quote data loaded, load server finished");
      //            } else {
      //                /** even though, we may have loaded data in preLoad(), so, still need fire a FinishedLoading event */
      //                val loadedTime1 = serToBeFilled.lastOccurredTime
      //                evt = new TSerEvent(serToBeFilled, TSerEvent.Type.FinishedLoading, contract.symbol, loadedTime1, loadedTime1)
      //            }
      //
      //            serToBeFilled.fireTSerEvent(evt)
      
      storage synchronized {storage.clear}
    }
  }

  override protected def postRefresh {
    for (contract <- subscribedContracts) {
      val storage = storageOf(contract)

      val evt = composeSer(toUniSymbol(contract.symbol), serOf(contract).get, storage)
      //            if (evt != null) {
      //                evt.tpe = TSerEvent.Type.Updated
      //                evt.getSource.fireTSerEvent(evt)
      //                //WindowManager.getDefault().setStatusText(contract.getSymbol() + ": update event:");
      //            }

      storage synchronized {storage.clear}
    }
  }

  /**
   * Override to provide your options
   * @return supported frequency array.
   */
  def supportedFreqs: Array[TFreq] = Array()

  def isFreqSupported(freq: TFreq): Boolean = {
    if (supportedFreqs exists (_ == freq)) return true

    /**
     * means supporting customed freqs (such as csv etc.), should ask
     * contract if it has been set, so what ever:
     */
    currentContract match {
      case None => false
      case Some(x) => x.freq == freq
    }
  }

  def classOfTickerServer: Option[Class[_ <: TickerServer]]

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol
}

