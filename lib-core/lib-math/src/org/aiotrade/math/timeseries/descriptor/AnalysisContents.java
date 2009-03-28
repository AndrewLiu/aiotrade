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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.Action;
import org.aiotrade.math.PersistenceManager;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.math.timeseries.datasource.SerProvider;
import org.aiotrade.util.serialization.BeansDocument;
import org.aiotrade.util.swing.action.WithActions;
import org.aiotrade.util.swing.action.WithActionsHelper;
import org.aiotrade.util.swing.action.SaveAction;
import org.w3c.dom.Element;

/**
 *
 * @author Caoyuan Deng
 */
public class AnalysisContents implements WithActions {
    private final WithActionsHelper withActionsHelper = new WithActionsHelper(this);
    
    private String uniSymbol;
    
    /** Ser could be loaded lazily */
    private SerProvider serProvider;
    
    /** use List to store descriptor, so they can be ordered by index */
    private List<AnalysisDescriptor> descriptorList = new ArrayList<AnalysisDescriptor>();
    
    
    public AnalysisContents(String uniSymbol) {
        this.uniSymbol = uniSymbol;
    }
    
    public List<AnalysisDescriptor> getDescriptors() {
        return descriptorList;
    }
    
    public void addDescriptor(AnalysisDescriptor descriptor) {
        if (! descriptorList.contains(descriptor)) {
            descriptorList.add(descriptor);
            descriptor.setContainerContents(this);
        }
    }
    
    public void addDescriptor(int idx, AnalysisDescriptor descriptor) {
        if (! descriptorList.contains(descriptor)) {
            descriptorList.add(idx, descriptor);
            descriptor.setContainerContents(this);
        }
    }
    
    public void removeDescriptor(AnalysisDescriptor descriptor) {
        descriptorList.remove(descriptor);
    }
    
    public void removeDescriptor(int idx) {
        descriptorList.remove(idx);
    }
    
    public int indexOf(AnalysisDescriptor descriptor) {
        return descriptorList.indexOf(descriptor);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends AnalysisDescriptor> int lastIndexOf(Class<T> type) {
        T lastOne = null;
        for (AnalysisDescriptor descriptor: descriptorList) {
            if (type.isInstance(descriptor)) {
                lastOne = (T)descriptor;
            }
        }
        
        return lastOne != null ? descriptorList.indexOf(lastOne) : -1;
    }
    
    public <T extends AnalysisDescriptor> void clearDescriptors(Class<T> type) {
        /**
         * try to avoid java.util.ConcurrentModificationException by add those to
         * toBeRemoved, then call descriptorList.removeAll(toBeRemoved)
         */
        Collection<AnalysisDescriptor> toBeRemoved = new ArrayList<AnalysisDescriptor>();
        for (AnalysisDescriptor descriptor: descriptorList) {
            if (type.isInstance(descriptor)) {
                toBeRemoved.add(descriptor);
            }
        }
        
        descriptorList.removeAll(toBeRemoved);
    }
    
    /**
     *
     * @param clazz the Class being looking up
     * @return found collection of AnalysisDescriptor instances.
     *         If found none, return an empty collection other than null
     */
    @SuppressWarnings("unchecked")
    public <T extends AnalysisDescriptor> Collection<T> lookupDescriptors(Class<T> type) {
        Collection<T> result = new ArrayList<T>();
        for (AnalysisDescriptor descriptor: descriptorList) {
            if (type.isInstance(descriptor)) {
                result.add((T)descriptor);
            }
        }
        
        return result;
    }
    
    /**
     * Lookup the descriptorList of clazz (Indicator/Drawing/Source etc) with the same time frequency
     */
    @SuppressWarnings("unchecked")
    public <T extends AnalysisDescriptor> Collection<T> lookupDescriptors(Class<T> type, Frequency freq) {
        Collection<T> result = new ArrayList<T>();
        for (AnalysisDescriptor descriptor : descriptorList) {
            if (type.isInstance(descriptor) && descriptor.getFreq().equals(freq)) {
                result.add((T)descriptor);
            }
        }
        
        return result;
    }
    
    public <T extends AnalysisDescriptor> T lookupDescriptor(Class<T> type, String serviceClassName, Frequency freq) {
        for (T descriptor : lookupDescriptors(type)) {
            if (descriptor.idEquals(serviceClassName, freq)) {
                return descriptor;
            }
        }
        return null;
    }
    
    public <T extends AnalysisDescriptor> T lookupActiveDescriptor(Class<T> type) {
        for (T descriptor : lookupDescriptors(type)) {
            if (descriptor.isActive()) {
                return descriptor;
            }
        }
        return null;
    }
    
    public <T extends AnalysisDescriptor> T createDescriptor(Class<T> type, String serviceClassName, Frequency freq) {
        try {
            T descriptor = (T)type.newInstance();
            descriptor.set(serviceClassName, freq);
            addDescriptor(descriptor);
            
            return descriptor;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        }
        
        return null;
    }
    
    public String getUniSymbol() {
        return uniSymbol;
    }
    
    public void setUniSymbol(String uniSymbol) {
        this.uniSymbol = uniSymbol;
    }
    
    public void setSerProvider(SerProvider serProvider) {
        this.serProvider = serProvider;
    }
    
    public SerProvider getSerProvider() {
        return serProvider;
    }
    
    public Action addAction(Action action) {
        return withActionsHelper.addAction(action);
    }
    
    public <T extends Action> T lookupAction(Class<T> type) {
        return withActionsHelper.lookupAction(type);
    }
    
    public Action[] createDefaultActions() {
        return new Action[] { new ContentsSaveAction() };
    }
    
    public Element writeToBean(BeansDocument doc) {
        final Element bean = doc.createBean(this);
        
        Element list = doc.listPropertyOfBean(bean, "descriptors");
        for (AnalysisDescriptor descriptor : descriptorList) {
            doc.innerElementOfList(list, descriptor.writeToBean(doc));
        }
        
        return bean;
    }
    
    
    private class ContentsSaveAction extends SaveAction {
        
        public void execute() {
            PersistenceManager.getDefault().saveContents(AnalysisContents.this);
        }
        
    }
    
}

