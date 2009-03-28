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
import java.text.DecimalFormat;
import java.text.ParseException;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.aiotrade.charting.widget.PathWidget;
import org.aiotrade.math.timeseries.QuoteItem;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.ChartValidityObserver;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.MouseCursorObserver;
import org.aiotrade.charting.view.ReferCursorObserver;
import org.aiotrade.charting.view.WithQuoteChart;
import org.aiotrade.charting.view.pane.Pane.RenderStrategy;
import org.aiotrade.charting.widget.Label;

/**
 *
 * @author Caoyuan Deng
 */
public class AxisYPane extends Pane {

    private JLabel mouseCursorLabel;
    private JLabel referCursorLabel;
    private static DecimalFormat CURRENCY_DECIMAL_FORMAT = new DecimalFormat("0.###");
    private static DecimalFormat COMMON_DECIMAL_FORMAT = new DecimalFormat("0.00");
    private boolean symmetricOnMiddleValue;

    public AxisYPane(ChartView view, DatumPlane datumPlane) {
        super(view, datumPlane);

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

        view.addObserver(this, new ChartValidityObserver<ChartView>() {

            public void update(ChartView view) {
                updateReferCursorLabel();
            }
        });
    }

    private void updateMouseCursorLabel() {
        datumPlane.computeGeometry();
        ChartingController controller = getView().getController();
        if (controller.isMouseEnteredAnyChartPane()) {
            float y, v;
            if (datumPlane.getView() instanceof WithQuoteChart) {
                if (datumPlane.isMouseEntered()) {
                    y = datumPlane.getYMouse();
                    v = datumPlane.vy(y);
                } else {
                    final int mousePosition = controller.getMouseCursorRow();
                    QuoteSer quoteSer = ((WithQuoteChart) datumPlane.getView()).getQuoteSer();
                    QuoteItem item = (QuoteItem) quoteSer.getItemByRow(mousePosition);
                    v = item == null ? 0 : item.getClose();
                    y = datumPlane.yv(v);
                }
                String valueStr = COMMON_DECIMAL_FORMAT.format(v);

                mouseCursorLabel.setForeground(LookFeel.getCurrent().mouseCursorTextColor);
                mouseCursorLabel.setBackground(LookFeel.getCurrent().mouseCursorTextBgColor);
                mouseCursorLabel.setFont(LookFeel.getCurrent().axisFont);
                mouseCursorLabel.setText(valueStr);
                final FontMetrics fm = mouseCursorLabel.getFontMetrics(mouseCursorLabel.getFont());
                mouseCursorLabel.setBounds(
                        3, Math.round(y) - fm.getHeight() + 1,
                        fm.stringWidth(mouseCursorLabel.getText()) + 2, fm.getHeight() + 1);

                mouseCursorLabel.setVisible(true);
            } else {
                if (datumPlane.isMouseEntered()) {
                    y = datumPlane.getYMouse();
                    v = datumPlane.vy(y);
                    String valueStr = COMMON_DECIMAL_FORMAT.format(v);

                    mouseCursorLabel.setForeground(LookFeel.getCurrent().mouseCursorTextColor);
                    mouseCursorLabel.setBackground(LookFeel.getCurrent().mouseCursorTextBgColor);
                    mouseCursorLabel.setFont(LookFeel.getCurrent().axisFont);
                    mouseCursorLabel.setText(valueStr);
                    final FontMetrics fm = mouseCursorLabel.getFontMetrics(mouseCursorLabel.getFont());
                    mouseCursorLabel.setBounds(
                            3, Math.round(y) - fm.getHeight() + 1,
                            fm.stringWidth(mouseCursorLabel.getText()) + 2, fm.getHeight() + 1);

                    mouseCursorLabel.setVisible(true);
                } else {
                    mouseCursorLabel.setVisible(false);
                }
            }
        } else {
            mouseCursorLabel.setVisible(false);
        }
    }

    /**
     * CursorChangeEvent only notice the cursor's position changes, but the
     * referCursorLable is also aware of the refer value's changes, so we could
     * not rely on the CursorChangeEvent only, instead, we call this method via
     * syncWithView()
     */
    private void updateReferCursorLabel() {
        datumPlane.computeGeometry();
        ChartingController controller = getView().getController();

        float y, v;
        if (datumPlane.getView() instanceof WithQuoteChart) {
            final int referPosition = controller.getReferCursorRow();
            QuoteSer quoteSer = ((WithQuoteChart) datumPlane.getView()).getQuoteSer();
            QuoteItem item = (QuoteItem) quoteSer.getItemByRow(referPosition);
            v = item == null ? 0 : item.getClose();
            y = datumPlane.yv(v);
            String valueStr = COMMON_DECIMAL_FORMAT.format(v);

            referCursorLabel.setForeground(LookFeel.getCurrent().referCursorTextColor);
            referCursorLabel.setBackground(LookFeel.getCurrent().referCursorTextBgColor);
            referCursorLabel.setFont(LookFeel.getCurrent().axisFont);
            referCursorLabel.setText(valueStr);
            final FontMetrics fm = referCursorLabel.getFontMetrics(referCursorLabel.getFont());
            referCursorLabel.setBounds(
                    3, Math.round(y) - fm.getHeight() + 1,
                    fm.stringWidth(referCursorLabel.getText()) + 2, fm.getHeight());

            referCursorLabel.setVisible(true);
        } else {
            referCursorLabel.setVisible(false);
        }
    }

    public void syncWithView() {
        updateReferCursorLabel();
    }

    @Override
    protected void plotPane() {
        plotAxisY();
    }

    private void plotAxisY() {
        FontMetrics fm = getFontMetrics(getFont());
        int hFm = fm.getHeight();

        float nTicks = 6;
        while (datumPlane.getHCanvas() / nTicks < hFm + 20 && nTicks > -2) {
            nTicks -= 2; // always keep even
        }

        float maxValueOnCanvas = datumPlane.vy(datumPlane.getYCanvasUpper());
        float minValueOnCanvas = view.getYControlPane() != null ? datumPlane.vy(datumPlane.getYCanvasLower() - view.getYControlPane().getHeight()) : datumPlane.vy(datumPlane.getYCanvasLower());


        float vMaxTick = maxValueOnCanvas; // init value, will adjust later
        float vMinTick = minValueOnCanvas; // init value, will adjust later

        float vRange = vMaxTick - vMinTick;
        float vTickUnit = vRange / nTicks;

        if (!symmetricOnMiddleValue) {
            vTickUnit = roundTickUnit(vTickUnit);
            vMinTick = (int) (vMinTick / vTickUnit) * vTickUnit;
        }

        PathWidget pathWidget = addWidget(new PathWidget());
        pathWidget.setForeground(LookFeel.getCurrent().axisColor);
        GeneralPath path = pathWidget.getPath();
        path.reset();

        /** Draw left border line */
        path.moveTo(0, 0);
        path.lineTo(0, getHeight());

        boolean shouldScale = false;
        for (int i = 0; i < nTicks + 2; i++) {
            float vTick = vMinTick + vTickUnit * i;

            float yTick = datumPlane.yv(vTick);

            if (yTick < hFm) {
                break;
            }

            path.moveTo(0, yTick);
            path.lineTo(2, yTick);

            if (Math.abs(vTick) >= 100000) {
                vTick = Math.abs(vTick / 100000.0f);
                shouldScale = true;
            } else {
                vTick = Math.abs(vTick);
            }

            if (i == 0 && shouldScale) {
                String multiple = "x10000";

                Label label = addWidget(new Label());
                label.setForeground(LookFeel.getCurrent().axisColor);
                label.setFont(LookFeel.getCurrent().axisFont);
                label.model().set(4, yTick, multiple);
                label.plot();
            } else {
                Label label = addWidget(new Label());
                label.setForeground(vTick >= 0 ? LookFeel.getCurrent().axisColor : LookFeel.getCurrent().getNegativeColor());
                label.setFont(LookFeel.getCurrent().axisFont);
                label.model().set(4, yTick, COMMON_DECIMAL_FORMAT.format(vTick));
                label.plot();
            }

        }

    }

    /**
     * Try to round tickUnit
     */
    private float roundTickUnit(float vTickUnit) {
        /** sample : 0.032 */
        int roundedExponent = (int) Math.round(Math.log10(vTickUnit)) - 1;   // -2
        double adjustFactor = Math.pow(10, -roundedExponent);               // 100
        int adjustedValue = (int) (vTickUnit * adjustFactor);         // 3.2 -> 3
        vTickUnit = (float) adjustedValue / (float) adjustFactor;      // 0.03

        /** following DecimalFormat <-> float converts are try to round the decimal */
        if (vTickUnit <= 0.001) {
            /** for currency */
            vTickUnit = 0.001f;
        } else if (vTickUnit > 0.001 && vTickUnit < 0.005) {
            /** for currency */
            String unitStr = CURRENCY_DECIMAL_FORMAT.format(vTickUnit);
            try {
                vTickUnit = CURRENCY_DECIMAL_FORMAT.parse(unitStr.trim()).floatValue();
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        } else if (vTickUnit > 0.005 && vTickUnit < 1) {
            /** for stock */
            String unitStr = COMMON_DECIMAL_FORMAT.format(vTickUnit);
            try {
                vTickUnit = COMMON_DECIMAL_FORMAT.parse(unitStr.trim()).floatValue();
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }

        return vTickUnit;
    }

    public void setSymmetricOnMiddleValue(boolean b) {
        this.symmetricOnMiddleValue = b;
    }

    @Override
    protected void finalize() throws Throwable {
        view.getController().removeObserversOf(this);
        view.removeObserversOf(this);

        super.finalize();
    }
}
