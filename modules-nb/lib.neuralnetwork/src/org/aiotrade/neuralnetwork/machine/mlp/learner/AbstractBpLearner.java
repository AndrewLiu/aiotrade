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
package org.aiotrade.neuralnetwork.machine.mlp.learner;

import org.aiotrade.math.vector.DefaultVec;
import org.aiotrade.math.vector.Vec;
import org.aiotrade.neuralnetwork.machine.mlp.learner.Learner.Mode;
import org.aiotrade.neuralnetwork.machine.mlp.neuron.PerceptronNeuron;


/**
 * Backpropagation algorithm
 *
 * We define:
 *   delta: dE/dnet, is a real value
 *   gradient: dE/dW, is a vector
 * here,
 *   E is the error,
 *   net is the neuron's net input
 *   w is the weight vector
 *
 * @author Caoyuan Deng
 */
public abstract class AbstractBpLearner implements Learner {
    
    /**
     * defalut mode is Serial
     */
    protected Mode mode = Mode.Serial;
    
    private PerceptronNeuron neuron;
    
    /** differential coefficient of error to weight: (dE / dW), ie. gradient of E(W) */
    private Vec sumGradient;
    
    public AbstractBpLearner(PerceptronNeuron neuron) {
        this.neuron = neuron;
    }
    
    public PerceptronNeuron getNeuron() {
        return neuron;
    }
    
    public Mode getMode() {
        return mode;
    }
    
    /**
     * Compute gradient of error to weight (dE / dw_ij)
     * sum it if it's called again and again,
     * gradient should be set to zero after adpat() each time
     *
     * This should be called after computeDelaAs..()
     *
     * @NOTICE
     * gradient should has been computed (several times in batch mode, one time
     * in point by point mode) before adapt() to make sure the gradient has been
     * computed and summed (in batch mode).
     *
     * @param delta, (dE / dnet_ij)
     */
    public void computeGradientAndSumIt() {
        if (sumGradient == null) {
            sumGradient = new DefaultVec(neuron.getInputDimension());
        }
        
        /** compute and get neuron's gradient vector */
        Vec gradient = neuron.gradient();
        
        /** sum it */
        for (int i = 0, n = neuron.getInputDimension(); i < n; i++) {
            sumGradient.set(i, sumGradient.get(i) + gradient.get(i));
        }
    }
    
    /**
     * Reset leaner, here, we just set sumGradient to 0
     * This should be called after adapt() is called each time
     */
    protected void reset() {
        sumGradient.setAll(0);
    }
    
    public void computeDeltaAsOutputNeuron() {
        neuron.computerDeltaAsInOutputLayer();
    }
    
    /**
     * Compute the deltas using the subsequent layer deltas and the weight
     * emmanent from this neuron.
     * 
     * @return the value of this neuron's delta
     */
    public void computeDeltaAsHiddenNeuron() {
        neuron.computerDeltaAsInHiddenLayer();
    }
    
    protected Vec getSumGradient() {
        return sumGradient;
    }
    
    public String getLearnerName() {
        return "Delta Learner";
    }
    
    public void setOpts(Double... opts) {
        
    }
        
}



