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
package org.aiotrade.lib.neuralnetwork.machine.mlp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.aiotrade.lib.math.vector.InputOutputPointSet;
import org.aiotrade.lib.math.vector.Vec;
import org.aiotrade.lib.util.Argument;
import org.aiotrade.lib.neuralnetwork.core.NetworkChangeEvent;
import org.aiotrade.lib.neuralnetwork.core.descriptor.LayerDescriptor;
import org.aiotrade.lib.neuralnetwork.core.descriptor.NetworkDescriptor;
import org.aiotrade.lib.neuralnetwork.core.model.AbstractNetwork;
import org.aiotrade.lib.neuralnetwork.core.model.Layer;

/**
 * Multi-Layer Perceptron network.
 *
 * @author Caoyuan Deng
 */
public class MlpNetwork extends AbstractNetwork {
    
    private static final Logger log = Logger.getLogger(MlpNetwork.class.getName());
    
    protected MlpNetworkDescriptor descriptor;
    
    protected List<MlpLayer> layers = new ArrayList<MlpLayer>();
    
    protected Arg arg;
    
    public MlpNetwork() {
        super();
    }
    
    public void init(NetworkDescriptor descriptor) throws Exception {
        this.descriptor = (MlpNetworkDescriptor)descriptor;
        
        /** setup the network layers */
        
        layers.clear();
        
        LayerDescriptor firstLayerDescriptor = this.descriptor.getLayerDescriptors().get(0);
        MlpLayer firstLayer = null;
        firstLayer = new MlpHiddenLayer(
                null,
                descriptor.getDataSource().getInputDimension(),
                firstLayerDescriptor.getNNeurons(),
                firstLayerDescriptor.getNeuronClassName());
        
        layers.add(firstLayer);
        
        MlpLayer currLayer = null;
        for (int i = 1, n = descriptor.getNLayers(); i < n; i++) {
            LayerDescriptor currLayerDescriptor = this.descriptor.getLayerDescriptors().get(i);
            
            if (i == n - 1) {
                /** output layer */
                currLayer = new MlpOutputLayer(
                        layers.get(i - 1).getNNeurons(),
                        currLayerDescriptor.getNNeurons(),
                        currLayerDescriptor.getNeuronClassName());
                
            } else {
                /** hidden layer */
                currLayer = new MlpHiddenLayer(
                        null,
                        layers.get(i - 1).getNNeurons(),
                        currLayerDescriptor.getNNeurons(),
                        currLayerDescriptor.getNeuronClassName());
                
            }
            
            layers.add(currLayer);
            
            MlpLayer backLayer = layers.get(i - 1);
            backLayer.connectTo(currLayer);
        }
        
        setParam((Arg)descriptor.getArg());
    }

    public Arg getParam() {
        return arg;
    }

    public void setParam(Arg param) {
        this.arg = param;
    }
    
    public List<MlpLayer> getLayers() {
        return layers;
    }
    
    public String getNeuralNetworkName() {
        return "Multi-Layer Perceptron";
    }
    
    public int getInputDimension() {
        return layers.get(0).getInputDimension();
    }
    
    public int getNLayers() {
        return layers.size();
    }
    
    public int getOutputDimension() {
        return layers.get(layers.size() - 1).getNNeurons();
    }
    
    public Vec predict(Vec input) {
        propagate(input);
        
        return output();
    }
    
    protected Vec output() {
        Layer outputLayer = layers.get(layers.size() - 1);
        
        return outputLayer.getNeuronsOutput();
    }
    
    protected void propagate(Vec input) {
        Layer firstLayer = layers.get(0);
        
        firstLayer.setInputToNeurons(input);
        
        for (int i = 0, n = layers.size() - 1; i < n; i++) {
            layers.get(i).propagateToNextLayer();
        }
    }
    
    /**
     * Backpropagate layer to layer
     * Compute delta etc and do adapt
     */
    protected void backPropagate(Vec expectedOutput) {
        MlpOutputLayer outputLayer = (MlpOutputLayer)layers.get(layers.size() - 1);
        
        outputLayer.setExpectedOutputToNeurons(expectedOutput);
        
        for (int i = layers.size() - 1; i >= 0; i--) {
            layers.get(i).backPropagateFromNextLayerOrExpectedOutput();
        }
        
    }
    
    
    protected double propagteBidirection(Vec input, Vec expectedOutput) {
        /**
         * @NOTICE:
         * Should do propagate() first, then delta of layers, because propagate()
         * will reset the new input of network, and propagate in the network.
         * Delta should be computed after that.
         *
         * Be ware of the computing order: compute the delta backward. then adapt
         */
        
        /** 1. forward propagate */
        propagate(input);
        
        /** 2. get network output, this should be done before back-propagate */
        Vec prediction = output();
        
        /** 3. Compute error */
        double error = prediction.metric(expectedOutput) / (double)prediction.dimension();
        
        /** 4. back-propagate, this will compute delta etc and do adapt, ie, do  */
        backPropagate(expectedOutput);
        
        /** 5. return error */
        return error;
    }
    
    
    
    public void removeLayer(int idx) {
        layers.remove(idx);
    }
    
    public void setLayers(List<MlpLayer> layers) {
        this.layers = layers;
    }
    
    
    public void train(InputOutputPointSet iops) {
//        trainSerialMode(iops);
        trainBatchMode(iops);
    }
    
    private void trainSerialMode(InputOutputPointSet iops) {
        for (long epoch = 1, n = arg.maxEpoch; epoch <= n; epoch++) {
            
            /** re-randomize iops order each time */
            iops.randomizeOrder();
            
            double epochSumError = 0;
            for (int i = 0; i < iops.size(); i++) {
                epochSumError += propagteBidirection(iops.get(i).input, iops.get(i).output);
                adapt();
            }
            
            double epochMeanError = epochSumError / (double)iops.size();
            
            fireNetworkChangeEvent(new NetworkChangeEvent(
                    this,
                    NetworkChangeEvent.Type.Updated,
                    epoch,
                    epochMeanError
                    ));
            
            System.out.println("Mean Error at the end of epoch " + epoch + ": " + epochMeanError);
            
            if (epochMeanError < arg.predictionError) {
                break;
            }
            
        }
    }
    
    private void trainBatchMode(InputOutputPointSet iops) {
        for (long epoch = 1, n = arg.maxEpoch; epoch <= n; epoch++) {
            
            double epochSumError = 0;
            for (int i = 0; i < iops.size(); i++) {
                epochSumError += propagteBidirection(iops.get(i).input, iops.get(i).output);
            }
            adapt();
            
            double epochMeanError = epochSumError / (double)iops.size();
            
            fireNetworkChangeEvent(new NetworkChangeEvent(
                    this,
                    NetworkChangeEvent.Type.Updated,
                    epoch,
                    epochMeanError
                    ));
            
            System.out.println("Mean Error at the end of epoch " + epoch + ": " + epochMeanError);
            
            if (epochMeanError < arg.predictionError) {
                break;
            }
            
        }
    }
    
    
    public double learnOnePoint(Vec input, Vec expectedOutput) {
        double error = propagteBidirection(input, expectedOutput);
        
        adapt();
        
        return error;
    }
    
    private void adapt() {
        for (int i = 0, n = layers.size(); i < n; i++) {
            layers.get(i).adapt(arg.learningRate, arg.momentumRate);
        }
    }
    
    
    
    public NetworkDescriptor cloneDescriptor() {
        /** @TODO */
        return descriptor;
    }
    
    public static class Arg implements Argument {
        public long   maxEpoch;
        public double learningRate;
        public double momentumRate;
        public double predictionError;
    }
}