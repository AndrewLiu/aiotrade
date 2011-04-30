/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.indicator

import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.indicator.ComputeFrom
import org.aiotrade.lib.math.indicator.Factor
import org.aiotrade.lib.math.indicator.Id
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.ValidTime
import org.aiotrade.lib.util.actors.Publisher

/**
 * @author Caoyuan Deng
 */
object PanelIndicator extends Publisher {
  private val log = Logger.getLogger(this.getClass.getName)

  private val idToIndicator = new ConcurrentHashMap[Id, PanelIndicator[_]]
  
  private case object PanelHeartBeat
  private val interval = 10000L // 10 seconds
  private val timer = new Timer("PanelIndictorTimer")
  timer.scheduleAtFixedRate(new TimerTask {
      def run {
        publish(PanelHeartBeat)
      }
    }, 1000, interval)

  def apply[T <: PanelIndicator[_]](klass: Class[T], sectorKey: String, freq: TFreq, factors: Factor*): T = {
    val factorArr = factors.toArray
    val factorLen = factorArr.length
    val args = new Array[Any](factorLen + 1)
    args(0) = freq
    System.arraycopy(factorArr, 0, args, 1, factorLen)
    
    val id = Id(klass, sectorKey, args: _*)
    
    idToIndicator.get(id) match {
      case null =>
        /** if got none from idToIndicator, try to create new one */
        try {
          val indicator = klass.getConstructor(classOf[TFreq]).newInstance(freq)
          indicator.factors = factors.toArray
          idToIndicator.putIfAbsent(id, indicator)
          indicator
        } catch {
          case ex => log.log(Level.SEVERE, ex.getMessage, ex); null.asInstanceOf[T]
        }
      case x => x.asInstanceOf[T]
    }
  }
}

abstract class PanelIndicator[T <: Indicator]($freq: TFreq)(implicit m: Manifest[T]) extends FreeIndicator(null, $freq) {
  private val log = Logger.getLogger(this.getClass.getName)

  val indicators = new ArrayList[(T, ValidTime[Sec])]
  
  private var lastFromTime = Long.MaxValue
  reactions += {
    case PanelIndicator.PanelHeartBeat => 
      computeFrom(lastFromTime)
      lastFromTime = computedTime + 1
    case ComputeFrom(time) => 
      lastFromTime = time
    case TSerEvent.Loaded(_, _, fromTime, toTime, _, callback) => 
      lastFromTime = math.min(fromTime, lastFromTime)
      //computeFrom(fromTime)
    case TSerEvent.Refresh(_, _, fromTime, toTime, _, callback) =>
      lastFromTime = math.min(fromTime, lastFromTime)
      //computeFrom(fromTime)
    case TSerEvent.Updated(_, _, fromTime, toTime, _, callback) =>
      lastFromTime = math.min(fromTime, lastFromTime)
      //computeFrom(fromTime)
    case TSerEvent.Computed(src, _, fromTime, toTime, _, callback) =>
      lastFromTime = math.min(fromTime, lastFromTime)
      //computeFrom(fromTime)
    case TSerEvent.Cleared(src, _, fromTime, toTime, _, callback) =>
      clear(fromTime)
  }
  listenTo(PanelIndicator)

  def addSecs(secValidTimes: collection.Seq[ValidTime[Sec]]) {
    secValidTimes foreach addSec
    publish(ComputeFrom(0))
  }

  def addSec(secValidTime: ValidTime[Sec]): Option[T] = {
    secValidTime.ref.serOf(freq) match {
      case Some(baseSer) =>
        val ind = org.aiotrade.lib.math.indicator.Indicator(m.erasure.asInstanceOf[Class[T]], baseSer, factors: _*)
        listenTo(ind)
        indicators += ((ind, secValidTime))
        ind.computeFrom(0)
        Some(ind)
      case _ => None
    }
  }
  
  override def computeFrom(fromTime: Long) {
    var fromTime1 = fromTime
    if (fromTime == 0 | fromTime == 1) { // fromTime maybe 1, when called by computeFrom(afterThisTime)
      val firstTime = firstTimeOf(indicators)
      if (firstTime == Long.MinValue) return else fromTime1 = firstTime
    }
    
    val lastTime = lastTimeOf(indicators)

    log.info("Compute " + fromTime1 + " - " + lastTime)
    val start = System.currentTimeMillis
    compute(fromTime1, lastTime)
    log.info("Computed in " + (System.currentTimeMillis - start) + "ms")
  }
  
  /**
   * Implement this method for actual computing.
   * @param from time, included
   * @param to time, included
   */
  protected def compute(fromTime: Long, toTime: Long)
  
  protected def firstTimeOf(inds: ArrayList[(T, ValidTime[Sec])]) = {
    var firstTime = Long.MinValue

    var i = 0
    while (i < inds.length) {
      val ind = inds(i)._1
      if (ind != null && ind.timestamps.size > 0) {
        val fTime = ind.timestamps(0)
        firstTime = if (firstTime == Long.MinValue) fTime else math.min(firstTime, fTime)
      }
      i += 1
    }

    firstTime
  }

  protected def lastTimeOf(inds: ArrayList[(T, ValidTime[Sec])]) = {
    if (inds.isEmpty) 0 else inds.map(_._1.lastOccurredTime).max
  }
}
