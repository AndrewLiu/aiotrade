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

import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
class RSIFunction extends AbstractFunction {
    
    var period :Opt = _
    
    val _up = new DefaultVar[Float]
    val _dn = new DefaultVar[Float]
    
    val _rsi = new DefaultVar[Float]
    
    override
    def set(baseSer:Ser, args:Any*) :Unit = {
        super.set(baseSer)
        
        this.period = args(0).asInstanceOf[Opt]
    }
    
    def idEquals(baseSer:Ser, args:Any*) :Boolean = {
        this._baseSer == baseSer &&
        this.period.equals(args(0))
    }

    protected def computeSpot(i:Int) :Unit = {
        if (i == 0) {
            
            _up(i) = Float.NaN
            _dn(i) = Float.NaN
            
            _rsi(i) = Float.NaN
            
        } else {
            
            val change = C(i) - C(i - 1)
            if (change > 0) {
                _up(i) = change
                _dn(i) = 0f
            } else {
                _up(i) = 0f
                _dn(i) = -change
            }
            
            if (i < period.value - 1) {
                
                _rsi(i) = Float.NaN
            
            } else {
            
                val up_sum_i = sum(i, _up, period)
                val dn_sum_i = sum(i, _dn, period)
                
                _rsi(i) = if (up_sum_i + dn_sum_i == 0) 0f else up_sum_i / (up_sum_i + dn_sum_i) * 100f
            }
        }
    }
    
    def rsi(sessionId:Long, idx:int) :Float = {
        computeTo(sessionId, idx)
        
        _rsi(idx)
    }
    
}


