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
package org.aiotrade.math.timeseries.computable;

import javax.swing.event.EventListenerList;
import org.aiotrade.util.serialization.BeansDocument;
import org.w3c.dom.Element;

/**
 * Class for defining indicator's option
 *
 * @author Caoyuan Deng
 */
public interface Opt extends Cloneable {
    
    void setName(final String name);
    String getName();
    
    void setValue(final Number value);
    float value();
    
    void setStep(final Number step);
    float getStep();
    
    void setMaxValue(final Number maxValue);
    float getMaxValue();
    
    void setMinValue(final Number minValue);
    float getMinValue();
    
    Opt clone();
    
    void addOptChangeListener(final OptChangeListener listener);
    void removeOptChangeListener(final OptChangeListener listener);
    void fireOptChangeEvent(final OptChangeEvent evt);
    
    Element writeToBean(BeansDocument doc);
    
    static abstract class AbstractOpt implements Opt {
        private String name;
        private final EventListenerList optChangeEventListenerList = new EventListenerList();
        
        public void setName(final String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public void addOptChangeListener(final OptChangeListener listener) {
            optChangeEventListenerList.add(OptChangeListener.class, listener);
        }
        
        public void removeOptChangeListener(final OptChangeListener listener) {
            optChangeEventListenerList.remove(OptChangeListener.class, listener);
        }
        
        public void fireOptChangeEvent(final OptChangeEvent evt) {
            Object[] listeners = optChangeEventListenerList.getListenerList();
            /** Each listener occupies two elements - the first is the listener class */
            for (int i = 0; i < listeners.length; i += 2) {
                if (listeners[i] == OptChangeListener.class) {
                    ((OptChangeListener)listeners[i + 1]).optChanged(evt);
                }
            }
        }
        
        public Opt clone() {
            try {
                final Opt newOpt = (Option)super.clone();
                
                newOpt.setName(getName());
                newOpt.setValue(value());
                newOpt.setStep(getStep());
                newOpt.setMinValue(getMinValue());
                newOpt.setMaxValue(getMaxValue());
                
                return newOpt;
            } catch (CloneNotSupportedException ex) {
                throw  new InternalError(ex.toString());
            }
        }
        
        public Element writeToBean(BeansDocument doc) {
            final Element bean = doc.createBean(this);
            
            doc.valuePropertyOfBean(bean, "name", getName());
            doc.valuePropertyOfBean(bean, "value", value());
            doc.valuePropertyOfBean(bean, "step", getStep());
            doc.valuePropertyOfBean(bean, "minValue", getMinValue());
            doc.valuePropertyOfBean(bean, "maxValue", getMaxValue());
            
            return bean;
        }
        
    }
    
    public static class Float extends AbstractOpt implements Opt {
        private float value;
        private float step;
        private float minValue;
        private float maxValue;
        
        public void setValue(final Number value) {
            this.value = value.floatValue();
        }
        
        public float value() {
            return value;
        }
        
        public void setStep(final Number step) {
            this.step = step.floatValue();
        }
        
        public float getStep() {
            return step;
        }
        
        public void setMaxValue(final Number maxValue) {
            this.maxValue = maxValue.floatValue();
        }
        
        public float getMaxValue() {
            return maxValue;
        }
        
        public void setMinValue(final Number minValue) {
            this.minValue = minValue.floatValue();
        }
        
        public float getMinValue() {
            return minValue;
        }
        
    }
}



