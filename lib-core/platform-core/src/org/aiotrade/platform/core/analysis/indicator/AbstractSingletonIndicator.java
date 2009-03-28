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
package org.aiotrade.platform.core.analysis.indicator;

import org.aiotrade.lib.math.timeseries.computable.Indicator;

/**
 * Usually, indicator instances are created by call createNewInstance(),
 * But in this class, createNewInstance() don't really create a new singletonInstance,
 * it just return the singletonInstance.
 * 
 * Here is not the traditional singleton pattern, which implement singleton by 
 * using <em>private</em> constructor and a static getInstance() method. This is
 * because that, in many cases (such as NetBeans FileSystem, or serializtion etc.), 
 * a <em>public</em> constructor with empty args is required.
 * 
 * @author Caoyuan Deng
 */
@IndicatorName("Abstract Singleton Indicator")
public abstract class AbstractSingletonIndicator extends AbstractContIndicator {
    private static AbstractSingletonIndicator singletonInstance;
    
    public AbstractSingletonIndicator() {
        super();
        singletonInstance = this;
    }
    
    public Indicator createInstance() {
        if (singletonInstance == null) {
            Class clazz = this.getClass();
            try {
                singletonInstance = (AbstractSingletonIndicator)clazz.newInstance();
            } catch (Exception ex) {
                ex.printStackTrace();;
            }
        }
        return singletonInstance;
    }
    
}

