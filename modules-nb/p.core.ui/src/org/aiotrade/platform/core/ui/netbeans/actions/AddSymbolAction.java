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
package org.aiotrade.platform.core.ui.netbeans.actions;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.JOptionPane;
import org.aiotrade.math.timeseries.datasource.DataContract;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.platform.core.analysis.ContentsPersistenceHandler;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.platform.core.ui.dialog.ImportSymbolDialog;
import org.aiotrade.platform.core.ui.netbeans.explorer.SymbolListTopComponent;
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;/**
 *
 * @author Caoyuan Deng
 */
public class AddSymbolAction extends CallableSystemAction {
    private SymbolListTopComponent symbolListTc;
    private Node currentNode;
    
    /** Creates a new instance
     */
    public AddSymbolAction() {
    }
    
    
    public void performAction() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                
                symbolListTc = SymbolListTopComponent.getDefault();
                symbolListTc.requestActive();
                
                Node[] selectedNodes = symbolListTc.getExplorerManager().getSelectedNodes();
                Node selectedNode = null;
                DataFolder currentFolder = null;
                if (selectedNodes.length > 0) {
                    currentNode = selectedNodes[0];
                }
                
                if (currentNode != null) {
                    currentFolder = currentNode.getLookup().lookup(DataFolder.class);
                }
                
                if (currentFolder == null) {
                    /** add this stock in root folder */
                    currentNode = symbolListTc.getExplorerManager().getRootContext();
                    currentFolder = currentNode.getLookup().lookup(DataFolder.class);
                }
                
                /** this will expand this node */
                symbolListTc.getExplorerManager().setExploredContext(currentNode);
                
                
                /** Now begin the dialog */
                
                QuoteContract quoteContract = new QuoteContract();
                
                ImportSymbolDialog pane = new ImportSymbolDialog(
                        WindowManager.getDefault().getMainWindow(), 
                        quoteContract, 
                        true);
                if (pane.showDialog() != JOptionPane.OK_OPTION) {
                    return;
                }
                
                /** quoteContract may bring in more than one symbol, should process it later */
                String sourceSymbol = quoteContract.getSymbol();
                if (sourceSymbol.equals("")) {
                    return;
                }
                
                String[] sourceSymbols = sourceSymbol.split(",");
                
                for (String symbol : sourceSymbols) {
                    symbol = symbol.trim();
                    
                    /** dataSourceDescriptor may has been set to more than one symbols, process it here */
                    quoteContract.setSymbol(symbol);
                    
                    createSymbolXmlFile(currentFolder, symbol, quoteContract);
                }
                
            }
        });
        
    }
    
    private void createSymbolXmlFile(DataFolder folder, String symbol, QuoteContract quoteContract) {
        
        FileObject folderObject = folder.getPrimaryFile();
        String baseName = symbol;
        int ix = 1;
        while (folderObject.getFileObject(baseName + ix, "xml") != null) {
            ix++;
        }
        
        FileLock lock = null;
        try {
            FileObject writeTo = folderObject.createData(baseName + ix, "xml");
            lock = writeTo.lock();
            PrintStream out = new PrintStream(writeTo.getOutputStream(lock));
            
            AnalysisContents contents = PersistenceManager.getDefault().getDefaultContents();
            /** clear default dataSourceContract */
            contents.clearDescriptors(DataContract.class);
            
            contents.setUniSymbol(symbol);
            contents.addDescriptor(quoteContract);
            
            out.print(ContentsPersistenceHandler.dumpContents(contents));
            
            /** should remember to do out.close() here */
            out.close();
            
            /** 
             * set attr: "new" for opening the view when a new node is
             * created late by SymbolNode.SymbolFolderChildren.creatNodes() 
             */
            writeTo.setAttribute("new", true);
        } catch (IOException ioe) {
            ErrorManager.getDefault().notify(ioe);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
        
    }
    
    public String getName() {
        return "Add Symbol";
    }
    
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected String iconResource() {
        return "org/aiotrade/platform/core/ui/netbeans/resources/newSymbol.gif";
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
    
}


