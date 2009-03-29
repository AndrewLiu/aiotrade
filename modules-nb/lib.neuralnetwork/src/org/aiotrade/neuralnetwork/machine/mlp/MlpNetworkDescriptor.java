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
package org.aiotrade.neuralnetwork.machine.mlp;

import java.util.ArrayList;
import java.util.List;

import org.aiotrade.neuralnetwork.core.descriptor.NetworkDescriptor;

/**
 * 
 * @author Caoyuan Deng
 */
public class MlpNetworkDescriptor extends NetworkDescriptor {
    
    private List<MlpLayerDescriptor> layerDescriptors = new ArrayList<MlpLayerDescriptor>();
    
    public MlpNetworkDescriptor() {
        super();
    }
    
    public int getNLayers() {
        return layerDescriptors.size();
    }
    
    public void addHiddenLayerDescriptor(MlpLayerDescriptor le) {
        layerDescriptors.add(le);
    }
    
    public List<MlpLayerDescriptor> getLayerDescriptors() {
        return layerDescriptors;
    }
    
    public void setLayerDescriptors(List<MlpLayerDescriptor> layerDescriptors) {
        this.layerDescriptors = layerDescriptors;
    }

    protected void checkValidation() throws Exception {
        for (MlpLayerDescriptor layerDescriptor : layerDescriptors) {
            if (layerDescriptor.getNNeurons() < 1) {
                throw new Exception(layerDescriptor.toString());
            }
            
            Class neuronClass = null;
            try {
                neuronClass = Class.forName(layerDescriptor.getNeuronClassName());
            } catch (ClassNotFoundException e) {
                throw new Exception(layerDescriptor.toString());
            }
 
        }

        if (layerDescriptors.size() == 0) {
            throw new Exception("no layers defined");
        }
        
        MlpNetwork.Arg param = (MlpNetwork.Arg)getArg();
        
        if (param.learningRate < 0) {
            throw new Exception("learning rate must > 0");
        }
        
        if (param.maxEpoch < 0) {
            throw new Exception("max epoch must be > 0");
        }
        
        if (param.predictionError <= 0) {
            throw new Exception("prediction error must > 0");
        }
        
        
    }
    
    public Class getServiceClass() {
        return MlpNetwork.class;
    }
    
}