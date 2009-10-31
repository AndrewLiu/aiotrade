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
package org.aiotrade.lib.securities

import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.SerChangeEvent
import org.aiotrade.lib.math.timeseries.SerChangeListener
import org.aiotrade.lib.securities.dataserver.{QuoteContract,QuoteServer,TickerServer,TickerContract}
import scala.collection.mutable.HashMap

/**
 * An implement of Sec.
 * each sofic has a default quoteSer and a tickerSer which will be created in the
 * initialization. The quoteSer will be put in the freq-ser map, the tickerSer
 * won't be.
 * You may put ser from outside, to the freq-ser map, so each sofic may have multiple
 * freq sers, but only per freq pre ser is allowed.
 *
 * @param uniSymbol a globe uniSymbol, may have different source uniSymbol.

 * @author Caoyuan Deng
 */
abstract class AbstractSec(_uniSymbol:String, quoteContracts:Seq[QuoteContract], _tickerContract:TickerContract) extends Sec {

  private val freqToQuoteContract = new HashMap[TFreq, QuoteContract]
  /** each freq may have a standalone quoteDataServer for easy control and thread safe */
  private val freqToQuoteServer = new HashMap[TFreq, QuoteServer]
  private val freqToSer = new HashMap[TFreq, QuoteSer]

  var market = Market.NYSE
  var description: String = ""
  var name: String = _uniSymbol.replace('.', '_')
  var defaultFreq: TFreq = _
  var tickerServer: TickerServer = _

  /** create freq ser */
  for (contract <- quoteContracts) {
    val freq = contract.freq
    if (defaultFreq == null) {
      defaultFreq = freq
    }
    freqToQuoteContract.put(freq, contract)
    freqToSer.put(freq, new QuoteSer(freq))
  }

  /** tickerContract will always be built according to quoteContrat ? */
  val tickerContract = if (_tickerContract == null) {
    val tc = new TickerContract
    tc.symbol = uniSymbol
    tc
  } else _tickerContract
    
  /** create tickerSer. We'll always have a standalone tickerSer, even we have another 1-min quoteSer */
  val tickerSer = new QuoteSer(tickerContract.freq)


  /**
   * As it's a SerProvider, when new create it, we also assign a quoteContract
   * to it, which is a the one with the default freq. And we can use it to
   * subscribe data server for various freqs, since the data server will invoke
   * the proper freq server according to ser's freq (the ser should be given to
   * the server meanwhile when you subscribe.
   *
   * The default contract holds most of the information to be used.
   */
  def this(uniSymbol: String, quoteContract: Seq[QuoteContract]) {
    this(uniSymbol, quoteContract, null)
  }

  def serOf(freq: TFreq): Option[QuoteSer] = {
    freqToSer.get(freq)
  }

  def putSer(ser: QuoteSer): Unit = {
    freqToSer.put(ser.freq, ser)
  }

  /**
   * synchronized this method to avoid conflict on variable: loadBeginning and
   * concurrent accessing to those maps.
   */
  def loadSer(freq: TFreq): Boolean= synchronized {

    /** ask contract instead of server */
    val contract = freqToQuoteContract.get(freq) match {
      case None => return false
      case Some(x) => x
    }

    val quoteServer = freqToQuoteServer.get(freq) match {
      case None => contract.serviceInstance(Nil) match {
          case None => return false
          case Some(x) => freqToQuoteServer.put(freq, x); x
        }
      case Some(x) => x
    }

    val serToBeLoaded = serOf(freq) match {
      case None =>
        val x = new QuoteSer(freq)
        freqToSer.put(freq, x)
        x
      case Some(x) => x
    }

    if (!quoteServer.isContractSubsrcribed(contract)) {
      quoteServer.subscribe(contract, serToBeLoaded);
    }

    var loadBeginning = false
    /** If there is already a dataServer running and not finished, don't load again */
    if (quoteServer.inLoading) {
      System.out.println("A loading procedure is already running and not finished yet!");
      loadBeginning = false
      serToBeLoaded.inLoading = true
    } else {
      quoteServer.startLoadServer
      loadBeginning = true
      serToBeLoaded.inLoading = true
    }

    if (loadBeginning) {
      val listener = new SerChangeListener {

        override def serChanged(evt: SerChangeEvent): Unit = {
          import org.aiotrade.lib.math.timeseries.SerChangeEvent.Type._
          val sourceSer = evt.getSource
          val freq = sourceSer.freq
          val contract = freqToQuoteContract.get(freq).get
          val quoteServer = freqToQuoteServer.get(freq).get
          evt.tpe match {
            case FinishedLoading =>
              sourceSer.loaded = true
              for (quoteServer <- freqToQuoteServer.get(freq)) {
                if (contract.refreshable) {
                  quoteServer.startUpdateServer(contract.refreshInterval * 1000)
                } else {
                  quoteServer.unSubscribe(contract)
                  freqToQuoteServer.removeKey(freq)
                }
              }
              sourceSer.removeSerChangeListener(this)
            case _ =>
          }
        }
      }
      serToBeLoaded.addSerChangeListener(listener)
    }

    loadBeginning
  }

  def isSerLoaded(freq:TFreq): Boolean = {
    freqToSer.get(freq) match {
      case None => false
      case Some(x) => x.loaded
    }
  }

  def isSerInLoading(freq: TFreq): Boolean = {
    freqToSer.get(freq) match {
      case None => false
      case Some(x) => x.inLoading
    }
  }

  def uniSymbol: String = _uniSymbol
  def uniSymbol_=(symbol: String): Unit = {
    this.uniSymbol = symbol
    name = symbol.replace('.', '_')
  }

  def stopAllDataServer: Unit = {
    for (server <- freqToQuoteServer.values) {
      if (server.inUpdating) {
        server.stopUpdateServer
      }
    }
    freqToQuoteServer.clear
  }

  def clearSer(freq: TFreq): Unit = {
    for (ser <- serOf(freq)) {
      ser.clear(0)
      ser.loaded = false
    }
  }

  override def toString: String = {
    uniSymbol
  }

  def setDataContract(quoteContract: DataContract[_]): Unit = {
    val freq = quoteContract.freq
    freqToQuoteContract.put(freq, quoteContract.asInstanceOf[QuoteContract])
    /** may need a new dataServer now: */
    freqToQuoteServer.removeKey(freq)
  }

  def dataContract: DataContract[_] = {
    freqToQuoteContract.get(defaultFreq).get
  }


  def subscribeTickerServer: Unit = {
    assert(tickerContract != null, "ticker contract not set yet !")

    /**
     * @TODO, temporary test code
     */
    if (tickerContract.serviceClassName == null) {
      val defaultContract = freqToQuoteContract.get(defaultFreq).get
      if (defaultContract.serviceClassName.toUpperCase.contains("IB")) {
        tickerContract.serviceClassName = "org.aiotrade.platform.modules.dataserver.ib.IBTickerServer"
      } else {
        tickerContract.serviceClassName = "org.aiotrade.platform.modules.dataserver.basic.YahooTickerServer"
      }
    }

    startTickerServerIfNecessary
  }

  private def startTickerServerIfNecessary: Unit = {
    /**
     * @TODO, if tickerServer switched, should check here.
     */
    if (tickerServer == null) {
      tickerServer = tickerContract.serviceInstance().get
    }

    if (!tickerServer.isContractSubsrcribed(tickerContract)) {
      var chainSers: List[TSer] = Nil
      // Only dailySer and minuteSre needs to chainly follow ticker change.
      serOf(TFreq.DAILY)  .foreach{x => chainSers = x :: chainSers}
      serOf(TFreq.ONE_MIN).foreach{x => chainSers = x :: chainSers}
      tickerServer.subscribe(tickerContract, tickerSer, chainSers)
      //
      //            var break = false
      //            while (!break) {
      //                val allChainSersLoaded = (true /: chainSers) {_ && _.loaded}
      //                if (allChainSersLoaded) {
      //                    tickerServer.subscribe(tickerContract, tickerSer, chainSers)
      //                    break = true
      //                }
      //                Thread.sleep(1000)
      //            }
            
    }

    tickerServer.startUpdateServer(tickerContract.refreshInterval * 1000)
  }

  def unSubscribeTickerServer: Unit = {
    if (tickerServer != null && tickerContract != null) {
      tickerServer.unSubscribe(tickerContract)
    }
  }

  def isTickerServerSubscribed: Boolean = {
    if (tickerServer != null) tickerServer.isContractSubsrcribed(tickerContract) else false
  }
}

