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
package org.aiotrade.platform.core.ui.dialog;
import java.awt.Dimension;
import java.awt.Frame;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.NumberFormatter;
import org.aiotrade.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.math.timeseries.computable.Opt;

/**
 * 
 * 
 * @author Caoyuan Deng
 * @NOTICE: IndicatorDescriptore IndicatorDescriptor's opts instead of indicator's opts
 */
public class ChangeIndicatorOptsPane extends JComponent {
    
    private Frame owner;
    private IndicatorDescriptor descriptor;
    
    private List<Opt> opts;
    private List<Opt> oldOpts;
    private int length;
    private JLabel[] optNameLables;
    private JSpinner[] optValueSpinners;
    private SpinnerNumberModel[] optSpinnerNumberModels;
    
    private JCheckBox previewCheckBox;
    private JCheckBox saveAsDefaultCheckBox;
    private JCheckBox applyToAllCheckBox;
    
    private boolean saveAsDefault = false;
    private boolean applyToAll = false;
    
    private EventListenerList spinnerChangelistenerList = new EventListenerList();
    
    public ChangeIndicatorOptsPane(Frame owner, IndicatorDescriptor descriptor) {
        this.owner = owner;
        this.descriptor = descriptor;
        
        opts = descriptor.getOpts();
        oldOpts = new ArrayList<Opt>();
        for (Opt opt : opts) {
            oldOpts.add(opt.clone());
        }
        
        length = opts.size();
        
        initComponents();
    }
    
    private void initComponents() {
        
        LayoutManager gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gbl);
        
        optNameLables = new JLabel[length];
        optValueSpinners = new JSpinner[length];
        optSpinnerNumberModels = new SpinnerNumberModel[length];
        
        /** @TODO use this NumberFormatter ? */
        DecimalFormat df = new DecimalFormat("#####");
        NumberFormatter nf = new NumberFormatter(df) {
            public String valueToString(Object iv) throws ParseException {
                if ((iv == null) || (((Float)iv).floatValue() == -1)) {
                    return "";
                } else {
                    return super.valueToString(iv);
                }
            }
            public Object stringToValue(String text) throws ParseException {
                if ("".equals(text)) {
                    return null;
                }
                return super.stringToValue(text);
            }
        };
        nf.setMinimum(0);
        nf.setMaximum(65534);
        nf.setValueClass(Float.class);
        
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.fill = gbc.HORIZONTAL;
        JPanel optsPanel = new JPanel();
        optsPanel.setBorder(BorderFactory.createTitledBorder(" Indicator Options "));
        add(optsPanel, gbc);
        
        optsPanel.setLayout(new GridBagLayout());
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.fill = gbc.HORIZONTAL;
        JLabel previewLabel = new JLabel("Preview:");
        previewLabel.setHorizontalAlignment(JLabel.RIGHT);
        optsPanel.add(previewLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        previewCheckBox = new JCheckBox();
        previewCheckBox.setSelected(true);
        optsPanel.add(previewCheckBox, gbc);
        
        ChangeListener spinnerChangeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (previewCheckBox.isSelected()) {
                    transferValues();
                    
                    /** forward the event to whom care about it */
                    fireSpinnerChangeEvent(e);
                }
            }
        };
        
        for (int i = 0, n = opts.size(); i < n; i++) {
            Opt opt = opts.get(i);
            
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            gbc.gridwidth = 2;
            gbc.gridheight = 1;
            gbc.fill = gbc.HORIZONTAL;
            gbc.ipadx = 5;
            optNameLables[i] = new JLabel();
            optNameLables[i].setText(opt.getName());
            optsPanel.add(optNameLables[i], gbc);
            
            optSpinnerNumberModels[i] = new SpinnerNumberModel();
            optSpinnerNumberModels[i].setValue(opt.value());
            optSpinnerNumberModels[i].setStepSize(opt.getStep());
            optSpinnerNumberModels[i].setMaximum((Comparable)opt.getMaxValue());
            optSpinnerNumberModels[i].setMinimum((Comparable)opt.getMinValue());
            
            gbc.gridx = 2;
            gbc.gridy = i + 1;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            optValueSpinners[i] = new JSpinner();
            optValueSpinners[i].setPreferredSize(new Dimension(50, 20));
            optValueSpinners[i].setModel(optSpinnerNumberModels[i]);
            optsPanel.add(optValueSpinners[i], gbc);
            
            optValueSpinners[i].addChangeListener(spinnerChangeListener);
            
            optValueSpinners[i].setEnabled(true);
        }
        
        optsPanel.setFocusable(true);
        
        gbc.gridx = 0;
        gbc.gridy = gbc.RELATIVE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        JPanel additionalDecisionsPanel = new JPanel();
        additionalDecisionsPanel.setBorder(BorderFactory.createTitledBorder(" Additional "));
        add(additionalDecisionsPanel, gbc);
        
        additionalDecisionsPanel.setLayout(new GridBagLayout());
        
        gbc.gridx = 0;
        gbc.gridy = gbc.RELATIVE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        JLabel saveAsDefaultLabel = new JLabel("Save As Default:");
        saveAsDefaultLabel.setHorizontalAlignment(JLabel.RIGHT);
        additionalDecisionsPanel.add(saveAsDefaultLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = gbc.RELATIVE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        saveAsDefaultCheckBox = new JCheckBox();
        saveAsDefaultCheckBox.setSelected(false);
        additionalDecisionsPanel.add(saveAsDefaultCheckBox, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = gbc.RELATIVE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        JLabel applyToAllLabel = new JLabel("Apply to All:");
        applyToAllLabel.setHorizontalAlignment(JLabel.RIGHT);
        additionalDecisionsPanel.add(applyToAllLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = gbc.RELATIVE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        applyToAllCheckBox = new JCheckBox();
        applyToAllCheckBox.setSelected(false);
        additionalDecisionsPanel.add(applyToAllCheckBox, gbc);
        
        additionalDecisionsPanel.setFocusable(true);
    }
    
    public int showDialog() {
        Object[] message = {"Please input new options of " + descriptor.getDisplayName() + ":",
        this
        };
        
        int retValue = JOptionPane.showConfirmDialog(
                owner,
                message,
                "Change options",
                JOptionPane.OK_CANCEL_OPTION
                );
        
        if (retValue == JOptionPane.OK_OPTION) {
            try {
                for (JSpinner optValueSpinner : optValueSpinners) {
                    optValueSpinner.commitEdit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            transferValues();
        } else {
            descriptor.setOpts(oldOpts);
        }
        
        return retValue;
    }
    
    private void transferValues() {
        for (int i = 0; i < length; i++) {
            Number optValue = (Number)optValueSpinners[i].getValue();
            opts.get(i).setValue(optValue);
        }
        
        saveAsDefault = saveAsDefaultCheckBox.isSelected();
        applyToAll = applyToAllCheckBox.isSelected();
    }
    
    public List<Opt> getOpts() {
        return opts;
    }
    
    public boolean isSaveAsDefault() {
        return saveAsDefault;
    }
    
    public boolean isApplyToAll() {
        return applyToAll;
    }
    
    public void addSpinnerChangeListener(ChangeListener listener) {
        spinnerChangelistenerList.add(ChangeListener.class, listener);
    }
    
    public void removeSpinnerChangeListener(ChangeListener listener) {
        spinnerChangelistenerList.remove(ChangeListener.class, listener);
    }
    
    private void fireSpinnerChangeEvent(ChangeEvent evt) {
        Object[] listeners = spinnerChangelistenerList.getListenerList();
        /** Each listener occupies two elements - the first is the listener class */
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener)listeners[i + 1]).stateChanged(evt);
            }
        }
    }
    
}




