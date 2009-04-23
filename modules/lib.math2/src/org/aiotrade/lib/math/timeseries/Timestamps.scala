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

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.concurrent.locks.{Lock,ReentrantReadWriteLock}
import scala.collection.mutable.ArrayBuffer

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
object TimestampsLog {
    private val FLAG   = 0xC000 // 1100 0000 0000 0000
    private val SIZE   = 0x3FFF // 0011 1111 1111 1111

    private val APPEND = 0x0000 // 0000 0000 0000 0000
    private val INSERT = 0x4000 // 0100 0000 0000 0000
    private val REMOVE = 0x8000 // 1000 0000 0000 0000
    private val NUMBER = 0xC000 // 1100 0000 0000 0000
}
class TimestampsLog extends ArrayBuffer[Short] {
    import TimestampsLog._

    private var _logCursor = -1
    private var _logTime = System.currentTimeMillis

    def logCursor = _logCursor
    def logTime = _logTime

    def checkSize(logFlag:Short) :Int = {
        logFlag & SIZE
    }

    def checkAppend(logFlag:Short) :Int = {
        if ((logFlag & FLAG) == APPEND) {
            logFlag & SIZE
        } else -1
    }

    def checkInsert(logFlag:Short) :Int = {
        if ((logFlag & FLAG) == INSERT) {
            logFlag & SIZE
        } else -1
    }

    def logAppend(size:Int) :Unit = {
        def appendLog(size:Int) :Unit = {
            if (size > SIZE) {
                this += (APPEND | SIZE).toShort
                _logCursor += 1
                appendLog(size - SIZE)
            } else {
                this += (APPEND | size).toShort
                _logCursor += 1
            }
        }
        
        if (_logCursor >= 0) {
            val prev = apply(_logCursor)
            val prevSize = checkAppend(prev)
            println("Append log: prevFlag=" + prev + ", prevCursor=" + _logCursor + ", prevSize=" + prevSize)
            if (prevSize > -1) {
                val newSize = prevSize + size
                if (newSize <= SIZE) {
                    // merge with previous one
                    println("Append log (merged with prev): newSize=" + newSize)
                    update(_logCursor, (APPEND | newSize).toShort)
                } else appendLog(size)
            } else appendLog(size)
        } else appendLog(size)

        _logTime = System.currentTimeMillis
    }

    def logInsert(size:Int, idx:Int) :Unit = {
        /** cursorIncr: if (prev == append) 1 else 3 */
        def appendLog(size:Int, idx:Int, cursorIncr:Int) :Unit = {
            if (size > SIZE) {
                this += (INSERT | SIZE).toShort
                this ++= intToShorts(idx)
                _logCursor += cursorIncr
                appendLog(size - SIZE, idx + SIZE, 3)
            } else {
                this += (INSERT | size).toShort
                this ++= intToShorts(idx)
                _logCursor += cursorIncr
            }
        }

        if (_logCursor >= 0) {
            val prev = apply(_logCursor)
            val prevSize = checkInsert(prev)
            println("Insert log: prevFlag=" + prev + ", prevCursor=" + _logCursor + ", prevSize=" + prevSize + ", idx=" + idx)
            if (prevSize > -1) {
                val prevIdx = shortsToInt(apply(_logCursor + 1), apply(_logCursor + 2))
                if (prevIdx + prevSize == idx) {
                    val newSize = prevSize + size
                    if (newSize <= SIZE) {
                        // merge with previous one
                        println("Insert log (merged with prev): idx=" + prevIdx + ", newSize=" + newSize)
                        update(_logCursor, (INSERT | newSize).toShort)
                    } else appendLog(size:Int, idx:Int, 3)
                } else appendLog(size:Int, idx:Int, 3)
            } else appendLog(size:Int, idx:Int, 1)
        } else appendLog(size:Int, idx:Int, 1)

        _logTime = System.currentTimeMillis
    }

    def insertIndexOfLog(cursor:Int) :Int = {
        shortsToInt(apply(cursor + 1), apply(cursor + 2))
    }

    /* [0] = lowest order 16 bits; [1] = highest order 16 bits. */
    private def intToShorts(i:Int) :Array[Short] = {
        Array((i >> 16).toShort, i.toShort)
    }

    private def shortsToInt(hi:Short, lo:Short) = {
        (hi << 16) + lo
    }
}

@cloneable
trait Timestamps extends ArrayBuffer[Long] {
    val LONG_LONG_AGO = new GregorianCalendar(1900, Calendar.JANUARY, 1).getTimeInMillis

    private val readWriteLock = new ReentrantReadWriteLock
    val readLock  :Lock = readWriteLock.readLock
    val writeLock :Lock = readWriteLock.writeLock

    val log = new TimestampsLog

    def isOnCalendar :Boolean
    
    def asOnCalendar :Timestamps
    
    /**
     * Get nearest row that can also properly extends before firstOccurredTime
     * or after lastOccurredTime
     */
    def rowOfTime(time:Long, freq:Frequency) :Int
    
    def timeOfRow(row:Int, freq:Frequency):Long
    
    def lastRow(freq:Frequency) :Int
    
    def sizeOf(freq:Frequency) :Int
    
    def indexOfOccurredTime(time:Long) :Int
    
    /**
     * Search the nearest index between '1' to 'lastIndex - 1'
     * We only need to use this computing in case of onOccurred.
     */
    def nearestIndexOfOccurredTime(time:Long) :Int
    
    /** return index of nearest behind or equal(if exist) time */
    def indexOfNearestOccurredTimeBehind(time:Long) :Int
    
    /** return index of nearest before or equal(if exist) time */
    def indexOfNearestOccurredTimeBefore(time:Long) :Int
    
    def firstOccurredTime :Long
    
    def lastOccurredTime :Long
    
    def iterator(freq:Frequency) :TimestampsIterator
    
    def iterator(freq:Frequency, fromTime:Long, toTime:Long) :TimestampsIterator

    /** this should not be abstract method to get scalac knowing it's a override of @cloneable instead of java.lang.Object#clone */
    override
    def clone:Timestamps = {super.clone; this}
}








