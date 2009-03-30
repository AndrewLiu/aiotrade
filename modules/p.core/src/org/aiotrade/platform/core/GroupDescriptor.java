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
package org.aiotrade.platform.core;

import java.awt.Image;
import javax.swing.Action;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;

/**
 * A lightweight class to carry the information that will be wrapped in Node
 *
 * It can be used to wrap the Descriptor, icon, actions of Descriptor
 * Although we've had Descriptor, but we here is just try to decouple the netbeans
 * relative org.openide.nodes.Node (which is neccessary to define the actions)
 * from Descriptor.
 *
 *
 * @author Caoyuan Deng
 */
public abstract class GroupDescriptor<T> implements Cloneable, Comparable<GroupDescriptor> {
    
    /** this public construction is for using by layer.xml to initialize it */
    public GroupDescriptor() {
    }
    
    public abstract Class<T> getBindClass();
    
    public abstract String getDisplayName();
    
    public abstract String getTooltip();
    
    public abstract Image getIcon(int type);
    
    public abstract Action[] createActions(AnalysisContents contents);
    
    @Override
    public GroupDescriptor<T> clone() {
        GroupDescriptor<T> instance = null;
        try {
            instance = (GroupDescriptor<T>)super.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }
        
        return instance;
    }
    
    public final int compareTo(GroupDescriptor another) {
        if (this.toString().equals(another.toString())) {
            return this.hashCode() < another.hashCode() ? -1 : (this.hashCode() == another.hashCode() ? 0 : 1);
        } else {
            return this.toString().compareTo(another.toString());
        }
    }
}



