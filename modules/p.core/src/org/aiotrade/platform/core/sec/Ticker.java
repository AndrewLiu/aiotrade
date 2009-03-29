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
package org.aiotrade.platform.core.sec;

import java.util.Calendar;
import org.aiotrade.lib.math.timeseries.datasource.TimeValue;

/**
 *
 * This is just a lightweight value object. So, it can be used to lightly store
 * tickers at various time. That is, you can store many many tickers for same
 * symbol efficiently, as in case of composing an one minute ser.
 *
 * The TickerSnapshot will present the last current snapshot ticker for one
 * symbol, and implement Observable. You only need one TickerSnapshot for each
 * symbol.
 *
 * @author Caoyuan Deng
 */
public final class Ticker implements TimeValue, Cloneable {

    public final static int PREV_CLOSE = 0;
    public final static int LAST_PRICE = 1;
    public final static int DAY_OPEN = 2;
    public final static int DAY_HIGH = 3;
    public final static int DAY_LOW = 4;
    public final static int DAY_VOLUME = 5;
    public final static int DAY_AMOUNT = 6;
    public final static int DAY_CHANGE = 7;
    private final float[] values = new float[8];
    private long time;
    private final int depth;
    private final float[] bidPrices;
    private final float[] bidSizes;
    private final float[] askPrices;
    private final float[] askSizes;
    private final Calendar cal = Calendar.getInstance();

    public Ticker() {
        this(5);
    }

    public Ticker(int depth) {
        this.depth = depth;
        bidPrices = new float[depth];
        bidSizes = new float[depth];
        askPrices = new float[depth];
        askSizes = new float[depth];
    }

    public final void setTime(long time) {
        this.time = time;
    }

    public final long getTime() {
        return time;
    }

    public final int getDepth() {
        return depth;
    }

    public final float getBidPrice(int idx) {
        return bidPrices[idx];
    }

    public final float getBidSize(int idx) {
        return bidSizes[idx];
    }

    public final float getAskPrice(int idx) {
        return askPrices[idx];
    }

    public final float getAskSize(int idx) {
        return askSizes[idx];
    }

    public final float setBidPrice(int idx, float value) {
        return bidPrices[idx] = value;
    }

    public final float setBidSize(int idx, float value) {
        return bidSizes[idx] = value;
    }

    public final float setAskPrice(int idx, float value) {
        return askPrices[idx] = value;
    }

    public final float setAskSize(int idx, float value) {
        return askSizes[idx] = value;
    }

    public float get(int field) {
        return values[field];
    }

    public void set(int field, float value) {
        this.values[field] = value;
    }

    public final void reset() {
        time = 0;
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
        for (int i = 0; i < depth; i++) {
            bidPrices[0] = 0;
        }
        for (int i = 0; i < depth; i++) {
            bidSizes[0] = 0;
        }
        for (int i = 0; i < depth; i++) {
            askPrices[0] = 0;
        }
        for (int i = 0; i < depth; i++) {
            askSizes[0] = 0;
        }
    }

    public final void copy(Ticker another) {
        this.time = another.time;
        System.arraycopy(another.values, 0, this.values, 0, this.values.length);
        System.arraycopy(another.bidPrices, 0, this.bidPrices, 0, depth);
        System.arraycopy(another.bidSizes, 0, this.bidSizes, 0, depth);
        System.arraycopy(another.askPrices, 0, this.askPrices, 0, depth);
        System.arraycopy(another.askSizes, 0, this.askSizes, 0, depth);
    }

    public final boolean isValueChanged(Ticker another) {
        for (int i = 0; i < values.length; i++) {
            if (this.values[i] != another.values[i]) {
                return true;
            }
        }
        for (int i = 0; i < depth; i++) {
            if (this.bidPrices[i] != another.bidPrices[i]) {
                return true;
            }
        }
        for (int i = 0; i < depth; i++) {
            if (this.bidSizes[i] != another.bidSizes[i]) {
                return true;
            }
        }
        for (int i = 0; i < depth; i++) {
            if (this.askPrices[i] != another.askPrices[i]) {
                return true;
            }
        }
        for (int i = 0; i < depth; i++) {
            if (this.askSizes[i] != another.askSizes[i]) {
                return true;
            }
        }

        return false;
    }

    public final boolean isDayVolumeGrown(Ticker prevTicker) {
        return this.values[DAY_VOLUME] > prevTicker.values[DAY_VOLUME] && isSameDay(prevTicker);
    }

    public final boolean isDayVolumeChanged(Ticker preTicker) {
        return this.values[DAY_VOLUME] != preTicker.values[DAY_VOLUME] && isSameDay(preTicker);
    }

    public final boolean isSameDay(Ticker preTicker) {
        cal.setTimeInMillis(time);
        int day0 = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTimeInMillis(preTicker.getTime());
        int day1 = cal.get(Calendar.DAY_OF_MONTH);
        return day0 == day1;
    }

    public final float getChangeInPercent() {
        return values[PREV_CLOSE] == 0 ? 0 : (values[LAST_PRICE] - values[PREV_CLOSE]) / values[PREV_CLOSE] * 100;
    }

    public final int compareLastCloseTo(Ticker prevTicker) {
        return values[LAST_PRICE] > prevTicker.values[Ticker.LAST_PRICE] ? 1 : values[LAST_PRICE] == prevTicker.values[Ticker.LAST_PRICE] ? 0 : 1;
    }

    @Override
    public final Ticker clone() {
        try {
            return (Ticker) super.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
