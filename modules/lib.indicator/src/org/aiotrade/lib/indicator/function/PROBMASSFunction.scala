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
package org.aiotrade.lib.indicator.function

import org.aiotrade.lib.math.StatisticFunction
import org.aiotrade.lib.math.timeseries.computable.Opt
import org.aiotrade.lib.math.timeseries.Ser
import org.aiotrade.lib.math.timeseries.Var

/**
 *
 * @author Caoyuan Deng
 */
object PROBMASSFunction {
    protected def probMass(idx:Int, var1:Var[Float], period:Float, nInterval:Float) :Array[Array[Float]] = {
        val begIdx = idx - period.intValue + 1
        val endIdx = idx

        StatisticFunction.probMass(var1.values, begIdx, endIdx, nInterval.intValue);
    }

    protected def probMass(idx:Int, var1:Var[Float], weight:Var[Float], period:Float, nInterval:Float) :Array[Array[Float]] = {
        val begIdx = idx - period.intValue + 1
        val endIdx = idx;

        StatisticFunction.probMass(var1.values, weight.values, begIdx, endIdx, nInterval.intValue)
    }
}

case class PROBMASSFunction extends AbstractFunction {
    import PROBMASSFunction._
    
    var period :Opt = _
    var nInterval :Opt = _
    var var1 :Var[Float] = _
    var weight :Var[Float] = _
    
    /**
     * as this function do not remember previous valus, do not need a Var as probMass
     */
    var _probMass:Array[Array[Float]] = _
    
    override
    def set(baseSer:Ser, args:Any*) :Unit = {
        super.set(baseSer)
        args match {
            case Seq(varX:Var[Float], weightX:Var[Float], periodX:Opt, nIntervalX:Opt) =>
                var1 = varX
                weight.equals(weightX)
                period.equals(periodX)
                nInterval.equals(nIntervalX)
        }
    }
    
    def idEquals(baseSer:Ser, args:Any*) :Boolean = {
        if (this._baseSer == baseSer) {
            args match {
                case Seq(varX:Var[Float], weightX:Var[Float], periodX:Opt, nIntervalX:Opt) =>
                    var1 == varX && weight == weightX && period == periodX && nInterval == nIntervalX
                case _ => false
            }
        } else false
    }

    protected def computeSpot(i:Int) :Unit = {
        if (weight == null) {
            
            _probMass = PROBMASSFunction.probMass(i, var1, period.value, nInterval.value);
            
        } else {
            
            _probMass = PROBMASSFunction.probMass(i, var1, weight, period.value, nInterval.value);
            
        }
    }
    
    /**
     * override compute(int), this function is not dependent on previous values
     */
    def compute(idx:Int) :Unit = {
        computeSpot(idx)
    }
    
    def probMass(sessionId:Long, idx:Int) :Array[Array[Float]] = {
        compute(idx)
        
        _probMass
    }
    
}




