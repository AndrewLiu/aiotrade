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

import java.awt.BorderLayout;
import java.io.Serializable;
import javax.swing.ActionMap;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/** 
 * Top component which displays something.
 *
 * @author Caoyuan Deng
 */
public final class SymbolListTopComponent extends TopComponent implements ExplorerManager.Provider {
    
    private static final long serialVersionUID = 1L;
    
    private static SymbolListTopComponent instance;
    /** holds currently scheduled/running task for set of activated node */
    private RequestProcessor.Task nodeSetterTask;
    private final Object NODE_SETTER_LOCK = new Object();
    
    private TopComponent paletteSource;
    
    private final ExplorerManager manager = new ExplorerManager();
    private final BeanTreeView treeView = new BeanTreeView();
    
    private SymbolNode rootNode;
    private Node watchListNode = null;
    
    private SymbolListTopComponent() {
        setName(NbBundle.getMessage(SymbolListTopComponent.class, "CTL_SymbolListTopComponent"));
        setToolTipText(NbBundle.getMessage(SymbolListTopComponent.class, "HINT_SymbolListTopComponent"));
        //setIcon(Utilities.loadImage("SET/PATH/TO/ICON/HERE", true));
        
        setLayout(new BorderLayout());
        add(treeView, BorderLayout.CENTER);
        treeView.setRootVisible(true);
        try {
            rootNode = new SymbolNode.RootSymbolNode();
            manager.setRootContext(rootNode);
        } catch (Exception ex) {
            ErrorManager.getDefault().notify(ex);
        }
        ActionMap map = getActionMap();
        map.put("delete", ExplorerUtils.actionDelete(manager, true));
        associateLookup(ExplorerUtils.createLookup(manager, map));
    }
    
    /** 
     * Gets default instance. Don't use directly, it reserved for '.settings' file only,
     * i.e. deserialization routines, otherwise you can get non-deserialized instance.
     */
    public static synchronized SymbolListTopComponent getDefault() {
        if (instance == null) {
            instance = new SymbolListTopComponent();
        }
        return instance;
    }
    
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }
    
    public void componentOpened() {
        // TODO add custom code on component opening
    }
    
    public void componentClosed() {
        // TODO add custom code on component closing
    }
    
    /** replaces this in object stream */
    public Object writeReplace() {
        return new ResolvableHelper();
    }
    
    protected String preferredID() {
        return "SymbolListTopComponent";
    }
    
    final static class ResolvableHelper implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object readResolve() {
            return SymbolListTopComponent.getDefault();
        }
    }
    
    public ExplorerManager getExplorerManager() {
        return manager;
    }
    
    public SymbolNode getRootNode() {
        return rootNode;
    }
}
