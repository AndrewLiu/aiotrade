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
package org.aiotrade.math.timeseries;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

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
public interface Timestamps extends List<Long> {
    public final static long LONG_LONG_AGO = new GregorianCalendar(1900, Calendar.JANUARY, 1).getTimeInMillis();
    
    boolean isOnCalendar();
    
    Timestamps asOnCalendar();
    
    /**
     * Get nearest row that can also properly extends before firstOccurredTime
     * or after lastOccurredTime
     */
    int rowOfTime(long time, Frequency freq);
    
    long timeOfRow(int row, Frequency freq);
    
    int lastRow(Frequency freq);
    
    int size(Frequency freq);
    
    int indexOfOccurredTime(long time);
    
    /**
     * Search the nearest index between '1' to 'lastIndex - 1'
     * We only need to use this computing in case of onOccurred.
     */
    int nearestIndexOfOccurredTime(long time);
    
    /** return index of nearest behind or equal(if exist) time */
    int indexOfNearestOccurredTimeBehind(long time);
    
    /** return index of nearest before or equal(if exist) time */
    int indexOfNearestOccurredTimeBefore(long time);
    
    long firstOccurredTime();
    
    long lastOccurredTime();
    
    TimestampsIterator iterator(Frequency freq);
    
    TimestampsIterator iterator(Frequency freq, long fromTime, long toTime);
    
}





