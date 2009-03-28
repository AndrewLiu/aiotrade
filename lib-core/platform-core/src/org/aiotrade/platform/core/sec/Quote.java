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
import org.aiotrade.math.timeseries.datasource.TimeValue;


/**
 * Quote value object
 *
 * @author Caoyuan Deng
 */
public final class Quote implements TimeValue {
    private final static int OPEN = 0;
    private final static int HIGH = 1;
    private final static int LOW = 2;
    private final static int CLOSE = 3;
    private final static int VOLUME = 4;
    private final static int AMOUNT = 5;
    private final static int CLOSE_ADJ = 6;
    private final static int WAP = 7;
    
    private final float[] values = new float[8];
    
    private long  time;
    private long sourceId;
    
    private boolean hasGaps;
    
    public Quote() {
    }
    
    public final long getTime() {
        return time;
    }
    
    public final void setTime(long time) {
        this.time = time;
    }
    
    public final float getAmount() {
        return values[AMOUNT];
    }
    
    public final float getClose() {
        return values[CLOSE];
    }
    
    public final float getClose_adj() {
        return values[CLOSE_ADJ];
    }
    
    public final float getHigh() {
        return values[HIGH];
    }
    
    public final float getLow() {
        return values[LOW];
    }
    
    public final float getOpen() {
        return values[OPEN];
    }
    
    public final long getSourceId() {
        return sourceId;
    }
    
    public final float getVolume() {
        return values[VOLUME];
    }
    
    public final float getWAP() {
        return values[WAP];
    }
    
    public final boolean hasGaps() {
        return hasGaps;
    }
    
    public final void setAmount(float amount) {
        this.values[AMOUNT] = amount;
    }
    
    public final void setClose(float close) {
        this.values[CLOSE] = close;
    }
    
    public final void setClose_adj(float close_adj) {
        this.values[CLOSE_ADJ] = close_adj;
    }
    
    public final void setHasGaps(boolean hasGaps) {
        this.hasGaps = hasGaps;
    }
    
    public final void setHigh(float high) {
        this.values[HIGH] = high;
    }
    
    public final void setLow(float low) {
        this.values[LOW] = low;
    }
    
    public final void setOpen(float open) {
        this.values[OPEN] = open;
    }
    
    public final void setSourceId(long sourceId) {
        this.sourceId = sourceId;
    }
    
    public final void setVolume(float volume) {
        this.values[VOLUME] = volume;
    }
    
    public final void setWAP(float wap) {
        this.values[WAP] = wap;
    }
    
    public final void reset() {
        time = 0;
        sourceId = 0;
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
        hasGaps = false;
    }
    
    public final String toString() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return this.getClass().getSimpleName() + ": " + cal.getTime() + 
                " O: " + values[OPEN] + 
                " H: " + values[HIGH] + 
                " L: " + values[LOW] +
                " C: " + values[CLOSE] +
                " V: " + values[VOLUME];
    }
}