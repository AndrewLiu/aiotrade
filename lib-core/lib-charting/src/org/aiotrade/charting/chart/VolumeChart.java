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
package org.aiotrade.charting.chart;

import java.awt.Color;
import org.aiotrade.charting.widget.HeavyPathWidget;
import org.aiotrade.charting.widget.WidgetModel;
import org.aiotrade.charting.widget.StickBar;
import org.aiotrade.math.timeseries.QuoteItem;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.charting.chart.VolumeChart.Model;
import org.aiotrade.charting.laf.LookFeel;

/**
 *
 * @author Caoyuan Deng
 */
public class VolumeChart extends AbstractChart<Model> {

    public final static class Model implements WidgetModel {

        boolean thin;

        public void set(boolean thin) {
            this.thin = thin;
        }
    }

    protected Model createModel() {
        return new Model();
    }

    protected void plotChart() {
        assert masterSer instanceof QuoteSer : "VolumeChart's masterSer should be QuoteSer!";

        final Model model = model();

        boolean thin = LookFeel.getCurrent().isThinVolumeBar() || model.thin;

        final HeavyPathWidget heavyPathWidget = addChild(new HeavyPathWidget());
        final StickBar template = new StickBar();
        float y1 = yv(0);
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {

            float open = Float.NaN;
            float close = Float.NaN;
            float high = -Float.MAX_VALUE;
            float low = +Float.MAX_VALUE;
            float volume = -Float.MAX_VALUE;
            for (int i = 0; i < nBarsCompressed; i++) {
                final long time = tb(bar + i);
                final QuoteItem item = (QuoteItem) masterSer.getItem(time);
                if (item != null && item.getClose() != 0) {
                    if (Float.isNaN(open)) {
                        /** only get the first open as compressing period's open */
                        open = item.getOpen();
                    }
                    high = Math.max(high, item.getHigh());
                    low = Math.min(low, item.getLow());
                    close = item.getClose();
                    volume = Math.max(volume, item.getVolume());
                }
            }

            if (volume != -Float.MAX_VALUE /** means we've got volume value */
                    ) {
                Color color = close >= open ? LookFeel.getCurrent().getPositiveColor() : LookFeel.getCurrent().getNegativeColor();
                setForeground(color);
                
                final float xCenter = xb(bar);
                final float y2 = yv(volume);

                template.setForeground(color);
                boolean fillBar = LookFeel.getCurrent().isFillBar();
                template.model().set(xCenter, y1, y2, wBar, thin, fillBar || close < open ? true : false);
                template.plot();
                heavyPathWidget.appendFrom(template);
            }
        }
    }
}
