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
package org.aiotrade.lib.charting.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.aiotrade.lib.charting.chart.Chart;
import org.aiotrade.lib.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.util.swing.GBC;

/**
 *
 * @author Caoyuan Deng
 */
public abstract class ChartViewContainer extends JPanel {

    private ChartingController controller;
    private ChartView masterView;
    private Map<IndicatorDescriptor, ChartView> descriptorsWithSlaveView = new HashMap<IndicatorDescriptor, ChartView>();
    /**
     * each viewContainer can only contains one selectedChart, so we define it here instead of
     * on ChartView or ChartPane;
     */
    private Chart selectedChart;
    private ChartView selectedView;
    private boolean interactive = true;
    private boolean pinned = false;
    private Component parent;

    /**
     * !NOTICE
     * For clazz.newInstance() only, don't call it except ChartingController.
     * Use factory method:
     * @see ChartingController#createChartViewContainerInstance(Class<T> clazz, Component focusableParent);
     * to get a instance.
     */
    public ChartViewContainer() {
    }

    /**
     * init this viewContainer instance. binding with controller (so, MaserSer and Descriptor) here
     */
    protected void init(Component focusableParent, ChartingController controller) {
        this.parent = focusableParent;
        this.controller = controller;

        initComponents();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    protected abstract void initComponents();

    public ChartingController getController() {
        return controller;
    }

    public void setInteractive(boolean b) {
        getMasterView().setInteractive(b);

        for (ChartView view : descriptorsWithSlaveView.values()) {
            view.setInteractive(b);
        }

        this.interactive = b;
    }

    /**
     * It's just an interactive hint, the behave for interactive will be defined
     * by ChartView(s) and its Pane(s).
     *
     * @return true if the mouse will work interacticely, false else.
     */
    public boolean isInteractive() {
        return interactive;
    }

    public void pin() {
        getMasterView().pin();

        this.pinned = true;
    }

    public void unPin() {
        getMasterView().unPin();

        this.pinned = false;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void adjustViewsHeight(int increment) {
        /**
         * @TODO
         * Need implement adjusting each views' height ?
         */
        GridBagLayout gbl = (GridBagLayout) getLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GBC.BOTH;
        gbc.gridx = 0;
        gbc.weightx = 100;

        int numSlaveViews = 0;
        float sumSlaveViewsHeight = 0;
        for (ChartView v : descriptorsWithSlaveView.values()) {
            /** overlapping view is also in masterView, should ignor it */
            if (v == getMasterView()) {
                continue;
            } else {
                sumSlaveViewsHeight += v.getHeight();
                numSlaveViews++;
            }
        }

        if (numSlaveViews == 1 && sumSlaveViewsHeight == 0) {
            /** first slaveView added */
            sumSlaveViewsHeight = 0.382f * masterView.getHeight();
        }

        setVisible(false);

        int adjustHeight = increment;
        gbc.weighty = masterView.getHeight() + adjustHeight;

        /**
         * We need setConstraints and setSize together to take the effect
         * according to GridBagLayout's behave.
         * We can setSize(new Dimension(0, 0)) and let GridBagLayout arrange
         * the size according to weightx and weighty, but for performence issue,
         * we'd better setSize() to the actual size that we want.
         */
        gbl.setConstraints(masterView, gbc);
        masterView.setSize(new Dimension(masterView.getWidth(), (int) gbc.weighty));
        for (ChartView v : descriptorsWithSlaveView.values()) {
            if (v.equals(getMasterView())) {
                continue;
            } else {
                /** average assigning */
                gbc.weighty = (sumSlaveViewsHeight - adjustHeight) / numSlaveViews;
                /*-
                 * proportional assigning
                 * gbc.weighty = v.getHeight() - adjustHeight * v.getHeight() / iHeight;
                 */
                gbl.setConstraints(v, gbc);
                v.setSize(new Dimension(v.getWidth(), (int) gbc.weighty));
            }
        }

        setVisible(true);
    }

    public ChartView getMasterView() {
        return masterView;
    }

    protected void setMasterView(ChartView masterView, GridBagConstraints gbc) {
        this.masterView = masterView;
        add(masterView, gbc);
    }

    public void addSlaveView(IndicatorDescriptor descriptor, Indicator indicator, GridBagConstraints gbc) {
        if (!descriptorsWithSlaveView.containsKey(descriptor)) {
            ChartView view;
            if (indicator.isOverlapping()) {
                view = getMasterView();
                view.addOverlappingCharts(indicator);
            } else {
                view = new IndicatorChartView(getController(), indicator);
                if (gbc == null) {
                    gbc = new GridBagConstraints();
                    gbc.fill = GridBagConstraints.BOTH;
                    gbc.gridx = 0;
                }
                add(view, gbc);
            }
            descriptorsWithSlaveView.put(descriptor, view);
            setSelectedView(view);
        }
    }

    public void removeSlaveView(IndicatorDescriptor descriptor) {
        final ChartView view = lookupChartView(descriptor);
        if (view == getMasterView()) {
            view.removeOverlappingCharts(descriptor.getCreatedServerInstance());
        } else {
            remove(view);
            adjustViewsHeight(0);
            view.getAllSers().clear();
            repaint();
        }
        descriptorsWithSlaveView.remove(descriptor);
    }

    public Collection<ChartView> getSlaveViews() {
        return descriptorsWithSlaveView.values();
    }

    public void setSelectedView(ChartView view) {
        if (selectedView != null) {
            selectedView.setSelected(false);
        }

        if (view != null) {
            selectedView = view;
            selectedView.setSelected(true);
        } else {
            selectedView = null;
        }
    }

    public ChartView getSelectedView() {
        return selectedView;
    }

    public Chart getSelectedChart() {
        return selectedChart;
    }

    /**
     * @param chart the chart to be set as selected, could be <b>null</b>
     */
    public void setSelectedChart(Chart chart) {
        if (selectedChart != null) {
            selectedChart.setSelected(false);
        }

        if (chart != null) {
            selectedChart = chart;
            selectedChart.setSelected(true);
        } else {
            selectedChart = null;
        }

        repaint();
    }

    public IndicatorDescriptor lookupIndicatorDescriptor(ChartView view) {
        for (IndicatorDescriptor descriptor : descriptorsWithSlaveView.keySet()) {
            ChartView theView = descriptorsWithSlaveView.get(descriptor);
            if (theView != null && theView == view) {
                return descriptor;
            }
        }
        return null;
    }

    public ChartView lookupChartView(IndicatorDescriptor descriptor) {
        return descriptorsWithSlaveView.get(descriptor);
    }

    public Map<IndicatorDescriptor, ChartView> getDescriptorsWithSlaveView() {
        return descriptorsWithSlaveView;
    }

    public Component getFocusableParent() {
        return parent;
    }

    public void saveToCustomSizeImage(File file, String fileFormat, int width, int height) throws Exception {
        /** backup: */
        Rectangle backupRect = getBounds();

        setBounds(0, 0, width, height);
        validate();

        saveToImage(file, fileFormat);

        /** restore: */
        setBounds(backupRect);
        validate();
    }

    public void saveToCustomSizeImage(File file, String fileFormat, long begTime, long endTime, int height) throws Exception {
        int begPos = controller.getMasterSer().rowOfTime(begTime);
        int endPos = controller.getMasterSer().rowOfTime(endTime);
        int nBars = endPos - begPos;
        int width = (int) (nBars * controller.getWBar());

        /** backup: */
        int backupRightCursorPos = controller.getRightSideRow();
        int backupReferCursorPos = controller.getReferCursorRow();

        controller.setCursorByRow(backupReferCursorPos, endPos, true);

        saveToCustomSizeImage(file, fileFormat, width, height);

        /** restore: */
        controller.setCursorByRow(backupReferCursorPos, backupRightCursorPos, true);
    }

    public void saveToImage(File file, String fileFormat) throws Exception {
        String fileName = (file.toString() + ".png");

        if (masterView.getXControlPane() != null) {
            masterView.getXControlPane().setVisible(false);
        }

        if (masterView.getYControlPane() != null) {
            masterView.getYControlPane().setVisible(false);
        }

        RenderedImage image = paintToImage();

        ImageIO.write(image, fileFormat, file);

        if (masterView.getXControlPane() != null) {
            masterView.getXControlPane().setVisible(true);
        }

        if (masterView.getYControlPane() != null) {
            masterView.getYControlPane().setVisible(true);
        }
    }

    public RenderedImage paintToImage() throws Exception {
        BufferedImage renderImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D gImg = renderImage.createGraphics();
        try {
            paint(gImg);
        } catch (Exception ex) {
            throw ex;
        } finally {
            gImg.dispose();
        }

        return renderImage;
    }

    @Override
    protected void finalize() throws Throwable {
        descriptorsWithSlaveView.clear();
        super.finalize();
    }
}