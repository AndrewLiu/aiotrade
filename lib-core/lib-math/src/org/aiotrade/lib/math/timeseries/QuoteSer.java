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

import java.util.List;
import org.aiotrade.lib.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.lib.math.timeseries.plottable.Plot;

/**
 *
 * @author Caoyuan Deng
 */
public class QuoteSer extends DefaultMasterSer {
    private boolean adjusted = false;
    private String shortDescription = "";
    
    private final Var<Float> open   = new DefaultVar<Float>("O", Plot.Quote);
    private final Var<Float> high   = new DefaultVar<Float>("H", Plot.Quote);
    private final Var<Float> low    = new DefaultVar<Float>("L", Plot.Quote);
    private final Var<Float> close  = new DefaultVar<Float>("C", Plot.Quote);
    private final Var<Float> volume = new DefaultVar<Float>("V", Plot.Volume);
    
    private final Var<Float> close_ori = new DefaultVar<Float>();
    private final Var<Float> close_adj = new DefaultVar<Float>();
    
    public QuoteSer(Frequency freq) {
        super(freq);
    }
    
    @Override
    protected QuoteItem createItem(long time) {
        return new QuoteItem(this, time);
    }
    
    public final Var<Float> getVolume() {
        return volume;
    }
    
    public final Var<Float> getOpen() {
        return open;
    }
    
    public final Var<Float> getHigh() {
        return high;
    }
    
    public final Var<Float> getLow() {
        return low;
    }
    
    public final Var<Float> getClose() {
        return close;
    }
    
    public final Var<Float> getClose_Adj() {
        return close_adj;
    }
    
    public final Var<Float> getClose_Ori() {
        return close_ori;
    }
    
    public final boolean isAdjusted() {
        return adjusted;
    }
    
    public final void setAdjusted(boolean b) {
        this.adjusted = b;
    }
    
    /**
     * @param boolean b: if true, do adjust, else, de adjust
     */
    public final void adjust(boolean b) {
        List<SerItem> itemList = itemList();
        
        for (int i = 0, n = itemList.size(); i < n; i++) {
            QuoteItem item = (QuoteItem)itemList.get(i);
            
            float prevNorm = item.getClose();
            float postNorm;
            if (b == true) {
                /** do adjust */
                postNorm = item.getClose_Adj();
            } else {
                /** de adjust */
                postNorm = item.getClose_Ori();
            }
            
            float adjustedValue;
            
            adjustedValue = linearAdjust(item.getHigh(), prevNorm, postNorm);
            item.setHigh(adjustedValue);
            
            adjustedValue = linearAdjust(item.getLow(), prevNorm, postNorm);
            item.setLow(adjustedValue);
            
            adjustedValue = linearAdjust(item.getOpen(), prevNorm, postNorm);
            item.setOpen(adjustedValue);
            
            adjustedValue = linearAdjust(item.getClose(), prevNorm, postNorm);
            item.setClose(adjustedValue);
        }
        
        setAdjusted(b);
        
        SerChangeEvent evt = new SerChangeEvent(this, SerChangeEvent.Type.Updated, null, 0, lastOccurredTime());
        fireSerChangeEvent(evt);
    }
    
    /**
     * This function keeps the adjusting linear according to a norm
     */
    private final float linearAdjust(float value, float prevNorm, float postNorm) {
        return ((value - prevNorm) / prevNorm) * postNorm + postNorm;
    }
    
    public void setShortDescription(String symbol) {
        this.shortDescription = symbol;
    }
    
    public String getShortDescription() {
        if (isAdjusted()) {
            return shortDescription + "(*)";
        } else {
            return shortDescription;
        }
    }
    
}





