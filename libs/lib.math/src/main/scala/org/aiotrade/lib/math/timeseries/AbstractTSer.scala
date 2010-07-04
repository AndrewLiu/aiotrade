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

import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.collection.mutable.HashMap

/**
 *
 * @author Caoyuan Deng
 */
abstract class AbstractTSer(var freq: TFreq) extends TSer {
    
  var inLoading: Boolean = false
  private var _loaded: Boolean = false

  private val readWriteLock = new ReentrantReadWriteLock
  protected val readLock  = readWriteLock.readLock
  protected val writeLock = readWriteLock.writeLock

  def this() = this(TFreq.DAILY)
    
  def set(freq: TFreq) {
    this.freq = freq.clone
    start
  }
    
  def loaded = _loaded
  def loaded_=(b: Boolean) {
    inLoading = if (b) false else inLoading
    _loaded = b
  }

  /**
   * Export times and vars to map. Only Var with no-empty name can be exported.
   * The key of times is always "."
   */
  def export(fromTime: Long, toTime: Long): Map[String, Array[_]] = {
    try {
      readLock.lock
      timestamps.readLock.lock

      val vs = vars filter (v => v.name != "" && v.name != null)
      val frIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
      var toIdx = timestamps.indexOfNearestOccurredTimeBefore(toTime)
      toIdx = vs.foldLeft(toIdx){(acc, v) => math.min(acc, v.values.length)}
      val len = toIdx - frIdx + 1
      
      if (frIdx >= 0 && toIdx >= 0 && toIdx >= frIdx) {
        var vmap: HashMap[String, Array[_]] = new HashMap

        val timesx = new Array[Long](len)
        timestamps.copyToArray(timesx, frIdx, len)
        vmap.put(".", timesx)

        for (v <- vs) {
          val valuesx = v.values.sliceToArray(frIdx, len)
          vmap.put(v.name, valuesx)
        }

        vmap.toMap
      } else {
        Map()
      }

    } finally {
      timestamps.readLock.unlock
      readLock.unlock
    }
  }

  protected def isAscending[V <: TVal](values: Array[V]): Boolean = {
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

  var grids: Array[Float] = Array()
  var isOverlapping = false

  override def toString: String = {
    this.getClass.getSimpleName + "(" + freq + ")"
  }
}

