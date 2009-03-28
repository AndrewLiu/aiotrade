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

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.aiotrade.charting.chart.Chart;
import org.aiotrade.charting.chart.CursorChart;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.MouseCursorObserver;
import org.aiotrade.charting.widget.Widget;
import org.aiotrade.util.ReferenceOnly;

/**
 *
 * @author Caoyuan Deng
 */
public abstract class Pane extends JComponent {

    public final static int DEPTH_FRONT = 1000;
    /** usually for quote chart, so charts of other indicatos can begin from 0: */
    public final static int DEPTH_DEFAULT = -1;
    public final static int DEPTH_CHART_BEGIN = 0;
    /** usually for drawing chart, it will be in front: */
    public final static int DEPTH_DRAWING = 100;
    public final static int DEPTH_GRADIENT_BEGIN = -10;
    public final static int DEPTH_INVISIBLE = -100;
    final protected ChartView view;
    protected DatumPlane datumPlane;
    private List<Widget<?>> widgets = new ArrayList<Widget<?>>();
    private SortedSet<Chart<?>> charts = new TreeSet<Chart<?>>();
    private CursorChart referCursorChart;
    private CursorChart mouseCursorChart;
    private float referCursorValue;
    private boolean autoReferCursorValue = true;
    private BufferedImage backRenderBuffer;
    private int wBackRenderBuffer, hBackRenderBuffer;
    private RenderStrategy renderStrategy = RenderStrategy.NoneBuffer;

    protected enum RenderStrategy {

        NoneBuffer,
        BufferedImage
    }

    public Pane(ChartView view, DatumPlane datumPlane) {
        this.view = view;

        if (datumPlane != null) {
            this.datumPlane = datumPlane;
        } else {
            /** if a null datumPlane given, we assume it will be just me, such as a ChartPane */
            assert this instanceof DatumPlane : "A null datumPlane given, the datumPlane should be me!";
            this.datumPlane = (DatumPlane) this;
        }

        if (this instanceof WithCursorChart) {
            createCursorChart(this.datumPlane);
            getView().getController().addObserver(this, new MouseCursorObserver<ChartingController>() {

                public void update(ChartingController controller) {
                    paintChartOnXORMode(mouseCursorChart);
                }
            });
        }

        setDoubleBuffered(true);
    }

    protected void setRenderStrategy(RenderStrategy renderStrategy) {
        this.renderStrategy = renderStrategy;
    }

    public DatumPlane getDatumPlane() {
        return datumPlane;
    }

    /** helper method for implementing WithCursorChart */
    private void createCursorChart(DatumPlane datumPlane) {
        if (!(this instanceof WithCursorChart)) {
            assert false : "Only WithCursorChart supports this method!";
            return;
        }

        /** create refer cursor chart */
        referCursorChart = ((WithCursorChart) this).createCursorChartInstance(datumPlane);
        referCursorChart.setType(CursorChart.Type.Refer);
        referCursorChart.set(datumPlane, view.getController().getMasterSer(), DEPTH_DEFAULT - 1);

        /** create mouse cursor chart */
        mouseCursorChart = ((WithCursorChart) this).createCursorChartInstance(datumPlane);
        mouseCursorChart.setType(CursorChart.Type.Mouse);
        mouseCursorChart.set(datumPlane, view.getController().getMasterSer(), DEPTH_FRONT);

        referCursorChart.setFirstPlotting(true);
        mouseCursorChart.setFirstPlotting(true);
    }

    /**
     * @NOTICE
     * Should reset: chart.setFirstPlotting(true) when ever repaint() or
     * paint() happened.
     *
     * @param chart, chart to be plot and paint
     * @see #postPaintComponent()
     */
    protected void paintChartOnXORMode(Chart<?> chart) {
        Graphics g = getGraphics();
        if (g != null) {
            try {
                g.setXORMode(getBackground());

                if (chart.isFirstPlotting()) {
                    chart.setFirstPlotting(false);
                } else {
                    /** erase previous drawing */
                    chart.render(g);
                }
                /** current new drawing */
                chart.plot();
                chart.render(g);

                /** restore to paintMode */
                g.setPaintMode();
            } finally {
                g.dispose();
            }
        }
    }

    public final ChartView getView() {
        return view;
    }

    @Override
    protected void paintComponent(Graphics g) {
        prePaintComponent();

        if (renderStrategy == RenderStrategy.NoneBuffer) {
            render(g);
        } else {
            paintToBackRenderBuffer();
            g.drawImage(getBackRenderBuffer(), 0, 0, this);
        }

        postPaintComponent();
    }

    protected void prePaintComponent() {
        assert datumPlane != null : "datumPlane can not be null!";
        /**
         * @NOTICE
         * The repaint order in Java Swing is not certain, the axisXPane and
         * axisYPane etc may be painted earlier than datumPlane, that will cause
         * incorrect geometry for axisXPane and axisYPane, for example: the
         * maxValue and minValue changed, but the datumPlane's computeGeometry()
         * is still not been called yet. Although we can let datumPlane aware of
         * all of the changes that may affect geometry, but the simplest way is
         * just ask the Objects that depend on datumPlane and may be painted
         * earlier than datumPlane, call datumPlane.computeGeomtry() first.
         */
        datumPlane.computeGeometry();
    }

    protected void postPaintComponent() {
        if (this instanceof WithCursorChart) {
            referCursorChart.setFirstPlotting(true);
            mouseCursorChart.setFirstPlotting(true);
        }
    }

    private void processUserRenderOptions(Graphics g0) {
        final Graphics2D g = (Graphics2D) g0;

        if (LookFeel.getCurrent().isAntiAlias()) {
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        if (isOpaque()) {
            /**
             * Process background by self,
             *
             * @NOTICE
             * don't forget to setBackgroud() to keep this component's properties consistent
             */
            setBackground(LookFeel.getCurrent().backgroundColor);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        setFont(LookFeel.getCurrent().axisFont);
        g.setFont(getFont());
    }

    /**
     * @NOTICE
     * charts should be set() here only, because this method will be called in
     * paintComponent() after fetching some very important parameters which will
     * be used by charts' plotting;
     */
    private void render(Graphics g0) {
        final Graphics2D g = (Graphics2D) g0;

        processUserRenderOptions(g);

        /** plot and render segments added by plotMore() */
        widgets.clear();
        plotPane();
        for (Widget<?> widget : widgets) {
            widget.render(g);
        }

        /** plot and render charts that have been put */
        for (Chart<?> chart : charts) {
            chart.plot();
            chart.render(g);
        }

        /** plot and render refer cursor chart */
        if (this instanceof WithCursorChart) {
            referCursorChart.plot();
            referCursorChart.render(g);
        }
    }

    private final BufferedImage getBackRenderBuffer() {
        return backRenderBuffer;
    }

    private final void checkBackRenderBuffer() {
        if (backRenderBuffer != null &&
                (wBackRenderBuffer != getWidth() || hBackRenderBuffer != getHeight())) {
            backRenderBuffer.flush();
            backRenderBuffer = null;
        }

        if (backRenderBuffer == null) {
            wBackRenderBuffer = getWidth();
            hBackRenderBuffer = getHeight();
            backRenderBuffer = createBackRenderBuffer();
        }
    }

    private final BufferedImage createBackRenderBuffer() {
        return new BufferedImage(wBackRenderBuffer, hBackRenderBuffer, BufferedImage.TYPE_INT_ARGB);
    }

    private final void paintToBackRenderBuffer() {
        checkBackRenderBuffer();
        final Graphics2D g = (Graphics2D) getBackRenderBuffer().getGraphics();

        /** clear image with transparent alpha */
        final Composite backupComposite = ((Graphics2D) g).getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
        g.fillRect(0, 0, wBackRenderBuffer, hBackRenderBuffer);
        g.setComposite(backupComposite);

        render(g);

        g.dispose();
    }

    protected SortedSet<Chart<?>> getCharts() {
        return charts;
    }

    public void putChart(Chart<?> chart) {
        charts.add(chart);
    }

    public boolean containsChart(Chart<?> chart) {
        return charts.contains(chart);
    }

    public void removeChart(Chart<?> chart) {
        charts.remove(chart);
    }

    public <T extends Widget<?>> T addWidget(T widget) {
        widgets.add(widget);
        return widget;
    }

    public void setReferCursorValue(float referCursorValue) {
        this.referCursorValue = referCursorValue;
    }

    public float getReferCursorValue() {
        return referCursorValue;
    }

    public void setAutoReferCursorValue(boolean b) {
        this.autoReferCursorValue = b;
    }

    public boolean isAutoReferCursorValue() {
        return autoReferCursorValue;
    }

    public Chart<?> getChartAt(int x, int y) {
        for (Chart<?> chart : charts) {
            if (chart instanceof CursorChart) {
                continue;
            } else if (chart.hits(x, y)) {
                return chart;
            }
        }
        return null;
    }

    public boolean isCursorCrossVisible() {
        return view.getController().isCursorCrossLineVisible();
    }

    /*- @RESERVER
     * MouseEvent retargetedEvent = new MouseEvent(target,
     *   e.getID(),
     *   e.getWhen(),
     *   e.getModifiers() | e.getModifiersEx(),
     *   e.getX(),
     *   e.getY(),
     *   e.getClickCount(),
     *   e.isPopupTrigger());
     *
     * Helper method
     */
    protected void forwardMouseEvent(Component source, Component target, MouseEvent e) {
        if (target != null && target.isVisible()) {
            MouseEvent retargetedEvent = SwingUtilities.convertMouseEvent(source, e, target);
            target.dispatchEvent(retargetedEvent);
        }
    }

    /**
     * plot more custom segments into segsPlotMore
     *   -- beyond the charts put by putCharts()
     */
    protected void plotPane() {
    }

    /**
     * The releasing is required for preventing memory leaks.
     */
    @Override
    protected void finalize() throws Throwable {
        view.getController().removeObserversOf(this);
        view.removeObserversOf(this);

        super.finalize();
    }

    /**
     * @Deprecated
     * @see AbstractChart#CompareTo(Chart)
     *
     * sort charts according to its depth
     */
    @ReferenceOnly
    private Chart<?>[] sortCharts(Set<Chart<?>> charts) {
        Chart<?>[] sortedCharts = charts.toArray(new Chart[charts.size()]);
        for (int i = 0; i < sortedCharts.length; i++) {
            boolean exchangeHappened = false;
            for (int j = sortedCharts.length - 2; j >= i; j--) {
                if (sortedCharts[j + 1].getDepth() < sortedCharts[j].getDepth()) {
                    final Chart tmp = sortedCharts[j + 1];
                    sortedCharts[j + 1] = sortedCharts[j];
                    sortedCharts[j] = tmp;

                    exchangeHappened = true;
                }
            }

            if (!exchangeHappened) {
                break;
            }
        }

        return sortedCharts;
    }
}







