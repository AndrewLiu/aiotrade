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
package org.aiotrade.neuralnetwork.core.model;

import org.aiotrade.math.vector.DefaultVec;
import org.aiotrade.math.vector.Vec;
import java.util.ArrayList;
import java.util.List;

/**
 * Neuron.
 *
 * @author Caoyuan Deng
 */
public abstract class Neuron {
    
    /** If true, hidden node, if false, output node */
    private boolean hidden = true;
    
    private List<Neuron> connectedNeurons = new ArrayList<Neuron>();
    
    protected Vec input;
    
    private double expectedOutput;
    
    public Neuron() {
        /** do nothing, need to call init() before use it */
    }
    
    public Neuron(int inputDimension) {
        init(inputDimension, true);
    }
    
    public Neuron(int inputDimension, boolean hidden) {
        init(inputDimension, hidden);
    }
    
    public void init(int inputDimension, boolean hidden) {
        this.input = new DefaultVec(inputDimension);
        this.hidden = hidden;
    }
    
    public int getInputDimension() {
        return input.dimension();
    }
    
    
    /**
     * reset the current activation. Such as: remove the current input.
     */
    public void reset() {
        input = null;
    }
    
    public void setInput(int idx, double value) {
        input.set(idx, value);
    }
    
    public Vec getInput() {
        return input;
    }
    
    public void setInput(Vec source) {
        if (this.input == null || this.input.dimension() != source.dimension()) {
            this.input = new DefaultVec(source.dimension());
        }
        
        /**
         * As input may be modified by this neuron (such as setInput(int, double)),
         * other class may not know about this, so we'd better to just copy it.
         */
        this.input.copy(source);
    }

    public void setExpectedOutput(double expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public double getExpectedOutput() {
        return expectedOutput;
    }
    
    public double output() {
        return activation();
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public void setHidden(boolean b) {
        this.hidden = b;
    }
    
    public void connectTo(List<Neuron> neurons) {
        for (Neuron neuron : neurons) {
            connectTo(neuron);
        }
    }
    
    public void connectTo(Neuron neuron) {
        connectedNeurons.add(neuron);
    }
    
    public List<Neuron> getConnectedNeurons() {
        return connectedNeurons;
    }
    
    public int getNConnectedNeurons() {
        return connectedNeurons.size();
    }
    
    protected abstract double activation();
    
}