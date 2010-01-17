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
package org.aiotrade.lib.math.timeseries
package datasource

import java.awt.Image
import java.awt.Toolkit
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.aiotrade.lib.math.timeseries.{TSer, SerChangeEvent}
import scala.collection.mutable.{HashMap, ArrayBuffer}

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * <K, V> data contract type, data pool type
 *
 * @author Caoyuan Deng
 */
object AbstractDataServer {
  var DEFAULT_ICON: Option[Image] = None

  private var _executorService: ExecutorService = _
  protected def executorService : ExecutorService = {
    if (_executorService == null) {
      _executorService = Executors.newFixedThreadPool(5)
    }

    _executorService
  }
}

abstract class AbstractDataServer[C <: DataContract[_], V <: TVal: Manifest] extends DataServer[C] {
  import AbstractDataServer._

  val ANCIENT_TIME: Long = -1

  // --- Following maps should be created once here, since server may be singleton:
  private val contractToStorage = new HashMap[C, ArrayBuffer[V]]
  private val subscribedContractToSer = new HashMap[C, TSer]
  /** a quick seaching map */
  private val subscribedSymbolToContract = new HashMap[String, C]
  // --- Above maps should be created once here, since server may be singleton
    
  /**
   * first ser is the master one,
   * second one (if available) is that who concerns first one.
   * Example: ticker ser also will compose today's quoteSer
   */
  private val serToChainSers = new HashMap[TSer, ArrayBuffer[TSer]]
  private var loadServer: LoadServer = _
  private var updateServer: UpdateServer = _
  private var updateTimer: Timer = _
  protected var count: Int = 0
  protected var loadedTime: Long = _
  protected var fromTime: Long = _
  protected var inputStream: Option[InputStream] = None

  var inLoading: Boolean = _
  var inUpdating: Boolean = _

  protected def init {
  }

  /** @Note DateFormat is not thread safe, so we always return a new instance */
  protected def dateFormatOf(timeZone: TimeZone): DateFormat = {
    val dateFormatStr = currentContract.get.dateFormatPattern match {
      case null => defaultDateFormatPattern
      case x => x
    }
    val dateFormat = new SimpleDateFormat(dateFormatStr)
    dateFormat.setTimeZone(timeZone)
    dateFormat
  }

  protected def resetCount {
    this.count = 0
  }

  protected def countOne {
    this.count += 1

    /*- @Reserve
     * Don't do refresh in loading any more, it may cause potential conflict
     * between connected refresh events (i.e. when processing one refresh event,
     * another event occured concurrent.)
     * if (count % 500 == 0 && System.currentTimeMillis() - startTime > 2000) {
     *     startTime = System.currentTimeMillis();
     *     preRefresh();
     *     fireDataUpdateEvent(new DataUpdatedEvent(this, DataUpdatedEvent.Type.RefreshInLoading, newestTime));
     *     System.out.println("refreshed: count " + count);
     * }
     */
  }

  protected def storageOf(contract: C): ArrayBuffer[V] = {
    contractToStorage.get(contract) getOrElse {
      val storage1 = new ArrayBuffer[V]
      contractToStorage.synchronized {
        contractToStorage.put(contract, storage1)
      }
      storage1
    }
  }

  /**
   * @TODO
   * temporary method? As in some data feed, the symbol is not unique,
   * it may be same in different markets with different secType.
   */
  protected def lookupContract(symbol: String): Option[C] = {
    subscribedSymbolToContract.get(symbol)
  }

  private def releaseStorage(contract: C): Unit = {
    /** don't get storage via getStorage(contract), which will create a new one if none */
    for (storage <- contractToStorage.get(contract)) {
      returnBorrowedTimeValues(storage.toArray)
      storage.synchronized {
        storage.clear
      }
    }
    contractToStorage.synchronized {
      contractToStorage.remove(contract)
    }
  }

  protected def returnBorrowedTimeValues(datas: Array[V])

  protected def isAscending(values: Array[V]): Boolean = {
    val size = values.length
    if (size <= 1) {
      true
    } else {
      var i = 0
      while (i < size - 1) {
        if (values(i).time < values(i + 1).time) {
          return true
        } else if (values(i).time > values(i + 1).time) {
          return false
        }
        i += 1
      }
      false
    }
  }


  protected def currentContract: Option[C] = {
    /**
     * simplely return the contract currently in the front
     * @Todo, do we need to implement a scheduler in case of multiple contract?
     * Till now, only QuoteDataServer call this method, and they all use the
     * per server per contract approach.
     */
    for (contract <- subscribedContracts) {
      return Some(contract)
    }

    None
  }

  def subscribedContracts: Iterator[C] = subscribedContractToSer.keysIterator

  protected def serOf(contract: C): Option[TSer] = {
    subscribedContractToSer.get(contract)
  }

  protected def chainSersOf(ser: TSer): Seq[TSer] = {
    serToChainSers.get(ser) match {
      case None => Nil
      case Some(x) => x
    }
  }

  /**
   * @param symbol symbol in source
   * @param set the Ser that will be filled by this server
   */
  def subscribe(contract: C, ser: TSer): Unit = {
    subscribe(contract, ser, Nil)
  }

  def subscribe(contract: C, ser: TSer, chainSers: Seq[TSer]): Unit = {
    subscribedContractToSer.synchronized {
      subscribedContractToSer.put(contract, ser)
    }
    subscribedSymbolToContract.synchronized {
      subscribedSymbolToContract.put(contract.symbol, contract)
    }
    serToChainSers.synchronized {
      val chainSersX = serToChainSers.get(ser) getOrElse new ArrayBuffer[TSer]
      chainSersX ++= chainSers
      serToChainSers.put(ser, chainSersX)
    }
  }

  def unSubscribe(contract: C): Unit = {
    cancelRequest(contract)
    serToChainSers.synchronized {
      serToChainSers.remove(subscribedContractToSer.get(contract).get)
    }
    subscribedContractToSer.synchronized {
      subscribedContractToSer.remove(contract)
    }
    subscribedSymbolToContract.synchronized {
      subscribedSymbolToContract.remove(contract.symbol)
    }
    releaseStorage(contract)
  }

  protected def cancelRequest(contract: C): Unit = {
  }

  def isContractSubsrcribed(contract: C): Boolean = {
    subscribedContractToSer.keysIterator exists {_.symbol == (contract.symbol)}
  }

  def startLoadServer: Unit = {
    if (currentContract == None) {
      assert(false, "dataContract not set!")
    }

    if (subscribedContractToSer.size == 0) {
      assert(false, "none ser subscribed!")
    }

    if (loadServer == null) {
      loadServer = new LoadServer
    }

    if (!inLoading) {
      inLoading = true
      new Thread(loadServer, "LoadServer").start
      // @Note: ExecutorSrevice will cause access denied of modifyThreadGroup in Applet !!
      //getExecutorService().submit(loadServer);
    }
  }

  def startUpdateServer(updateInterval: Int): Unit = {
    if (inLoading) {
      println("should start update server after loaded")
      inUpdating = false
      return
    }

    inUpdating = true

    // * in context of applet, a page refresh may cause timer into a unpredict status,
    // * so it's always better to restart this timer, so, cancel it first.
    if (updateTimer != null) {
      updateTimer.cancel
    }
    updateTimer = new Timer

    if (updateServer != null) {
      updateServer.cancel
    }
    updateServer = new UpdateServer

    updateTimer.schedule(updateServer, 1000, updateInterval)
  }

  def stopUpdateServer: Unit = {
    inUpdating = false
    updateServer = null
    updateTimer.cancel
    updateTimer = null

    postStopUpdateServer
  }

  protected def postStopUpdateServer: Unit = {
  }

  protected def loadFromPersistence: Long

  /**
   * @param afterThisTime. when afterThisTime equals ANCIENT_TIME, you should
   *        process this condition.
   * @return loadedTime
   */
  protected def loadFromSource(afterThisTime: Long): Long

  /**
   * compose ser using data from timeValues
   * @param symbol
   * @param serToBeFilled Ser
   * @param time values
   */
  protected def composeSer(symbol: String, serToBeFilled: TSer, storage: Array[V]): SerChangeEvent

  protected class LoadServer extends Runnable {
    override def run: Unit = {
      loadFromPersistence

      loadedTime = loadFromSource(loadedTime)

      inLoading = false

      postLoad
    }
  }

  protected def postLoad: Unit = {
  }

  private class UpdateServer extends TimerTask {
    override def run: Unit = {
      loadedTime = loadFromSource(loadedTime)

      postUpdate
    }
  }

  protected def postUpdate: Unit = {
  }

  override def createNewInstance: Option[DataServer[C]] = {
    try {
      val instance = getClass.newInstance.asInstanceOf[AbstractDataServer[C, V]]
      instance.init

      Some(instance)
    } catch {
      case ex: InstantiationException => ex.printStackTrace; None
      case ex: IllegalAccessException => ex.printStackTrace; None
    }
  }

  /**
   * Override it to return your icon
   * @return a predifined image as the default icon
   */
  def icon: Option[Image] = {
    if (DEFAULT_ICON == None) {
      val url = classOf[AbstractDataServer[Any,Any]].getResource("defaultIcon.gif")
      DEFAULT_ICON = if (url != null) Some(Toolkit.getDefaultToolkit().createImage(url)) else None
    }
    DEFAULT_ICON
  }

  /**
   * Convert source sn to source id in format of :
   * sn (0-63)       id (64 bits)
   * 0               ..,0000,0000
   * 1               ..,0000,0001
   * 2               ..,0000,0010
   * 3               ..,0000,0100
   * 4               ..,0000,1000
   * ...
   * @return source id
   */
  def sourceId: Long = {
    val sn = sourceSerialNumber
    assert(sn >= 0 && sn < 63, "source serial number should be between 0 to 63!")

    if (sn == 0) 0 else 1 << (sn - 1)
  }

  override def compare(another: DataServer[C]): Int = {
    if (this.displayName.equalsIgnoreCase(another.displayName)) {
      if (this.hashCode < another.hashCode) -1
      else {
        if (this.hashCode == another.hashCode) 0 else 1
      }
    } else {
      this.displayName.compareTo(another.displayName)
    }
  }

  def sourceTimeZone: TimeZone

  override def toString: String = displayName
}



