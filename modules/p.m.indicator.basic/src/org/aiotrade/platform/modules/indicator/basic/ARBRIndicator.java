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
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.plottable.Plot;
import org.aiotrade.lib.indicator.AbstractContIndicator;
import org.aiotrade.lib.indicator.AbstractIndicator.DefaultOpt;
import org.aiotrade.lib.indicator.IndicatorName;

/**
 *
 * @author Caoyuan Deng
 */
@IndicatorName("AR/BR")
public class ARBRIndicator extends AbstractContIndicator {
    
    Opt period = new DefaultOpt("Period", 10.0);
    
    Var<Float> up = new DefaultVar("up");
    Var<Float> dn = new DefaultVar("dn");
    Var<Float> bs = new DefaultVar("bs");
    Var<Float> ss = new DefaultVar("ss");
    
    Var<Float> ar = new DefaultVar("AR", Plot.Line);
    Var<Float> br = new DefaultVar("BR", Plot.Line);
    
    {
        _sname = "AR/BR";
        _grids = new Float[] {50f, 200f};
    }
    
    protected void computeCont(int begIdx) {
        for (int i = begIdx; i < _itemSize; i++) {
            
            up.set(i, H.get(i) - O.get(i));
            float up_sum_i = sum(i, up, period);
            
            dn.set(i, O.get(i) - L.get(i));
            float dn_sum_i = sum(i, dn, period);
            
            ar.set(i, up_sum_i / dn_sum_i * 100);
            
            float bs_tmp = H.get(i) - C.get(i);
            bs.set(i, Math.max(0, bs_tmp));
            float bs_sum_i = sum(i, bs, period);
            
            float ss_tmp = C.get(i) - L.get(i);
            ss.set(i, Math.max(0, ss_tmp));
            float ss_sum_i = sum(i, ss, period);
            
            br.set(i, bs_sum_i / ss_sum_i * 100);
        }
    }
    
}



