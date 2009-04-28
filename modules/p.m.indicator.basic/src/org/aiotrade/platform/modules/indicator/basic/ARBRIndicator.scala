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

import org.aiotrade.lib.math.timeseries.{Ser,Var}
import org.aiotrade.lib.math.timeseries.computable.Factor;
import org.aiotrade.lib.math.timeseries.plottable.Plot;
import org.aiotrade.lib.indicator.ContIndicator;

/**
 *
 * @author Caoyuan Deng
 */
class ARBRIndicator extends ContIndicator {
    _sname = "AR/BR"
    _grids = Array(50f, 200f)
    
    val period = Factor("Period", 10)
    
    val up = Var[Float]("up")
    val dn = Var[Float]("dn")
    val bs = Var[Float]("bs")
    val ss = Var[Float]("ss")
    
    val ar = Var[Float]("AR", Plot.Line)
    val br = Var[Float]("BR", Plot.Line)
    
    
    protected def computeCont(begIdx:Int, size:Int) :Unit = {
        var i = begIdx
        while (i < size) {
            up(i) = H(i) - O(i)
            val up_sum_i = sum(i, up, period)
            
            dn(i) = O(i) - L(i)
            val dn_sum_i = sum(i, dn, period)
            
            ar(i) = up_sum_i / dn_sum_i * 100
            
            val bs_tmp = H(i) - C(i)
            bs(i) = Math.max(0, bs_tmp)
            val bs_sum_i = sum(i, bs, period)
            
            val ss_tmp = C(i) - L(i)
            ss(i) = Math.max(0, ss_tmp)
            val ss_sum_i = sum(i, ss, period)
            
            br(i) = bs_sum_i / ss_sum_i * 100

            i += 1
        }
    }
    
}



