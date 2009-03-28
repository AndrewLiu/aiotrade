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

import java.awt.Component;
import java.awt.Dimension;
import org.aiotrade.math.timeseries.MasterSer;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.util.ChangeObservable;

/**
 * Each MasterSer can have more than one ChartingController instances.
 *
 * A ChartingController instance keeps the 1-1 relation with:
 *   the MasterSer,
 *   the AnalysisDescriptor, and
 *   a ChartViewContainer
 * Thus, ChartingController couples MasterSer-AnalysisDescriptor-ChartViewContainer
 * together from outside.
 *
 * A ChartView's container can be any Component even without a ChartViewContainer,
 * but should reference back to a controller. All ChartViews shares the same
 * controller will have the same cursor behaves.
 *
 * @author Caoyuan Deng
 */
public interface ChartingController extends ChangeObservable {

    MasterSer getMasterSer();

    AnalysisContents getContents();

    void setCursorCrossLineVisible(boolean b);

    boolean isCursorCrossLineVisible();

    boolean isMouseEnteredAnyChartPane();

    void setMouseEnteredAnyChartPane(boolean b);

    float getWBar();

    void growWBar(int increment);

    void setWBarByNBars(int wViewPort, int nBars);

    boolean isOnCalendarMode();

    void setOnCalendarMode(boolean b);

    void setCursorByRow(int referRow, int rightRow, boolean updateViews);

    void setReferCursorByRow(int Row, boolean updateViews);

    void scrollReferCursor(int increment, boolean updateViews);

    /** keep refer cursor stay on same x of screen, and scroll charts left or right by bar */
    void scrollChartsHorizontallyByBar(int increment);

    void scrollReferCursorToLeftSide();

    void setMouseCursorByRow(int row);

    void setAutoScrollToNewData(boolean b);

    void updateViews();

    void popupViewToDesktop(final ChartView view, Dimension dimension, boolean alwaysOnTop, boolean joint);

    /**
     * ======================================================
     * Bellow is the methods for cursor etc:
     */
    int getReferCursorRow();

    long getReferCursorTime();

    int getRightSideRow();

    long getRightSideTime();

    int getLeftSideRow();

    long getLeftSideTime();

    int getMouseCursorRow();

    long getMouseCursorTime();

    boolean isCursorAccelerated();

    void setCursorAccelerated(boolean b);

    /**
     * Factory method to create ChartViewContainer instance
     */
    <T extends ChartViewContainer> T createChartViewContainer(Class<T> clazz, Component focusableParent);
}
