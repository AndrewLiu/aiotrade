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
package org.aiotrade.lib.charting.util;

import java.awt.geom.GeneralPath;
import org.aiotrade.lib.util.pool.PoolableObjectFactory;
import org.aiotrade.lib.util.pool.StackObjectPool;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, 11/27/2006
 * @since   1.0.4
 */
public class PathPool extends StackObjectPool<GeneralPath> implements PoolableObjectFactory<GeneralPath> {
    private final int initialCapacityOfPath;

    public PathPool(int maxIdle, int initIdleCapacity, int initialCapacityOfPath) {
        super(maxIdle, initIdleCapacity);
        setFactory(this);
        this.initialCapacityOfPath = initialCapacityOfPath;
    }
    
    public void activateObject(GeneralPath obj) throws RuntimeException {
        obj.reset();
    }
    
    public void destroyObject(GeneralPath obj) throws RuntimeException {
        obj = null;
    }
    
    public GeneralPath makeObject() throws RuntimeException {
        return new GeneralPath(GeneralPath.WIND_NON_ZERO, initialCapacityOfPath);
    }
    
    public void passivateObject(GeneralPath obj) throws RuntimeException {
    }
    
    public boolean validateObject(GeneralPath obj) {
        return true;
    }
    
    
}
