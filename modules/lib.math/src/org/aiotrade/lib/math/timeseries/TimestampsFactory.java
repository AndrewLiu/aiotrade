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
package org.aiotrade.lib.math.timeseries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.TimeZone;

/**
 *
 *
 * @author  Caoyuan Deng
 * @version 1.02, 11/25/2006
 * @since   1.0.4
 */
public class TimestampsFactory {
    
    public final static Timestamps createInstance(int initialCapacity) {
        return new TimestampsOnOccurred(initialCapacity);
    }
    
    private final static class TimestampsOnOccurred extends ArrayList<Long> implements Timestamps {
        private final TimestampsOnCalendar onCalendarShadow;
        
        public TimestampsOnOccurred(int initialCapacity) {
            super(initialCapacity);
            this.onCalendarShadow = new TimestampsOnCalendar(this);
        }
        
        public final boolean isOnCalendar() {
            return false;
        }
        
        public Timestamps asOnCalendar() {
            return onCalendarShadow;
        }
        
        /**
         * Get nearest row that can also properly extends before firstOccurredTime
         * or after lastOccurredTime
         */
        public final int rowOfTime(long time, Frequency freq) {
            final int lastOccurredIdx = size() - 1;
            if (lastOccurredIdx == -1) {
                return -1;
            }
            
            final long firstOccurredTime = get(0);
            final long lastOccurredTime  = get(lastOccurredIdx);
            if (time <= firstOccurredTime) {
                return freq.nFreqsBetween(firstOccurredTime, time);
            } else if (time >= lastOccurredTime) {
                /**
                 * @NOTICE
                 * The number of bars of onOccurred between first-last is different
                 * than onCalendar, so we should count from lastOccurredIdx in case
                 * of onOccurred. so, NEVER try:
                 * <code>return freq.nFreqsBetween(firstOccurredTime, time);</code>
                 * in case of onOccurred
                 */
                return lastOccurredIdx + freq.nFreqsBetween(lastOccurredTime, time);
            } else {
                return nearestIndexOfOccurredTime(time);
            }
        }
        
        /**
         * This is an efficent method
         */
        public final long timeOfRow(int row, Frequency freq) {
            final int lastOccurredIdx = size() - 1;
            if (lastOccurredIdx < 0) {
                return 0;
            }
            
            final long firstOccurredTime = get(0);
            final long lastOccurredTime  = get(lastOccurredIdx);
            if (row < 0) {
                return freq.timeAfterNFreqs(firstOccurredTime, row);
            } else if (row > lastOccurredIdx) {
                return freq.timeAfterNFreqs(lastOccurredTime, row - lastOccurredIdx);
            } else {
                return get(row);
            }
        }
        
        public final int lastRow(Frequency freq) {
            final int lastOccurredIdx = size() - 1;
            return lastOccurredIdx;
        }
        
        public final int size(Frequency freq) {
            return size();
        }
        
        public final int indexOfOccurredTime(long time) {
            final int size = size();
            if (size == 0) {
                return -1;
            } else if (size == 1) {
                if (get(0) == time) {
                    return 0;
                } else {
                    return -1;
                }
            }
            
            int from = 0;
            int to = size - 1;
            int length = to - from;
            while (length > 1) {
                length /= 2;
                final long midTime = get(from + length);
                if (time > midTime) {
                    from += length;
                } else if (time < midTime) {
                    to -= length;
                } else {
                    /** time == midTime */
                    return from + length;
                }
                length = to - from;
            }
            
            /**
             * if we reach here, that means the time should between (start) and (start + 1),
             * and the length should be 1 (end - start). So, just do following checking,
             * if can't get exact index, just return -1.
             */
            if (time == get(from)) {
                return from;
            } else if (time == get(from + 1)) {
                return from + 1;
            } else {
                return -1;
            }
        }
        
        /**
         * Search the nearest index between '1' to 'lastIndex - 1'
         * We only need to use this computing in case of onOccurred.
         */
        public final int nearestIndexOfOccurredTime(long time) {
            int from = 0;
            int to = size() - 1;
            int length = to - from;
            while (length > 1) {
                length /= 2;
                final long midTime = get(from + length);
                if (time > midTime) {
                    from += length;
                } else if (time < midTime) {
                    to -= length;
                } else {
                    /** time == midTime */
                    return from + length;
                }
                length = to - from;
            }
            
            /**
             * if we reach here, that means the time should between (start) and (start + 1),
             * and the length should be 1 (end - start). So, just do following checking,
             * if can't get exact index, just return nearest one: 'start'
             */
            if (time == get(from)) {
                return from;
            } else if (time == get(from + 1)) {
                return from + 1;
            } else {
                return from;
            }
        }
        
        /** return index of nearest behind or equal(if exist) time */
        public final int indexOfNearestOccurredTimeBehind(long time) {
            final int size = size();
            if (size == 0) {
                return -1;
            } else if (size == 1) {
                if (get(0) >= time) {
                    return 0;
                } else {
                    return -1;
                }
            }
            
            int from = 0;
            int to = size - 1;
            int length = to - from;
            while (length > 1) {
                length /= 2;
                final long midTime = get(from + length);
                if (time > midTime) {
                    from += length;
                } else if (time < midTime) {
                    to -= length;
                } else {
                    /** time == midTime */
                    return from + length;
                }
                length = to - from;
            }
            
            /**
             * if we reach here, that means the time should between (from) and (from + 1),
             * and the 'length' should be 1 (end - start). So, just do following checking,
             * if can't get exact index, just return -1.
             */
            if (get(from) >= time) {
                return from;
            } else if (get(from + 1) >= time) {
                return from + 1;
            } else {
                return -1;
            }
        }
        
        /** return index of nearest before or equal(if exist) time */
        public final int indexOfNearestOccurredTimeBefore(long time) {
            final int size = size();
            if (size == 0) {
                return -1;
            } else if (size == 1) {
                if (get(0) <= time) {
                    return 0;
                } else {
                    return -1;
                }
            }
            
            int from = 0;
            int to = size - 1;
            int length = to - from;
            while (length > 1) {
                length /= 2;
                final long midTime = get(from + length);
                if (time > midTime) {
                    from += length;
                } else if (time < midTime) {
                    to -= length;
                } else {
                    /** time == midTime */
                    return from + length;
                }
                length = to - from;
            }
            
            /**
             * if we reach here, that means the time should between (from) and (from + 1),
             * and the 'length' should be 1 (end - start). So, just do following checking,
             * if can't get exact index, just return -1.
             */
            if (get(from + 1) <= time) {
                return from + 1;
            } else if (get(from) <= time) {
                return from;
            } else {
                return -1;
            }
        }
        
        public final long firstOccurredTime() {
            final int size = size();
            return size > 0 ? get(0) : 0;
        }
        
        public final long lastOccurredTime() {
            final int size = size();
            return size > 0 ? get(size - 1) : 0;
        }
        
        public final TimestampsIterator iterator(Frequency freq) {
            return new ItrOnOccurred(freq);
        }
        
        public final TimestampsIterator iterator(Frequency freq, long fromTime, long toTime) {
            return new ItrOnOccurred(freq, fromTime, toTime);
        }
        
        protected transient int modCount = 0;
        
        class ItrOnOccurred implements TimestampsIterator {
            final Frequency freq;
            long fromTime;
            long toTime;
            TimeZone timeZone = TimeZone.getDefault();
            
            ItrOnOccurred(Frequency freq) {
                this(freq, firstOccurredTime(), lastOccurredTime());
            }
            
            ItrOnOccurred(Frequency freq, long fromTime, long toTime) {
                this.freq = freq;
                this.fromTime = freq.round(fromTime, timeZone);
                this.toTime = toTime;
            }
            
            long cursorTime = fromTime;
            /** Reset to LONG_LONG_AGO if this element is deleted by a call to remove. */
            long lastReturnTime = LONG_LONG_AGO;
            
            /**
             * Row of element to be returned by subsequent call to next.
             */
            int cursorRow = 0;
            
            /**
             * Index of element returned by most recent call to next or
             * previous.  Reset to -1 if this element is deleted by a call
             * to remove.
             */
            int lastRet = -1;
            
            /**
             * The modCount value that the iterator believes that the backing
             * List should have.  If this expectation is violated, the iterator
             * has detected concurrent modification.
             */
            int expectedModCount = modCount;
            
            public boolean hasNext() {
                return cursorTime <= toTime;
            }
            
            public Long next() {
                checkForComodification();
                try {
                    cursorRow++;
                    final Long next = cursorRow >= size() ? freq.nextTime(cursorTime) : get(cursorRow);
                    lastReturnTime = cursorTime = next;
                    return next;
                } catch(IndexOutOfBoundsException e) {
                    checkForComodification();
                    throw new NoSuchElementException();
                }
            }
            
            final void checkForComodification() {
                if (modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
            
            public boolean hasPrevious() {
                return cursorTime >= fromTime;
            }
            
            public Long previous() {
                checkForComodification();
                try {
                    cursorRow--;
                    final long previous = cursorRow < 0 ? freq.previousTime(cursorTime): get(cursorRow);
                    lastReturnTime = cursorTime = previous;
                    return previous;
                } catch(IndexOutOfBoundsException e) {
                    checkForComodification();
                    throw new NoSuchElementException();
                }
            }
            
            public int nextOccurredIndex() {
                return indexOfNearestOccurredTimeBehind(cursorTime);
            }
            
            public int previousOccurredIndex() {
                return indexOfNearestOccurredTimeBefore(cursorTime);
            }
            
            public int nextRow() {
                return cursorRow;
            }
            
            public int previousRow() {
                return cursorRow - 1;
            }
        }
    }
    
    
    /**
     * A shadow and extrem lightweight class for Timestamps, it will be almost the same
     * instance as delegateTimestamps, especially shares the elements data. Except its
     * isOnCalendar() always return true.
     * Why not to use Proxy.class ? for performance reason.
     */
    private final static class TimestampsOnCalendar implements Timestamps {
        /**
         * the timestamps to be wrapped, it not necessary to be a TimestampsOnOccurred,
         * any class implemented Timestamps is ok.
         */
        final Timestamps delegateTimestamps;
        
        protected TimestampsOnCalendar(Timestamps delegateTimestamps) {
            this.delegateTimestamps = delegateTimestamps;
        }
        
        public final boolean isOnCalendar() {
            return true;
        }
        
        public final Timestamps asOnCalendar() {
            return delegateTimestamps.asOnCalendar();
        }
        
        /**
         * Get nearest row that can also properly extends before firstOccurredTime
         * or after lastOccurredTime
         */
        public final int rowOfTime(long time, Frequency freq) {
            final int lastOccurredIdx = size() - 1;
            if (lastOccurredIdx == -1) {
                return -1;
            }
            
            final long firstOccurredTime = get(0);
            return freq.nFreqsBetween(firstOccurredTime, time);
        }
        
        /**
         * This is an efficent method
         */
        public final long timeOfRow(int row, Frequency freq) {
            final int lastOccurredIdx = size() - 1;
            if (lastOccurredIdx < 0) {
                return 0;
            }
            
            final long firstOccurredTime = get(0);
            return freq.timeAfterNFreqs(firstOccurredTime, row);
        }
        
        public final int lastRow(Frequency freq) {
            final int lastOccurredIdx = size() - 1;
            if (lastOccurredIdx < 0) {
                return 0;
            }
            
            final long firstOccurredTime = get(0);
            final long lastOccurredTime  = get(lastOccurredIdx);
            return freq.nFreqsBetween(firstOccurredTime, lastOccurredTime);
        }
        
        public final int size(Frequency freq) {
            return lastRow(freq) + 1;
        }
        
        /** -------------------------------------------- */
        
        public final int indexOfOccurredTime(long time) {
            return delegateTimestamps.indexOfOccurredTime(time);
        }
        
        public final int nearestIndexOfOccurredTime(long time) {
            return delegateTimestamps.nearestIndexOfOccurredTime(time);
        }
        
        public final int indexOfNearestOccurredTimeBehind(long time) {
            return delegateTimestamps.indexOfNearestOccurredTimeBehind(time);
        }
        
        /** return index of nearest before or equal (if exist) time */
        public final int indexOfNearestOccurredTimeBefore(long time) {
            return delegateTimestamps.indexOfNearestOccurredTimeBefore(time);
        }
        
        public final long firstOccurredTime() {
            return delegateTimestamps.firstOccurredTime();
        }
        
        public final long lastOccurredTime() {
            return delegateTimestamps.lastOccurredTime();
        }
        
        public final int size() {
            return delegateTimestamps.size();
        }
        
        public final boolean isEmpty() {
            return delegateTimestamps.isEmpty();
        }
        
        public final boolean contains(Object o) {
            return delegateTimestamps.contains(o);
        }
        
        public final Iterator<Long> iterator() {
            return delegateTimestamps.iterator();
        }
        
        public final Object[] toArray() {
            return delegateTimestamps.toArray();
        }
        
        public final <T> T[] toArray(T[] a) {
            return delegateTimestamps.toArray(a);
        }
        
        public final boolean add(Long o) {
            return delegateTimestamps.add(o);
        }
        
        public final boolean remove(Object o) {
            return delegateTimestamps.remove(o);
        }
        
        public final boolean containsAll(Collection<?> c) {
            return delegateTimestamps.containsAll(c);
        }
        
        public final boolean addAll(Collection<? extends Long> c) {
            return delegateTimestamps.addAll(c);
        }
        
        public final boolean addAll(int index, Collection<? extends Long> c) {
            return delegateTimestamps.addAll(index, c);
        }
        
        public final boolean removeAll(Collection<?> c) {
            return delegateTimestamps.removeAll(c);
        }
        
        public final boolean retainAll(Collection<?> c) {
            return delegateTimestamps.retainAll(c);
        }
        
        public final void clear() {
            delegateTimestamps.clear();
        }
        
        public final boolean equals(Object o) {
            return delegateTimestamps.equals(o);
        }
        
        public final int hashCode() {
            return delegateTimestamps.hashCode();
        }
        
        public final Long get(int index) {
            return delegateTimestamps.get(index);
        }
        
        public final Long set(int index, Long element) {
            return delegateTimestamps.set(index, element);
        }
        
        public final void add(int index, Long element) {
            delegateTimestamps.add(index, element);
        }
        
        public final Long remove(int index) {
            return delegateTimestamps.remove(index);
        }
        
        public final int indexOf(Object o) {
            return delegateTimestamps.indexOf(o);
        }
        
        public final int lastIndexOf(Object o) {
            return delegateTimestamps.lastIndexOf(o);
        }
        
        public final ListIterator<Long> listIterator() {
            return delegateTimestamps.listIterator();
        }
        
        public final ListIterator<Long> listIterator(int index) {
            return delegateTimestamps.listIterator(index);
        }
        
        public final List<Long> subList(int fromIndex, int toIndex) {
            return delegateTimestamps.subList(fromIndex, toIndex);
        }
        
        public final TimestampsIterator iterator(Frequency freq) {
            return new ItrOnCalendar(freq);
        }
        
        public final TimestampsIterator iterator(Frequency freq, long fromTime, long toTime) {
            return new ItrOnCalendar(freq, fromTime, toTime);
        }
        
        protected transient int modCount = 0;
        
        class ItrOnCalendar implements TimestampsIterator {
            final Frequency freq;
            long fromTime;
            long toTime;
            TimeZone timeZone = TimeZone.getDefault();
            
            ItrOnCalendar(Frequency freq) {
                this(freq, firstOccurredTime(), lastOccurredTime());
            }
            
            ItrOnCalendar(Frequency freq, long fromTime, long toTime) {
                this.freq = freq;
                this.fromTime = freq.round(fromTime, timeZone);
                this.toTime = toTime;
            }
            
            long cursorTime = fromTime;
            /** Reset to LONG_LONG_AGO if this element is deleted by a call to remove. */
            long lastReturnTime = LONG_LONG_AGO;
            
            /**
             * Row of element to be returned by subsequent call to next.
             */
            int cursorRow = 0;
            
            /**
             * Index of element returned by most recent call to next or
             * previous.  Reset to -1 if this element is deleted by a call
             * to remove.
             */
            int lastRet = -1;
            
            /**
             * The modCount value that the iterator believes that the backing
             * List should have.  If this expectation is violated, the iterator
             * has detected concurrent modification.
             */
            int expectedModCount = modCount;
            
            public boolean hasNext() {
                return cursorTime <= toTime;
            }
            
            public Long next() {
                checkForComodification();
                try {
                    cursorRow++;
                    final Long next = freq.nextTime(cursorTime);
                    lastReturnTime = cursorTime = next;
                    return next;
                } catch(IndexOutOfBoundsException e) {
                    checkForComodification();
                    throw new NoSuchElementException();
                }
            }
            
            final void checkForComodification() {
                if (modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
            
            public boolean hasPrevious() {
                return cursorTime >= fromTime;
            }
            
            public Long previous() {
                checkForComodification();
                try {
                    cursorRow--;
                    final long previous = freq.previousTime(cursorTime);
                    lastReturnTime = cursorTime = previous;
                    return previous;
                } catch(IndexOutOfBoundsException e) {
                    checkForComodification();
                    throw new NoSuchElementException();
                }
            }
            
            public int nextOccurredIndex() {
                return indexOfNearestOccurredTimeBehind(cursorTime);
            }
            
            public int previousOccurredIndex() {
                return indexOfNearestOccurredTimeBefore(cursorTime);
            }
            
            public int nextRow() {
                return cursorRow;
            }
            
            public int previousRow() {
                return cursorRow - 1;
            }
        }
        
    }
}


