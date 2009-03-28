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
package org.aiotrade.platform.core.analysis;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.swing.text.DateFormatter;
import org.aiotrade.charting.descriptor.DrawingDescriptor;
import org.aiotrade.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.charting.chart.handledchart.HandledChart;
import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.charting.chart.util.ValuePoint;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.util.serialization.BeansDocument;

/**
 * @author Caoyuan Deng
 */

public class ContentsPersistenceHandler {
    
    public ContentsPersistenceHandler() {
    }
    
    public static String dumpContents(AnalysisContents contents) {
        StringBuilder buffer = new StringBuilder(500);
        BeansDocument beans = new BeansDocument();
        beans.appendBean(contents.writeToBean(beans));
        
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        //buffer.append("<!DOCTYPE settings PUBLIC \"-//AIOTrade//DTD AnalysisContents settings 1.0//EN\" >\n");
        buffer.append("<sec unisymbol=\"" + contents.getUniSymbol() + "\">\n");
        
        Collection<QuoteContract> dataContracts = contents.lookupDescriptors(QuoteContract.class);
        if (dataContracts.size() > 0) {
            buffer.append("    <sources>\n");
            for (QuoteContract dataContract : dataContracts) {
                buffer.append("        <source ");
                buffer.append("active=\"" + dataContract.isActive() + "\" ");
                buffer.append("class=\"" + dataContract.getServiceClassName() + "\" ");
                buffer.append("symbol=\"" + dataContract.getSymbol() + "\" ");
                buffer.append("sectype=\"" + dataContract.getSecType() + "\" ");
                buffer.append("exchange=\"" + dataContract.getExchange() + "\" ");
                buffer.append("primaryexchange=\"" + dataContract.getPrimaryExchange() + "\" ");
                buffer.append("currency=\"" + dataContract.getCurrency() + "\" ");
                buffer.append("dateformat=\"" + dataContract.getDateFormatString() + "\" ");
                buffer.append("nunits=\"" + dataContract.getFreq().nUnits + "\" ");
                buffer.append("unit=\"" + dataContract.getFreq().unit + "\" ");
                buffer.append("refreshable=\"" + dataContract.isRefreshable() + "\" ");
                buffer.append("refreshinterval=\"" + dataContract.getRefereshInterval() + "\" ");
                DateFormatter df = new DateFormatter(new SimpleDateFormat("yyyy-MM-dd"));
                try {
                    buffer.append("begdate=\"" + df.valueToString(dataContract.getBegDate().getTime()) + "\" ");
                    buffer.append("enddate=\"" + df.valueToString(dataContract.getEndDate().getTime()) + "\" ");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                buffer.append("url=\"" + dataContract.getUrlString() + "\"");
                buffer.append(">\n");
                buffer.append("        </source>\n");
            }
            buffer.append("    </sources>\n");
        }
        
        Collection<IndicatorDescriptor> indicatorDescriptors = contents.lookupDescriptors(IndicatorDescriptor.class);
        if (indicatorDescriptors.size() > 0) {
            buffer.append("    <indicators>\n");
            for (IndicatorDescriptor descriptor : indicatorDescriptors) {
                buffer.append("        <indicator ");
                buffer.append("active=\"" + descriptor.isActive() + "\" ");
                buffer.append("class=\"" + descriptor.getServiceClassName() + "\" ");
                buffer.append("nunits=\"" + descriptor.getFreq().nUnits + "\" ");
                buffer.append("unit=\"" + descriptor.getFreq().unit + "\">\n");
                
                List<Opt> opts = descriptor.getOpts();
                for (Opt opt : opts) {
                    buffer.append("            <opt name=\"").append(opt.getName())
                            .append("\" value=\"").append(opt.value())
                            .append("\" step=\"").append(opt.getStep())
                            .append("\" minvalue=\"").append(opt.getMinValue())
                            .append("\" maxvalue=\"").append(opt.getMaxValue())
                            .append("\"/>\n");
                }
                
                buffer.append("        </indicator>\n");
            }
            buffer.append("    </indicators>\n");
        }
        
        Collection<DrawingDescriptor> drawingDescriptors = contents.lookupDescriptors(DrawingDescriptor.class);
        if (drawingDescriptors.size() > 0) {
            buffer.append("    <drawings>\n");
            for (DrawingDescriptor descriptor : drawingDescriptors) {
                buffer.append("        <layer ");
                buffer.append("name=\"" + descriptor.getServiceClassName() + "\" ");
                buffer.append("nunits=\"" + descriptor.getFreq().nUnits + "\" ");
                buffer.append("unit=\"" + descriptor.getFreq().unit + "\">\n ");
                Map<HandledChart, List<ValuePoint>> chartMapPoints = descriptor.getHandledChartMapPoints();
                for (HandledChart chart : chartMapPoints.keySet()) {
                    buffer.append("            <chart class=\"" + chart.getClass().getName() + "\">\n");
                    for (ValuePoint point : chartMapPoints.get(chart)) {
                        buffer.append("                <handle t=\"" + point.t + "\" v=\"" + point.v + "\"/>\n");
                    }
                    buffer.append("            </chart>\n");
                }
                buffer.append("        </layer>\n");
            }
            buffer.append("    </drawings>\n");
        }
        
        buffer.append("</sec>");
        
        beans.saveDoc();
        
        return buffer.toString();
    }
    
    public static void loadContents() {
        
    }
    
    
}
