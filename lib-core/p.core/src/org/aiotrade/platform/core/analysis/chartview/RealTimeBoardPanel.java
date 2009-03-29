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
package org.aiotrade.platform.core.analysis.chartview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.platform.core.dataserver.TickerContract;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.platform.core.sec.Ticker;
import org.aiotrade.platform.core.sec.TickerObserver;
import org.aiotrade.platform.core.sec.TickerSnapshot;
import org.aiotrade.lib.util.swing.GBC;
import org.aiotrade.lib.util.swing.plaf.AIOScrollPaneStyleBorder;
import org.aiotrade.lib.util.swing.table.AttributiveCellRenderer;
import org.aiotrade.lib.util.swing.table.AttributiveCellTableModel;
import org.aiotrade.lib.util.swing.table.DefaultCellAttribute;
import org.aiotrade.lib.util.swing.table.MultiSpanCellTable;

/**
 *
 * @author Caoyuan Deng
 */
public class RealTimeBoardPanel extends javax.swing.JPanel implements TickerObserver<TickerSnapshot> {

    private static ResourceBundle BUNDLE = ResourceBundle.getBundle("org.aiotrade.platform.core.analysis.chartview.Bundle");
    private static NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();
    private Sec sec;
    private TickerContract tickerContract;
    private Ticker prevTicker;
    private DefaultTableModel infoModel;
    private DefaultTableModel depthModel;
    private DefaultTableModel tickerModel;
    private DefaultCellAttribute infoCellAttr;
    private DefaultCellAttribute depthCellAttr;
    private Calendar marketCal;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private RealTimeChartViewContainer viewContainer;
    private JTable infoTable;
    private JTable depthTable;
    private JTable tickerTable;
    private JScrollPane tickerPane;
    private ValueCell symbol = new ValueCell();
    private ValueCell sname = new ValueCell();
    private ValueCell currentTime = new ValueCell();
    private ValueCell dayChange = new ValueCell();
    private ValueCell dayHigh = new ValueCell();
    private ValueCell dayLow = new ValueCell();
    private ValueCell dayOpen = new ValueCell();
    private ValueCell dayVolume = new ValueCell();
    private ValueCell lastPrice = new ValueCell();
    private ValueCell dayPercent = new ValueCell();
    private ValueCell prevClose = new ValueCell();

    /**
     * Creates new form RealtimeBoardPanel
     */
    public RealTimeBoardPanel(Sec sec, AnalysisContents contents) {
        this.sec = sec;
        TimeZone timeZone = sec.getMarket().getTimeZone();
        this.marketCal = Calendar.getInstance(timeZone);
        this.sdf.setTimeZone(timeZone);
        this.tickerContract = sec.getTickerContract();
        initComponents();

        TableColumnModel columeModel;
        columeModel = infoTable.getColumnModel();
        columeModel.getColumn(0).setMaxWidth(35);
        columeModel.getColumn(2).setMaxWidth(35);

        columeModel = depthTable.getColumnModel();
        columeModel.getColumn(0).setMinWidth(12);
        columeModel.getColumn(1).setMinWidth(35);

        columeModel = tickerTable.getColumnModel();
        columeModel.getColumn(0).setMinWidth(22);
        columeModel.getColumn(1).setMinWidth(30);

//        ChartingController controller = ChartingControllerFactory.createInstance(
//                sec.getTickerSer(), contents);
//        viewContainer = controller.createChartViewContainer(
//                RealTimeChartViewContainer.class, this);
//
//        chartPane.setLayout(new BorderLayout());
//        chartPane.add(viewContainer, BorderLayout.CENTER);
    }

    private void initComponents() {
        setFocusable(false);

        tickerPane = new JScrollPane();
        //chartPane = new JPanel();

        Object[][] infoModelData = {
            {BUNDLE.getString("lastPrice"), lastPrice, BUNDLE.getString("dayVolume"), dayVolume},
            {BUNDLE.getString("dayChange"), dayChange, BUNDLE.getString("dayHigh"), dayHigh},
            {BUNDLE.getString("dayPercent"), dayPercent, BUNDLE.getString("dayLow"), dayLow},
            {BUNDLE.getString("prevClose"), prevClose, BUNDLE.getString("dayOpen"), dayOpen}
        };
        infoModel = new AttributiveCellTableModel(infoModelData,
                new String[]{"A", "B", "C", "D"});

        infoCellAttr = (DefaultCellAttribute) ((AttributiveCellTableModel) infoModel).getCellAttribute();
        /* Code for combining cells
        infoCellAttr.combine(new int[]{0}, new int[]{0, 1});
        infoCellAttr.combine(new int[]{1}, new int[]{0, 1, 2, 3});
         */

        ValueCell.setRowColumn(infoModelData);
        symbol.value = sec.getUniSymbol();
        if (tickerContract != null) {
            sname.value = tickerContract.getShortName();
        }

        for (ValueCell cell : new ValueCell[]{
                    lastPrice, dayChange, dayPercent, prevClose, dayVolume, dayHigh, dayLow, dayOpen
                }) {
            infoCellAttr.setHorizontalAlignment(JLabel.TRAILING, cell.row, cell.column);
        }

        infoTable = new MultiSpanCellTable(infoModel);
        infoTable.setDefaultRenderer(Object.class, new AttributiveCellRenderer());
        infoTable.setFocusable(false);
        infoTable.setCellSelectionEnabled(false);
        infoTable.setShowHorizontalLines(false);
        infoTable.setShowVerticalLines(false);
        infoTable.setBorder(new AIOScrollPaneStyleBorder(LookFeel.getCurrent().heavyBackgroundColor));
        infoTable.setBackground(LookFeel.getCurrent().heavyBackgroundColor);

        depthModel = new AttributiveCellTableModel(
                new Object[][]{
                    {"卖⑤", null, null},
                    {"卖④", null, null},
                    {"卖③", null, null},
                    {"卖②", null, null},
                    {"卖①", null, null},
                    {"成交", null, null},
                    {"买①", null, null},
                    {"买②", null, null},
                    {"买③", null, null},
                    {"买④", null, null},
                    {"买⑤", null, null}
                },
                new String[]{
                    BUNDLE.getString("askBid"), BUNDLE.getString("price"), BUNDLE.getString("size")
                });

        int depth = 5;
        int dealRow = 5;
        depthModel.setValueAt(BUNDLE.getString("deal"), dealRow, 0);
        for (int i = 0; i < depth; i++) {
            int askIdx = depth - 1 - i;
            int askRow = i;
            depthModel.setValueAt(BUNDLE.getString("bid") + numbers[askIdx], askRow, 0);
            int bidIdx = i;
            int bidRow = depth + 1 + i;
            depthModel.setValueAt(BUNDLE.getString("ask") + numbers[bidIdx], bidRow, 0);
        }

        depthTable = new MultiSpanCellTable(depthModel);
        depthCellAttr = (DefaultCellAttribute) ((AttributiveCellTableModel) depthModel).getCellAttribute();

        for (int i = 0; i < 11; i++) {
            for (int j = 1; j < 3; j++) {
                depthCellAttr.setHorizontalAlignment(JLabel.TRAILING, i, j);
            }
        }
        depthCellAttr.setHorizontalAlignment(JLabel.LEADING, 5, 0);
//        for (int j = 0; j < 3; j++) {
//            depthCellAttr.setBackground(Color.gray, 5, j);
//        }

        depthTable.setDefaultRenderer(Object.class, new AttributiveCellRenderer());
        depthTable.setTableHeader(null);
        depthTable.setFocusable(false);
        depthTable.setCellSelectionEnabled(false);
        depthTable.setShowHorizontalLines(false);
        depthTable.setShowVerticalLines(false);
        depthTable.setBorder(new AIOScrollPaneStyleBorder(LookFeel.getCurrent().borderColor));
        depthTable.setBackground(LookFeel.getCurrent().infoBackgroundColor);

        tickerModel = new DefaultTableModel(
                new Object[][]{
                    {null, null, null},
                    {null, null, null},
                    {null, null, null},
                    {null, null, null},
                    {null, null, null},
                    {null, null, null},
                    {null, null, null},
                    {null, null, null},
                    {null, null, null},
                    {null, null, null}
                },
                new String[]{
                    BUNDLE.getString("time"), BUNDLE.getString("price"), BUNDLE.getString("size")
                }) {

            boolean[] canEdit = new boolean[]{
                false, false, false
            };

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        tickerTable = new JTable(tickerModel);
        tickerTable.setDefaultRenderer(Object.class, new TrendSensitiveCellRenderer());
        tickerTable.setFocusable(false);
        tickerTable.setCellSelectionEnabled(false);
        tickerTable.setShowHorizontalLines(false);
        tickerTable.setShowVerticalLines(false);

        /* @Note Border of JScrollPane may cannot be set by #setBorder, at least in Metal L&F: */
        UIManager.put("ScrollPane.border", AIOScrollPaneStyleBorder.class.getName());
        tickerPane.setBackground(LookFeel.getCurrent().infoBackgroundColor);
        tickerPane.setViewportView(tickerTable);

        // put infoTable to a box to simple the insets setting:
        Box infoBox = new Box(BoxLayout.Y_AXIS) {

            // box does not paint anything, override paintComponent to get background:
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(getBackground());
                Rectangle rect = getBounds();
                g.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
        };
        infoBox.setBackground(LookFeel.getCurrent().heavyBackgroundColor);
        infoBox.add(Box.createVerticalStrut(5));
        infoBox.add(infoTable);
        infoBox.add(Box.createVerticalStrut(4));

        // put fix size components to box
        Box box = Box.createVerticalBox();
        box.add(infoBox);
        box.add(Box.createVerticalStrut(2));
        box.add(depthTable);
        box.add(Box.createVerticalStrut(2));

        setLayout(new GridBagLayout());
        add(box, new GBC(0, 0).setFill(GBC.BOTH).setWeight(100, 0));
        add(tickerPane, new GBC(0, 1).setFill(GBC.BOTH).setWeight(100, 100));
        //add(chartPane, new GBC(0,2).setFill(GBC.BOTH).setWeight(100, 100));
    }
    String[] numbers = new String[]{"①", "②", "③", "④", "⑤"};

    public void update(TickerSnapshot tickerSnapshot) {
        Color neutralColor = LookFeel.getCurrent().getNeutralColor();
        Color positiveColor = LookFeel.getCurrent().getPositiveColor();
        Color negativeColor = LookFeel.getCurrent().getNegativeColor();
        symbol.value = tickerSnapshot.getSymbol();

        final Ticker snapshotTicker = tickerSnapshot.readTicker();

        int currentSize;
        if (prevTicker != null) {
            currentSize = (int) (snapshotTicker.get(Ticker.DAY_VOLUME) - prevTicker.get(Ticker.DAY_VOLUME));
        } else {
            currentSize = 0;
        }

        int depth = snapshotTicker.getDepth();
        int dealRow = 5;
        depthModel.setValueAt(String.format("%8.2f", snapshotTicker.get(Ticker.LAST_PRICE)), dealRow, 1);
        depthModel.setValueAt(prevTicker == null ? "-" : currentSize, dealRow, 2);
        for (int i = 0; i < depth; i++) {
            int askIdx = depth - 1 - i;
            int askRow = i;
            depthModel.setValueAt(String.format("%8.2f", snapshotTicker.getAskPrice(askIdx)), askRow, 1);
            depthModel.setValueAt(String.valueOf((int) snapshotTicker.getAskSize(askIdx)), askRow, 2);
            int bidIdx = i;
            int bidRow = depth + 1 + i;
            depthModel.setValueAt(String.format("%8.2f", snapshotTicker.getBidPrice(bidIdx)), bidRow, 1);
            depthModel.setValueAt(String.valueOf((int) snapshotTicker.getBidSize(bidIdx)), bidRow, 2);
        }

        marketCal.setTimeInMillis(snapshotTicker.getTime());
        Date lastTradeTime = marketCal.getTime();
        currentTime.value = sdf.format(lastTradeTime);
        lastPrice.value = String.format("%8.2f", snapshotTicker.get(Ticker.LAST_PRICE));
        prevClose.value = String.format("%8.2f", snapshotTicker.get(Ticker.PREV_CLOSE));
        dayOpen.value = String.format("%8.2f", snapshotTicker.get(Ticker.DAY_OPEN));
        dayHigh.value = String.format("%8.2f", snapshotTicker.get(Ticker.DAY_HIGH));
        dayLow.value = String.format("%8.2f", snapshotTicker.get(Ticker.DAY_LOW));
        dayChange.value = String.format("%+8.2f", snapshotTicker.get(Ticker.DAY_CHANGE));
        dayPercent.value = String.format("%+3.2f", snapshotTicker.getChangeInPercent()) + "%";
        dayVolume.value = String.valueOf(snapshotTicker.get(Ticker.DAY_VOLUME));

        Color fgColor = Color.BLACK;
        Color bgColor = neutralColor;
        if (snapshotTicker.get(Ticker.DAY_CHANGE) > 0) {
            fgColor = Color.WHITE;
            bgColor = positiveColor;
        } else if (snapshotTicker.get(Ticker.DAY_CHANGE) < 0) {
            fgColor = Color.WHITE;
            bgColor = negativeColor;
        }
        infoCellAttr.setForeground(fgColor, dayChange.row, dayChange.column);
        infoCellAttr.setForeground(fgColor, dayPercent.row, dayPercent.column);
        infoCellAttr.setBackground(bgColor, dayChange.row, dayChange.column);
        infoCellAttr.setBackground(bgColor, dayPercent.row, dayPercent.column);

        /**
         * Sometimes, DataUpdatedEvent is fired by other symbols' new ticker,
         * so assert here again.
         * @see UpdateServer.class in AbstractTickerDataServer.class and YahooTickerDataServer.class
         */
        if (prevTicker != null && snapshotTicker.isDayVolumeChanged(prevTicker)) {
            fgColor = Color.BLACK;
            bgColor = neutralColor;
            switch (snapshotTicker.compareLastCloseTo(prevTicker)) {
                case 1:
                    fgColor = Color.WHITE;
                    bgColor = positiveColor;
                    break;
                case -1:
                    fgColor = Color.WHITE;
                    bgColor = negativeColor;
                    break;
                default:
            }

        }
        infoCellAttr.setForeground(fgColor, lastPrice.row, lastPrice.column);
        infoCellAttr.setBackground(bgColor, lastPrice.row, lastPrice.column);
        depthCellAttr.setForeground(fgColor, dealRow, 1); // last deal
        depthCellAttr.setBackground(bgColor, dealRow, 1); // last deal

        Object[] tickerRow = new Object[]{
            sdf.format(lastTradeTime),
            String.format("%5.2f", snapshotTicker.get(Ticker.LAST_PRICE)),
            prevTicker == null ? "-" : currentSize};
        tickerModel.insertRow(0, tickerRow);

        if (prevTicker == null) {
            prevTicker = new Ticker();
        }
        prevTicker.copy(snapshotTicker);

        repaint();
    }

    private void showCell(JTable table, int row, int column) {
        Rectangle rect = table.getCellRect(row, column, true);
        table.scrollRectToVisible(rect);
        table.clearSelection();
        table.setRowSelectionInterval(row, row);
        /* notify the model */
        ((DefaultTableModel) table.getModel()).fireTableDataChanged();
    }

    public class TrendSensitiveCellRenderer extends DefaultTableCellRenderer {

        public TrendSensitiveCellRenderer() {
            this.setForeground(Color.BLACK);
            this.setBackground(LookFeel.getCurrent().backgroundColor);
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            /** Beacuse this will be a sinleton for all cells, so, should clear it first */
            this.setForeground(Color.BLACK);
            this.setBackground(LookFeel.getCurrent().backgroundColor);
            this.setText(null);

            if (value != null) {
                switch (column) {
                    case 0: // Time
                        this.setHorizontalAlignment(JLabel.LEADING);
                        break;
                    case 1: // Price
                        this.setHorizontalAlignment(JLabel.TRAILING);
                        if (row + 1 < table.getRowCount()) {
                            try {
                                float floatValue;
                                floatValue = NUMBER_FORMAT.parse(value.toString().trim()).floatValue();
                                Object prevValue = table.getValueAt(row + 1, column);
                                if (prevValue != null) {
                                    float prevFloatValue;
                                    prevFloatValue = NUMBER_FORMAT.parse(prevValue.toString().trim()).floatValue();
                                    if (floatValue > prevFloatValue) {
                                        this.setForeground(Color.WHITE);
                                        this.setBackground(LookFeel.getCurrent().getPositiveBgColor());
                                    } else if (floatValue < prevFloatValue) {
                                        this.setForeground(Color.WHITE);
                                        this.setBackground(LookFeel.getCurrent().getNegativeBgColor());
                                    } else {
                                        this.setForeground(Color.BLACK);
                                        this.setBackground(LookFeel.getCurrent().getNeutralBgColor());
                                    }
                                }
                            } catch (ParseException ex) {
                                ex.printStackTrace();
                            }
                        }
                        break;
                    case 2: // Size
                        this.setHorizontalAlignment(JLabel.TRAILING);
                        break;
                }
                this.setText(value.toString());
            }

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return this;
        }
    }

    public ChartViewContainer getChartViewContainer() {
        return viewContainer;
    }

    private void test() {
        tickerModel.addRow(new Object[]{"00:01", "12334", "1"});
        tickerModel.addRow(new Object[]{"00:02", "12333", "1234"});
        tickerModel.addRow(new Object[]{"00:03", "12335", "12345"});
        tickerModel.addRow(new Object[]{"00:04", "12334", "123"});
        tickerModel.addRow(new Object[]{"00:05", "12334", "123"});
        showCell(tickerTable, tickerTable.getRowCount() - 1, 0);
    }
}

class ValueCell {

    String value;
    int row;
    int column;

    ValueCell() {
    }

    ValueCell(int row, int column) {
        this.row = row;
        this.column = column;
    }

    @Override
    public String toString() {
        return value;
    }

    static void setRowColumn(Object[][] modelData) {
        for (int i = 0; i < modelData.length; i++) {
            Object[] rows = modelData[i];
            for (int j = 0; j < rows.length; j++) {
                Object o = rows[j];
                if (o != null && o instanceof ValueCell) {
                    ValueCell cell = (ValueCell) o;
                    cell.row = i;
                    cell.column = j;
                }
            }
        }


    }
}
