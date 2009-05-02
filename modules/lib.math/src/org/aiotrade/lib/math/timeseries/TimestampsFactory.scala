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

import java.util.ConcurrentModificationException
import java.util.{Calendar,TimeZone}
import scala.collection.mutable.ArrayBuffer

/**
 *
 *
 * @author  Caoyuan Deng
 * @version 1.02, 11/25/2006
 * @since   1.0.4
 */
object TimestampsFactory {
    
   def createInstance(initialCapacity:Int) :Timestamps = {
      new TimestampsOnOccurred(initialCapacity){override val initialSize = initialCapacity}
   }
    
   private class TimestampsOnOccurred(initialCapacity:Int) extends Timestamps {

      private val onCalendarShadow = new TimestampsOnCalendar(this)

      def isOnCalendar :Boolean = false
        
      def asOnCalendar :Timestamps = onCalendarShadow
        
      /**
       * Get nearest row that can also properly extends before firstOccurredTime
       * or after lastOccurredTime
       */
      def rowOfTime(time:Long, freq:Frequency) :Int = {
         val lastOccurredIdx = size - 1
         if (lastOccurredIdx == -1) {
            return -1
         }
            
         val firstOccurredTime = apply(0)
         val lastOccurredTime  = apply(lastOccurredIdx)
         if (time <= firstOccurredTime) {
            freq.nFreqsBetween(firstOccurredTime, time)
         } else if (time >= lastOccurredTime) {
            /**
             * @NOTICE
             * The number of bars of onOccurred between first-last is different
             * than onCalendar, so we should count from lastOccurredIdx in case
             * of onOccurred. so, NEVER try:
             * <code>return freq.nFreqsBetween(firstOccurredTime, time);</code>
             * in case of onOccurred
             */
            lastOccurredIdx + freq.nFreqsBetween(lastOccurredTime, time)
         } else {
            nearestIndexOfOccurredTime(time)
         }
      }
        
      /**
       * This is an efficent method
       */
      def timeOfRow(row:Int, freq:Frequency) :Long = {
         val lastOccurredIdx = size - 1
         if (lastOccurredIdx < 0) {
            return 0
         }
            
         val firstOccurredTime = apply(0)
         val lastOccurredTime  = apply(lastOccurredIdx)
         if (row < 0) {
            freq.timeAfterNFreqs(firstOccurredTime, row)
         } else if (row > lastOccurredIdx) {
            freq.timeAfterNFreqs(lastOccurredTime, row - lastOccurredIdx)
         } else {
            apply(row)
         }
      }
        
      def lastRow(freq:Frequency) :Int = {
         val lastOccurredIdx = size - 1
         lastOccurredIdx
      }
        
      def sizeOf(freq:Frequency) :Int = size
        
      def indexOfOccurredTime(time:Long) :Int = {
         val size1 = size
         if (size1 == 0) {
            return -1
         } else if (size1 == 1) {
            if (apply(0) == time) {
               return 0
            } else {
               return -1
            }
         }
            
         var from = 0
         var to = size1 - 1
         var length = to - from
         while (length > 1) {
            length /= 2
            val midTime = apply(from + length)
            if (time > midTime) {
               from += length
            } else if (time < midTime) {
               to -= length
            } else {
               /** time == midTime */
               return from + length
            }
            length = to - from
         }
            
         /**
          * if we reach here, that means the time should between (start) and (start + 1),
          * and the length should be 1 (end - start). So, just do following checking,
          * if can't get exact index, just return -1.
          */
         if (time == apply(from)) {
            from
         } else if (time == apply(from + 1)) {
            from + 1
         } else {
            -1
         }
      }
        
      /**
       * Search the nearest index between '1' to 'lastIndex - 1'
       * We only need to use this computing in case of onOccurred.
       */
      def nearestIndexOfOccurredTime(time:Long) :Int = {
         var from = 0
         var to = size - 1
         var length = to - from
         while (length > 1) {
            length /= 2
            val midTime = apply(from + length)
            if (time > midTime) {
               from += length
            } else if (time < midTime) {
               to -= length
            } else {
               /** time == midTime */
               return from + length
            }
            length = to - from
         }
            
         /**
          * if we reach here, that means the time should between (start) and (start + 1),
          * and the length should be 1 (end - start). So, just do following checking,
          * if can't get exact index, just return nearest one: 'start'
          */
         if (time == apply(from)) {
            from
         } else if (time == apply(from + 1)) {
            from + 1
         } else {
            from
         }
      }
        
      /** return index of nearest behind or equal(if exist) time */
      def indexOfNearestOccurredTimeBehind(time:Long) :Int = {
         val size1 = size
         if (size1 == 0) {
            return -1
         } else if (size1 == 1) {
            if (apply(0) >= time) {
               return 0
            } else {
               return -1
            }
         }
            
         var from = 0
         var to = size1 - 1
         var length = to - from
         while (length > 1) {
            length /= 2
            val midTime = apply(from + length)
            if (time > midTime) {
               from += length
            } else if (time < midTime) {
               to -= length
            } else {
               /** time == midTime */
               return from + length
            }
            length = to - from
         }
            
         /**
          * if we reach here, that means the time should between (from) and (from + 1),
          * and the 'length' should be 1 (end - start). So, just do following checking,
          * if can't get exact index, just return -1.
          */
         if (apply(from) >= time) {
            from
         } else if (apply(from + 1) >= time) {
            from + 1
         } else {
            -1
         }
      }
        
      /** return index of nearest before or equal(if exist) time */
      def indexOfNearestOccurredTimeBefore(time:Long) :Int = {
         val size1 = size
         if (size1 == 0) {
            return -1
         } else if (size1 == 1) {
            if (apply(0) <= time) {
               return 0
            } else {
               return -1
            }
         }
            
         var from = 0
         var to = size1 - 1
         var length = to - from
         while (length > 1) {
            length /= 2
            val midTime = apply(from + length)
            if (time > midTime) {
               from += length
            } else if (time < midTime) {
               to -= length
            } else {
               /** time == midTime */
               return from + length
            }
            length = to - from
         }
            
         /**
          * if we reach here, that means the time should between (from) and (from + 1),
          * and the 'length' should be 1 (end - start). So, just do following checking,
          * if can't get exact index, just return -1.
          */
         if (apply(from + 1) <= time) {
            from + 1
         } else if (apply(from) <= time) {
            from
         } else {
            -1
         }
      }
        
      def firstOccurredTime :Long = {
         val size1 = size
         if (size1 > 0) apply(0) else 0
      }
        
      def lastOccurredTime :Long = {
         val size1 = size
         if (size1 > 0) apply(size1 - 1) else 0
      }
        
      def iterator(freq:Frequency) :TimestampsIterator = {
         new ItrOnOccurred(freq)
      }
        
      def  iterator(freq:Frequency, fromTime:Long, toTime:Long, timeZone:TimeZone) :TimestampsIterator = {
         new ItrOnOccurred(freq, fromTime, toTime, timeZone)
      }

      override
      def clone :Timestamps = {
         val res = new TimestampsOnOccurred(this.size)
         res ++= this
         res
      }

      class ItrOnOccurred(freq:Frequency, _fromTime:Long, toTime:Long, timeZone:TimeZone) extends TimestampsIterator {
         private val cal = Calendar.getInstance(timeZone)

         val fromTime = freq.round(_fromTime, cal)

         def this(freq:Frequency) {
            this(freq, firstOccurredTime, lastOccurredTime, TimeZone.getDefault)
         }
                        
         var cursorTime = fromTime
         /** Reset to LONG_LONG_AGO if this element is deleted by a call to remove. */
         var lastReturnTime = LONG_LONG_AGO
            
         /**
          * Row of element to be returned by subsequent call to next.
          */
         var cursorRow = 0
            
         /**
          * Index of element returned by most recent call to next or
          * previous.  Reset to -1 if this element is deleted by a call
          * to remove.
          */
         var lastRet = -1
            
         /**
          * The modCount value that the iterator believes that the backing
          * List should have.  If this expectation is violated, the iterator
          * has detected concurrent modification.
          */
         @transient @volatile
         var modCount:Long = _

         var expectedModCount = modCount
            
         def hasNext :Boolean = {
            cursorTime <= toTime
         }
            
         def next :Long = {
            checkForComodification
            try {
               cursorRow += 1
               val next = if (cursorRow >= size) freq.nextTime(cursorTime) else apply(cursorRow)
               cursorTime = next
               lastReturnTime = cursorTime
               return next
            } catch {
               case e:IndexOutOfBoundsException =>
                  checkForComodification
                  throw new NoSuchElementException
            }
         }
            
         def checkForComodification :Unit = {
            if (modCount != expectedModCount)
            throw new ConcurrentModificationException
         }
            
         def hasPrevious :Boolean = {
            cursorTime >= fromTime
         }
            
         def previous :Long = {
            checkForComodification
            try {
               cursorRow -= 1
               val previous = if (cursorRow < 0) freq.previousTime(cursorTime) else apply(cursorRow)
               cursorTime = previous
               lastReturnTime = cursorTime
               return previous;
            } catch {
               case e:IndexOutOfBoundsException =>
                  checkForComodification
                  throw new NoSuchElementException
            }
         }
            
         def nextOccurredIndex :Int = {
            indexOfNearestOccurredTimeBehind(cursorTime)
         }
            
         def previousOccurredIndex :Int = {
            indexOfNearestOccurredTimeBefore(cursorTime)
         }
            
         def nextRow :Int = {
            cursorRow
         }
            
         def previousRow :Int = {
            cursorRow - 1
         }
      }
   }
    
    
   /**
    * A shadow and extrem lightweight class for Timestamps, it will be almost the same
    * instance as delegateTimestamps, especially shares the elements data. Except its
    * isOnCalendar() always return true.
    * Why not to use Proxy.class ? for performance reason.
    */
   private class TimestampsOnCalendar(delegateTimestamps:Timestamps) extends Timestamps {
      /**
       * the timestamps to be wrapped, it not necessary to be a TimestampsOnOccurred,
       * any class implemented Timestamps is ok.
       */
      def isOnCalendar :Boolean = true
        
      def asOnCalendar :Timestamps = delegateTimestamps.asOnCalendar
        
      /**
       * Get nearest row that can also properly extends before firstOccurredTime
       * or after lastOccurredTime
       */
      def rowOfTime(time:Long, freq:Frequency) :Int = {
         val lastOccurredIdx = size - 1
         if (lastOccurredIdx == -1) {
            return -1
         }
            
         val firstOccurredTime = apply(0)
         freq.nFreqsBetween(firstOccurredTime, time)
      }
        
      /**
       * This is an efficent method
       */
      def timeOfRow(row:Int, freq:Frequency) :Long = {
         val lastOccurredIdx = size - 1
         if (lastOccurredIdx < 0) {
            return 0
         }
            
         val firstOccurredTime = apply(0)
         freq.timeAfterNFreqs(firstOccurredTime, row)
      }
        
      def lastRow(freq:Frequency) :Int = {
         val lastOccurredIdx = size - 1
         if (lastOccurredIdx < 0) {
            return 0
         }
            
         val firstOccurredTime = apply(0)
         val lastOccurredTime  = apply(lastOccurredIdx)
         freq.nFreqsBetween(firstOccurredTime, lastOccurredTime)
      }
        
      def sizeOf(freq:Frequency) :Int = {
         lastRow(freq) + 1
      }
        
      /** -------------------------------------------- */
        
      def indexOfOccurredTime(time:Long) = delegateTimestamps.indexOfOccurredTime(time)
        
      def nearestIndexOfOccurredTime(time:Long) = delegateTimestamps.nearestIndexOfOccurredTime(time)
        
      def indexOfNearestOccurredTimeBehind(time:Long) = delegateTimestamps.indexOfNearestOccurredTimeBehind(time)
        
      /** return index of nearest before or equal (if exist) time */
      def indexOfNearestOccurredTimeBefore(time:Long) = delegateTimestamps.indexOfNearestOccurredTimeBefore(time)
        
      def firstOccurredTime = delegateTimestamps.firstOccurredTime
        
      def lastOccurredTime = delegateTimestamps.lastOccurredTime

      override
      def size = delegateTimestamps.size
        
      override
      def isEmpty = delegateTimestamps.isEmpty
        
      override
      def elements = delegateTimestamps.elements
        
      override
      def toArray[B >: Long] = delegateTimestamps.toArray
        
      override
      def copyToArray[B >: Long](xs:Array[B], start:Int) = delegateTimestamps.copyToArray(xs, start)
        
      override
      def +(elem:Long) = delegateTimestamps + elem
        
      override
      def remove(idx:Int) = delegateTimestamps.remove(idx)
        
      override
      def contains(elem:Any) = delegateTimestamps.contains(elem)
        
      override
      def ++[B >: Long](that:Iterable[B]) = delegateTimestamps ++ that

      def insert(n:Int, elems:Long) = delegateTimestamps.insert(n, elems)

      override
      def insertAll(n:Int, iter:Iterable[Long]) = delegateTimestamps.insertAll(n, iter)
        
      override
      def clear = delegateTimestamps.clear
        
      override
      def equals(o:Any) = delegateTimestamps.equals(o)
        
      override
      def hashCode = delegateTimestamps.hashCode
        
      override
      def apply(index:Int) = delegateTimestamps.apply(index)
        
      override
      def update(index:Int, element:Long) = delegateTimestamps.update(index, element)
                
      override
      def indexOf[B >: Long](o:B) = delegateTimestamps.indexOf(o)
        
      override
      def lastIndexOf[B >: Long](o:B) = delegateTimestamps.lastIndexOf(o)
                        
      def iterator(freq:Frequency) :TimestampsIterator = {
         new ItrOnCalendar(freq)
      }
        
      def iterator(freq:Frequency, fromTime:Long, toTime:Long, timeZone:TimeZone) :TimestampsIterator = {
         new ItrOnCalendar(freq, fromTime, toTime, timeZone)
      }

      @transient @volatile
      protected var modCount:Long = 0


      override
      def clone :TimestampsOnCalendar = {
         new TimestampsOnCalendar(delegateTimestamps.clone)
      }

      class ItrOnCalendar(freq:Frequency, _fromTime:Long, toTime:Long, timeZone:TimeZone) extends TimestampsIterator {
         private val cal = Calendar.getInstance(timeZone)

         val fromTime = freq.round(_fromTime, cal)
            
         def this(freq:Frequency) {
            this(freq, firstOccurredTime, lastOccurredTime, TimeZone.getDefault)
         }
            
         var cursorTime = fromTime
         /** Reset to LONG_LONG_AGO if this element is deleted by a call to remove. */
         var lastReturnTime = LONG_LONG_AGO
            
         /**
          * Row of element to be returned by subsequent call to next.
          */
         var cursorRow = 0
            
         /**
          * Index of element returned by most recent call to next or
          * previous.  Reset to -1 if this element is deleted by a call
          * to remove.
          */
         var lastRet = -1
            
         /**
          * The modCount value that the iterator believes that the backing
          * List should have.  If this expectation is violated, the iterator
          * has detected concurrent modification.
          */
         var expectedModCount = modCount
            
         def hasNext :Boolean = {
            cursorTime <= toTime
         }
            
         def next :Long = {
            checkForComodification
            try {
               cursorRow += 1
               val next = freq.nextTime(cursorTime)
               cursorTime = next
               lastReturnTime = cursorTime
               return next
            } catch {
               case e:IndexOutOfBoundsException =>
                  checkForComodification
                  throw new NoSuchElementException
            }
         }
            
         def checkForComodification :Unit = {
            if (modCount != expectedModCount) {
               throw new ConcurrentModificationException
            }
         }
            
         def hasPrevious :Boolean = {
            cursorTime >= fromTime
         }
            
         def previous :Long = {
            checkForComodification
            try {
               cursorRow -= 1
               val previous = freq.previousTime(cursorTime)
               cursorTime = previous
               lastReturnTime = cursorTime
               return previous;
            } catch {
               case e:IndexOutOfBoundsException =>
                  checkForComodification
                  throw new NoSuchElementException
            }
         }
            
         def nextOccurredIndex :Int = {
            indexOfNearestOccurredTimeBehind(cursorTime)
         }
            
         def previousOccurredIndex :Int = {
            indexOfNearestOccurredTimeBefore(cursorTime)
         }
            
         def nextRow :Int = {
            cursorRow
         }
            
         def previousRow :Int = {
            cursorRow - 1
         }
      }
        
   }
}


