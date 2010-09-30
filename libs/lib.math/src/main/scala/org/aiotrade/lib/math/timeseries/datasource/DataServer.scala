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
import java.util.logging.Logger
import org.aiotrade.lib.util.actors.Event
import org.aiotrade.lib.util.actors.Publisher
import scala.collection.mutable.{HashMap, HashSet}

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 *
 * @author Caoyuan Deng
 */
object DataServer extends Publisher {
  private lazy val DEFAULT_ICON: Option[Image] = {
    val url = classOf[DataServer[_]].getResource("defaultIcon.gif")
    if (url != null) Some(Toolkit.getDefaultToolkit.createImage(url)) else None
  }

  private val config = org.aiotrade.lib.util.config.Config()

  case class HeartBeat(interval: Long) extends Event
  val heartBeatInterval = config.getInt("dataserver.heartbeat", 50)
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
abstract class DataServer[V <: TVal: Manifest] extends Ordered[DataServer[V]] with Publisher {
  type C <: DataContract[_]

  protected val EmptyValues = Array[V]()

  private val log = Logger.getLogger(this.getClass.getName)

  val ANCIENT_TIME: Long = Long.MinValue

  protected val subscribingMutex = new Object
  // --- Following maps should be created once here, since server may be singleton:
  //private val contractToStorage = new HashMap[C, ArrayList[V]] // use ArrayList instead of ArrayBuffer here, for toArray performance
  val _subscribedContracts = new HashSet[C]
  /** a quick seaching map */
  private val _subscribedSymbolToContract = new HashMap[String, C]
  // --- Above maps should be created once here, since server may be singleton

  /**
   * key ser is the base one,
   * values (if available) are that who concern key ser.
   * Example: ticker ser also will compose today's quoteSer
   */
  protected var count: Int = 0
  protected var loadedTime: Long = _
  protected var fromTime: Long = _

  var refreshable = false

  // --- a proxy actor for HeartBeat event etc, which will detect the speed of
  // refreshing requests, if consumer can not catch up the producer, will drop some requests.
  // We here also avoid concurent racing risk of Refresh/LoadHistory requests @see reactions += {...
  private case object Stop extends Event
  private case object Refresh extends Event
  private case class LoadHistory(afterTime: Long) extends Event
  private var inRefreshing: Boolean = _
  private lazy val loadActor = new scala.actors.Reactor[Event] {
    start
    
    def act = loop {
      react {
        case Refresh =>
          //log.info("loadActor Received Refresh message")
          inRefreshing = true
          val values = loadFromSource(loadedTime)
          if (values.length > 0) {
            loadedTime = postRefresh(values)
          }
          //log.info("loadActor Finished Refresh")
          inRefreshing = false

        case LoadHistory(afterTime) =>
          //log.info("loadActor Received LoadHistory message")
          val values = loadFromSource(afterTime)
          loadedTime = postLoadHistory(values)
          
        case Stop => exit
        case _ =>
      }
    }
  }

  reactions += {
    case HeartBeat(interval) if refreshable && !inRefreshing => loadActor ! Refresh
    case HeartBeat(_) => // should match this to avoid MatchError
  }

  listenTo(DataServer)

  // --- public interfaces

  def loadHistory(afterTime: Long) {
    assert(currentContract.isDefined, "dataContract not set!")
    assert(!_subscribedContracts.isEmpty, "none ser subscribed!")

    log.info("Fired LoadHistory message to loadActor")
    /**
     * Transit to async load reaction to avoid shared variable lock (loadedTime etc)
     */
    loadActor ! LoadHistory(afterTime)
  }

  protected def postLoadHistory(values: Array[V]): Long = loadedTime
  protected def postRefresh(values: Array[V]): Long = loadedTime

  def startRefresh(refreshInterval: Int) {
    refreshable = true
  }

  def stopRefresh {
    refreshable = false
    postStopRefresh
  }

  protected def postStopRefresh {}

  /**
   * first ser is the base one,
   * second one (if available) is that who concerns first one, etc.
   * Example: tickering ser also will compose today's quoteSer
   *
   * @param contract DataContract which contains all the type, market info for this source
   * @param ser the Ser that will be filled by this server
   * @param chairSers
   */
  def subscribe(contract: C): Unit = subscribingMutex synchronized {
    _subscribedContracts.add(contract)
    _subscribedSymbolToContract.put(contract.srcSymbol, contract)
  }
  
  def unSubscribe(contract: C): Unit = subscribingMutex synchronized {
    cancelRequest(contract)
    _subscribedContracts -= contract
    _subscribedSymbolToContract -= contract.srcSymbol
  }

  def subscribedContracts  = _subscribedContracts
  def subscribedSrcSymbols = _subscribedSymbolToContract

  def isContractSubsrcribed(contract: C): Boolean = {
    _subscribedContracts contains contract
  }

  def isSymbolSubscribed(srcSymbol: String): Boolean = {
    _subscribedSymbolToContract contains srcSymbol
  }

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
     *     fireDataUpdateEvent(new DataUpdatedEvent(this, DataUpdatedEvent.Type.Refresh, newestTime));
     *     System.out.println("refreshed: count " + count);
     * }
     */
  }

  /**
   * @TODO
   * temporary method? As in some data feed, the symbol is not unique,
   * it may be same in different exchanges with different secType.
   */
  def contractOf(symbol: String): Option[C] = {
    _subscribedSymbolToContract.get(symbol)
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
    _subscribedContracts.headOption
  }


  protected def cancelRequest(contract: C) {}

  /**
   * @param afterThisTime. when afterThisTime equals ANCIENT_TIME, you should
   *        process this condition.
   * @return TVals, if you want to manually call postRefresh during loadFromSource, just return an empty Array
   */
  protected def loadFromSource(afterThisTime: Long): Array[V]

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



