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
package org.aiotrade.lib.charting.laf;

import java.awt.Color;
import java.awt.Font;
import org.aiotrade.lib.charting.chart.QuoteChart;

/**
 *
 * @author Caoyuan Deng
 */
public abstract class LookFeel {

    private static LookFeel current;
    protected Color[] monthColors;
    protected Color[] planetColors;
    public Font axisFont;
    public Font defaultFont = new Font("Dialog Input", Font.PLAIN, 9);
    protected Color[] chartColors;
    public Color systemBackgroundColor;
    public Color nameColor;
    public Color backgroundColor;
    public Color infoBackgroundColor;
    public Color heavyBackgroundColor;
    public Color axisBackgroundColor;
    public Color stickChartColor;
    protected Color positiveColor;
    protected Color negativeColor;
    protected Color positiveBgColor;
    protected Color negativeBgColor;
    protected Color neutralColor = Color.YELLOW;
    protected Color neutralBgColor;
    public Color borderColor;
    public Color axisColor = borderColor;
    /** same as new Color(0.0f, 0.0f, 1.0f, 0.382f) */
    public Color referCursorColor; //new Color(0.5f, 0.0f, 0.5f, 0.618f); //new Color(0.0f, 0.0f, 1.0f, 0.618f);
    //new Color(131, 129, 221);
    /** same as new Color(1.0f, 1.0f, 1.0f, 0.618f) */
    public Color mouseCursorColor;
    //new Color(239, 237, 234);
    public Color mouseCursorTextColor;
    public Color mouseCursorTextBgColor;
    public Color referCursorTextColor;
    public Color referCursorTextBgColor;
    public Color drawingMasterColor; // new Color(128, 128, 255); //new Color(128, 0, 128);
    public Color drawingColor; // new Color(128, 128, 255); //new Color(128, 0, 128);
    public Color drawingColorTransparent; //new Color(128, 0, 128);
    public Color handleColor; // new Color(128, 128, 255); //new Color(128, 0, 128);
    public Color astrologyColor;
    private static boolean positiveNegativeColorReversed;
    private static QuoteChart.Type quoteChartType = QuoteChart.Type.Ohlc;
    private static boolean thinVolumeBar;
    private static boolean antiAlias;
    private static boolean autoHideScroll;
    private static boolean fillBar = true;
    /** won't persistent */
    private static boolean allowMultipleIndicatorOnQuoteChartView;
    /** scrolling controller colors */
    protected Color trackColor;
    protected Color thumbColor;

    public LookFeel() {
    }

    public static LookFeel getCurrent() {
        if (current == null) {
            current = new CityLights();
        }
        return current;
    }

    public static void setCurrent(LookFeel colorTheme) {
        current = colorTheme;
    }

    public boolean isPositiveNegativeColorReversed() {
        return positiveNegativeColorReversed;
    }

    public void setPositiveNegativeColorReversed(boolean b) {
        positiveNegativeColorReversed = b;
    }

    public boolean isAllowMultipleIndicatorOnQuoteChartView() {
        return allowMultipleIndicatorOnQuoteChartView;
    }

    public void setAllowMultipleIndicatorOnQuoteChartView(boolean b) {
        allowMultipleIndicatorOnQuoteChartView = b;
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setAntiAlias(boolean b) {
        antiAlias = b;
    }

    public boolean isAutoHideScroll() {
        return autoHideScroll;
    }

    public void setAutoHideScroll(boolean b) {
        autoHideScroll = b;
    }

    public boolean isThinVolumeBar() {
        return thinVolumeBar;
    }

    public void setThinVolumeBar(boolean b) {
        thinVolumeBar = b;
    }

    public void setQuoteChartType(QuoteChart.Type type) {
        quoteChartType = type;
    }

    public QuoteChart.Type getQuoteChartType() {
        return quoteChartType;
    }

    public Color getGradientColor(int depth, int beginDepth) {
        double steps = Math.abs((depth - beginDepth));
        float alpha = (float) Math.pow(0.618d, steps);

        //        Color color = Color.RED;
        //        int r = alpha * color.getRed();
        //        int g = alpha * color.getGreen();
        //        int b = alpha * color.getBlue();

        //        return new Color(r * alpha, g * alpha, b * alpha);
        return new Color(0.0f * alpha, 1.0f * alpha, 1.0f * alpha);
    }

    //    public Color getGradientColor(int depth) {
    //        double steps = Math.abs((depth - AbstractPart.DEPTH_GRADIENT_BEGIN));
    //        float  alpha = (float)Math.pow(0.618d, steps);
    //
    //        Color color = Color.RED;
    //        for (int i = 0; i < steps; i++) {
    //            color.brighter().brighter();
    //        }
    //
    //        return color;
    //    }
    public Color getChartColor(int depth) {
        int multiple = depth / chartColors.length;
        int remainder = depth % chartColors.length;
        Color color = chartColors[remainder];
        for (int i = 1; i <= multiple; i++) {
            color = color.darker();
        }
        return color;
    }

    public boolean isFillBar() {
        return fillBar;
    }

    public Color getNeutralColor() {
        return neutralColor;
    }

    public Color getNeutralBgColor() {
        return neutralBgColor;
    }

    public Color getPositiveColor() {
        return isPositiveNegativeColorReversed() ? negativeColor : positiveColor;
    }

    public Color getNegativeColor() {
        return isPositiveNegativeColorReversed() ? positiveColor : negativeColor;
    }

    public Color getPositiveBgColor() {
        return isPositiveNegativeColorReversed() ? negativeBgColor : positiveBgColor;
    }

    public Color getNegativeBgColor() {
        return isPositiveNegativeColorReversed() ? positiveBgColor : negativeBgColor;

    }

    public Color getMonthColor(int month) {
        return monthColors[month];
    }

    public Color getPlanetColor(int planet) {
        return planetColors[planet];
    }

    public Color getTrackColor() {
        if (trackColor == null) {
            return axisColor;
        } else {
            return trackColor;
        }
    }

    public Color getThumbColor() {
        if (thumbColor == null) {
            return axisColor;
        } else {
            return thumbColor;
        }
    }

    public static double compareColor(Color c1, Color c2) {
        int R1 = c1.getRed();
        int G1 = c1.getGreen();
        int B1 = c1.getBlue();

        int R2 = c2.getRed();
        int G2 = c2.getGreen();
        int B2 = c2.getBlue();
        return Math.sqrt(((R1 - R2) * (R1 - R2) + (G1 - G2) * (G1 - G2) + (B1 - B1) * (B1 - B2)));

    }
}