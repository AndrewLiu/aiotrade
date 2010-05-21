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

import java.util.{Calendar}
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import ru.circumflex.orm._

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
abstract class QuoteServer extends DataServer[Quote] {
  type C = QuoteContract
  type T = QuoteSer

  reactions += {
    case Exchange.Opened(exchange: Exchange) =>
    case Exchange.Closed(exchange: Exchange) =>
  }

  listenTo(Exchange)

  protected def loadFromPersistence: Long = {
    var loadedTime1 = loadedTime
    for (contract <- subscribedContracts) {
      loadedTime1 = loadFromPersistence(contract)
    }
    loadedTime1
  }

  private def loadFromPersistence(contract: QuoteContract): Long = {
    val serToBeFilled = serOf(contract).get

    /**
     * 1. restore data from database
     */
    val freq = serToBeFilled.freq
    val sec = Exchange.secOf(contract.symbol).get
    val quotes = if (freq == TFreq.DAILY) {
      Quotes1d.closedQuotesOf(sec).toArray
    } else if (freq == TFreq.ONE_MIN) {
      Quotes1m.closedQuotesOf(sec).toArray
    } else Array[Quote]()
    //val quotes = PersistenceManager().restoreQuotes(contract.symbol, freq)
    composeSer(contract.symbol, serToBeFilled, quotes)

    /**
     * 2. get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    val size = quotes.length
    val loadedTime1 = if (size > 0) quotes(size - 1).time else 0L
    serToBeFilled.publish(TSerEvent.RefreshInLoading(serToBeFilled,
                                                     contract.symbol,
                                                     0,
                                                     loadedTime1))

    /**
     * 3. clear quotes for following loading usage, as these quotes is borrowed
     * from pool, return them
     */
    quotes synchronized {
      //storage.clear
    }
        
    loadedTime1
  }

  override protected def postLoadHistory {
    for (contract <- subscribedContracts) {
      val serToBeFilled = serOf(contract).get

      val freq = serToBeFilled.freq
      val storage = storageOf(contract).toArray
      val sec = Exchange.secOf(contract.symbol).get
      storage foreach {_.sec = sec}
      if (freq == TFreq.DAILY) {
        Quotes1d.insertBatch(storage)
      } else if (freq == TFreq.ONE_MIN) {
        Quotes1m.insertBatch(storage)
      }

      var evt = composeSer(contract.symbol, serToBeFilled, storage)
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

//      if (freq == TFreq.DAILY) {
//        Quotes1d.evictCacheOfClosedQuotes(storage)
//      } else if (freq == TFreq.ONE_MIN) {
//        Quotes1m.evictCacheOfClosedQuotes(storage)
//      }
      
      storageOf(contract) synchronized {storageOf(contract).clear}
    }
  }

  override protected def postRefresh {
    for (contract <- subscribedContracts) {
      val storage = storageOf(contract).toArray

      val evt = composeSer(contract.symbol, serOf(contract).get, storage)
      //            if (evt != null) {
      //                evt.tpe = TSerEvent.Type.Updated
      //                evt.getSource.fireTSerEvent(evt)
      //                //WindowManager.getDefault().setStatusText(contract.getSymbol() + ": update event:");
      //            }

      storageOf(contract) synchronized {storageOf(contract).clear}
    }
  }

  /**
   * compose ser using data from TVal(s)
   * @param symbol
   * @param serToBeFilled Ser
   * @param TVal(s)
   */
  protected def composeSer(symbol: String, quoteSer: QuoteSer, quotes: Array[Quote]): TSerEvent = {
    var evt: TSerEvent = null

    val size = quotes.length
    if (size > 0) {
      val cal = Calendar.getInstance(Exchange.exchangeOf(toUniSymbol(symbol)).timeZone)
      val freq = quoteSer.freq

      //println("==== " + symbol + " ====")
      quotes foreach {x => x.time = freq.round(x.time, cal)}
      //println("==== after rounded ====")

      // * copy to a new array and don't change it anymore, so we can ! it as message
      val values = new Array[Quote](size)
      quotes.copyToArray(values, 0)

      quoteSer ++= values
    }

    evt
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

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol
}

