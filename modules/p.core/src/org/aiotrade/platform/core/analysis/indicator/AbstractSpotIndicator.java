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

import org.aiotrade.lib.math.timeseries.computable.SpotComputable;
import org.aiotrade.lib.math.timeseries.SerItem;
import org.aiotrade.lib.math.timeseries.Ser;

/**
 * Abstract Profile Indicator
 * The indicator's factor is time - mulitiple values (time, and values in z plane),
 * The series usually contains only one SerItem instance of requried time.
 * 
 * 
 * 
 * 
 * 
 * @author Caoyuan Deng
 */
@IndicatorName("Abstract Spot Indicator")
public abstract class AbstractSpotIndicator extends AbstractIndicator implements SpotComputable {
    
    protected long spotTime = -Integer.MAX_VALUE;
    
    public AbstractSpotIndicator() {
        super();
    }
    
    public AbstractSpotIndicator(Ser baseSer) {
        super(baseSer);
    }
    
    public void setSpotTime(long time) {
        this.spotTime = time;
    }
    
    public SerItem computeItem(long time) {
        
        /** get masterIndex before preCalc(), which may clear this data */
        int baseIdx = _baseSer.indexOfOccurredTime(time);
        
        preComputeFrom(time);
        
        SerItem newItem = computeSpot(time, baseIdx);
        
        setSpotTime(time);
        
        postComputeFrom();
        
        return newItem;
    }
    
    protected void computeCont(int begIdx) {
        for (int i = begIdx; i < _itemSize; i++) {
            long time = _baseSer.timestamps().get(i);
            if (time == spotTime) {
                computeSpot(time, i);
            }
        }
    }
    
    protected abstract SerItem computeSpot(long time, int baseIdx);
    
    
}

