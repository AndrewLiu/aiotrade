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
package org.aiotrade.platform.core.ui.panel;

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.platform.core.sec.Ticker;
import org.aiotrade.platform.core.sec.TickerObserver;
import org.aiotrade.platform.core.sec.TickerSnapshot;

/**
 *
 * @author  Caoyuan Deng
 */
public class RealtimeWatchListPanel extends javax.swing.JPanel implements TickerObserver<TickerSnapshot> {
    
    private final String SYMBOL     = "Symbol";
    private final String TIME       = "Time";
    private final String LAST_PRICE = "Last";
    private final String DAY_VOLUME = "Volume";
    private final String PREV_CLOSE = "Prev. cls";
    private final String DAY_CHANGE = "Change";
    private final String PERCENT    = "Percent";
    private final String DAY_HIGH   = "High";
    private final String DAY_LOW    = "Low";
    private final String DAY_OPON   = "Open";
    
    private final int COLUME_COUNT = 10;
    
    private final String[] columeNames  = new String[] {
        SYMBOL,
        TIME,
        LAST_PRICE,
        DAY_VOLUME,
        PREV_CLOSE,
        DAY_CHANGE,
        PERCENT,
        DAY_HIGH,
        DAY_LOW,
        DAY_OPON
    };
    
    private Map<String, Boolean> symbolMapInWatching   = new HashMap<String, Boolean>();
    private Map<String, Integer> symbolMapRow          = new HashMap<String, Integer>();
    private Map<String, Ticker> smbolMapPreviousTicker = new HashMap<String, Ticker>();
    private Map<Integer, Map<String, Color>> rowMapColColors = 
            new HashMap<Integer, Map<String, Color>>();
    
    private WatchListTableModel tableModel;
    private SimpleDateFormat df = new SimpleDateFormat("hh:mm", Locale.US);
    private Calendar calendar = Calendar.getInstance();
    
    private Color bgColorSelected = new Color(169, 178, 202);
    
    public RealtimeWatchListPanel() {
        initComponents();
        
        tableModel = new WatchListTableModel(columeNames, 0);
        table.setModel(tableModel);
        table.setDefaultRenderer(Object.class, new TrendSensitiveCellRenderer());
    }
    
    private class WatchListTableModel extends DefaultTableModel {
        private Class[] types = new Class[] {
            String.class, String.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class
        };
        
        private boolean[] canEdit = new boolean[] {
            false, false, false, false, false, false, false, false, false, false
        };
        
        public WatchListTableModel(String[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }
        
        @Override
        public Class getColumnClass(int columnIndex) {
            return types[columnIndex];
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit[columnIndex];
        }
        
    }
    
    private final int getColumnIndex(String columeName) {
        int idx = -1;
        
        for (int i = 0, n = table.getColumnCount(); i < n; i++) {
            if (table.getColumnName(i).equals(columeName)) {
                idx = i;
                break;
            }
        }
        
        return idx;
    }
    
    public void update(TickerSnapshot tickerSnapshot) {
        String symbol = tickerSnapshot.getSymbol();
        Ticker snapshotTicker = tickerSnapshot.readTicker();
        
        Ticker previousTicker = smbolMapPreviousTicker.get(symbol);
        if (previousTicker == null) {
            previousTicker = new Ticker();
            smbolMapPreviousTicker.put(symbol, previousTicker);
        }
        
        Boolean inWatching = symbolMapInWatching.get(symbol);
        if (inWatching == null || !inWatching) {
            return;
        }
        
        /**
         * @NOTICE
         * Should set columeColors[] before addRow() or setValue() of table to
         * make the color effects take place at once.
         */
        
        Integer row = symbolMapRow.get(symbol);
        if (row == null) {
            row = tableModel.getRowCount();
            symbolMapRow.put(symbol, row);
            
            Map<String, Color> symbolMapColColor = new HashMap<String, Color>();
            for (String string : columeNames) {
                symbolMapColColor.put(string, Color.WHITE);
            }
            rowMapColColors.put(row, symbolMapColColor);
            
            setColColorsByTicker(symbolMapColColor, snapshotTicker, null, inWatching);
            
            tableModel.addRow(composeRowData(symbol, snapshotTicker));
        } else {
            if (snapshotTicker.isDayVolumeGrown(previousTicker)) {
                Map<String, Color> symbolMapColColor = rowMapColColors.get(row);
                setColColorsByTicker(symbolMapColColor, snapshotTicker, previousTicker, inWatching);
                
                Object[] rowData1 = composeRowData(symbol, snapshotTicker);
                for (int i = 0; i < rowData1.length; i++) {
                    table.setValueAt(rowData1[i], row, i);
                }
            }
        }
        
        previousTicker.copy(snapshotTicker);
    }
    
    private final static Color SWITCH_COLOR_A = Color.WHITE;
    private final static Color SWITCH_COLOR_B = new Color(128, 192, 192); //Color.CYAN;
    private void setColColorsByTicker(Map<String, Color> symbolMapColColor, Ticker ticker, Ticker prevTicker, boolean inWatching) {
        Color bgColor = inWatching ? Color.WHITE : Color.GRAY.brighter();
        
        for (String columeName : symbolMapColColor.keySet()) {
            symbolMapColColor.put(columeName, bgColor);
        }
        
        Color positiveColor = LookFeel.getCurrent().getPositiveBgColor();
        Color negativeColor = LookFeel.getCurrent().getNegativeBgColor();
        
        if (inWatching) {
            /** color of volume should be remembered for switching between two colors */
            symbolMapColColor.put(DAY_VOLUME, bgColor);
        }
        
        if (ticker != null) {
            if (ticker.get(Ticker.DAY_CHANGE) > 0) {
                symbolMapColColor.put(DAY_CHANGE, positiveColor);
                symbolMapColColor.put(PERCENT,    positiveColor);
            } else if (ticker.get(Ticker.DAY_CHANGE) < 0) {
                symbolMapColColor.put(DAY_CHANGE, negativeColor);
                symbolMapColColor.put(PERCENT,    negativeColor);
            } else {
                symbolMapColColor.put(DAY_CHANGE, Color.YELLOW);
                symbolMapColColor.put(PERCENT,    Color.YELLOW);
            }
            
            if (prevTicker != null) {
                if (ticker.isDayVolumeChanged(prevTicker)) {
                    /** lastPrice's color */
                    switch (ticker.compareLastCloseTo(prevTicker)) {
                        case 1:
                            symbolMapColColor.put(LAST_PRICE, positiveColor);
                            break;
                        case 0:
                            symbolMapColColor.put(LAST_PRICE, Color.YELLOW);
                            break;
                        case -1:
                            symbolMapColColor.put(LAST_PRICE, negativeColor);
                            break;
                        default:
                    }
                    
                    /** volumes color switchs between two colors if ticker renewed */
                    if (symbolMapColColor.get(DAY_VOLUME).equals(SWITCH_COLOR_A)) {
                        symbolMapColColor.put(DAY_VOLUME, SWITCH_COLOR_B);
                    } else {
                        symbolMapColColor.put(DAY_VOLUME, SWITCH_COLOR_A);
                    }
                }
            }
        }
        
        
        
    }
    
    private Object[] rowData = new Object[COLUME_COUNT];
    private Object[] composeRowData(String symbol, Ticker ticker) {
        calendar.setTimeInMillis(ticker.getTime());
        
        rowData[getColumnIndex(SYMBOL)]     = symbol;
        rowData[getColumnIndex(TIME)]       = df.format(calendar.getTime());
        rowData[getColumnIndex(LAST_PRICE)] = String.format("%5.2f", ticker.get(Ticker.LAST_PRICE));
        rowData[getColumnIndex(DAY_VOLUME)] = ticker.get(Ticker.DAY_VOLUME);
        rowData[getColumnIndex(PREV_CLOSE)] = String.format("%5.2f", ticker.get(Ticker.PREV_CLOSE));
        rowData[getColumnIndex(DAY_CHANGE)] = String.format("%5.2f", ticker.get(Ticker.DAY_CHANGE));
        rowData[getColumnIndex(PERCENT)]    = String.format("%+3.2f", ticker.getChangeInPercent()) + "%";
        rowData[getColumnIndex(DAY_HIGH)]   = String.format("%5.2f", ticker.get(Ticker.DAY_HIGH));
        rowData[getColumnIndex(DAY_LOW)]    = String.format("%5.2f", ticker.get(Ticker.DAY_LOW));
        rowData[getColumnIndex(DAY_OPON)]   = String.format("%5.2f", ticker.get(Ticker.DAY_OPEN));
        
        return rowData;
    }
    
    public void watch(String symbol) {
        symbolMapInWatching.put(symbol, true);
        
        Integer row = symbolMapRow.get(symbol);
        if (row == null) {
            return;
        }
        
        Ticker lastTicker = smbolMapPreviousTicker.get(symbol);
        Map<String, Color> columeColorMap = rowMapColColors.get(row);
        if (columeColorMap != null) {
            setColColorsByTicker(columeColorMap, lastTicker, null, true);
        }
        
        repaint();
    }
    
    public void unWatch(String symbol) {
        symbolMapInWatching.put(symbol, false);
        
        Integer row = symbolMapRow.get(symbol);
        if (row == null) {
            return;
        }
        
        Ticker lastTicker = smbolMapPreviousTicker.get(symbol);
        Map<String, Color> columeColorMap = rowMapColColors.get(row);
        if (columeColorMap != null) {
            setColColorsByTicker(columeColorMap, lastTicker, null, false);
        }
        
        repaint();
    }
    
    public void clearAllWatch() {
        symbolMapInWatching.clear();
        symbolMapRow.clear();
        smbolMapPreviousTicker.clear();
        rowMapColColors.clear();
    }
    
    public JTable getWatchListTable() {
        return table;
    }
    
    public String getSymbolAtRow(int row) {
        if (row < table.getRowCount() && row >= 0) {
            return table.getValueAt(row, getColumnIndex(SYMBOL)).toString();
        } else {
            return null;
        }
    }
    
    public class TrendSensitiveCellRenderer extends JLabel implements TableCellRenderer {
        
        String symbol;
        
        public TrendSensitiveCellRenderer() {
            this.setOpaque(true);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            /**
             * @NOTICE
             * Here we should use watchListTable.getColumeName(column) which is
             * not the same as watchListTableModel.getColumeName(column).
             * Especially: after you draged and moved the table colume, the
             * column index of watchListTable will change, but the column index
             * of watchListTableModel will remain the same.
             */
            String columnName = table.getColumnName(column);
            
            Map<String, Color> symbolMapColColor = rowMapColColors.get(row);
            
            this.setBackground(symbolMapColColor.get(columnName));
            if (isSelected && columnName.equals(SYMBOL)) {
                this.setBackground(bgColorSelected);
            }
            
            this.setForeground(Color.BLACK);
            
            this.setText(null);
            
            if (value != null) {
                if (columnName.equals(SYMBOL)) {
                    this.setHorizontalAlignment(JLabel.LEADING);
                } else if (columnName.equals(TIME)) {
                    this.setHorizontalAlignment(JLabel.CENTER);
                } else if (columnName.equals(LAST_PRICE)) {
                    this.setHorizontalAlignment(JLabel.TRAILING);
                    this.setBackground(symbolMapColColor.get(columnName));
                } else if (columnName.equals(DAY_VOLUME)) {
                    this.setHorizontalAlignment(JLabel.TRAILING);
                    this.setBackground(symbolMapColColor.get(columnName));
                } else if (columnName.equals(PREV_CLOSE)) {
                    this.setHorizontalAlignment(JLabel.TRAILING);
                } else if (columnName.equals(DAY_CHANGE)) {
                    this.setHorizontalAlignment(JLabel.TRAILING);
                    this.setBackground(symbolMapColColor.get(columnName));
                } else if (columnName.equals(PERCENT)) {
                    this.setHorizontalAlignment(JLabel.TRAILING);
                    this.setBackground(symbolMapColColor.get(columnName));
                } else if (columnName.equals(DAY_OPON)) {
                    this.setHorizontalAlignment(JLabel.TRAILING);
                } else {
                    this.setHorizontalAlignment(JLabel.TRAILING);
                }
            }
            this.setText(value.toString());
            
            return this;
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        table.setFont(new java.awt.Font("SansSerif", 0, 12));
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(table);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
    
}
