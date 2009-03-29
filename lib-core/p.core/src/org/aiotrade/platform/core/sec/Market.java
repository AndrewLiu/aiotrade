/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.platform.core.sec;

import java.util.Calendar;
import java.util.TimeZone;

/**
 *
 * @author dcaoyuan
 */
public class Market {

    private TimeZone timeZone;
    private Calendar cal;
    private int openHour;
    private int openMin;
    private int closeHour;
    private int closeMin;
    private long openTimeOfDay;
    public final static Market NYSE = new Market(TimeZone.getTimeZone("America/New_York"), 9, 30, 15, 00);  // New York
    public final static Market SHSE = new Market(TimeZone.getTimeZone("Asia/Shanghai"), 9, 30, 15, 0); // Shanghai
    public final static Market SZSE = new Market(TimeZone.getTimeZone("Asia/Shanghai"), 9, 30, 15, 0); // Shenzhen
    public final static Market LDSE = new Market(TimeZone.getTimeZone("UTC"), 9, 30, 15, 0); // London

    public Market(int openHour, int openMin, int closeHour, int closeMin) {
        this(TimeZone.getTimeZone("UTC"), openHour, openMin, closeHour, closeMin);
    }

    public Market(TimeZone timeZone, int openHour, int openMin, int closeHour, int closeMin) {
        this.timeZone = timeZone;
        this.openHour = openHour;
        this.openMin = openMin;
        this.closeHour = closeHour;
        this.closeMin = closeMin;
        this.openTimeOfDay = (closeHour * 60 + closeMin) * 60 * 1000;
        this.cal = Calendar.getInstance(timeZone);
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public long openTime(long time) {
        cal.clear();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, openHour);
        cal.set(Calendar.MINUTE, openMin);
        return cal.getTimeInMillis();
    }

    public long closeTime(long time) {
        cal.clear();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, closeHour);
        cal.set(Calendar.MINUTE, closeMin);
        return cal.getTimeInMillis();
    }

    public long getOpenTimeOfDay() {
        return openTimeOfDay;
    }

    @Override
    public String toString() {
        return timeZone.getDisplayName();
    }
}
