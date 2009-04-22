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
@cloneable
trait Timestamps extends ArrayBuffer[Long] {
    val LONG_LONG_AGO = new GregorianCalendar(1900, Calendar.JANUARY, 1).getTimeInMillis

    private val readWriteLock = new ReentrantReadWriteLock
    val readLock  :Lock = readWriteLock.readLock
    val writeLock :Lock = readWriteLock.writeLock
    
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








