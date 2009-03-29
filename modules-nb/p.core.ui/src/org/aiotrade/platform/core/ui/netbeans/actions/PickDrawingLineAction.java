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
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import javax.swing.DefaultSingleSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.util.swing.action.ViewAction;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.charting.view.WithDrawingPane;
import org.aiotrade.charting.descriptor.DrawingDescriptor;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.charting.view.pane.DrawingPane;
import org.aiotrade.charting.chart.handledchart.HandledChart;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;


/**
 *
 * @author Caoyuan Deng
 */
public class PickDrawingLineAction extends CallableSystemAction {
    private static JToggleButton toggleButton;
    private static JPopupMenu popupMenu;
    private MyMenuItemListener menuItemListener;
    
    Collection<HandledChart> handledCharts;
    
    public PickDrawingLineAction() {
    }
    
    
    public void performAction() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                toggleButton.setSelected(true);
            }
        });
    }
    
    public String getName() {
        return "Pick Drawing Line";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
    public Component getToolbarPresenter() {
        Image iconImage = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/drawingLine.png");
        ImageIcon icon = new ImageIcon(iconImage);
        
        toggleButton = new JToggleButton();
        toggleButton.setIcon(icon);
        toggleButton.setToolTipText("Pick Drawing Line");
        
        handledCharts = PersistenceManager.getDefault().lookupAllRegisteredServices(HandledChart.class, "HandledCharts");
        popupMenu = new JPopupMenu();
        popupMenu.setSelectionModel(new DefaultSingleSelectionModel());
        menuItemListener = new MyMenuItemListener();
        for (HandledChart handledChart : handledCharts) {
            /** it's a selection menu other than an action menu, so use JRadioButtonMenuItem instead of JMenuItem */
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(handledChart.toString());
            item.addItemListener(menuItemListener);
            popupMenu.add(item);
        }
        
        toggleButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    /** show popup menu on toggleButton at position: (0, height) */
                    popupMenu.show(toggleButton, 0, toggleButton.getHeight());
                }
            }
        });
        
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
                toggleButton.setSelected(false);
            }
            
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                toggleButton.setSelected(false);
            }
            
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        
        return toggleButton;
    }
    
    private class MyMenuItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            
            JMenuItem item = (JMenuItem)e.getSource();
            /**
             * clear selected state. if want to do this, do not add items to buttonGroup. see bug report:
             * http://sourceforge.net/tracker/index.php?func=detail&aid=1579592&group_id=152032&atid=782880
             */
            item.setSelected(false);
            AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.getSelected();
            if (analysisWin == null) {
                return;
            }
            
            ChartViewContainer viewContainer = analysisWin.getSelectedViewContainer();
            ChartView masterView = viewContainer.getMasterView();
            if (!(masterView instanceof WithDrawingPane)) {
                return;
            }
            
            DrawingPane drawingPane = ((WithDrawingPane)masterView).getSelectedDrawing();
            if (drawingPane == null) {
                JOptionPane.showMessageDialog(
                        WindowManager.getDefault().getMainWindow(),
                        "Please add a layer before pick drawing line",
                        "Pick drawing line",
                        JOptionPane.OK_OPTION,
                        null);
                return;
            }
            
            String selectedStr = item.getText();
            HandledChart theHandledChart = null;
            for (HandledChart handledChart : handledCharts) {
                if (handledChart.toString().equalsIgnoreCase(selectedStr)) {
                    theHandledChart = handledChart;
                    break;
                }
            }
            assert theHandledChart != null : "A just picked handled chart should be there!";
            
            AnalysisContents contents = viewContainer.getController().getContents();
            
            DrawingDescriptor descriptor = contents.lookupDescriptor(
                    DrawingDescriptor.class,
                    drawingPane.getLayerName(),
                    viewContainer.getController().getMasterSer().getFreq());
            if (descriptor != null) {
                HandledChart handledChart = theHandledChart.createNewInstance();
                handledChart.attachDrawingPane(drawingPane);
                drawingPane.setSelectedHandledChart(handledChart);
                
                descriptor.lookupAction(ViewAction.class).execute();
            } else {
                /** best effort, should not happen */
                viewContainer.getController().setCursorCrossLineVisible(false);
                drawingPane.activate();
                
                SwitchHideShowDrawingLineAction.updateToolbar(viewContainer);
            }
        }
    }
    
}


