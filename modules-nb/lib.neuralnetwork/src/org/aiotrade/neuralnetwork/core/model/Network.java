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

import org.aiotrade.math.vector.InputOutputPointSet;
import org.aiotrade.math.vector.Vec;
import org.aiotrade.neuralnetwork.core.NetworkChangeEvent;
import org.aiotrade.neuralnetwork.core.NetworkChangeListener;
import org.aiotrade.neuralnetwork.core.descriptor.NetworkDescriptor;

/**
 * A neural network
 * 
 * @author Caoyuan Deng
 */
public interface Network {
    
    public void init(NetworkDescriptor descriptor) throws Exception;
    
    /**
     * one learning step to learn one of the training points.
     *
     * @param input  The input vector.
     * @param output The desired output vector.
     *
     * @return the error of this learning step.
     */
    public double learnOnePoint(Vec input, Vec output);
    
    /**
     * Compute a network prediction
     *
     * @param input The input to propagate.
     * @return the network output.
     */
    public Vec predict(Vec input);
    
    /**
     * Train the network until the stop criteria is met.
     *
     * @param iop  The training set to be learned.
     */
    public void train(InputOutputPointSet iops);
    
    public int getInputDimension();
    
    public int getOutputDimension();
    
    public NetworkDescriptor cloneDescriptor();
    
    public String getNeuralNetworkName();
    
    public boolean isInAdapting();
    
    public void setInAdapting(boolean b);
    
    public void addNetWorkChangeListener(NetworkChangeListener listener);
    
    public void removeNetworkChangeListener(NetworkChangeListener listener);
    
    public void fireNetworkChangeEvent(NetworkChangeEvent evt);
    
    
}
