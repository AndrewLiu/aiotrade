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

import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author Caoyuan Deng
 *
 * @credits:
 *     stebridev@users.sourceforge.net - fix case of Week : beginTimeOfUnitThatInclude(long)
 */
public enum Unit {
    Second,
    Minute,
    Hour,
    Day,
    Week,
    Month,
    Year;
    
    /**
     * the unit(interval) of each Unit
     */
    private static final int  ONE_SECOND = 1000;
    private static final int  ONE_MINUTE = 60 * ONE_SECOND;
    private static final int  ONE_HOUR   = 60 * ONE_MINUTE;
    private static final long ONE_DAY    = 24 * ONE_HOUR;
    private static final long ONE_WEEK   =  7 * ONE_DAY;
    private static final long ONE_MONTH  = 30 * ONE_DAY;
    private static final long ONE_Year   = (long)365.24 * ONE_DAY;
        
    /**
     *
     *
     *
     * @NOTICE: Should avoid declaring Calendar instance as static, it's not thread-safe
     * see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6231579
     *
     * As Unit is enum, which actually is a kind of singleton, so delcare a
     * none static Calendar instance here also means an almost static instance,
     * so, if we declare class scope instance of Calendar in enum, we should also
     * synchronized each method that uses this instance or declare the cal
     * instance as volatile to share this instance by threads.
     */
    private static volatile Calendar cal = Calendar.getInstance();
    
    public final long getInterval() {
        switch (this) {
            case Second:
                return ONE_SECOND;
            case Minute:
                return ONE_MINUTE;
            case Hour:
                return ONE_HOUR;
            case Day:
                return ONE_DAY;
            case Week:
                return ONE_WEEK;
            case Month:
                return ONE_MONTH;
            case Year:
                return ONE_Year;
            default:
                return ONE_DAY;
        }
    }
    
    /**
     * round time to unit's begin 0
     * @param time time in milliseconds from the epoch (1 January 1970 0:00 UTC)
     */
    public final long round(long time) {
        //return (time + offsetToUTC / getInterval()) * getInterval() - offsetToUTC;
        return (time / getInterval()) * getInterval();
    }
    
    public String getShortDescription() {
        switch (this) {
            case Second:
                return "s";
            case Minute:
                return "m";
            case Hour:
                return "h";
            case Day:
                return "D";
            case Week:
                return "W";
            case Month:
                return "M";
            default:
                return "d";
        }
    }
    
    public String getCompactDescription() {
        switch (this) {
            case Second:
                return "Sec";
            case Minute:
                return "Min";
            case Hour:
                return "Hour";
            case Day:
                return "Day";
            case Week:
                return "Week";
            case Month:
                return "Month";
            default:
                return "Day";
        }
    }
    
    public String getLongDescription() {
        switch (this) {
            case Second:
                return "Second";
            case Minute:
                return "Minute";
            case Hour:
                return "Hourly";
            case Day:
                return "Daily";
            case Week:
                return "Weekly";
            case Month:
                return "Monthly";
            default:
                return "Daily";
        }
    }
    
    public final int nUnitsBetween(long fromTime, long toTime) {
        int nUnits = 0;
        
        switch (this) {
            case Week:
                nUnits = nWeeksBetween(fromTime, toTime);
                break;
            case Month:
                nUnits = nMonthsBetween(fromTime, toTime);
                break;
            default:
                nUnits = (int)((toTime - fromTime) / getInterval());
        }
        
        return nUnits;
    }
    
    private final int nWeeksBetween(long fromTime, long toTime) {
        int between = (int)((toTime - fromTime) / ONE_WEEK);
        
        /**
         * If between >= 1, between should be correct.
         * Otherwise, the days between fromTime and toTime is <= 6,
         * we should consider it as following:
         */
        if (Math.abs(between) < 1) {
            cal.setTimeInMillis(fromTime);
            final int weekOfYearA = cal.get(Calendar.WEEK_OF_YEAR);
            
            cal.setTimeInMillis(toTime);
            final int weekOfYearB = cal.get(Calendar.WEEK_OF_YEAR);
            
            /** if is in same week, between = 0, else between = 1 */
            between = weekOfYearA == weekOfYearB ?
                0 : (between > 0) ? 1 : -1;
        }
        
        return between;
    }
    
    private final int nMonthsBetween(long fromTime, long toTime) {
        int between = 0;
        
        cal.setTimeInMillis(fromTime);
        final int monthOfYearA = cal.get(Calendar.MONTH);
        final int yearA = cal.get(Calendar.YEAR);
        
        cal.setTimeInMillis(toTime);
        final int monthOfYearB = cal.get(Calendar.MONTH);
        final int yearB = cal.get(Calendar.YEAR);
        
        /** here we assume each year has 12 months */
        between = (yearB * 12 + monthOfYearB) - (yearA * 12 + monthOfYearA);
        
        return between;
    }
    
    public final long timeAfterNUnits(long fromTime, int nUnits) {
        long time = -1;
        
        switch (this) {
            case Week:
                time = timeAfterNWeeks(fromTime, nUnits);
                break;
            case Month:
                time = timeAfterNMonths(fromTime, nUnits);
                break;
            default:
                time = fromTime + nUnits * getInterval();
        }
        
        return time;
    }
    
    /** snapped to first day of the week */
    private final long timeAfterNWeeks(long fromTime, int nWeeks) {
        cal.setTimeInMillis(fromTime);
        
        /** set the time to first day of this week */
        final int firstDayOfWeek = cal.getFirstDayOfWeek();
        cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);
        
        cal.add(Calendar.WEEK_OF_YEAR, nWeeks);
        
        return cal.getTimeInMillis();
    }
    
    /** snapped to 1st day of the month */
    private final long timeAfterNMonths(long fromTime, int nMonths) {
        cal.setTimeInMillis(fromTime);
        
        /** set the time to this month's 1st day */
        final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        cal.add(Calendar.DAY_OF_YEAR, -(dayOfMonth - 1));
        
        cal.add(Calendar.MONTH, nMonths);
        
        return cal.getTimeInMillis();
    }
    
    public final long beginTimeOfUnitThatInclude(long time, TimeZone timeZone) {
        cal.setTimeZone(timeZone);
        cal.setTimeInMillis(time);
        
        switch (this) {
            case Day:
                /** set the time to today's 0:00 by clear hour, minute, second etc. */
                final int year  = cal.get(Calendar.YEAR);
                final int month = cal.get(Calendar.MONTH);
                final int date  = cal.get(Calendar.DAY_OF_MONTH);
                cal.clear();
                cal.set(year, month, date);
                break;
            case Week:
                /**
                 * set the time to this week's first day of one week
                 *     int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                 *     calendar.add(Calendar.DAY_OF_YEAR, -(dayOfWeek - Calendar.SUNDAY));
                 *
                 * From stebridev@users.sourceforge.net:
                 * In some place of the world the first day of month is Monday,
                 * not Sunday like in the United States. For example Sunday 15
                 * of August of 2004 is the week 33 in Italy and not week 34
                 * like in US, while Thursday 19 of August is in the week 34 in
                 * boot Italy and US.
                 */
                final int firstDayOfWeek = cal.getFirstDayOfWeek();
                cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);
                break;
            case Month:
                /** set the time to this month's 1st day */
                final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                cal.add(Calendar.DAY_OF_YEAR, -(dayOfMonth - 1));
                break;
            default:
        }
        
        return cal.getTimeInMillis();
    }
    
    public String formatNormalDate(Date date, TimeZone timeZone) {
        DateFormat df;
        switch (this) {
            case Second:
                df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
                break;
            case Minute:
                df = DateFormat.getTimeInstance(DateFormat.SHORT);
                break;
            case Hour:
                df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
                break;
            case Day:
                df = DateFormat.getDateInstance(DateFormat.SHORT);
                break;
            case Week:
                df = DateFormat.getDateInstance(DateFormat.SHORT);
                break;
            default:
                df = DateFormat.getDateInstance(DateFormat.SHORT);
        }

        df.setTimeZone(timeZone);
        return df.format(date);
    }
    
    public String formatStrideDate(Date date, TimeZone timeZone) {
        DateFormat df;
        switch (this) {
            case Second:
                df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                break;
            case Minute:
                df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                break;
            case Hour:
                df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                break;
            case Day:
                df = DateFormat.getDateInstance(DateFormat.SHORT);
                break;
            case Week:
                df = DateFormat.getDateInstance(DateFormat.SHORT);
                break;
            default:
                df = DateFormat.getDateInstance(DateFormat.SHORT);
        }
        
        StringBuffer buffer = new StringBuffer();
        df.setTimeZone(timeZone);
        df.format(date, buffer, new FieldPosition(DateFormat.MONTH_FIELD));
        return buffer.toString();
    }
}
