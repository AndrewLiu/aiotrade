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
import java.util.HashSet;
import java.util.Set;
import javax.swing.Action;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisDescriptor;
import org.aiotrade.lib.util.swing.action.AddAction;
import org.aiotrade.lib.util.swing.action.RefreshAction;
import org.aiotrade.lib.util.swing.action.UpdateAction;
import org.aiotrade.platform.core.netbeans.GroupDescriptor;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.BeanNode;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
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
 *      +- Indicators (DescriptorGroupNode)
 *      |  +- MACD (DescriptorNode)
 *      |  |   +-opt1
 *      |  |   +-opt2
 *      |  +- ROC
 *      |     +-opt1
 *      |     +-opt2
 *      +- Drawings (DescriptorGroupNode)
 *         +- layer1
 *         |  +- line
 *         |  +- parallel
 *         |  +- gann period
 *         +- layer2
 */
public class GroupNode extends FilterNode {

    private GroupDescriptor<AnalysisDescriptor> group;
    private Frequency freq = Frequency.DAILY;

    public GroupNode(GroupDescriptor<AnalysisDescriptor> group, AnalysisContents contents) throws IntrospectionException {
        this(group, contents, new InstanceContent());

        this.group = group;
        setName(group.getDisplayName());
    }

    private GroupNode(GroupDescriptor<AnalysisDescriptor> group, AnalysisContents contents, InstanceContent content) throws IntrospectionException {
        super(new BeanNode<GroupDescriptor>(group), new GroupChildren(contents, group.getBindClass()), new AbstractLookup(content));

        /* add this node to our own lookup */
        content.add(this);

        /* add aditional items to the lookup */
        content.add(contents);

        content.add(new GroupRefreshAction(this));
        content.add(new GroupUpdateAction(this));

        /**
         * add actions carried with nodeInfo
         */
        for (Action action : group.createActions(contents)) {
            /**
             * as content only do flat lookup, should add actions one by one,
             * instead of adding an array, otherwise this.getLookup().loopup
             * can only search an array.
             */
            content.add(action);
        }
    }

    /**
     * Providing the Open action on a each descriptor groupClass
     */
    @Override
    public Action[] getActions(boolean popup) {
        /**
         * Use SystemAction to find instance of those actions registered in layer.xml
         *
         * The following code works for any kind of group node witch implemented
         * AddAction and has been added into the lookup content in construction.
         */
        return new Action[]{
                    getLookup().lookup(AddAction.class),};

    }

    @Override
    public Action getPreferredAction() {
        return getActions(false)[0];
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(0);
    }

    @Override
    public String getDisplayName() {
        return group.getDisplayName();
    }

    /**
     * Making a tooltip out of the descriptor's description
     */
    @Override
    public String getShortDescription() {
        return group.getTooltip();
    }

    @Override
    public Image getIcon(int type) {
        return group.getIcon(type);
    }

    public void setTimeFrequency(Frequency freq) {
        this.freq = freq.clone();

        getLookup().lookup(UpdateAction.class).execute();
    }

    private Frequency getFreq() {
        return freq;
    }

    /**
     * The children wrap class
     * ------------------------------------------------------------------------
     */
    private static class GroupChildren extends Children.Keys<AnalysisDescriptor> {

        private AnalysisContents contents;
        private Class<AnalysisDescriptor> groupClass;

        public GroupChildren(AnalysisContents contents, Class<AnalysisDescriptor> groupClass) {
            this.contents = contents;
            this.groupClass = groupClass;
        }
        /**
         * since setKeys(childrenKeys) will copy the elements of childrenKeys, it's safe to
         * use a repeatly used bufChildrenKeys here.
         * And, to sort them in letter order, we can use a SortedSet to copy from collection.(TODO)
         */
        private Set<AnalysisDescriptor> bufChildrenKeys = new HashSet<AnalysisDescriptor>();

        @Override
        protected void addNotify() {
            GroupNode node = (GroupNode) getNode();
            bufChildrenKeys.clear();
            bufChildrenKeys.addAll(contents.lookupDescriptors(groupClass, node.getFreq()));
            setKeys(bufChildrenKeys);
        }

        public Node[] createNodes(AnalysisDescriptor key) {
            try {
                return new Node[]{new DescriptorNode(key, contents)};
            } catch (final IntrospectionException ex) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                /** Should never happen - no reason for it to fail above */
                return new Node[]{new AbstractNode(Children.LEAF) {

                        @Override
                        public String getHtmlDisplayName() {
                            return "<font color='red'>" + ex.getMessage() + "</font>";
                        }
                    }};
            }
        }
    }

    private static class GroupRefreshAction extends RefreshAction {

        private final GroupNode node;

        GroupRefreshAction(GroupNode node) {
            this.node = node;
        }

        public void execute() {
            GroupChildren children = ((GroupChildren) node.getChildren());
            /** if new descriptor is added, this will add it to children */
            children.addNotify();
            for (Node child : children.getNodes()) {
                ((DescriptorNode) child).refreshIcon();
                ((DescriptorNode) child).refreshDisplayName();
            }
        }
    }

    private static class GroupUpdateAction extends UpdateAction {

        private final GroupNode node;

        GroupUpdateAction(GroupNode node) {
            this.node = node;
        }

        public void execute() {
            GroupChildren children = ((GroupChildren) node.getChildren());
            /**
             * by calling children.addNotify(), the children will re setKeys()
             * according to the current time unit and nUnits.
             * @see GroupChildren.addNotify()
             */
            children.addNotify();
        }
    }
}
