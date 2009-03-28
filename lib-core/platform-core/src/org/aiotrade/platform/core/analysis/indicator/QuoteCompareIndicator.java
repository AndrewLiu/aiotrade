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
package org.aiotrade.platform.core.analysis.indicator;

import org.aiotrade.math.timeseries.QuoteItem;
import org.aiotrade.platform.core.analysis.indicator.AbstractIndicator.DefaultOpt;
import org.aiotrade.math.timeseries.plottable.Plot;
import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.math.timeseries.MasterSer;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.math.timeseries.SerItem;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
@IndicatorName("QUOTECOMPARE")
public class QuoteCompareIndicator extends AbstractContIndicator {
    
    private QuoteSer serToBeCompared;
    
    public QuoteCompareIndicator(Ser baseSer) {
        super(baseSer);
    }
    
    public void setSerToBeCompared(QuoteSer serToBeCompared) {
        this.serToBeCompared = serToBeCompared;
    }
    
    Opt begPosition = new DefaultOpt("Begin of Time Frame", 0L);
    Opt endPosition = new DefaultOpt("End of Time Frame",   0L);
    Opt maxValue    = new DefaultOpt("Max Value", -Float.MAX_VALUE);
    Opt minValue    = new DefaultOpt("Min Value", +Float.MAX_VALUE);
    
    public Var open   = new DefaultVar("O", Plot.Quote);
    public Var high   = new DefaultVar("H", Plot.Quote);
    public Var low    = new DefaultVar("L", Plot.Quote);
    public Var close  = new DefaultVar("C", Plot.Quote);
    public Var volume = new DefaultVar("V", Plot.Quote);
    
    protected void computeCont(int begIdx) {
        /** camparing base point is the value of begin time (the most left on screen */
        
        /** always compute from most left position on screen */
        int begPos = (int)begPosition.value();//Math.min((int)begPosition.value(), begIdx);
        int endPos = (int)endPosition.value();//Math.min((int)endPosition.value(),   _dataSize - 1);
        
        /** get first value of baseSer in time frame, it will be the comparing base point */
        float baseNorm = Float.NaN;
        for (int position = (int)begPosition.value(); position <= (int)endPosition.value(); position++) {
            QuoteItem baseItem = (QuoteItem)((QuoteSer)_baseSer).getItemByRow(position);
            
            if (baseItem != null) {
                baseNorm = baseItem.getClose();
                break;
            }
        }
        
        if (Float.isNaN(baseNorm)) {
            return;
        }
        
        if (((QuoteSer)_baseSer).isAdjusted()) {
            if (!serToBeCompared.isAdjusted()) {
                serToBeCompared.adjust(true);
            }
        } else {
            if (serToBeCompared.isAdjusted()) {
                serToBeCompared.adjust(false);
            }
        }
        
        float compareNorm = Float.NaN;
        /**
         * !NOTICE
         * we only calculate this indicator's value for a timeSet showing in screen,
         * instead of all over the time frame of baseSer, thus, we use
         * this time set for loop instead of the usaully usage in other indicators:
         *        for (int i = fromIndex; i < baseItemList.size(); i++) {
         *            ....
         *        }
         *
         * Think about it, when the baseSer updated, we should re-calculate
         * all Ser instead from fromIndex.
         */
        for (int i = begPos; i <= endPos; i++) {
            if (i < begPosition.value()) {
                /** don't calulate those is less than beginPosition to got a proper compareBeginValue */
                continue;
            }
            
            long time = ((MasterSer)_baseSer).timeOfRow(i);
            
            /**
             * !NOTICE:
             * we should fetch itemToBeCompared by time instead by position which may
             * not sync with baseSer.
             */
            QuoteItem itemToBeCompared = (QuoteItem)serToBeCompared.getItem(time);
            
            /** get first value of serToBeCompared in time frame */
            if (itemToBeCompared != null && Float.isNaN(compareNorm)) {
                compareNorm = itemToBeCompared.getClose();
            }
            
            if (itemToBeCompared != null) {
                SerItem item = getItem(time);
                if (item != null) {
                    item.set(open,  linearAdjust(itemToBeCompared.getOpen(),  compareNorm, baseNorm));
                    item.set(high,  linearAdjust(itemToBeCompared.getHigh(),  compareNorm, baseNorm));
                    item.set(low,   linearAdjust(itemToBeCompared.getLow(),   compareNorm, baseNorm));
                    item.set(close, linearAdjust(itemToBeCompared.getClose(), compareNorm, baseNorm));
                }
            }
        }
    }
    
    /**
     * This function keeps the adjusting linear according to a norm
     */
    private final float linearAdjust(float value, float prevNorm, float postNorm) {
        return ((value - prevNorm) / prevNorm) * postNorm + postNorm;
    }

}
