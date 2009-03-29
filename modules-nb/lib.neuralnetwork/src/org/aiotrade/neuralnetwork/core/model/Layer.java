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
 * 
 * @author Caoyuan Deng
 */
public class Layer {
    
    protected boolean inAdapting;
    
    protected int inputDimension;
    
    private Layer nextLayer;
    
    private List<Neuron> neurons = new ArrayList();
    
    
    protected Layer() {
    }
    
    public Layer(Layer nextLayer, int inputDimension) {
        this();
        this.nextLayer = nextLayer;
        this.inputDimension = inputDimension;
    }
    
    public void connectTo(Layer nextLayer) {
        this.nextLayer = nextLayer;
        
        for (Neuron neuron : getNeurons()) {
            neuron.connectTo(nextLayer.getNeurons());
        }
    }
    
    protected Vec getNeuronsActivation() {
        Vec result = new DefaultVec(getNNeurons());
        
        for (int i = 0; i < getNNeurons(); i++) {
            result.set(i, neurons.get(i).output());
        }
        
        return result;
    }
    
    public int getInputDimension() {
        return inputDimension;
    }
    
    public void setNeurons(List<Neuron> neurons) {
        this.neurons = neurons;
    }
    
    public void setNextLayer(Layer nextLayer) {
        this.nextLayer = nextLayer;
    }
    
    public List<Neuron> getNeurons() {
        return neurons;
    }
    
    public Layer getNextLayer() {
        return nextLayer;
    }
    
    public boolean isInAdapting() {
        return this.inAdapting;
    }
    
    
    public Vec getNeuronsOutput() {
        return getNeuronsActivation();
    }
    
    public void propagateToNextLayer() {
        if (nextLayer != null) {
            nextLayer.setInputToNeurons(getNeuronsOutput());
        }
    }
    
    public void reset() {
        for (Neuron neuron : getNeurons()) {
            neuron.reset();
        }
    }
    
    public void setInAdapting(boolean b) {
        this.inAdapting = b;
    }
    
    
    public void setInputToNeurons(Vec input) {
        for (Neuron neuron : getNeurons()) {
            neuron.setInput(input);
        }
    }
    
    public void setExpectedOutputToNeurons(Vec expectedOutput) {
        List<Neuron> neurons = getNeurons();
        for (int i = 0, n = getNNeurons(); i < n; i++) {
            Neuron neuron = neurons.get(i);
            
            neuron.setExpectedOutput(expectedOutput.get(i));
        }
    }
    
    protected void setInputDimension(int inputDimension) {
        this.inputDimension = inputDimension;
    }
    
    public int getNNeurons() {
        return neurons.size();
    }
    
    
    public void addNeuron(Neuron neuron) {
        neurons.add(neuron);
    }
    
}