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
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import ru.circumflex.orm._

object QuoteServer {

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
  override protected def postLoadHistory {
    for (contract <- subscribedContracts;
         ser <- serOf(contract)
    ) {
      val freq = ser.freq
      val sec = Exchange.secOf(contract.srcSymbol).get
      val quotes = storageOf(contract)
      quotes synchronized {
        quotes foreach {_.sec = sec}
        freq match {
          case TFreq.DAILY =>
            Quotes1d.saveBatch(sec, quotes)
            commit
          case TFreq.ONE_MIN =>
            Quotes1m.saveBatch(sec, quotes)
            commit
          case _ =>
        }

        ser ++= quotes.toArray
        quotes.clear
      }
    }
  }

  override protected def postRefresh {
    for (contract <- subscribedContracts;
         ser <- serOf(contract)
    ) {
      val quotes = storageOf(contract)
      quotes synchronized {
        ser ++= quotes.toArray
        quotes.clear
      }
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

