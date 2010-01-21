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
import org.aiotrade.lib.math.timeseries.SerChangeEvent
import org.aiotrade.lib.math.timeseries.datasource.AbstractDataServer
import org.aiotrade.lib.math.timeseries.{TSer}
import org.aiotrade.lib.securities.{Market, Quote, QuotePool, PersistenceManager}

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
object QuoteServer {
  protected val quotePool = new QuotePool
}

abstract class QuoteServer extends AbstractDataServer[QuoteContract, Quote] {
  import QuoteServer._

  protected def borrowQuote: Quote = {
    quotePool.borrowObject
  }

  protected def returnQuote(quote: Quote) {
    quotePool.returnObject(quote)
  }

  protected def returnBorrowedTimeValues(quotes: Array[Quote]) {
    quotes foreach {quotePool.returnObject(_)}
  }

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
    val storage = PersistenceManager().restoreQuotes(contract.symbol, freq)
    composeSer(contract.symbol, serToBeFilled, storage)

    /**
     * 2. get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    val size = storage.length
    val loadedTime1 = if (size > 0) storage(size - 1).time else 0L
    serToBeFilled.fireSerChangeEvent(new SerChangeEvent(serToBeFilled,
                                                        SerChangeEvent.Type.RefreshInLoading,
                                                        contract.symbol,
                                                        0, loadedTime1))

    /**
     * 3. clear quotes for following loading usage, as these quotes is borrowed
     * from pool, return them
     */
    storage.synchronized {
      returnBorrowedTimeValues(storage)
      //storage.clear
    }
        
    loadedTime1
  }

  override protected def postLoad: Unit = {
    for (contract <- subscribedContracts) {
      val serToBeFilled = serOf(contract).get

      val freq = serToBeFilled.freq
      val storage = storageOf(contract).toArray
      PersistenceManager().saveQuotes(contract.symbol, freq, storage, sourceId)

      var evt = composeSer(contract.symbol, serToBeFilled, storage)
      //            if (evt != null) {
      //                evt.tpe = SerChangeEvent.Type.FinishedLoading
      //                //WindowManager.getDefault().setStatusText(contract.getSymbol() + ": " + getCount() + " quote data loaded, load server finished");
      //            } else {
      //                /** even though, we may have loaded data in preLoad(), so, still need fire a FinishedLoading event */
      //                val loadedTime1 = serToBeFilled.lastOccurredTime
      //                evt = new SerChangeEvent(serToBeFilled, SerChangeEvent.Type.FinishedLoading, contract.symbol, loadedTime1, loadedTime1)
      //            }
      //
      //            serToBeFilled.fireSerChangeEvent(evt)

      storage.synchronized {
        returnBorrowedTimeValues(storage)
        storageOf(contract).clear
      }
    }
  }

  override protected def postUpdate: Unit =  {
    for (contract <- subscribedContracts) {
      val storage = storageOf(contract).toArray

      val evt = composeSer(contract.symbol, serOf(contract).get, storage)
      //            if (evt != null) {
      //                evt.tpe = SerChangeEvent.Type.Updated
      //                evt.getSource.fireSerChangeEvent(evt)
      //                //WindowManager.getDefault().setStatusText(contract.getSymbol() + ": update event:");
      //            }

      storage.synchronized {
        returnBorrowedTimeValues(storage)
        storageOf(contract).clear
      }
    }
  }

  protected def composeSer(symbol: String, quoteSer: TSer, storage: Array[Quote]): SerChangeEvent =  {
    var evt: SerChangeEvent = null

    val size = storage.length
    if (size > 0) {
      val cal = Calendar.getInstance(marketOf(symbol).timeZone)
      val freq = quoteSer.freq

      //println("==== " + symbol + " ====")
      storage foreach {x => x.time = freq.round(x.time, cal)}
      //println("==== after rounded ====")

      // * copy to a new array and don't change it anymore, so we can ! it as message
      val values = new Array[Quote](size)
      storage.copyToArray(values, 0)

      quoteSer ++= values
      //            var begTime = +Long.MaxValue
      //            var endTime = -Long.MaxValue
      //
      //            val shouldReverse = !isAscending(values)
      //
      //            var i = if (shouldReverse) size - 1 else 0
      //            while (i >= 0 && i <= size - 1) {
      //                val quote = values(i)
      //                val item =  quoteSer.createItemOrClearIt(quote.time).asInstanceOf[QuoteItem]
      //
      //                item.open   = quote.open
      //                item.high   = quote.high
      //                item.low    = quote.low
      //                item.close  = quote.close
      //                item.volume = quote.volume
      //
      //                item.close_ori = quote.close
      //
      //                val adjuestedClose = if (quote.close_adj != 0 ) quote.close_adj else quote.close
      //                item.close_adj = adjuestedClose
      //
      //                if (shouldReverse) {
      //                    /** the recent quote's index is more in quotes, thus the order in timePositions[] is opposed to quotes */
      //                    i -= 1
      //                } else {
      //                    /** the recent quote's index is less in quotes, thus the order in timePositions[] is same as quotes */
      //                    i += 1
      //                }
      //
      //                val itemTime = item.time
      //                begTime = math.min(begTime, itemTime)
      //                endTime = math.max(endTime, itemTime)
      //            }
      //
      //            evt = new SerChangeEvent(quoteSer, SerChangeEvent.Type.None, symbol, begTime, endTime)
    }

    evt
  }

  /**
   * Override to provide your options
   * @return supported frequency array.
   */
  def supportedFreqs: Array[TFreq] = {
    Array()
  }

  def isFreqSupported(freq: TFreq): Boolean = {
    for (afreq <- supportedFreqs) {
      if (afreq.equals(freq)) {
        return true
      }
    }

    /**
     * means supporting customed freqs (such as csv etc.), should ask
     * contract if it has been set, so what ever:
     */
    currentContract match {
      case None => false
      case Some(x) => x.freq.equals(freq)
    }
  }

  def marketOf(symbol: String): Market

  def toSourceSymbol(market: Market, uniSymbol: String): String
}

