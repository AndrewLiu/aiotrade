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
package org.aiotrade.lib.math.timeseries.computable;

import org.aiotrade.lib.math.timeseries.computable.Opt.AbstractOpt;
import org.aiotrade.lib.util.serialization.JavaDocument;

/**
 * Class for defining indicator's Option
 *
 *
 * @author Caoyuan Deng
 * @Notice
 * If you use Option in indicator, please considerate AbstractIndicator.DefaultOpt
 * first, which will be added to Indicator's opts automatically when new it.
 */
public class Option extends AbstractOpt implements Opt {
    
    private float value;
    private float step;
    private float minValue;
    private float maxValue;
    
    public Option(final String name, final Number value) {
        this(name, value, null, null, null);
    }
    
    public Option(final String name, final Number value, final Number step) {
        this(name, value, step, null, null);
    }
    
    public Option(final String name, final Number value,
            final Number step, final Number minValue, final Number maxValue) {
        
        setName(name);
        this.value = value.floatValue();
        this.step = step == null ? 1.0f : step.floatValue();
        this.minValue = minValue == null ? -java.lang.Float.MAX_VALUE : minValue.floatValue();
        this.maxValue = maxValue == null ? +java.lang.Float.MAX_VALUE : maxValue.floatValue();
    }
    
    public void setValue(final Number value) {
        this.value = value.floatValue();
    }
    
    public float value() {
        return value;
    }
    
    public void setStep(final Number step) {
        this.step = step.floatValue();
    }
    
    public float getStep() {
        return step;
    }
    
    public void setMaxValue(final Number maxValue) {
        this.maxValue = maxValue.floatValue();
    }
    
    public float getMaxValue() {
        return maxValue;
    }
    
    public void setMinValue(final Number minValue) {
        this.minValue = minValue.floatValue();
    }
    
    public float getMinValue() {
        return minValue;
    }
    
    public String writeToJava(String id) {
        return
                JavaDocument.set(id, "setName", getName()) +
                JavaDocument.set(id, "setValue", value()) +
                JavaDocument.set(id, "setStep", getStep()) +
                JavaDocument.set(id, "setMinValue", getMinValue()) +
                JavaDocument.set(id, "setMaxValue", getMaxValue())
                ;
    }
    
}


