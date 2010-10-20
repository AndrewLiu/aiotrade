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

import org.aiotrade.lib.math.indicator.Factor
import org.aiotrade.lib.math.indicator.Id
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.securities.model.Sec
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList

/**
 * @author Caoyuan Deng
 */
object PanelIndicator {
  private val log = Logger.getLogger(this.getClass.getName)

  private val idToIndicator = new ConcurrentHashMap[Id, PanelIndicator[_]]

  def apply[T <: PanelIndicator[_]](klass: Class[T], sectionName: String, freq: TFreq, factors: Factor*): T = {
    val factorArr = factors.toArray
    val factorLen = factorArr.length
    val args = new Array[Any](factorLen + 1)
    args(0) = freq
    System.arraycopy(factorArr, 0, args, 1, factorLen)
    
    val id = Id(klass, sectionName, args: _*)
    
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

class PanelIndicator[T <: Indicator](freq: => TFreq)(
  protected implicit val m: Manifest[T]
) extends FreeFillIndicator(null, freq) {

  private val log = Logger.getLogger(this.getClass.getName)

  val indicators = new ArrayList[T]

  reactions += {
    case TSerEvent.Loaded(_, _, fromTime, toTime, _, callback) =>
      computeFrom(fromTime)
    case TSerEvent.Refresh(_, _, fromTime, toTime, _, callback) =>
      computeFrom(fromTime)
    case TSerEvent.Updated(_, _, fromTime, toTime, _, callback) =>
      computeFrom(fromTime)
    case TSerEvent.Computed(src, _, fromTime, toTime, _, callback) =>
      computeFrom(fromTime)
    case TSerEvent.Cleared(src, _, fromTime, toTime, _, callback) =>
      clear(fromTime)
  }

  def addSec(sec: Sec) {
    sec.serOf(freq) match {
      case Some(baseSer) =>
        m.erasure
        val ind = org.aiotrade.lib.math.indicator.Indicator(m.erasure.asInstanceOf[Class[T]], baseSer, factors: _*)
        listenTo(ind)
        indicators += ind
        ind.computeFrom(0)
      case _ =>
    }
  }

  final protected def firstTimeOf(sers: ArrayList[_ <: TSer]) = {
    var firstTime = Long.MinValue

    val length = sers.length
    var i = 0
    while (i < length) {
      var ser = sers(i)
      if (ser.timestamps.size > 0) {
        val fTime = ser.timestamps(0)
        firstTime = if (firstTime == Long.MinValue) fTime else math.min(firstTime, fTime)
      }
      i += 1
    }

    firstTime
  }
}
