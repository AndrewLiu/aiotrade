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
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.util.actors.Publisher
import scala.collection.mutable

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 *
 * @author Caoyuan Deng
 */
object DataServer extends Publisher {
  private lazy val DEFAULT_ICON: Option[Image] = {
    Option(classOf[DataServer[_]].getResource("defaultIcon.gif")) map {url => Toolkit.getDefaultToolkit.createImage(url)}
  }

  private val config = org.aiotrade.lib.util.config.Config()
  private val heartBeatInterval = config.getInt("dataserver.heartbeat", 318)
  case class HeartBeat(interval: Long) 
  
  // in context of applet, a page refresh may cause timer into a unpredict status,
  // so it's always better to restart this timer? , if so, cancel it first.
  //    if (timer != null) {
  //      timer.cancel
  //    }
  private val timer = new Timer("DataServer Heart Beat Timer")
  timer.schedule(new TimerTask {
      def run = publish(HeartBeat(heartBeatInterval))
    }, 1000, heartBeatInterval)
}

/**
 * V data storege type
 */
import DataServer._
abstract class DataServer[V: Manifest] extends Ordered[DataServer[V]] with Publisher {
  type C <: DataContract[_]

  case object DataProcessed
  case class DataLoaded(values: Array[V], contract: C)

  protected val EmptyValues = Array[V]()

  private val log = Logger.getLogger(this.getClass.getName)

  protected val ANCIENT_TIME: Long = Long.MinValue

  protected val subscribingMutex = new Object
  // --- Following maps should be created once here, since server may be singleton:
  //private val contractToStorage = new HashMap[C, ArrayList[V]] // use ArrayList instead of ArrayBuffer here, for toArray performance
  private val _refreshContracts = mutable.Set[C]()
  /** a quick seaching map */
  private val _refreshSymbolToContract = mutable.Map[String, C]()
  // --- Above maps should be created once here, since server may be singleton

  protected var loadedTime: Long = _

  private var isRefreshable = false
  private var inLoading = false

  private case class AskLoadData(afterTime: Long, contract: Iterable[C])

  reactions += {
    // --- a proxy actor for HeartBeat event etc, which will detect the speed of
    // refreshing requests, if consumer can not catch up the producer, will drop
    // some requests.
    case HeartBeat(interval) =>
      if (isRefreshable && !inLoading) {
        // refresh from loadedTime for subscribedContracts
        publish(AskLoadData(loadedTime, subscribedContracts))
      }
      
    case AskLoadData(afterTime, contracts) =>
      inLoading = true
      try {
        requestData(afterTime, contracts)
      } catch {
        case ex => log.log(Level.WARNING, ex.getMessage, ex)
      }
      inLoading = false

    case DataLoaded(values: Array[V], contract) => // don't specify contract type as C, which won't match 'null'
      log.info("Received DataLoaded event")
      inLoading = true
      try {
        loadedTime = processData(values, contract.asInstanceOf[C])
      } catch {
        case ex => log.log(Level.WARNING, ex.getMessage, ex)
      }
      inLoading = false
      
      publish(DataProcessed)
  }
  listenTo(DataServer)

  // --- public interfaces

  def loadData(afterTime: Long, contracts: Iterable[C]) {
    log.info("Fired LoadData message")
    /**
     * Transit to async load reactor to avoid shared variables lock (loadedTime etc)
     */
    publish(AskLoadData(afterTime, contracts))
  }

  /**
   * Implement this method to request data from data source.
   * It should publish DataLoaded event to enable processData
   *
   * @param afterThisTime When afterThisTime equals ANCIENT_TIME, you should process this condition.
   *        contracts
   * @publish DataLoaded
   */
  protected def requestData(afterThisTime: Long, contracts: Iterable[C])

  /**
   * Publish loaded data to local reactor (including this DataServer instance), 
   * or to remote message system (by overridding it).
   * @Note this DataServer will react to DataLoaded with processData automatically if it
   * received this event
   * @See reactions += {...}
   * 
   */
  protected def publishData(msg: Any) {
    publish(msg)
  }
    
  /**
   * @param values the TVal values
   * @param contract could be null
   * @return loadedTime
   */
  protected def processData(values: Array[V], contract: C): Long

  def startRefresh {isRefreshable = true}
  def stopRefresh {isRefreshable = false}

  // ----- subscribe/unsubscribe is used for refresh only

  def subscribe(contract: C): Unit = subscribingMutex synchronized {
    _refreshContracts += contract
    _refreshSymbolToContract += contract.srcSymbol -> contract
  }

  def unsubscribe(contract: C): Unit = subscribingMutex synchronized {
    cancelRequest(contract)
    _refreshContracts -= contract
    _refreshSymbolToContract -= contract.srcSymbol
  }

  def subscribedContracts  = _refreshContracts
  def subscribedSrcSymbols = _refreshSymbolToContract

  def isContractSubsrcribed(contract: C): Boolean = {
    _refreshContracts contains contract
  }

  def isSymbolSubscribed(srcSymbol: String): Boolean = {
    _refreshSymbolToContract contains srcSymbol
  }

  /**
   * @TODO
   * temporary method? As in some data feed, the symbol is not unique,
   * it may be same in different exchanges with different secType.
   */
  def contractOf(srcSymbol: String): Option[C] = {
    _refreshSymbolToContract.get(srcSymbol)
  }

  def displayName: String
  def defaultDatePattern: String
  def sourceTimeZone: TimeZone
  /**
   * @return serial number, valid only when >= 0
   */
  def serialNumber: Int

  /**
   * Override it to return your icon
   * @return an image as the data server icon
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
   def id: Long = {
      val sn = serialNumber
      assert(sn >= 0 && sn < 63, "source serial number should be between 0 to 63!")

      if (sn == 0) 0 else 1 << (sn - 1)
    }

   // -- end of public interfaces

   /** @Note DateFormat is not thread safe, so we always return a new instance */
   protected def dateFormatOf(timeZone: TimeZone): DateFormat = {
      val pattern = defaultDatePattern
      val dateFormat = new SimpleDateFormat(pattern)
      dateFormat.setTimeZone(timeZone)
      dateFormat
    }

   protected def isAscending(values: Array[_ <: TVal]): Boolean = {
      val size = values.length
      if (size <= 1) {
        true
      } else {
        var i = -1
        while ({i += 1; i < size - 1}) {
          if (values(i).time < values(i + 1).time) {
            return true
          } else if (values(i).time > values(i + 1).time) {
            return false
          }
        }
        false
      }
    }

   protected def cancelRequest(contract: C) {}

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



