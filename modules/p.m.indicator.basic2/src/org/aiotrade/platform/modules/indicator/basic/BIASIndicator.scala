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
package org.aiotrade.platform.modules.indicator.basic;

import org.aiotrade.lib.math.timeseries.Var;
import org.aiotrade.lib.math.timeseries.computable.Factor;
import org.aiotrade.lib.math.timeseries.plottable.Plot;
import org.aiotrade.lib.indicator.AbstractContIndicator;

/**
 *
 * @author Caoyuan Deng
 */
class BIASIndicator extends AbstractContIndicator {
    _sname = "BIAS"
    _lname = "Bias to Moving Average"
    
    val period1 = Factor("Period Short",   6)
    val period2 = Factor("Period Mediaum", 12)
    val period3 = Factor("Period Long",    24)
    
    val bias1 = TimeVar[Float]("BIAS1", Plot.Line)
    val bias2 = TimeVar[Float]("BIAS2", Plot.Line)
    val bias3 = TimeVar[Float]("BIAS3", Plot.Line)
    
    protected def computeCont(begIdx:Int) :Unit = {
        var i = begIdx
        while (i < _itemSize) {
            
            val ma1 = ma(i, C, period1)
            val ma2 = ma(i, C, period2)
            val ma3 = ma(i, C, period3)
            
            bias1(i) = (C(i) - ma1) / ma1 * 100f
            bias2(i) = (C(i) - ma2) / ma2 * 100f
            bias3(i) = (C(i) - ma3) / ma3 * 100f

            i += 1
        }
    }
    
}


