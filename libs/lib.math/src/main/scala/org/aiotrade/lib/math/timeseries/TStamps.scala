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

import org.aiotrade.lib.util.collection.ArrayList

/**
 *
 * The row always begin from 0, and corresponds to first occurred time
 *
 * onOccurred: ...........|xxxxxxxxxxxxxxx|............
 * index                   01234567....
 * row                     01234567....
 *
 * onCalendar: ...........|xxxxxooxxxxoooxxxxxoox|........
 * index                   01234  5678...
 * row                     01234567....
 *
 * @author  Caoyuan Deng
 * @version 1.02, 11/25/2006
 * @since   1.0.4
 */
object TStampsLog {
  private val KIND   = 0xC000 // 1100 0000 0000 0000
  private val SIZE   = 0x3FFF // 0011 1111 1111 1111

  val APPEND = 0x0000 // 0000 0000 0000 0000
  val INSERT = 0x4000 // 0100 0000 0000 0000
  val REMOVE = 0x8000 // 1000 0000 0000 0000
  val NUMBER = 0xC000 // 1100 0000 0000 0000
}

import TStampsLog._
class TStampsLog(initialSize: Int) extends ArrayList[Short](initialSize) {

  private var _logCursor = -1
  private var _logTime = System.currentTimeMillis

  def logCursor = _logCursor
  def logTime = _logTime

  def checkKind(logFlag: Short): Int = {
    logFlag & KIND
  }

  def checkSize(logFlag: Short): Int = {
    logFlag & SIZE
  }

  def logAppend(size: Int): Unit = {
    def addLog(size: Int): Unit = {
      if (size > SIZE) {
        this += (APPEND | SIZE).toShort
        _logCursor = nextCursor(_logCursor)
        addLog(size - SIZE)
      } else {
        this += (APPEND | size).toShort
        _logCursor = nextCursor(_logCursor)
      }
    }
        
    if (_logCursor >= 0) {
      val prev = apply(_logCursor)
      val prevKind = checkKind(prev)
      val prevSize = checkSize(prev)
      //println("Append log: prevKind=" + prevKind + ", prevCursor=" + _logCursor + ", prevSize=" + prevSize)
      if (prevKind == APPEND) {
        val newSize = prevSize + size
        if (newSize <= SIZE) {
          // merge with previous one
          //println("Append log (merged with prev): newSize=" + newSize)
          update(_logCursor, (APPEND | newSize).toShort)
        } else addLog(size)
      } else addLog(size)
    } else addLog(size)

    _logTime = System.currentTimeMillis
  }

  def logInsert(size: Int, idx: Int): Unit = {
    def addLog(size: Int, idx: Int): Unit = {
      if (size > SIZE) {
        this += (INSERT | SIZE).toShort
        this ++= intToShorts(idx)
        _logCursor = nextCursor(_logCursor)
        addLog(size - SIZE, idx + SIZE)
      } else {
        this += (INSERT | size).toShort
        this ++= intToShorts(idx)
        _logCursor = nextCursor(_logCursor)
      }
    }

    if (_logCursor >= 0) {
      val prev = apply(_logCursor)
      val prevKind = checkKind(prev)
      val prevSize = checkSize(prev)
      //println("Insert log: prevKind=" + prevKind + ", prevCursor=" + _logCursor + ", prevSize=" + prevSize + ", idx=" + idx)
      if (prevKind == INSERT) {
        val prevIdx = shortsToInt(apply(_logCursor + 1), apply(_logCursor + 2))
        if (prevIdx + prevSize == idx) {
          val newSize = prevSize + size
          if (newSize <= SIZE) {
            // merge with previous one
            //println("Insert log (merged with prev): idx=" + prevIdx + ", newSize=" + newSize)
            update(_logCursor, (INSERT | newSize).toShort)
          } else addLog(size, idx)
        } else addLog(size, idx)
      } else addLog(size, idx)
    } else addLog(size, idx)

    _logTime = System.currentTimeMillis
  }

  def insertIndexOfLog(cursor: Int): Int = {
    shortsToInt(apply(cursor + 1), apply(cursor + 2))
  }

  /** cursorIncr: if (prev == append) 1 else 3 */
  def nextCursor(cursor: Int): Int = {
    if (cursor == -1) {
      0
    } else {
      checkKind(apply(cursor)) match {
        case APPEND => cursor + 1
        case INSERT => cursor + 3
      }
    }
  }
  /* [0] = lowest order 16 bits; [1] = highest order 16 bits. */
  private def intToShorts(i: Int): Array[Short] = {
    Array((i >> 16).toShort, i.toShort)
  }

  private def shortsToInt(hi: Short, lo: Short) = {
    (hi << 16) + lo
  }

  override def toString: String = {
    val sb = new StringBuilder
    sb.append("TimestampsLog: cursor=").append(_logCursor).append(", size=").append(size).append(", content=")
    var i = 0
    while (i < size) {
      val flag = apply(i)
      checkKind(flag) match {
        case APPEND =>
          sb.append("A").append(checkSize(flag)).append(",")
          i += 1
        case INSERT => sb.append("I").append(checkSize(flag)).append("@").append(shortsToInt(apply(i + 1), apply(i + 2))).append(",")
          i += 3
        case x => sb.append("\nflag").append(x).append("X").append(i).append(",")
      }
    }
    sb.toString
  }
}

import java.util.{Calendar,GregorianCalendar,TimeZone}
import java.util.concurrent.locks.{Lock,ReentrantReadWriteLock}

@cloneable
abstract class TStamps(initialSize: Int) extends ArrayList[Long](initialSize) {
  val LONG_LONG_AGO = new GregorianCalendar(1900, Calendar.JANUARY, 1).getTimeInMillis

  private val readWriteLock = new ReentrantReadWriteLock
  val readLock:  Lock = readWriteLock.readLock
  val writeLock: Lock = readWriteLock.writeLock

  val log = new TStampsLog(initialSize)

  def isOnCalendar: Boolean
    
  def asOnCalendar: TStamps
    
  /**
   * Get nearest row that can also properly extends before firstOccurredTime
   * or after lastOccurredTime
   */
  def rowOfTime(time: Long, freq: TFreq): Int
    
  def timeOfRow(row: Int, freq: TFreq): Long
    
  def lastRow(freq: TFreq): Int
    
  def sizeOf(freq: TFreq): Int
    
  def indexOfOccurredTime(time: Long): Int
    
  /**
   * Search the nearest index between '1' to 'lastIndex - 1'
   * We only need to use this computing in case of onOccurred.
   */
  def nearestIndexOfOccurredTime(time: Long): Int
    
  /** return index of nearest behind or equal(if exist) time */
  def indexOfNearestOccurredTimeBehind(time: Long): Int
    
  /** return index of nearest before or equal(if exist) time */
  def indexOfNearestOccurredTimeBefore(time: Long): Int
    
  def firstOccurredTime: Long
    
  def lastOccurredTime: Long
    
  def iterator(freq: TFreq): TStampsIterator
    
  def iterator(freq: TFreq, fromTime: Long, toTime: Long, timeZone: TimeZone): TStampsIterator

  /**
   *this should not be abstract method to get scalac knowing it's a override of
   * @cloneable instead of java.lang.Object#clone
   */
  override def clone: TStamps = {super.clone; this}
}








