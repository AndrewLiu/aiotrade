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
package org.aiotrade.lib.math.timeseries;

import org.aiotrade.lib.math.timeseries.plottable.Plot;


/**
 * This is a horizotal view of DefaultSeries. Is' a reference of one of
 * the field vars.
 *
 * @author Caoyuan Deng
 */
public abstract class AbstractVar<E> implements Var<E> {
    
    public final static int LAYER_NOT_SET = -1;
    
    private String name;
    
    private Plot plot;
    
    private int layer = LAYER_NOT_SET;
    
    public AbstractVar() {
        this("", Plot.None);
    }
    
    public AbstractVar(String name) {
        this(name, Plot.None);
    }
    
    public AbstractVar(String name, Plot plot) {
        this.name =name;
        this.plot = plot;
    }
    
    public double[] toDoubleArray() {
        int length = size();
        double[] result = new double[length];
        
        if (length > 0 && get(0) instanceof Number) {
            for (int i = 0; i < length; i++) {
                result[i] = ((Number)get(i)).doubleValue();
            }
        }
        
        return result;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Plot getPlot() {
        return plot;
    }
    
    public void setPlot(Plot plot) {
        this.plot = plot;
    }
    
    public void setLayer(int layer) {
        this.layer = layer;
    }
    
    public int getLayer() {
        return layer;
    }
    
    public String toString() {
        return name;
    }
    
}



