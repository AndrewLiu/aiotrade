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

import java.awt.FontMetrics;
import java.awt.geom.GeneralPath;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.math.timeseries.Unit;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.ChartValidityObserver;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.MouseCursorObserver;
import org.aiotrade.charting.view.ReferCursorObserver;
import org.aiotrade.charting.view.pane.Pane.RenderStrategy;
import org.aiotrade.charting.widget.Label;
import org.aiotrade.charting.widget.PathWidget;

/**
 *
 * @author Caoyuan Deng
 */
public class AxisXPane extends Pane {

    private final static int TICK_SPACING = 100; // in pixels
    private JLabel mouseCursorLabel;
    private JLabel referCursorLabel;
    private TimeZone timeZone;
    private Calendar cal;
    private Date currDate;
    private Date prevDate;

    public AxisXPane(ChartView view, DatumPlane datumPlane) {
        super(view, datumPlane);

        setTimeZone(TimeZone.getDefault());

        setOpaque(true);
        setRenderStrategy(RenderStrategy.NoneBuffer);

        mouseCursorLabel = new JLabel();
        mouseCursorLabel.setOpaque(true);
        mouseCursorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mouseCursorLabel.setVisible(false);

        referCursorLabel = new JLabel();
        referCursorLabel.setOpaque(true);
        referCursorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        referCursorLabel.setVisible(false);

        setLayout(null);
        add(mouseCursorLabel);
        add(referCursorLabel);

        view.getController().addObserver(this, new MouseCursorObserver<ChartingController>() {

            public void update(ChartingController controller) {
                updateMouseCursorLabel();
            }
        });

        view.getController().addObserver(this, new ReferCursorObserver<ChartingController>() {

            public void update(ChartingController controller) {
                updateReferCursorLabel();
            }
        });

        view.getController().addObserver(this, new ChartValidityObserver<ChartingController>() {

            public void update(ChartingController controller) {
                updateReferCursorLabel();
            }
        });
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        this.cal = Calendar.getInstance(timeZone);
        this.currDate = cal.getTime();
        this.prevDate = cal.getTime();
    }

    private void updateMouseCursorLabel() {
        datumPlane.computeGeometry();
        ChartingController controller = getView().getController();

        if (controller.isMouseEnteredAnyChartPane()) {
            final int mousePosition = controller.getMouseCursorRow();
            final long mouseTime = controller.getMouseCursorTime();
            final Frequency freq = controller.getMasterSer().getFreq();
            final int x = (int) datumPlane.xr(mousePosition);
            cal.setTimeInMillis(mouseTime);
            final String dateStr = freq.getUnit().formatNormalDate(cal.getTime(), timeZone);

            mouseCursorLabel.setForeground(LookFeel.getCurrent().mouseCursorTextColor);
            mouseCursorLabel.setBackground(LookFeel.getCurrent().mouseCursorTextBgColor);
            mouseCursorLabel.setFont(LookFeel.getCurrent().axisFont);
            mouseCursorLabel.setText(dateStr);
            final FontMetrics fm = mouseCursorLabel.getFontMetrics(mouseCursorLabel.getFont());
            mouseCursorLabel.setBounds(
                    x + 1, 1,
                    fm.stringWidth(mouseCursorLabel.getText()) + 2, getHeight() - 2);

            mouseCursorLabel.setVisible(true);
        } else {
            mouseCursorLabel.setVisible(false);
        }

    }

    private void updateReferCursorLabel() {
        datumPlane.computeGeometry();
        ChartingController controller = getView().getController();

        final int referPosition = controller.getReferCursorRow();
        final long referTime = controller.getReferCursorTime();
        final Frequency freq = controller.getMasterSer().getFreq();
        final int x = (int) datumPlane.xr(referPosition);
        cal.setTimeInMillis(referTime);
        final String dateStr = freq.getUnit().formatNormalDate(cal.getTime(), timeZone);

        referCursorLabel.setForeground(LookFeel.getCurrent().referCursorTextColor);
        referCursorLabel.setBackground(LookFeel.getCurrent().referCursorTextBgColor);
        referCursorLabel.setFont(LookFeel.getCurrent().axisFont);
        referCursorLabel.setText(dateStr);
        final FontMetrics fm = referCursorLabel.getFontMetrics(referCursorLabel.getFont());
        referCursorLabel.setBounds(
                x + 1, 1,
                fm.stringWidth(referCursorLabel.getText()) + 2, getHeight() - 2);

        referCursorLabel.setVisible(true);
    }

    public void syncWithView() {
        updateReferCursorLabel();
    }

    @Override
    protected void plotPane() {
        plotAxisX();
    }

    private void plotAxisX() {
        int nTicks = getWidth() / TICK_SPACING;

        int nBars = datumPlane.getNBars();
        /** bTickUnit(bars per tick) cound not be 0, actually it should not less then 2 */
        int bTickUnit = Math.round((float) nBars / (float) nTicks);
        if (bTickUnit < 2) {
            bTickUnit = 2;
        }

        PathWidget pathWidget = addWidget(new PathWidget());
        pathWidget.setForeground(LookFeel.getCurrent().axisColor);
        GeneralPath path = pathWidget.getPath();
        path.reset();

        /** Draw border line */
        path.moveTo(0, 0);
        path.lineTo(getWidth(), 0);
        path.moveTo(0, getHeight() - 1);
        path.lineTo(getWidth(), getHeight() - 1);

        int hTick = getHeight();
        float xLastTick = datumPlane.xb(nBars);
        for (int i = 1; i <= nBars; i++) {
            if (i % bTickUnit == 0 || i == nBars || i == 1) {
                float xCurrTick = datumPlane.xb(i);

                if (xLastTick - xCurrTick < TICK_SPACING && i != nBars) {
                    /** too close */
                    continue;
                }

                path.moveTo(xCurrTick, 1);
                path.lineTo(xCurrTick, hTick);

                long time = datumPlane.tb(i);
                cal.setTimeInMillis(time);
                currDate = cal.getTime();
                boolean stridingDate = false;
                Unit freqUnit = view.getMainSer().getFreq().unit;
                switch (freqUnit) {
                    case Day:
                        cal.setTime(currDate);
                        int currDateYear = cal.get(Calendar.YEAR);
                        cal.setTime(prevDate);
                        int prevDateYear = cal.get(Calendar.YEAR);
                        if (currDateYear > prevDateYear && i != nBars || i == 1) {
                            stridingDate = true;
                        }
                        break;
                    case Hour:
                    case Minute:
                    case Second:
                        cal.setTime(currDate);
                        int currDateDay = cal.get(Calendar.DAY_OF_MONTH);
                        cal.setTime(prevDate);
                        int prevDateDay = cal.get(Calendar.DAY_OF_MONTH);
                        if (currDateDay > prevDateDay && i != nBars || i == 1) {
                            stridingDate = true;
                        }
                    default:
                }

                String dateStr = stridingDate 
                        ? freqUnit.formatStrideDate(currDate, cal.getTimeZone())
                        : freqUnit.formatNormalDate(currDate, cal.getTimeZone());

                Label label = addWidget(new Label());
                label.setForeground(LookFeel.getCurrent().axisColor);
                label.setFont(LookFeel.getCurrent().axisFont);
                label.model().set(xCurrTick, getHeight() - 2, " " + dateStr);
                label.plot();

                cal.setTimeInMillis(time);
                prevDate = cal.getTime();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        view.getController().removeObserversOf(this);
        view.removeObserversOf(this);

        super.finalize();
    }
}
