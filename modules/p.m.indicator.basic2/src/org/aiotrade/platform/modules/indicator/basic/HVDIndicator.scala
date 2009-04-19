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
package org.aiotrade.platform.modules.indicator.basic

import org.aiotrade.lib.math.timeseries.SerItem
import org.aiotrade.lib.math.timeseries.Var
import org.aiotrade.lib.math.timeseries.computable.Factor
import org.aiotrade.lib.math.timeseries.plottable.Plot
import org.aiotrade.lib.indicator.AbstractSpotIndicator

/**
 *
 * @author Caoyuan Deng
 */
class HVDIndicator extends AbstractSpotIndicator {
    _sname = "HVD"
    _lname = "Historical Volume Distribution"
    _overlapping = true
    
    val nIntervals = Factor("Number of Intervals", 30.0, 1.0, 1.0, 100.0)
    val period1    = Factor("Period1",  50.0)
    val period2    = Factor("Period2",  100.0)
    val period3    = Factor("Period3",  200.0)
    
    val HVD1 = TimeVar[Array[Array[Float]]]("HVD1", Plot.Profile)
    val HVD2 = TimeVar[Array[Array[Float]]]("HVD2", Plot.Profile)
    val HVD3 = TimeVar[Array[Array[Float]]]("HVD3", Plot.Profile)
    
    def computeSpot(time:Long, baseIdx:Int) :SerItem = {
        val item = createItemOrClearIt(time)
        
        val probability_mass1 = probMass(baseIdx, C, V, period1, nIntervals)
        val probability_mass2 = probMass(baseIdx, C, V, period2, nIntervals)
        val probability_mass3 = probMass(baseIdx, C, V, period3, nIntervals)
        
        item.set(HVD1, probability_mass1)
        item.set(HVD2, probability_mass2)
        item.set(HVD3, probability_mass3)
        
        item
    }
}







