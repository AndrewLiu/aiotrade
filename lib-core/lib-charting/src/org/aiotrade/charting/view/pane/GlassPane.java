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
package org.aiotrade.charting.view.pane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;
import org.aiotrade.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.math.timeseries.QuoteItem;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.math.timeseries.plottable.Plot;
import org.aiotrade.math.timeseries.computable.ComputableHelper;
import org.aiotrade.math.timeseries.SerItem;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;
import org.aiotrade.charting.chart.Chart;
import org.aiotrade.charting.chart.CursorChart;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.charting.view.pane.Pane.RenderStrategy;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.ChartValidityObserver;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.ReferCursorObserver;
import org.aiotrade.charting.view.WithDrawingPane;
import org.aiotrade.charting.view.WithQuoteChart;
import org.aiotrade.charting.widget.Label;
import org.aiotrade.util.awt.AWTUtil;
import org.aiotrade.util.swing.AIOAutoHideComponent;
import org.aiotrade.util.swing.AIOCloseButton;
import org.aiotrade.util.swing.action.EditAction;
import org.aiotrade.util.swing.action.HideAction;

/**
 * GlassPane overlaps mainChartPane, and is not opaque, thus we should carefully
 * define the contents of it, try the best to avoid add components on it, since
 * when the tranparent components change size, bounds, text etc will cause the
 * components repaint(), and cause the overlapped mainChartPane repaint() in chain.
 * That's why we here use a lot of none component-based lightweight textSegments,
 * pathSegments to draw texts, paths directly on GlassPane. Otherwise, should
 * carefully design container layout manage of the labels, especially do not call
 * any set property methods of labels in paint() routine.
 *
 * @author Caoyuan Deng
 */
public class GlassPane extends Pane implements WithCursorChart {

    private final static Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
    private final static int BUTTON_SIZE = 12;
    private final static Dimension BUTTON_DIMENSION = new Dimension(BUTTON_SIZE, BUTTON_SIZE);
    private final static DecimalFormat MONEY_DECIMAL_FORMAT = new DecimalFormat("0.###");
    private JPanel titlePanel;
    private PinnedMark pinnedMark;
    private AIOCloseButton closeButton;
    private JLabel nameLabel;
    private Map<Ser, AIOCloseButton> overlappingSersToCloseButton = new HashMap<Ser, AIOCloseButton>();
    private Map<Ser, JLabel> overlappingSersToNameLabel = new HashMap<Ser, JLabel>();
    private Map<Var<?>, JLabel> selectedSerVarsToValueLabel = new HashMap<Var<?>, JLabel>();
    private Ser selectedSer;
    private boolean selected;
    private JLabel instantValueLabel;
    private boolean usingInstantTitleValue;

    public GlassPane(ChartView view, DatumPlane datumPlane) {
        super(view, datumPlane);

        setOpaque(false);
        setRenderStrategy(RenderStrategy.NoneBuffer);

        titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setPreferredSize(new Dimension(10, ChartView.TITLE_HEIGHT_PER_LINE));

        setLayout(new BorderLayout());
        add(titlePanel, BorderLayout.NORTH);

        selectedSer = getView().getMainSer();

        closeButton = createCloseButton(getView().getMainSer());
        nameLabel = createNameLabel(getView().getMainSer());

        /** Container should be JComponent instead of JPanel to show selectedMark ? */
        pinnedMark = new PinnedMark();
        pinnedMark.setPreferredSize(BUTTON_DIMENSION);

        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.add(closeButton);
        titlePanel.add(nameLabel);
        //titlePanel.add(pinnedMark);

        PaneMouseInputAdapter paneMouseListener = new PaneMouseInputAdapter();
        addMouseListener(paneMouseListener);
        addMouseMotionListener(paneMouseListener);

        view.getController().addObserver(this, new ReferCursorObserver<ChartingController>() {

            public void update(ChartingController controller) {
                if (!isUsingInstantTitleValue()) {
                    updateSelectedSerVarValues();
                }
            }
        });

        view.getController().addObserver(this, new ChartValidityObserver<ChartingController>() {

            public void update(ChartingController controller) {
                updateMainName();
                updateOverlappingNames();
                if (!isUsingInstantTitleValue()) {
                    updateSelectedSerVarValues();
                }
            }
        });

        view.addObserver(this, new ChartValidityObserver<ChartView>() {

            public void update(ChartView view) {
                updateMainName();
                updateOverlappingNames();
                if (!isUsingInstantTitleValue()) {
                    updateSelectedSerVarValues();
                }
            }
        });

        /**
         * @todo updateTitle() when
         * comparing chart added
         */
    }

    private final AIOCloseButton createCloseButton(final Ser ser) {
        final AIOCloseButton button = new AIOCloseButton();
        button.setOpaque(false);
        button.setForeground(LookFeel.getCurrent().axisColor);
        button.setFocusable(false);
        button.setPreferredSize(BUTTON_DIMENSION);
        button.setMaximumSize(BUTTON_DIMENSION);
        button.setMinimumSize(BUTTON_DIMENSION);
        button.setVisible(true);

        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (getView().getParent() instanceof ChartViewContainer) {
                    if (ser == getSelectedSer()) {
                        if (ser != getView().getMainSer()) {
                            setSelectedSer(getView().getMainSer());
                        } else {
                            setSelectedSer(null);
                        }
                    }
                    final AnalysisContents contents = getView().getController().getContents();
                    final IndicatorDescriptor descriptor = contents.lookupDescriptor(
                            IndicatorDescriptor.class,
                            ser.getClass().getName(),
                            ser.getFreq());
                    if (descriptor != null) {
                        HideAction action = descriptor.lookupAction(HideAction.class);
                        if (action != null) {
                            action.execute();
                        }
                    }
                }
            }
        });

        return button;
    }

    private final JLabel createNameLabel(Ser ser) {
        final JLabel label = new JLabel();
        label.setOpaque(false);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(null); // null, let layer manager to decide the size
        label.setVisible(true);
        final NameLabelMouseInputListener mouseListener = new NameLabelMouseInputListener(ser, label);
        label.addMouseListener(mouseListener);
        label.addMouseMotionListener(mouseListener);

        return label;
    }

    private final class NameLabelMouseInputListener extends MouseInputAdapter {

        private final Ser ser;
        private final JLabel label;
        private boolean rolloverEffectSet;

        public NameLabelMouseInputListener(Ser ser, JLabel label) {
            this.ser = ser;
            this.label = label;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            setSelectedSer(ser);
            setSelected(true);
            if (e.getClickCount() == 2) {
                final AnalysisContents contents = getView().getController().getContents();
                final IndicatorDescriptor descriptor = contents.lookupDescriptor(
                        IndicatorDescriptor.class,
                        ser.getClass().getName(),
                        ser.getFreq());
                if (descriptor != null) {
                    EditAction action = descriptor.lookupAction(EditAction.class);
                    if (action != null) {
                        action.execute();
                    }
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (!rolloverEffectSet) {
                /** @todo */
                label.setBackground(LookFeel.getCurrent().borderColor);
                rolloverEffectSet = true;
                label.repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            /** @todo */
            label.setBackground(LookFeel.getCurrent().backgroundColor);
            rolloverEffectSet = false;
            label.repaint();
        }
    }

    @Override
    protected void plotPane() {
    }

    public boolean isUsingInstantTitleValue() {
        return usingInstantTitleValue;
    }

    public void setUsingInstantTitleValue(boolean b) {
        this.usingInstantTitleValue = b;
    }

    private void updateMainName() {
        closeButton.setForeground(LookFeel.getCurrent().axisColor);
        closeButton.setBackground(LookFeel.getCurrent().backgroundColor);
        if (getSelectedSer() == getView().getMainSer()) {
            closeButton.setChosen(true);
        } else {
            closeButton.setChosen(false);
        }

        nameLabel.setForeground(LookFeel.getCurrent().nameColor);
        nameLabel.setBackground(LookFeel.getCurrent().backgroundColor);
        nameLabel.setFont(LookFeel.getCurrent().axisFont);
        nameLabel.setText(ComputableHelper.getDisplayName(getView().getMainSer()));

        titlePanel.revalidate();
        titlePanel.repaint();

        /** name of comparing quote */
        //        if ( view instanceof AnalysisQuoteChartView) {
        //            final FontMetrics fm = getFontMetrics(getFont());
        //            int xPositionLine2 = MARK_SIZE + 1;
        //            Map<QuoteCompareIndicator, QuoteChart> quoteCompareSerChartMap = ((AnalysisQuoteChartView)view).getQuoteCompareSerChartMap();
        //            for (Ser ser : quoteCompareSerChartMap.keySet()) {
        //                String comparingQuoteName = ser.getShortDescription() + "  ";
        //                Color color = quoteCompareSerChartMap.get(ser).getForeground();
        //                TextSegment text2 = new TextSegment(comparingQuoteName, xPositionLine2, ChartView.TITLE_HEIGHT_PER_LINE * 2, color, null);
        //                addSegment(text2);
        //                xPositionLine2 += fm.stringWidth(comparingQuoteName);
        //            }
        //        }
    }

    private final void updateOverlappingNames() {
        int begIdx = 2;
        final Collection<Ser> overlappingSers = view.getOverlappingSers();
        for (Ser ser : overlappingSers) {
            AIOCloseButton button = overlappingSersToCloseButton.get(ser);
            JLabel label = overlappingSersToNameLabel.get(ser);
            if (button == null) {
                button = createCloseButton(ser);
                label = createNameLabel(ser);

                titlePanel.add(button, begIdx++);
                titlePanel.add(label, begIdx++);
                overlappingSersToCloseButton.put(ser, button);
                overlappingSersToNameLabel.put(ser, label);
            } else {
                begIdx += 2;
            }

            button.setForeground(LookFeel.getCurrent().axisColor);
            button.setBackground(LookFeel.getCurrent().backgroundColor);
            if (getSelectedSer() == ser) {
                button.setChosen(true);
            } else {
                button.setChosen(false);
            }

            label.setForeground(LookFeel.getCurrent().nameColor);
            label.setBackground(LookFeel.getCurrent().backgroundColor);
            label.setFont(LookFeel.getCurrent().axisFont);
            label.setText(ComputableHelper.getDisplayName(ser));
        }

        /** remove unused ser's buttons and labels */
        final Collection<Ser> toBeRemoved = new ArrayList<Ser>();
        for (Ser ser : overlappingSersToCloseButton.keySet()) {
            if (!overlappingSers.contains(ser)) {
                toBeRemoved.add(ser);
            }
        }
        for (Ser ser : toBeRemoved) {
            final AIOCloseButton button = overlappingSersToCloseButton.get(ser);
            final JLabel label = overlappingSersToNameLabel.get(ser);
            AWTUtil.removeAllAWTListenersOf(button);
            AWTUtil.removeAllAWTListenersOf(label);
            titlePanel.remove(button);
            titlePanel.remove(label);
            overlappingSersToCloseButton.remove(ser);
            overlappingSersToNameLabel.remove(ser);
        }

        titlePanel.revalidate();
        titlePanel.repaint();
    }

    /**
     * update name and valueStr of all the vars in this view's selected ser.
     * all those vars with var.getPlot() != Plot.None will be shown with value.
     */
    private void updateSelectedSerVarValues() {
        final Ser ser = getSelectedSer();
        if (ser == null) {
            return;
        }

        final long referTime = getView().getController().getReferCursorTime();
        final SerItem item = ser.getItem(referTime);
        if (item != null) {
            final Collection<Var<?>> serVars = ser.varSet();
            for (Var<?> var : serVars) {
                if (var.getPlot() == Plot.None) {
                    continue;
                }

                StringBuilder vStr = new StringBuilder().append(" ").append(var.getName()).append(": ").append(MONEY_DECIMAL_FORMAT.format(item.getFloat(var)));

                /** lookup this var's chart and use chart's color if possible */
                Chart<?> chartOfVar = null;
                Map<Chart<?>, Set<Var<?>>> chartVarsMap = getView().getChartMapVars(ser);
                for (Chart<?> chart : chartVarsMap.keySet()) {
                    if (chartOfVar != null) {
                        break;
                    }

                    Set<Var<?>> vars = chartVarsMap.get(chart);
                    if (vars != null) {
                        for (Var<?> _var : vars) {
                            if (_var == var) {
                                chartOfVar = chart;
                                break;
                            }
                        }
                    }
                }
                Color color = chartOfVar == null ? LookFeel.getCurrent().nameColor : chartOfVar.getForeground();

                JLabel valueLabel = selectedSerVarsToValueLabel.get(var);
                if (valueLabel == null) {
                    valueLabel = new JLabel();
                    valueLabel.setOpaque(false);
                    valueLabel.setHorizontalAlignment(SwingConstants.LEADING);
                    valueLabel.setPreferredSize(null); // null, let the UI delegate to decide the size

                    titlePanel.add(valueLabel);
                    selectedSerVarsToValueLabel.put(var, valueLabel);
                }

                valueLabel.setForeground(color);
                valueLabel.setBackground(LookFeel.getCurrent().backgroundColor);
                valueLabel.setFont(LookFeel.getCurrent().axisFont);
                valueLabel.setText(vStr.toString());
            }

            /** remove unused vars and their labels */
            Collection<Var<?>> toBeRemoved = new ArrayList<Var<?>>();
            for (Var<?> var : selectedSerVarsToValueLabel.keySet()) {
                if (!serVars.contains(var) || var.getPlot() == Plot.None) {
                    toBeRemoved.add(var);
                }
            }
            for (Var<?> var : toBeRemoved) {
                final JLabel label = selectedSerVarsToValueLabel.get(var);
                // label maybe null? not init yet?
                if (label != null) {
                    AWTUtil.removeAllAWTListenersOf(label);
                    titlePanel.remove(label);
                }
                selectedSerVarsToValueLabel.remove(var);
            }
        }

        titlePanel.revalidate();
        titlePanel.repaint();
    }

    public void updateInstantValue(String valueStr, Color color) {
        if (instantValueLabel == null) {
            instantValueLabel = new JLabel();
            instantValueLabel.setOpaque(false);
            instantValueLabel.setHorizontalAlignment(SwingConstants.LEADING);
            instantValueLabel.setPreferredSize(null); // null, let the UI delegate to decide the size

            titlePanel.add(instantValueLabel);
        }

        instantValueLabel.setForeground(color);
        instantValueLabel.setBackground(LookFeel.getCurrent().backgroundColor);
        instantValueLabel.setFont(LookFeel.getCurrent().axisFont);
        instantValueLabel.setText(valueStr);
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean b) {
        final boolean oldValue = isSelected();
        this.selected = b;
        if (isSelected() != oldValue) {
            /** todo: still need this? */
        }
    }

    private final void setSelectedSer(Ser selectedSer) {
        final Ser oldValue = getSelectedSer();
        this.selectedSer = selectedSer;
        if (getSelectedSer() != oldValue) {
            updateMainName();
            updateOverlappingNames();
            if (!isUsingInstantTitleValue()) {
                updateSelectedSerVarValues();
            }
        }
    }

    private final Ser getSelectedSer() {
        return selectedSer;
    }

    public void setInteractive(boolean b) {
        closeButton.setVisible(b);
    }

    public void setPinned(boolean b) {
        pinnedMark.setAutoHidden(!b);
    }

    /**
     * @NOTICE
     * This will be and only be called when I have mouse motion listener
     */
    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        /** fire to my listeners */
        super.processMouseMotionEvent(e);

        forwardMouseEventToWhoMayBeCoveredByMe(e);
    }

    /**
     * !NOTICE
     * This will be and only be called when I have mouse listener
     */
    @Override
    protected void processMouseEvent(MouseEvent e) {
        /** fire to my listeners */
        super.processMouseEvent(e);

        forwardMouseEventToWhoMayBeCoveredByMe(e);
    }

    private void forwardMouseEventToWhoMayBeCoveredByMe(MouseEvent e) {
        forwardMouseEvent(this, getView().getMainChartPane(), e);
        forwardMouseEvent(this, getView().getParent(), e);

        if (getView() instanceof WithDrawingPane) {
            DrawingPane drawingPane = ((WithDrawingPane) getView()).getSelectedDrawing();
            if (drawingPane != null) {
                forwardMouseEvent(this, drawingPane, e);
                if (drawingPane.getSelectedHandledChart() != null) {
                    setCursor(drawingPane.getSelectedHandledChart().getCursor());
                } else {
                    /**
                     * @credit from msayag@users.sourceforge.net
                     * set to default cursor what ever, especilly when a handledChart
                     * was just deleted.
                     */
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        getView().getController().removeObserversOf(this);
        getView().removeObserversOf(this);

        AWTUtil.removeAllAWTListenersOf(nameLabel);
        AWTUtil.removeAllAWTListenersOf(this);

        super.finalize();
    }

    private class PaneMouseInputAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            Component activeComponent = getActiveComponentAt(e);
            if (activeComponent == null) {
                return;
            }

            if (!(getView().getParent() instanceof ChartViewContainer)) {
                return;
            }
            ChartViewContainer viewContainer = (ChartViewContainer) view.getParent();

            if (activeComponent == titlePanel) {

                if (e.getClickCount() == 1) {

                    if (viewContainer.isInteractive()) {
                        viewContainer.setSelectedView(getView());
                    } else {
                        if (viewContainer.isPinned()) {
                            viewContainer.unPin();
                        } else {
                            viewContainer.pin();
                        }
                    }

                } else if (e.getClickCount() == 2) {

                    getView().popupToDesktop();

                }

            } else if (activeComponent == pinnedMark) {

                if (getView().isPinned()) {
                    getView().unPin();
                } else {
                    getView().pin();
                }

            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            getActiveComponentAt(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            getActiveComponentAt(e);
        }

        /**
         * Decide which componet is active and return it.
         * @return actived component or <code>null</code>
         */
        private Component getActiveComponentAt(MouseEvent e) {
            Point p = e.getPoint();

            if (pinnedMark.contains(p)) {
                pinnedMark.setHidden(false);
                return pinnedMark;
            } else {
                pinnedMark.setHidden(true);
            }

            if (titlePanel.contains(p)) {
                return titlePanel;
            }

            return null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }
    }

    /**
     * Inner pinned mark class
     */
    private class PinnedMark extends AIOAutoHideComponent {

        public PinnedMark() {
            setOpaque(false);
            setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (isHidden()) {
                return;
            }

            Graphics2D g = (Graphics2D) g0;
            g.setColor(LookFeel.getCurrent().axisColor);
            int w = getWidth() - 3;
            int h = getHeight() - 3;

            if (!autoHidden) {
                /** pinned, draw pinned mark (an filled circle) */
                g.fillOval(2, 2, w, h);
            } else {
                if (!hidden) {
                    /** draw to pin mark (an filled circle) */
                    g.fillOval(2, 2, w, h);
                }
            }
        }
    }

    /**
     * implement WithCursorChart
     * ----------------------------------------------------
     */
    public CursorChart createCursorChartInstance(DatumPlane datumPlane) {
        return new MyCursorChart();
    }

    private class MyCursorChart extends CursorChart {

        private final Calendar cal = Calendar.getInstance();

        protected void plotReferCursor() {
            int h = GlassPane.this.getHeight();
            int w = GlassPane.this.getWidth();

            /** plot cross' vertical line */
            if (isCursorCrossVisible()) {
                cursorPath.moveTo(x, 0);
                cursorPath.lineTo(x, h);
            }

            if (getView() instanceof WithQuoteChart) {
                final QuoteSer quoteSer = ((WithQuoteChart) GlassPane.this.getView()).getQuoteSer();
                final QuoteItem item = (QuoteItem) quoteSer.getItemByRow(referRow);
                if (item != null) {
                    float y = isAutoReferCursorValue() ? yv(item.getClose()) : yv(getReferCursorValue());

                    /** plot cross' horizonal line */
                    if (isCursorCrossVisible()) {
                        cursorPath.moveTo(0, y);
                        cursorPath.lineTo(w, y);
                    }
                }
            }

        }

        protected void plotMouseCursor() {
            final int w = GlassPane.this.getWidth();
            final int h = GlassPane.this.getHeight();

            final ChartPane mainChartPane = GlassPane.this.getView().getMainChartPane();

            /** plot vertical line */
            if (isCursorCrossVisible()) {
                cursorPath.moveTo(x, 0);
                cursorPath.lineTo(x, h);
            }

            float y;
            if (GlassPane.this.getView() instanceof WithQuoteChart) {
                cal.setTimeInMillis(mouseTime);

                final QuoteSer quoteSer = ((WithQuoteChart) GlassPane.this.getView()).getQuoteSer();
                QuoteItem item = (QuoteItem) quoteSer.getItemByRow(mouseRow);
                final float vMouse = item == null ? 0 : item.getClose();

                if (mainChartPane.isMouseEntered()) {
                    y = mainChartPane.getYMouse();
                } else {
                    y = item == null ? 0 : mainChartPane.yv(item.getClose());
                }

                /** plot horizonal line */
                if (isCursorCrossVisible()) {
                    cursorPath.moveTo(0, y);
                    cursorPath.lineTo(w, y);
                }

                final float vDisplay = mainChartPane.vy(y);

                String str = null;
                /** normal QuoteChartView ? */
                if (isAutoReferCursorValue()) {
                    item = (QuoteItem) quoteSer.getItemByRow(referRow);
                    final float vRefer = item == null ? 0 : item.getClose();

                    final int period = br(mouseRow) - br(referRow);
                    final float percent = vRefer == 0 ? 0 : 100 * (mainChartPane.vy(y) - vRefer) / vRefer;

                    float volumeSum = 0;
                    final int rowBeg = Math.min(referRow, mouseRow);
                    final int rowEnd = Math.max(referRow, mouseRow);
                    for (int i = rowBeg; i <= rowEnd; i++) {
                        item = (QuoteItem) quoteSer.getItemByRow(i);
                        if (item != null) {
                            volumeSum += item.getVolume();
                        }
                    }

                    str = new StringBuilder(20).append("P: ").append(period).append("  ").append(String.format("%+3.2f", percent)).append("%").append("  V: ").append(String.format("%5.0f", volumeSum)).toString();
                } /** else, usually RealtimeQuoteChartView */
                else {
                    final float vRefer = GlassPane.this.getReferCursorValue();
                    final float percent = vRefer == 0 ? 0 : 100 * (mainChartPane.vy(y) - vRefer) / vRefer;

                    str = new StringBuilder(20).append(MONEY_DECIMAL_FORMAT.format(vDisplay)).append("  ").append(String.format("%+3.2f", percent)).append("%").toString();
                }

                final Label label = addChild(new Label());
                label.setForeground(laf.nameColor);
                label.setFont(laf.axisFont);

                final FontMetrics fm = getFontMetrics(label.getFont());
                label.model().set(w - fm.stringWidth(str) - (BUTTON_SIZE + 1), ChartView.TITLE_HEIGHT_PER_LINE - 2, str);
                label.plot();
            } /** indicator view */
            else {
                if (mainChartPane.isMouseEntered()) {
                    y = mainChartPane.getYMouse();

                    /** plot horizonal line */
                    if (isCursorCrossVisible()) {
                        cursorPath.moveTo(0, y);
                        cursorPath.lineTo(w, y);
                    }
                }
            }

        }
    }
}
