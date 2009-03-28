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
package org.aiotrade.charting.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.math.timeseries.MasterSer;
import org.aiotrade.math.timeseries.SerChangeEvent;
import org.aiotrade.math.timeseries.SerChangeListener;
import org.aiotrade.charting.view.pane.DrawingPane;
import org.aiotrade.util.ChangeObserver;
import org.aiotrade.util.ChangeObservableHelper;

/**
 * Only DefaultChartingController will be provided in this factory
 *
 * @author Caoyuan Deng
 */
public class ChartingControllerFactory {

    /** a static map to know how many controllers are bound with each MasterSer */
    private static Map<MasterSer, Set<ChartingController>> sersWithcontrollers = new HashMap<MasterSer, Set<ChartingController>>();
    private static boolean cursorAccelerated = false;

    public static ChartingController createInstance(MasterSer masterSer, AnalysisContents contents) {
        Set<ChartingController> controllers = sersWithcontrollers.get(masterSer);
        if (controllers == null) {
            controllers = new HashSet<ChartingController>();
            sersWithcontrollers.put(masterSer, controllers);
        }

        ChartingController controller = new DefaultChartingController(masterSer, contents);
        controllers.add(controller);

        return controller;
    }

    public static boolean isCursorAccelerated() {
        return cursorAccelerated;
    }

    public static void setCursorAccelerated(boolean b) {
        cursorAccelerated = b;
    }

    /**
     * DefaultChartingController that implements ChartingController
     */
    private static class DefaultChartingController implements ChartingController {

        /**
         * min spacing between rightRow and left / right edge, if want more, such as:
         *     minSpacing = (int)(nBars * 0.168);
         */
        private final static int MIN_RIGHT_SPACING = 2;
        private final static int MIN_LEFT_SPACING = 0;
        private Set<ChartView> popupViews = new HashSet<ChartView>();
        final private MasterSer masterSer;
        final private AnalysisContents contents;
        private ChartViewContainer viewContainer;
        /** BASIC_BAR_WIDTH = 6 */
        private final static float[] BAR_WIDTHS_ARRAY = {
            0.00025f, 0.0005f, 0.001f, 0.025f, 0.05f, 0.1f, 0.25f, 0.5f, 1f, 2f, 4f, 6f, 10f, 20f
        };
        private int wBarIdx = 11;
        /** pixels per bar (bar width in pixels) */
        private float wBar = BAR_WIDTHS_ARRAY[wBarIdx];
        private int referCursorRow;
        private int mouseCursorRow;
        private int rightSideRow;
        private int lastOccurredRowOfMasterSer;
        private boolean autoScrollToNewData = true;
        private boolean mouseEnteredAnyChartPane;
        private boolean cursorCrossLineVisible = true;
        private MasterSerChangeListener mySerChangeListener;
        private ChangeObservableHelper observableHelper = new ChangeObservableHelper();

        /**
         * Create a new viewContainer instance. Connect stock and its contents here
         */
        private DefaultChartingController(MasterSer masterSer, AnalysisContents contents) {
            this.masterSer = masterSer;
            this.contents = contents;
        }

        private void internal_setChartViewContainer(ChartViewContainer viewContainer) {
            this.viewContainer = viewContainer;

            internal_initCursorRow();

            if (mySerChangeListener == null) {
                mySerChangeListener = new MasterSerChangeListener();
                masterSer.addSerChangeListener(mySerChangeListener);
            }

            addKeyMouseListenersTo(viewContainer);
        }

        private void internal_initCursorRow() {
            /**
             * masterSer may have finished computing at this time, to adjust
             * the cursor to proper row, update it here.
             * @NOTICE
             * don't set row directly, instead, use setCursorByRow(row, row);
             */
            int row = getMasterSer().lastOccurredRow();
            setCursorByRow(row, row, true);

            mouseCursorRow = getReferCursorRow();
        }

        private void addKeyMouseListenersTo(JComponent component) {
            component.setFocusable(true);
            component.addKeyListener(new ViewKeyAdapter());
            component.addMouseWheelListener(new ViewMouseWheelListener());
        }

        private void removeKeyMouseListenersFrom(JComponent component) {
            /** use a list to avoid concurrent issue */
            List<Object> toBeRemovedList = new ArrayList<Object>();

            for (KeyListener l : component.getKeyListeners()) {
                toBeRemovedList.add(l);
            }
            for (Object l : toBeRemovedList) {
                component.removeKeyListener((KeyListener) l);
            }

            toBeRemovedList.clear();
            for (MouseWheelListener l : component.getMouseWheelListeners()) {
                toBeRemovedList.add(l);
            }
            for (Object l : toBeRemovedList) {
                component.removeMouseWheelListener((MouseWheelListener) l);
            }
        }

        public MasterSer getMasterSer() {
            return masterSer;
        }

        public AnalysisContents getContents() {
            return contents;
        }

        public void setCursorCrossLineVisible(boolean b) {
            this.cursorCrossLineVisible = b;
        }

        public boolean isCursorCrossLineVisible() {
            return cursorCrossLineVisible;
        }

        public boolean isMouseEnteredAnyChartPane() {
            return mouseEnteredAnyChartPane;
        }

        public void setMouseEnteredAnyChartPane(boolean b) {
            boolean oldValue = this.mouseEnteredAnyChartPane;
            this.mouseEnteredAnyChartPane = b;

            if (!mouseEnteredAnyChartPane) {
                /** this cleanups mouse cursor */
                if (this.mouseEnteredAnyChartPane != oldValue) {
                    notifyObserversChanged(MouseCursorObserver.class);
                    updateViews();
                }
            }

        }

        public void setAutoScrollToNewData(boolean autoScrollToNewData) {
            this.autoScrollToNewData = autoScrollToNewData;
        }

        public float getWBar() {
            return wBar;
        }

        public void growWBar(int increment) {
            wBarIdx += increment;
            if (wBarIdx < 0) {
                wBarIdx = 0;
            } else if (wBarIdx > BAR_WIDTHS_ARRAY.length - 1) {
                wBarIdx = BAR_WIDTHS_ARRAY.length - 1;
            }

            internal_setWBar(BAR_WIDTHS_ARRAY[wBarIdx]);
            updateViews();
        }

        public void setWBarByNBars(int wViewPort, int nBars) {
            if (nBars < 0) {
                return;
            }

            /** decide wBar according to wViewPort. Do not use integer divide here */
            float newWBar = (float) wViewPort / (float) nBars;

            /** adjust xfactorIdx to nearest */
            if (newWBar < BAR_WIDTHS_ARRAY[0]) {
                /** avoid too small xfactor */
                newWBar = BAR_WIDTHS_ARRAY[0];

                wBarIdx = 0;
            } else if (newWBar > BAR_WIDTHS_ARRAY[BAR_WIDTHS_ARRAY.length - 1]) {
                wBarIdx = BAR_WIDTHS_ARRAY.length - 1;
            } else {
                for (int i = 0, n = BAR_WIDTHS_ARRAY.length - 1; i < n; i++) {
                    if (newWBar > BAR_WIDTHS_ARRAY[i] && newWBar < BAR_WIDTHS_ARRAY[i + 1]) {
                        /** which one is the nearest ? */
                        wBarIdx = Math.abs(BAR_WIDTHS_ARRAY[i] - newWBar) < Math.abs(BAR_WIDTHS_ARRAY[i + 1] - newWBar) ? i : i + 1;
                        break;
                    }
                }
            }

            internal_setWBar(newWBar);
            updateViews();
        }

        public boolean isOnCalendarMode() {
            return getMasterSer().isOnCalendarMode();
        }

        public void setOnCalendarMode(boolean b) {
            if (isOnCalendarMode() != b) {
                long referCursorTime = getReferCursorTime();
                long rightCursorTime = getRightSideTime();

                if (b == true) {
                    getMasterSer().setOnCalendarMode();
                } else {
                    getMasterSer().setOnOccurredMode();
                }

                internal_setReferCursorByTime(referCursorTime);
                internal_setRightCursorByTime(rightCursorTime);

                notifyObserversChanged(ChartValidityObserver.class);
                updateViews();
            }
        }

        public void setCursorByRow(int referRow, int rightRow, boolean updateViews) {
            /** set right cursor row first and directly */
            internal_setRightSideRow(rightRow);

            final int oldValue = getReferCursorRow();
            scrollReferCursor(referRow - oldValue, updateViews);
        }

        public void setReferCursorByRow(int row, boolean updateViews) {
            int increment = row - getReferCursorRow();
            scrollReferCursor(increment, updateViews);
        }

        public void scrollReferCursor(int increment, boolean updateViews) {
            int referRow = getReferCursorRow();
            int rightRow = getRightSideRow();

            referRow += increment;

            int rightSpacing = rightRow - referRow;
            if (rightSpacing >= MIN_RIGHT_SPACING) {
                /** right spacing is enough, check left spacing: */
                final int nBars = viewContainer.getMasterView().getNBars();
                int leftRow = rightRow - nBars + 1;
                int leftSpacing = referRow - leftRow;
                if (leftSpacing < MIN_LEFT_SPACING) {
                    internal_setRightSideRow(rightRow + leftSpacing - MIN_LEFT_SPACING);
                }
            } else {
                internal_setRightSideRow(rightRow + MIN_RIGHT_SPACING - rightSpacing);

            }

            internal_setReferCursorRow(referRow);
            if (updateViews) {
                updateViews();
            }
        }

        /** keep refer cursor stay on same x of screen, and scroll charts left or right by bar */
        public void scrollChartsHorizontallyByBar(int increment) {
            int rightRow = getRightSideRow();
            internal_setRightSideRow(rightRow + increment);

            scrollReferCursor(increment, true);
        }

        public void scrollReferCursorToLeftSide() {
            int rightRow = getRightSideRow();
            int nBars = viewContainer.getMasterView().getNBars();

            int leftRow = rightRow - nBars + MIN_LEFT_SPACING;
            setReferCursorByRow(leftRow, true);
        }

        public void setMouseCursorByRow(int row) {
            internal_setMouseCursorRow(row);
        }

        public void updateViews() {
            if (viewContainer != null) {
                viewContainer.repaint();
            }

            /**
             * as repaint() may be called by awt in instance's initialization, before
             * popupViewSet is created, so, check null.
             */
            if (popupViews != null) {
                for (ChartView view : popupViews) {
                    view.repaint();
                }
            }
        }

        public void addObserver(Object owner, ChangeObserver observer) {
            observableHelper.addObserver(owner, observer);
        }

        public void removeObserver(ChangeObserver observer) {
            observableHelper.removeObserver(observer);
        }

        public void removeObserversOf(Object owner) {
            observableHelper.removeObserversOf(owner);
        }

        /**
         * Changed cases for ChartValidityObserver:
         *   rightSideRow
         *   referCursorRow
         *   wBar
         *   onCalendarMode
         * Change cases for MouseCursorObserver:
         *   mosueCursor
         *   mouseEnteredAnyChartPane
         */
        public void notifyObserversChanged(Class<? extends ChangeObserver> observerType) {
            observableHelper.notifyObserversChanged(this, observerType);
        }

        public final int getReferCursorRow() {
            return referCursorRow;
        }

        public final long getReferCursorTime() {
            return getMasterSer().timeOfRow(referCursorRow);
        }

        public final int getRightSideRow() {
            return rightSideRow;
        }

        public final long getRightSideTime() {
            return getMasterSer().timeOfRow(rightSideRow);
        }

        public final int getLeftSideRow() {
            int rightRow = getRightSideRow();
            int nBars = viewContainer.getMasterView().getNBars();

            return rightRow - nBars + MIN_LEFT_SPACING;
        }

        public final long getLeftSideTime() {
            return getMasterSer().timeOfRow(getLeftSideRow());
        }

        public final int getMouseCursorRow() {
            return mouseCursorRow;
        }

        public final long getMouseCursorTime() {
            return getMasterSer().timeOfRow(mouseCursorRow);
        }

        /**
         * @NOTICE
         * =======================================================================
         * as we don't like referCursor and rightCursor being set directly by others,
         * the following setter methods are named internal_setXXX, and are private.
         */
        private void internal_setWBar(float wBar) {
            final float oldValue = this.wBar;
            this.wBar = wBar;
            if (this.wBar != oldValue) {
                notifyObserversChanged(ChartValidityObserver.class);
            }
        }

        private void internal_setReferCursorRow(int row) {
            final int oldValue = this.referCursorRow;
            this.referCursorRow = row;
            /** remember the lastRow for decision if need update cursor, see changeCursorByRow() */
            this.lastOccurredRowOfMasterSer = masterSer.lastOccurredRow();
            if (this.referCursorRow != oldValue) {
                notifyObserversChanged(ReferCursorObserver.class);
                notifyObserversChanged(ChartValidityObserver.class);
            }
        }

        private void internal_setRightSideRow(int row) {
            final int oldValue = this.rightSideRow;
            this.rightSideRow = row;
            if (this.rightSideRow != oldValue) {
                notifyObserversChanged(ChartValidityObserver.class);
            }
        }

        private void internal_setReferCursorByTime(long time) {
            internal_setReferCursorRow(getMasterSer().rowOfTime(time));
        }

        private void internal_setRightCursorByTime(long time) {
            internal_setRightSideRow(getMasterSer().rowOfTime(time));
        }

        private void internal_setMouseCursorRow(int row) {
            final int oldValue = this.mouseCursorRow;
            this.mouseCursorRow = row;

            /**
             * even mouseCursor row not changed, the mouse's y may has been changed,
             * so, notify observers without comparing the oldValue and newValue.
             */
            notifyObserversChanged(MouseCursorObserver.class);
        }

        public boolean isCursorAccelerated() {
            return cursorAccelerated;
        }

        public void setCursorAccelerated(boolean b) {
            cursorAccelerated = b;
        }

        public void popupViewToDesktop(final ChartView view, Dimension dimendion, boolean alwaysOnTop, boolean joint) {
            final ChartView popupView = view;

            popupViews.add(popupView);
            addKeyMouseListenersTo(popupView);

            int w = dimendion.width;
            int h = dimendion.height;
            JFrame frame = new JFrame();//new JDialog (), true);
            frame.setAlwaysOnTop(alwaysOnTop);
            frame.setTitle(popupView.getMainSer().getShortDescription());
            frame.add(popupView, BorderLayout.CENTER);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setBounds((screenSize.width - w) / 2, (screenSize.height - h) / 2, w, h);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosed(WindowEvent e) {
                    removeKeyMouseListenersFrom(popupView);
                    popupViews.remove(popupView);
                }
            });

            frame.setVisible(true);
        }

        /**
         * Factory method to create ChartViewContainer instance, got the relations
         * between ChartViewContainer and Controller ready.
         */
        public <T extends ChartViewContainer> T createChartViewContainer(Class<T> clazz, Component focusableParent) {
            T instance = null;
            try {
                instance = clazz.newInstance();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }

            if (instance != null) {
                instance.init(focusableParent, this);
                /**
                 * @NOTICE
                 * Always call internal_setChartViewContainer(instance) next to
                 * instance.init(focusableParent, this), since the internal_initCursorRow()
                 * procedure needs the children of chartViewContainer ready.
                 */
                internal_setChartViewContainer(instance);
            }

            return instance;
        }

        @Override
        protected void finalize() throws Throwable {
            if (mySerChangeListener != null) {
                masterSer.removeSerChangeListener(mySerChangeListener);
            }

            super.finalize();
        }

        /**
         * listen to masterSer and process loading, update events to check if need to update cursor
         */
        private class MasterSerChangeListener implements SerChangeListener {

            public void serChanged(SerChangeEvent evt) {
                if (!autoScrollToNewData) {
                    return;
                }

                /** this method only process loading, update events to check if need to update cursor */
                switch (evt.getType()) {
                    case FinishedLoading:
                    case RefreshInLoading:
                    case Updated:
                        ChartView masterView = viewContainer.getMasterView();
                        if (masterView instanceof WithDrawingPane) {
                            DrawingPane drawing = ((WithDrawingPane) masterView).getSelectedDrawing();
                            if (drawing != null && drawing.isInDrawing()) {
                                return;
                            }
                        }

                        int oldReferRow = getReferCursorRow();
                        if (oldReferRow == lastOccurredRowOfMasterSer || lastOccurredRowOfMasterSer <= 0) {
                            /** refresh only when the old lastRow is extratly oldReferRow, or prev lastRow <= 0 */
                            long lastTime = Math.max(evt.getEndTime(), masterSer.lastOccurredTime());
                            int rightRow = masterSer.rowOfTime(lastTime);
                            int referRow = rightRow;

                            setCursorByRow(referRow, rightRow, true);
                        }

                        notifyObserversChanged(ChartValidityObserver.class);

                        break;
                    default:
                }
            }
        }

        private ChartView internal_getCorrespondingChartView(InputEvent e) {
            ChartView correspondingChartView = null;

            Object source = e.getSource();
            if (source instanceof ChartViewContainer) {
                correspondingChartView = ((ChartViewContainer) source).getMasterView();
            } else if (source instanceof ChartView) {
                correspondingChartView = ((ChartView) source);
            }

            return correspondingChartView;
        }

        /**
         * =============================================================
         * Bellow is the private listener classes for key and mouse:
         */
        private class ViewKeyAdapter extends KeyAdapter {

            private static final int LEFT = -1;
            private static final int RIGHT = 1;

            @Override
            public void keyPressed(KeyEvent e) {
                ChartView view = internal_getCorrespondingChartView(e);
                if (view == null || !view.isInteractive()) {
                    return;
                }

                final int fastSteps = (int) (view.getNBars() * 0.168f);

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        if (e.isControlDown()) {
                            moveCursorInDirection(fastSteps, LEFT);
                        } else {
                            moveChartsInDirection(fastSteps, LEFT);
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (e.isControlDown()) {
                            moveCursorInDirection(fastSteps, RIGHT);
                        } else {
                            moveChartsInDirection(fastSteps, RIGHT);
                        }
                        break;
                    case KeyEvent.VK_UP:
                        growWBar(+1);
                        break;
                    case KeyEvent.VK_DOWN:
                        growWBar(-1);
                    default:
                        break;
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    setCursorAccelerated(!isCursorAccelerated());
                    /*-
                     * let action to process this
                    setCursorAccelerated(!isCursorAccelerated());
                     */
                }

            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            private void moveCursorInDirection(int fastSteps, int DIRECTION) {
                int steps = isCursorAccelerated() ? fastSteps : 1;
                steps *= DIRECTION;

                scrollReferCursor(steps, true);
            }

            private void moveChartsInDirection(int fastSteps, int DIRECTION) {
                int steps = isCursorAccelerated() ? fastSteps : 1;
                steps *= DIRECTION;

                scrollChartsHorizontallyByBar(steps);
            }
        }

        private class ViewMouseWheelListener implements MouseWheelListener {

            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                ChartView view = internal_getCorrespondingChartView(e);
                if (view == null || !view.isInteractive()) {
                    return;
                }

                int fastSteps = (int) (view.getNBars() * 0.168f);

                if (e.isShiftDown()) {
                    /** zoom in / zoom out */
                    growWBar(e.getWheelRotation());
                } else if (e.isControlDown()) {
                    if (!view.isInteractive()) {
                        return;
                    }

                    int unitsToScroll = isCursorAccelerated() ? e.getWheelRotation() * fastSteps : e.getWheelRotation();
                    /** move refer cursor left / right */
                    scrollReferCursor(unitsToScroll, true);
                } else {
                    if (!view.isInteractive()) {
                        return;
                    }

                    int unitsToScroll = isCursorAccelerated() ? e.getWheelRotation() * fastSteps : e.getWheelRotation();
                    /** keep referCursor stay same x in screen, and move */
                    scrollChartsHorizontallyByBar(unitsToScroll);
                }
            }
        }
    }
}
