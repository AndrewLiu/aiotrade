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
 * 
 * @author Caoyuan Deng
 */
public class MomentumBpLearner extends AbstractBpLearner {
    
    /** weight updated value vector: deltaW_ij(t) */
    private Vec deltaWeight;
    
    /** parameters */
    private double learningRate, momentumRate;
    
    public MomentumBpLearner(PerceptronNeuron neuron) {
        super(neuron);
        
        this.mode = Mode.Serial;
    }
    
    public void adapt(Double... args) {
        double learningRate = args[0];
        double momentumRate = args[1];
        
        adapt(learningRate, momentumRate);
    }
    
    /**
     * Adapt the weight using the delta rule.
     *
     * @param learningRate
     * @param momentumRate
     */
    public void adapt(double learningRate, double momentumRate) {
        if (deltaWeight == null) {
            deltaWeight = new DefaultVec(getNeuron().getInputDimension());
        }
        
        Vec weight = getNeuron().getWeight();
        Vec gradient = getSumGradient();
        
        for (int i = 0, n = getNeuron().getInputDimension(); i < n; i++) {
            double gradientTerm = gradient.get(i) * learningRate;
            double prevDeltaWeightTerm = deltaWeight.get(i) * momentumRate;
            
            deltaWeight.set(i, gradientTerm + prevDeltaWeightTerm);
            
            weight.set(i, weight.get(i) + deltaWeight.get(i));
        }
        
        /** this learner should reset gradient to 0 after adapt() is called each time */
        reset();
    }
    
    public String getLearnerName() {
        return "Momentum Leaner";
    }
    
    public void setOpts(Double... opts) {
        this.learningRate = opts[0];
        this.momentumRate = opts[1];
    }
    
}



