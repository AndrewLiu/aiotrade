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
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
object MAFunction {
    protected def ima(idx:Int, var1:Var[Float], period:Float, prev:Float) :Float = {
        return StatisticFunction.ima(idx, var1.values, period.toInt, prev)
    }
}

class MAFunction extends AbstractFunction {
    
    var period: Opt = _
    var var1: Var[Float] = _
    
    val _ma = new DefaultVar[Float]
    
    override
    def set(baseSer:Ser, args:Any*):Unit = {
        super.set(baseSer)
        args match {
            case Seq(varX:Var[Float], periodX:Opt) =>
                this.var1 = varX
                this.period = periodX
        }
    }
    
    def idEquals(baseSer:Ser, args:Any*) :Boolean = {
        this._baseSer == baseSer &&
        this.var1 == args(0) &&
        this.period == args(1)
    }

    protected def computeSpot(i:Int) :Unit = {
        if (i < period.value - 1) {
            
            _ma(i) = Float.NaN
            
        } else {
            
            _ma(i) = MAFunction.ima(i, var1, period.value, _ma(i - 1))
            
        }
    }
    
    
    def ma(sessionId:Long, idx:Int) :Float = {
        computeTo(sessionId, idx)
        
        _ma(idx)
    }
    
}



