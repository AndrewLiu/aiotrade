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
import java.util.List;
import java.util.TimeZone;

/**
 *
 * @author Caoyuan Deng
 */
public class QuoteSerCombiner {
    private QuoteSer sourceQuoteSer;
    private QuoteSer targetQuoteSer;
    private TimeZone timeZone = TimeZone.getDefault();
    
    private SerChangeListener sourceSerChangelistener;
    
    public QuoteSerCombiner(QuoteSer sourceQuoteSer, QuoteSer targetQuoteSer) {
        this.sourceQuoteSer = sourceQuoteSer;
        this.targetQuoteSer = targetQuoteSer;
        
        sourceSerChangelistener = new SerChangeListener() {
            public void serChanged(SerChangeEvent evt) {
                if (evt.getType() == SerChangeEvent.Type.FinishedComputing) {
                    computeCont(evt.getBeginTime());
                }
            }
        };
        
        this.sourceQuoteSer.addSerChangeListener(sourceSerChangelistener);
    }
    
    public void computeFrom(long fromTime) {
        computeCont(fromTime);
    }
    
    /**
     * Combine data according to wanted frequency, such as Weekly, Monthly etc.
     */
    protected void computeCont(long fromTime) {
        Unit targetUnit = targetQuoteSer.getFreq().unit;
        
        long masterFromTime = targetUnit.beginTimeOfUnitThatInclude(fromTime, timeZone);
        int  masterFromIdx  = sourceQuoteSer.timestamps().indexOfNearestOccurredTimeBehind(masterFromTime);
        masterFromIdx = (masterFromIdx < 0) ?
            0 : masterFromIdx;
        
        //System.out.println("myFromIdx: " + myFromIdx + " materFromIdx: " + masterFromIdx);
        //targetQuoteSer.clear(myFromTime);
        
        /** begin combining: */
        
        Calendar calendar = Calendar.getInstance();
        
        List<SerItem> sourceItemList = sourceQuoteSer.itemList();
        
        int size = sourceItemList.size();
        for (int i = masterFromIdx; i < size;) {
            QuoteItem item_i = (QuoteItem)sourceItemList.get(i);
            
            long time_i = item_i.getTime();
            
            if (time_i < masterFromTime) {
                continue;
            }
            
            long intervalBegin = targetUnit.beginTimeOfUnitThatInclude(time_i, timeZone);
            
            calendar.setTimeInMillis(intervalBegin);
            int currWeekOfYear  = calendar.get(Calendar.WEEK_OF_YEAR);
            int currMonthOfYear = calendar.get(Calendar.MONTH);
            int currYear        = calendar.get(Calendar.YEAR);
            
            QuoteItem targetItem = (QuoteItem)targetQuoteSer.createItemOrClearIt(intervalBegin);
            
            float prevNorm = item_i.getClose();
            float postNorm = item_i.getClose_Adj();
            
            targetItem.setOpen    (linearAdjust(item_i.getOpen(),  prevNorm, postNorm));
            targetItem.setHigh    (-Float.MAX_VALUE);
            targetItem.setLow     (+Float.MAX_VALUE);
            targetItem.setVolume  (0);
            
            /** compose followed source data of this interval to targetData */
            int j = 0;
            while (i + j < size) {
                QuoteItem item_j = (QuoteItem)sourceItemList.get(i + j);
                long time_j = item_j.getTime();
                
                calendar.setTimeInMillis(time_j);
                
                boolean inSameInterval = true;
                switch (targetUnit) {
                    case Week:
                        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
                        inSameInterval = (weekOfYear == currWeekOfYear);
                        break;
                    case Month:
                        int monthOfYear = calendar.get(Calendar.MONTH);
                        int year = calendar.get(Calendar.YEAR);
                        inSameInterval = (year == currYear && monthOfYear == currMonthOfYear);
                        break;
                    default:
                }
                
                /**
                 * @NOTICE
                 * There is strange behave of JDK's Calendar when a very big Long 
                 * timeInMillis assigned to it. so anyway, we let inSamePeriod = true
                 * if j == 0, because jdata is the same data as idata in this case: 
                 */
                inSameInterval = (j == 0) ?
                    true : inSameInterval;
                
                if (!inSameInterval) {
                    break;
                }
                
                /**
                 * @TIPS
                 * when combine, do adjust on source's value, then de adjust on combined quote data.
                 * this will prevent bad high, open, and low into combined quote data:
                 *
                 * Duaring the combining period, an adjust may happened, but we only record last
                 * close_adj, the high, low, and open of the data before adjusted acutally may has
                 * different scale close_adj, so must do adjust with its own close_adj firstly. then
                 * use the last close_orj to de-adjust it.
                 */
                prevNorm = item_j.getClose();
                postNorm = item_j.getClose_Adj();
                
                targetItem.setHigh   (Math.max(targetItem.getHigh(), linearAdjust(item_j.getHigh(),  prevNorm, postNorm)));
                targetItem.setLow    (Math.min(targetItem.getLow(),  linearAdjust(item_j.getLow(),   prevNorm, postNorm)));
                targetItem.setClose  (linearAdjust(item_j.getClose(),   prevNorm, postNorm));
                
                targetItem.setVolume(targetItem.getVolume() + item_j.getVolume());
                
                targetItem.setClose_Ori(item_j.getClose_Ori());
                targetItem.setClose_Adj(item_j.getClose_Adj());
                
                j++;
            }
            
            /** de adjust on combined quote data */
            
            prevNorm = targetItem.getClose();
            postNorm = targetItem.getClose_Ori();
            
            targetItem.setHigh   (linearAdjust(targetItem.getHigh(),  prevNorm, postNorm));
            targetItem.setLow    (linearAdjust(targetItem.getLow(),   prevNorm, postNorm));
            targetItem.setOpen   (linearAdjust(targetItem.getOpen(),  prevNorm, postNorm));
            targetItem.setClose  (linearAdjust(targetItem.getClose(), prevNorm, postNorm));
            
            i += j;
        }
        
        SerChangeEvent evt = new SerChangeEvent(targetQuoteSer, SerChangeEvent.Type.Updated, null, masterFromTime, targetQuoteSer.lastOccurredTime());
        targetQuoteSer.fireSerChangeEvent(evt);
        
    }
    
    /**
     * This function keeps the adjusting linear according to a norm
     */
    private final float linearAdjust(float value, float prevNorm, float postNorm) {
        return ((value - prevNorm) / prevNorm) * postNorm + postNorm;
    }
    
    public void dispose() {
        if (sourceSerChangelistener != null) {
            sourceQuoteSer.removeSerChangeListener(sourceSerChangelistener);
        }
    }
}
