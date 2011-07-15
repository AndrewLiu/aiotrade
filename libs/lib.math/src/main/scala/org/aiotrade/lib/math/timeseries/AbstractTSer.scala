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
import scala.collection.mutable

/**
 *
 * @author Caoyuan Deng
 */
abstract class AbstractTSer(var freq: TFreq) extends TSer {
    
  private var _isInLoading: Boolean = false
  private var _isLoaded: Boolean = false

  private val readWriteLock = new ReentrantReadWriteLock
  protected val readLock  = readWriteLock.readLock
  protected val writeLock = readWriteLock.writeLock

  def this() = this(TFreq.DAILY)
   
  def set(freq: TFreq) {
    this.freq = freq
  }
    
  def isLoaded = _isLoaded
  def isLoaded_=(b: Boolean) {
    if (b) _isInLoading = false
    _isLoaded = b
  }

  def isInLoading = _isInLoading
  def isInLoading_=(b: Boolean) {
    _isInLoading = b
  }

  def exportableVars: Seq[TVar[_]] = vars
  /**
   * Export times and vars to map. Only Var with no-empty name can be exported.
   * The key of times is always "."
   * 
   * @Note use collection.Map[String, Array[_]] here will cause some caller of
   * this method to be comipled with lots of stack space and time. 
   * and use collection.Map[String, Array[Any]] wil cause runtime exception of
   * cast Array[T] (where T is primary type) to Array[Object]
   * 
   * @Todo a custom vmap
   */
  def export(fromTime: Long, toTime: Long): collection.Map[String, Any] = {
    try {
      readLock.lock
      timestamps.readLock.lock

      if (size > 0) {
        val frIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
        if (frIdx >= 0) {
          var toIdx = timestamps.indexOfNearestOccurredTimeBefore(toTime)
          if (toIdx >= 0) {
            // best effort to avoid index out of bounds
            val vs = exportableVars filter (v => v.name != null && v.name != "")
            toIdx = vs.foldLeft(toIdx){(acc, v) => math.min(acc, v.values.length)}
            if (toIdx >= frIdx) {
              val len = toIdx - frIdx + 1
              val vmap = new mutable.HashMap[String, Array[_]]()

              val times = timestamps.sliceToArray(frIdx, len)
              vmap.put(".", times)

              for (v <- vs) {
                val values = v.values.sliceToArray(frIdx, len)
                vmap.put(v.name, values)
              }

              vmap
            } else Map()
          } else Map()
        } else Map()
      } else Map()
      
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

  def nonExists(time: Long) = !exists(time)

  var grids: Array[Double] = Array()
  var isOverlapping = false

  override def toString: String = {
    this.getClass.getSimpleName + "(" + freq + ")"
  }
}

