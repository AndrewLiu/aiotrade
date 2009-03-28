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
package org.aiotrade.math.timeseries.descriptor;

import javax.swing.Action;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.util.serialization.BeansDocument;
import org.aiotrade.util.serialization.DeserializationConstructor;
import org.aiotrade.util.serialization.JavaDocument;
import org.aiotrade.util.swing.action.WithActions;
import org.aiotrade.util.swing.action.WithActionsHelper;
import org.w3c.dom.Element;

/**
 * Descriptor is something like NetBeans' DataObject
 *
 * <S> Service class type
 *
 * @author Caoyuan Deng
 */
public abstract class AnalysisDescriptor<S> implements WithActions {
    private final WithActionsHelper withActionsHelper = new WithActionsHelper(this);
    
    private AnalysisContents containerContents;
    
    private S serviceInstance;

    private String serviceClassName;
    
    private Frequency freq = Frequency.DAILY;
    
    private boolean active;
    
    public AnalysisDescriptor() {
    }
    
    @DeserializationConstructor
    public AnalysisDescriptor(String serviceClassName, Frequency freq, boolean active) {
        this.serviceClassName = serviceClassName;
        this.freq = freq;
        this.active = active;
    }
    
    public void setContainerContents(AnalysisContents containerContents) {
        this.containerContents = containerContents;
    }
    
    public AnalysisContents getContainerContents() {
        return containerContents;
    }
    
    public void set(String serviceClassName, Frequency freq) {
        setServiceClassName(serviceClassName);
        setFreq(freq);
    }
    
    public void setServiceClassName(String serviceClassName) {
        this.serviceClassName = serviceClassName;
    }
    
    public String getServiceClassName() {
        return serviceClassName;
    }
    
    public Frequency getFreq() {
        return freq;
    }
    
    public void setFreq(Frequency freq) {
        this.freq = freq.clone();
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean b) {
        this.active = b;
    }
    
    public S getCreatedServerInstance() {
        assert serviceInstance != null : "This method should only be called after serviceInstance created!";
        return serviceInstance;
    }
    
    public S getServiceInstance(Object... args) {
        return serviceInstance == null ? serviceInstance = createServiceInstance(args) : serviceInstance;
    }
    
    protected boolean isServiceInstanceCreated() {
        return serviceInstance == null ? false : true;
    }
    
    protected abstract S createServiceInstance(Object... args);
    
    public abstract String getDisplayName();
    
    public boolean idEquals(String serviceClassName, Frequency freq) {
        if (this.getServiceClassName().equals(serviceClassName) && this.freq.equals(freq)) {
            return true;
        } else {
            return false;
        }
    }
    
    public Action addAction(Action action) {
        return withActionsHelper.addAction(action);
    }
    
    public <T extends Action> T lookupAction(Class<T> type) {
        return withActionsHelper.lookupAction(type);
    }
    
    public Action[] createDefaultActions() {
        return new Action[] {};
    }
    
    public Element writeToBean(BeansDocument doc) {
        final Element bean = doc.createBean(this);
        
        doc.valuePropertyOfBean(bean, "active", isActive());
        doc.valuePropertyOfBean(bean, "serviceClassName", getServiceClassName());
        doc.innerPropertyOfBean(bean, "freq", getFreq().writeToBean(doc));
        
        return bean;
    }
    
    public String writeToJava(String id) {
        return
                getFreq().writeToJava("freq") +
                JavaDocument.create(id, this.getClass(), 
                "" + getServiceClassName() +
                "freq" + 
                isActive())
                ;
    }
    
}
