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

import java.util.List;
import org.aiotrade.util.ReferenceOnly;

/**
 *
 * @author Caoyuan Deng
 */
public class DefaultMasterSer extends DefaultSer implements MasterSer {
    private boolean onCalendarMode = false;
    
    public DefaultMasterSer() {
        super();
    }
    
    public DefaultMasterSer(final Frequency freq) {
        super(freq);
    }
    
    public void setOnCalendarMode() {
        this.onCalendarMode = true;
    }
    
    public void setOnOccurredMode() {
        this.onCalendarMode = false;
    }
    
    public boolean isOnCalendarMode() {
        return onCalendarMode;
    }
    
    public final int rowOfTime(final long time) {
        return onCalendarMode ?
            timestamps().asOnCalendar().rowOfTime(time, freq):
            timestamps().rowOfTime(time, freq);
    }
    
    public final long timeOfRow(final int position) {
        return onCalendarMode ?
            timestamps().asOnCalendar().timeOfRow(position, freq):
            timestamps().timeOfRow(position, freq);
    }
    
    public final SerItem getItemByRow(final int position) {
        return onCalendarMode ?
            getItem(timestamps().asOnCalendar().timeOfRow(position, freq)):
            getItem(timestamps().timeOfRow(position, freq));
    }
    
    public final int lastOccurredRow() {
        return onCalendarMode ?
            timestamps().asOnCalendar().lastRow(freq):
            timestamps().lastRow(freq);
    }
    
    @Override
    public final int size() {
        return onCalendarMode ?
            timestamps().asOnCalendar().size(freq):
            timestamps().size();
    }
    
    /**
     * @deprecated
     */
    @ReferenceOnly private final void internal_fillTimeGapToCalendarTimes(List<Long> calendarTimes, final long time) {
        /**
         * calendarLastTime is actually the last time in calendartimes
         * if size == 0, means this is the first data, set calendarLast = time - unit,
         * @NOTICE
         * don't let calendarLastTime == 0, otherwise, the next step could be a very
         * very long cycle.
         */
        final int lastIdx = calendarTimes.size();
        long lastTime = timestamps().asOnCalendar().lastOccurredTime();
        if (lastTime == 0) {
            /** go back one interval is enough */
            lastTime = freq.previousTime(time);
        }
        
        if (time < lastTime) {
            /** should insert it to proper position to keep the order: */
            for (int i = 0; i <= lastIdx; i++) {
                final long itime = calendarTimes.get(i);
                if (itime < time) {
                    /** in order, continue */
                    continue;
                } else if (itime == time) {
                    /** it has been in series, must do nothing and finish cycle at once */
                    break;
                } else {
                    /** itime > time, insert it before this itime, then finish cycle at once */
                    final long fillBeginTime = (i > 0) ?
                        calendarTimes.get(i - 1) :
                        time;
                    
                    /** fill non-valued gap times between time and itime */
                    int j = 0;
                    for (long n = fillBeginTime; n < itime; n = freq.nextTime(n)) {
                        calendarTimes.add(i + j, n);
                        j++;
                        if (j > 100) {
                            /** avoid wrong time cause this loop too much times */
                            break;
                        }
                    }
                    
                    break;
                }
            }
            
        } else if (time > lastTime) {
            final long nextTimeAfterLastTime = freq.nextTime(lastTime);
            
            if (time > nextTimeAfterLastTime) {
                /** there may are more than one gaps that need to be filled */
                int j = 0;
                for (long n = nextTimeAfterLastTime; n <= freq.previousTime(time); n = freq.nextTime(n)) {
                    calendarTimes.add(n);
                    j++;
                    if (j > 100) {
                        /** avoid wrong time cause this loop too much times */
                        break;
                    }
                }
            }
            
            calendarTimes.add(time);
        } else {
            /** time == lastTime, should do nothing */
        }
        
    }
}






