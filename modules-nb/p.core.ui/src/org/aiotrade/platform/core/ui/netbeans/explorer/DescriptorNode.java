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
package org.aiotrade.platform.core.ui.netbeans.explorer;

import java.awt.Image;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import javax.swing.Action;
import org.aiotrade.util.swing.action.DeleteAction;
import org.aiotrade.util.swing.action.EditAction;
import org.aiotrade.util.swing.action.HideAction;
import org.aiotrade.util.swing.action.SaveAction;
import org.aiotrade.util.swing.action.ViewAction;
import org.aiotrade.charting.view.pane.DrawingPane;
import org.aiotrade.math.timeseries.descriptor.AnalysisDescriptor;
import org.aiotrade.charting.descriptor.DrawingDescriptor;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.Utilities;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 *
 *
 * @author Caoyuan Deng
 *
 * This node is just a virtul node without any physical object in file system
 *
 * The tree view of Stock and others
 * + Stocks (config/Stocks)
 *   +- sunw (sunw.ser)
 *      +- Indicators DescriptorGroupNodee)
 *      |  +- MACD (DescriptorNode)
 *      |  |   +-opt1
 *      |  |   +-opt2
 *      |  +- ROC
 *      |     +-opt1
 *      |     +-opt2
 *      +- Drawings DescriptorGroupNodee)
 *         +- layer1
 *         |  +- line
 *         |  +- parallel
 *         |  +- gann period
 *         +- layer2
 */
public class DescriptorNode extends FilterNode {
    private AnalysisDescriptor descriptor;
    
    private static Image ACTIVE_ICON   = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/showingTrue.gif");
    private static Image NOACTIVE_ICON = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/showingFalse.gif");
    
    public DescriptorNode(AnalysisDescriptor descriptor, AnalysisContents contents) throws IntrospectionException {
        this(descriptor, contents, new InstanceContent());
        
        /**
         * @TODO
         * this method seems that will call descriptor.setName(String) automatically,
         * so should avoid define setName(String) method in desctiptor to avoid
         * unexpected behave
         *
         * Should use another super class to replace FilterNode? (because FilterNode
         * wraps descriptor as a BeanNode)
         */
        setName(descriptor.getDisplayName());
    }
    
    private DescriptorNode(AnalysisDescriptor descriptorInfo,  AnalysisContents contents, InstanceContent content) throws IntrospectionException {
        super(new BeanNode<AnalysisDescriptor>(descriptorInfo), Children.LEAF, new AbstractLookup(content));
        
        /* adds the node to our own lookup */
        content.add(this);
        
        /**
         * the descriptor param may be a clone of descritor in contents, so we
         * should lookup that one in contents instead put it in lookup content.
         * Otherwise, the descriptor stored in viewContainer (which is from contents)
         * may not the one stored here.
         * @TODO The better solution is give a NodeInfo param
         */
        this.descriptor = contents.lookupDescriptor(
                descriptorInfo.getClass(),
                descriptorInfo.getServiceClassName(),
                descriptorInfo.getFreq());
        
        /* adds aditional items to the lookup */
        content.add(contents);
    }
    
    public String getDisplayName() {
        return descriptor.getDisplayName();
    }
    
    /**
     * Making a tooltip out of the descriptorInfo's description
     */
    public String getShortDescription()    {
        return descriptor.getDisplayName();
    }
    
    public boolean canRename() {
        if (descriptor instanceof DrawingDescriptor) {
            return true;
        }
        return false;
    }
    
    
    /**
     * Providing the Open action on a stock descriptorInfo
     */
    public Action[] getActions(boolean popup) {
        final AnalysisContents contents = getLookup().lookup(AnalysisContents.class);
        
        /** Use SystemAction to find instance of those actions registered in layer.xml */
        Action[] actions = new Action[] {
            descriptor.lookupAction(ViewAction.class),
            descriptor.lookupAction(HideAction.class),
            descriptor.lookupAction(EditAction.class),
            null,
            descriptor.lookupAction(DeleteAction.class)
        };
        
        return actions;
    }
    
    public Action getPreferredAction() {
        return descriptor.lookupAction(ViewAction.class);
    }
    
    public Image getIcon(int type) {
        return descriptor.isActive() ? ACTIVE_ICON : NOACTIVE_ICON;
    }
    
    public Image getOpenedIcon(int type) {
        return getIcon(0);
    }
    
    public void refreshIcon() {
        fireIconChange();
    }
    
    public void refreshDisplayName() {
        fireDisplayNameChange(getDisplayName(), getDisplayName());
    }
    
    @Override
    protected NodeListener createNodeListener() {
        final NodeListener delegate = super.createNodeListener();
        NodeListener newListener = new NodeListener() {
            public void childrenAdded(NodeMemberEvent nodeMemberEvent) {
                delegate.childrenAdded(nodeMemberEvent);
            }
            
            public void childrenRemoved(NodeMemberEvent nodeMemberEvent) {
                delegate.childrenRemoved(nodeMemberEvent);
            }
            
            public void childrenReordered(NodeReorderEvent nodeReorderEvent) {
                delegate.childrenReordered(nodeReorderEvent);
            }
            
            public void nodeDestroyed(NodeEvent nodeEvent) {
                delegate.nodeDestroyed(nodeEvent);
            }
            
            public void propertyChange(PropertyChangeEvent evt) {
                delegate.propertyChange(evt);
                if (evt.getPropertyName().equals(PROP_NAME)) {
                    String newName = evt.getNewValue().toString();
                    
                    if (descriptor instanceof DrawingDescriptor) {
                        AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.getSelected();
                        DrawingPane drawing = analysisWin != null ?
                            analysisWin.lookupDrawing((DrawingDescriptor)descriptor) : null;
                        if (drawing != null) {
                            drawing.setLayerName(newName);
                        }
                        
                        descriptor.setServiceClassName(evt.getNewValue().toString());
                        refreshDisplayName();
                    }
                    descriptor.getContainerContents().lookupAction(SaveAction.class).execute();
                }
            }
        };
        return newListener;
    }
    
}

