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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.util.actors.Event
import org.aiotrade.lib.util.actors.Publisher
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.{HashMap}

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 *
 * @author Caoyuan Deng
 */
object DataServer extends Publisher {
  lazy val DEFAULT_ICON: Option[Image] = {
    val url = classOf[DataServer[_]].getResource("defaultIcon.gif")
    if (url != null) Some(Toolkit.getDefaultToolkit.createImage(url)) else None
  }

  private var _executorService: ExecutorService = _
  protected def executorService : ExecutorService = {
    if (_executorService == null) {
      _executorService = Executors.newFixedThreadPool(5)
    }

    _executorService
  }

  case class HeartBeat(interval: Long) extends Event
  val heartBeatInterval = 3000
  actor {
    // in context of applet, a page refresh may cause timer into a unpredict status,
    // so it's always better to restart this timer? , if so, cancel it first.
    //    if (timer != null) {
    //      timer.cancel
    //    }
    //    timer = new Timer("DataServer Heart Beat Timer")
    val timer = new Timer("DataServer Heart Beat Timer")
    timer.schedule(new TimerTask {
        def run = publish(HeartBeat(heartBeatInterval))
      }, 1000, heartBeatInterval)
  }
}

/**
 * V data storege type
 */
import DataServer._
abstract class DataServer[V <: TVal: Manifest] extends Ordered[DataServer[V]] with Publisher {

  type C <: DataContract[_]
  type T <: TSer

  val ANCIENT_TIME: Long = Long.MinValue

  protected val subscribingMutex = new Object
  // --- Following maps should be created once here, since server may be singleton:
  private val contractToStorage = new HashMap[C, ArrayList[V]] // use ArrayList instead of ArrayBuffer here, for toArray performance
  private val subscribedContractToSer = new HashMap[C, T]
  /** a quick seaching map */
  private val subscribedSymbolToContract = new HashMap[String, C]
  // --- Above maps should be created once here, since server may be singleton

  /**
   * key ser is the base one,
   * values (if available) are that who concern key ser.
   * Example: ticker ser also will compose today's quoteSer
   */
  private val serToChainSers = new HashMap[T, List[T]]
  protected var count: Int = 0
  protected var loadedTime: Long = _
  protected var fromTime: Long = _
  var inUpdating: Boolean = _

  var refreshable = false

  private case class LoadHistory(afterTime: Long) extends Event

  /**
   * asynced loadHistory and refresh requests via actor modeled reactions
   */
  reactions += {
    case HeartBeat(interval) if refreshable =>
      loadedTime = loadFromSource(loadedTime)
      postRefresh
    case LoadHistory(afterTime) =>
      loadedTime = loadFromSource(afterTime)
      postLoadHistory
  }

  listenTo(DataServer)

  // -- public interfaces

  def loadHistory(afterTime: Long) {
    if (currentContract == None) {
      assert(false, "dataContract not set!")
    }

    if (subscribedContractToSer.size == 0) {
      assert(false, "none ser subscribed!")
    }

    /**
     * Transit to async load reaction to avoid shared variable lock (loadedTime etc)
     */
    publish(LoadHistory(afterTime))
  }

  protected def postLoadHistory {}
  protected def postRefresh {}

  def startRefresh(refreshInterval: Int) {
    refreshable = true
  }

  def stopRefresh {
    refreshable = false
    postStopRefresh
  }

  protected def postStopRefresh {}

  /**
   * @param contract DataContract which contains all the type, market info for this source
   * @param ser the Ser that will be filled by this server
   */
  def subscribe(contract: C, ser: T): Unit =
    subscribe(contract, ser, Nil)

  /**
   * first ser is the base one,
   * second one (if available) is that who concerns first one, etc.
   * Example: tickering ser also will compose today's quoteSer
   *
   * @param contract DataContract which contains all the type, market info for this source
   * @param ser the Ser that will be filled by this server
   * @param chairSers
   */
  def subscribe(contract: C, ser: T, chainSers: List[T]): Unit = subscribingMutex synchronized {
    subscribedContractToSer += (contract -> ser)
    subscribedSymbolToContract += (contract.symbol -> contract)
    val chainSersX = chainSers ::: (serToChainSers.get(ser) getOrElse Nil)
    serToChainSers += (ser -> chainSersX)
  }

  def unSubscribe(contract: C) = subscribingMutex synchronized {
    cancelRequest(contract)
    serToChainSers -= subscribedContractToSer.get(contract).get
    subscribedContractToSer -= contract
    subscribedSymbolToContract -= contract.symbol
    releaseStorage(contract)
  }

  def isContractSubsrcribed(contract: C): Boolean =
    subscribedSymbolToContract contains contract.symbol

  def subscribedContracts: Iterator[C] =
    subscribedContractToSer.keysIterator

  def createNewInstance: Option[DataServer[V]] = {
    try {
      val instance = getClass.newInstance.asInstanceOf[DataServer[V]]
      instance.init

      Option(instance)
    } catch {
      case ex: InstantiationException => ex.printStackTrace; None
      case ex: IllegalAccessException => ex.printStackTrace; None
    }
  }

  def displayName: String
  def defaultDateFormatPattern: String
  def sourceTimeZone: TimeZone
  /**
   * @return serial number, valid only when >= 0
   */
  def sourceSerialNumber: Int

  /**
   * Override it to return your icon
   * @return a predifined image as the default icon
   */
  def icon: Option[Image] = DEFAULT_ICON

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

  // -- end of public interfaces

  protected def init {}

  /** @Note DateFormat is not thread safe, so we always return a new instance */
  protected def dateFormatOf(timeZone: TimeZone): DateFormat = {
    val pattern = currentContract.get.dateFormatPattern getOrElse defaultDateFormatPattern
    val dateFormat = new SimpleDateFormat(pattern)
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

  protected def storageOf(contract: C): ArrayList[V] = {
    contractToStorage.get(contract) match {
      case None =>
        val x = new ArrayList[V]
        contractToStorage synchronized {contractToStorage(contract) = x}
        x
      case Some(x) => x
    }
  }

  /**
   * @TODO
   * temporary method? As in some data feed, the symbol is not unique,
   * it may be same in different exchanges with different secType.
   */
  protected def contractOf(symbol: String): Option[C] = {
    subscribedSymbolToContract.get(symbol)
  }

  private def releaseStorage(contract: C) {
    /** don't get storage via getStorage(contract), which will create a new one if none */
    for (storage <- contractToStorage.get(contract)) {
      storage synchronized {storage.clear}
    }
    contractToStorage synchronized {contractToStorage.remove(contract)}
  }

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
    if (subscribedContracts.isEmpty) None else Some(subscribedContracts.next)
  }

  protected def serOf(contract: C): Option[T] = 
    subscribedContractToSer.get(contract)

  protected def chainSersOf(ser: T): List[T] = 
    serToChainSers.get(ser) getOrElse Nil

  protected def cancelRequest(contract: C) {}

  /**
   * @param afterThisTime. when afterThisTime equals ANCIENT_TIME, you should
   *        process this condition.
   * @return loadedTime
   */
  protected def loadFromSource(afterThisTime: Long): Long

  override def compare(another: DataServer[V]): Int = {
    if (this.displayName.equalsIgnoreCase(another.displayName)) {
      if (this.hashCode < another.hashCode) -1
      else if (this.hashCode == another.hashCode) 0
      else 1
    } else {
      this.displayName.compareTo(another.displayName)
    }
  }

  override def toString: String = displayName
}



