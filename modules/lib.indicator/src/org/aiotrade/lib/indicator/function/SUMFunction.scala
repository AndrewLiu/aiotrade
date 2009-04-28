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
package org.aiotrade.lib.indicator.function;

import org.aiotrade.lib.math.StatisticFunction;
import org.aiotrade.lib.math.timeseries.computable.Factor;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
object SUMFunction {
    protected def isum(idx:Int, baseVar:Var[Float], period:Float, prev:Float) :Float = {
        StatisticFunction.isum(idx, baseVar.values, period.toInt, prev)
    }
}

class SUMFunction extends AbstractFunction {
    
    var period :Factor = _
    var baseVar :Var[Float] = _
    
    val _sum = Var[Float]()
    
    override
    def set(baseSer:Ser, args:Any*) :Unit = {
        super.set(baseSer)
        
        this.baseVar = args(0).asInstanceOf[Var[Float]]
        this.period = args(1).asInstanceOf[Factor]
    }
    
    protected def computeSpot(i:Int) :Unit = {
        if (i < period.value - 1) {
            
            _sum(i) = Float.NaN
            
        } else {
            
            _sum(i) = SUMFunction.isum(i, baseVar, period.value, _sum(i - 1))
            
        }
    }
    
    def sum(sessionId:Long, idx:int) :Float = {
        computeTo(sessionId, idx)
        
        _sum(idx)
    }
    
}





