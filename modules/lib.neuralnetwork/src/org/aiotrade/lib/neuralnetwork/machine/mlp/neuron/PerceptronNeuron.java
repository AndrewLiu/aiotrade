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
package org.aiotrade.lib.neuralnetwork.machine.mlp.neuron;

import org.aiotrade.lib.neuralnetwork.core.model.Neuron;
import org.aiotrade.lib.neuralnetwork.machine.mlp.learner.AbstractBpLearner;
import org.aiotrade.lib.neuralnetwork.machine.mlp.learner.RpropBpLearner;
import java.util.List;
import org.aiotrade.lib.math.vector.DefaultVec;
import org.aiotrade.lib.math.vector.Vec;

/**
 * Perceptron Neuron
 *
 * As y is the output, w is the weight vector, x is the input vector, the function
 * form of perceptron is written as:
 *
 * y = <w * x> + b,
 * here, b is called 'bias' in this form of function
 *
 * or,
 * y = <w * x> - theta
 * here, theta = -b, theta is called 'threshold' in this form of fucntion.
 *
 * @author Caoyuan Deng
 */
public abstract class PerceptronNeuron extends Neuron {
    
    public final static double THRESHOLD_INPUT_VALUE = -1.0;
    
    private Vec weight;
    
    /** delta: dE/dnet, is a real value */
    private double delta;
    
    /** differential coefficient of error to weight: (dE / dW), ie. gradient of E(W) */
    private Vec gradient;
    
    private AbstractBpLearner learner;
    
    private double minInitWeightValue = Double.NaN; //-0.05;
    
    private double maxInitWeightValue = Double.NaN; //0.05;
    
    
    @Override
    public void init(int inputDimensionWithoutThreshold, boolean hidden) {
        /** add a dimension for threshold */
        int inputDimensionWithThreshold = inputDimensionWithoutThreshold + 1;
        
        super.init(inputDimensionWithThreshold, hidden);
        
        if (inputDimensionWithoutThreshold > 0) {
            initWeight();
            initLearner();
        }
    }
    
    private void initLearner() {
        setLearner(new RpropBpLearner(this));
//        setLearner(new MomentumBpLearner(this));
    }
    
    /**
     *
     *
     * @NOTICE
     * If initial weight value is too large, the action function may work at
     * saturated status.
     * If initial weight value is too small, the action function may work at a
     * <b>same</b> local minimum each time, this may cause the result is almost
     * always the same.
     */
    private void initWeight() {
        this.weight = new DefaultVec(getInputDimension());
        
        if (Double.isNaN(minInitWeightValue) && Double.isNaN(maxInitWeightValue)) {
            
            maxInitWeightValue = +1.0 / Math.sqrt(getInputDimension());
            minInitWeightValue = -1.0 * maxInitWeightValue;
            
        } else if (Double.isNaN(minInitWeightValue) && !Double.isNaN(maxInitWeightValue)) {
            
            minInitWeightValue = -1.0 * maxInitWeightValue;
            
        } else if (Double.isNaN(maxInitWeightValue) && !Double.isNaN(minInitWeightValue)) {
            
            maxInitWeightValue = -1.0 * minInitWeightValue;
            
        }
        
        weight.randomize(minInitWeightValue, maxInitWeightValue);
        
        /**
         * threhold's initial weight value should also be set to a very small value.
         * but do not set to 0, which will cause no adapting of its weight value.
         *
         * As the threshold's input value keeps -1.0, it's better to initial its
         * weight value to a very small real value, so we multiple it 0.1, which
         * cause it's less than the other input's weights in 10 scale:
         *
         *   weight.set(THRESHOLD_DIMENSION_IDX, weight.get(THRESHOLD_DIMENSION_IDX) * 0.001);
         *
         * Do we need to do this? a bit large weight for threhold may also be useful
         * to adapt it's value faster and priorly (it's better to adapt threshold's
         * weight priorly than inputs' weight)
         *
         * Actually we can think the weight of threshold as the input, the -1 threhold
         * as weight (value = -1), so we only need to keep threshold as a small value
         * between [-1, 1]. which has the same scale level of initial value as other
         * inputs.
         *
         */
    }
    
    @Override
    public void setInput(Vec inputWithoutThreshold) {
        /** add a dimension for threshold */
        int inputDimensionWithThreshold = inputWithoutThreshold.dimension() + 1;
        
        if (input == null || input.dimension() != inputDimensionWithThreshold) {
            input = new DefaultVec(inputDimensionWithThreshold);
        }
        
        /** set threshold input value (always be -1) */
        input.set(getThreholdDimensionIdx(), THRESHOLD_INPUT_VALUE);
        
        /** As threshold's dimension idx is 0, so we copy to store beginning with 1 */
        input.copy(inputWithoutThreshold, 0, 1, inputWithoutThreshold.dimension());
    }
    
    private final int getThreholdDimensionIdx() {
        /**
         * @NOTICE
         * We assume the idx of threshold dimension in input vector is 0,
         * Don't change it to other value.
         *
         * @see setInput(Vec inputWithoutThreshold)
         */
        return 0;
    }
    
    /**
     *
     * @return the weigths of this neuron
     */
    public Vec getWeight() {
        return weight;
    }
    
    /**
     * @param weight
     */
    public void setWeight(Vec weight) {
        this.weight = weight;
    }
    
    public double getDelta() {
        return delta;
    }
    
    public void setLearner(AbstractBpLearner learner) {
        this.learner = learner;
    }
    
    public AbstractBpLearner getLearner() {
        return learner;
    }
    
    public void adapt(double learningRate, double momentumRate) {
        learner.adapt(learningRate, momentumRate);
    }
    
    /**
     *
     * The output of neuron's activation.
     *
     * It may has another form:
     *   f(net(input)) - threshold,
     * if we do not define threshold as a input dimension that weighted -1
     *
     */
    protected double activation() {
        return f(net());
    }
    
    /**
     * @return net input value of this neuron 
     */
    protected double net() {
        return input.innerProduct(weight);
    }
    
    public void computerDeltaAsInOutputLayer() {
        delta = (getExpectedOutput() - output()) * df(net());
    }
    
    /**
     * Compute the deltas using the subsequent layer deltas and the weights
     * to connected neurons.
     * 
     */
    public void computerDeltaAsInHiddenLayer() {
        delta = getWeightToConnectedNeurons().innerProduct(getDeltaOfConnectedNeurons()) * df(net());
    }
    
    public Vec gradient() {
        if (gradient == null || gradient.dimension() != getInputDimension()) {
            gradient = new DefaultVec(getInputDimension());
        }
        
        for (int i = 0, n = getInputDimension(); i < n; i++) {
            /** gradient (dE / dW_ij) = input_ij * delta */
            gradient.set(i, input.get(i) * delta);
        }
        
        return gradient;
    }
    
    private Vec buf_deltaOfConnectedNeurons;
    private Vec getDeltaOfConnectedNeurons() {
        if (buf_deltaOfConnectedNeurons == null || buf_deltaOfConnectedNeurons.dimension() != getNConnectedNeurons()) {
            buf_deltaOfConnectedNeurons = new DefaultVec(getNConnectedNeurons());
        }
        
        List<Neuron> connectedNeurons = getConnectedNeurons();
        for (int i = 0, n = getNConnectedNeurons(); i < n; i++) {
            PerceptronNeuron connectedNeuron = (PerceptronNeuron)connectedNeurons.get(i);
            buf_deltaOfConnectedNeurons.set(i, connectedNeuron.getDelta());
        }
        
        return buf_deltaOfConnectedNeurons;
    }
    
    /**
     * We should re-organize the weight from the connected neurons in next layer
     *  Layer(i)      nextLayer(j)
     * ---------      ------------
     *
     *  (0) bias-----------+
     *                     |
     *  (1) neuron------+  |
     *                  V  V
     *  (2) neuron----> neuron       (*) index in weight of connectedNeuron
     *                  ^
     *  (n) neuron------+
     */
    private Vec buf_weightToConnectedNeurons;
    private Vec getWeightToConnectedNeurons() {
        if (buf_weightToConnectedNeurons == null || buf_weightToConnectedNeurons.dimension() != getNConnectedNeurons()) {
            buf_weightToConnectedNeurons = new DefaultVec(getNConnectedNeurons());
        }
        
        List<Neuron> connectedNeurons = getConnectedNeurons();
        /**
         * @NOTICE
         * We should consider the weight of connectedNeuron's threshold.
         * which is of the idx of 0, so the neurons' weight start from 1,
         * so, we should use weight.get(1 + i) here.
         **/
        for (int i = 0, n = getNConnectedNeurons(); i < n; i++) {
            PerceptronNeuron connectedNeuron = (PerceptronNeuron)connectedNeurons.get(i);
            
            double weightToConnectedNeuron = 0;
            if (i < connectedNeuron.getThreholdDimensionIdx()) {
                weightToConnectedNeuron = connectedNeuron.getWeight().get(i);
            } else {
                weightToConnectedNeuron = connectedNeuron.getWeight().get(i + 1);
            }
            
            buf_weightToConnectedNeurons.set(i, weightToConnectedNeuron);
        }
        
        return buf_weightToConnectedNeurons;
    }
    
    public double getMaxInitWeightValue() {
        return this.maxInitWeightValue;
    }
    
    public void setMaxInitWeightValue(double maxInitWeightValue) {
        this.maxInitWeightValue = maxInitWeightValue;
    }
    
    public double getMinInitWeightValue() {
        return this.minInitWeightValue;
    }
    
    public void setMinInitWeightValue(double minInitWeightValue) {
        this.minInitWeightValue = minInitWeightValue;
    }
    
    public abstract double f(double x);
    
    public abstract double df(double x);
    
}