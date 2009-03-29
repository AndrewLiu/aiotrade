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
package org.aiotrade.neuralnetwork.datasource;

import org.aiotrade.math.vector.InputOutputPoint;
import org.aiotrade.math.vector.InputOutputPointSet;
import org.aiotrade.math.vector.Vec;

/**
 * 
 * @author Caoyuan Deng
 */
public abstract class DataSource {
    
    private int nNetworks;
    
    private InputOutputPointSet[] trainingPointSets;
    private InputOutputPointSet[] validatingPointSets;
    
    private int inputDimension;
    private int outputDimension;
    
    public DataSource() {
    }
    
    public DataSource(int nNetworks, int inputDimension, int outputDimension) {
        init(nNetworks, inputDimension, outputDimension);
    }
    
    public void init(int nNetworks, int inputDimension, int outputDimension) {
        this.nNetworks = nNetworks;
        this.inputDimension = inputDimension;
        this.outputDimension = outputDimension;
        
        this.trainingPointSets = new InputOutputPointSet[nNetworks];
        this.validatingPointSets = new InputOutputPointSet[nNetworks];
    }
    
    /**
     * Return the number of times the networks will be run. 
     * 
     * @return total number of networks in running
     */
    public int getNNetworks() {
        return nNetworks;
    }
    
    public void setNNetworks(int nNetworks) {
        this.nNetworks = nNetworks;
    }
    
    /**
     * Read a training points from the environment.
     *
     *
     * @param networkIdx  The index of the network that will run
     * @return the training points as an array of InputOutputPoint
     * @throws Exception
     */
    public abstract InputOutputPoint[] getTrainingPoints(int networkIdx) throws Exception;
    
    public InputOutputPointSet getTrainingPointSet(int networkIdx) {
        return trainingPointSets[networkIdx];
    }
    
    public void setTrainingPointSet(int networkIdx, InputOutputPointSet trainingPointSet) {
        this.trainingPointSets[networkIdx] = trainingPointSet;
    }
    
    public InputOutputPointSet getValidatingPointSet(int networkIdx) {
        return validatingPointSets[networkIdx];
    }
    
    public void setValidatingPointSet(int networkIdx, InputOutputPointSet validatingPointSet) {
        this.validatingPointSets[networkIdx] = validatingPointSet;
    }
    
    /**
     * Read validating points from the source. only input vector will be read.
     *
     * @param  networkIdx the networkIdx of the network that to run
     * @return Validating points set
     * @throws Exception
     */
    public abstract Vec[] getValidatingInputs(int networkIdx) throws Exception;
    
    public abstract void writeResults(Vec[] results, int networkIdx) throws Exception;
    
    public abstract void checkValidation() throws Exception;
    
    public int getInputDimension() {
        return inputDimension;
    }
    
    public int getOutputDimension() {
        return outputDimension;
    }
    
    public void setInputDimension(int inputDimension) {
        this.inputDimension = inputDimension;
    }
    
    public void setOutputDimension(int outputDimension) {
        this.outputDimension = outputDimension;
    }
    
}