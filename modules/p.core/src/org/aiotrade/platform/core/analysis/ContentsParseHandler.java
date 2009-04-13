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

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.indicator.IndicatorDescriptor;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.charting.chart.util.ValuePoint;
import org.aiotrade.lib.charting.chart.handledchart.HandledChart;
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.computable.Option;
import org.aiotrade.lib.math.timeseries.Unit;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.platform.core.sec.Sec;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;


/**
 *
 * @author Caoyuan Deng
 */
public class ContentsParseHandler extends DefaultHandler {
    private static NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();
    
    private AnalysisContents contents;
    
    private IndicatorDescriptor indicatorDescriptor;
    private List<Opt> opts;
    
    private DrawingDescriptor drawingDescriptor;
    private Map<HandledChart, List<ValuePoint>> handledChartMapPoints;
    private String handledChartClassName;
    private List<ValuePoint> points;
    
    public static final boolean DEBUG = false;
    
    private StringBuffer buffer;
    private Stack context;
    
    private Calendar calendar = Calendar.getInstance();
    
    public ContentsParseHandler() {
        buffer = new StringBuffer(500);
        context = new Stack();
    }
    
    public final void startElement(String ns, String name, String qname, Attributes attrs) throws SAXException {
        dispatch(true);
        context.push(new Object[] {qname, new AttributesImpl(attrs)});
        if ("handle".equals(qname)) {
            handle_handle(attrs);
        } else if ("indicator".equals(qname)) {
            start_indicator(attrs);
        } else if ("chart".equals(qname)) {
            start_chart(attrs);
        } else if ("opt".equals(qname)) {
            handle_opt(attrs);
        } else if ("indicators".equals(qname)) {
            start_indicators(attrs);
        } else if ("drawings".equals(qname)) {
            start_drawings(attrs);
        } else if ("sec".equals(qname)) {
            start_sec(attrs);
        } else if ("layer".equals(qname)) {
            start_layer(attrs);
        } else if ("sources".equals(qname)) {
            start_sources(attrs);
        } else if ("source".equals(qname)) {
            start_source(attrs);
        }
    }
    
    /**
     *
     * This SAX interface method is implemented by the parser.
     */
    public final void endElement(String ns, String name, String qname) throws SAXException    {
        dispatch(false);
        context.pop();
        if ("indicator".equals(qname)) {
            end_indicator();
        } else if ("chart".equals(qname)) {
            end_chart();
        } else if ("indicators".equals(qname)) {
            end_indicators();
        } else if ("drawings".equals(qname)) {
            end_drawings();
        } else if ("sec".equals(qname)) {
            end_sec();
        } else if ("layer".equals(qname)) {
            end_layer();
        } else if ("sources".equals(qname)) {
            end_sources();
        }
    }
    
    
    private void dispatch(final boolean fireOnlyIfMixed) throws SAXException {
        if (fireOnlyIfMixed && buffer.length() == 0) {
            return; //skip it
        }
        Object[] ctx = (Object[]) context.peek();
        String here = (String) ctx[0];
        Attributes attrs = (Attributes) ctx[1];
        buffer.delete(0, buffer.length());
    }
    
    
    public void handle_handle(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("handle_handle: " + meta);
        }
        try {
            ValuePoint point = new ValuePoint();
            point.t = NUMBER_FORMAT.parse(meta.getValue("t").trim()).longValue();
            point.v = NUMBER_FORMAT.parse(meta.getValue("v").trim()).floatValue();
            points.add(point);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
    
    public void start_indicator(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("start_indicator: " + meta.getValue("class"));
        }
        indicatorDescriptor = new IndicatorDescriptor();
        indicatorDescriptor.setActive(Boolean.parseBoolean(meta.getValue("active").trim()));
        indicatorDescriptor.setServiceClassName(meta.getValue("class"));
        Frequency freq = new Frequency(
                Unit.valueOf(meta.getValue("unit")),
                Integer.parseInt(meta.getValue("nunits").trim()));
        indicatorDescriptor.setFreq(freq);
        
        opts = new ArrayList();
    }
    
    public void end_indicator() throws SAXException {
        if (DEBUG) {
            System.err.println("end_indicator()");
        }
        indicatorDescriptor.setOpts(opts);
        contents.addDescriptor(indicatorDescriptor);
    }
    
    public void start_chart(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("start_chart: " + meta);
        }
        handledChartClassName = meta.getValue("class");
        points = new ArrayList<ValuePoint>();
    }
    
    public void end_chart() throws SAXException {
        if (DEBUG) {
            System.err.println("end_chart()");
        }
        
        HandledChart handledChart = null;
        try {
            handledChart = (HandledChart)Class.forName(handledChartClassName).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (handledChart !=null) {
            handledChartMapPoints.put(handledChart, points);
        }
    }
    
    public void handle_opt(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("handle_opt: " + meta.getValue("value"));
        }
        
        String nameStr     = meta.getValue("name");
        String valueStr    = meta.getValue("value");
        String stepStr     = meta.getValue("step");
        String maxValueStr = meta.getValue("maxvalue");
        String minValueStr = meta.getValue("minvalue");
        
        try {
            Number value    = NUMBER_FORMAT.parse(valueStr.trim());
            Number step     = stepStr     == null ? null : NUMBER_FORMAT.parse(stepStr.trim());
            Number maxValue = maxValueStr == null ? null : NUMBER_FORMAT.parse(maxValueStr.trim());
            Number minValue = minValueStr == null ? null : NUMBER_FORMAT.parse(minValueStr.trim());
            
            Opt opt = new Option(nameStr, value, step, minValue, maxValue);
            opts.add(opt);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
    
    public void start_indicators(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("start_indicators: " + meta);
        }
    }
    
    public void end_indicators() throws SAXException {
        if (DEBUG) {
            System.err.println("end_indicators()");
        }
        
    }
    
    public void start_drawings(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("start_drawings: " + meta);
        }
    }
    
    public void end_drawings() throws SAXException {
        if (DEBUG)
            System.err.println("end_drawings()");
    }
    
    public void start_sec(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("start_sofic: " + meta.getValue("unisymbol"));
        }
        String uniSymbol = meta.getValue("unisymbol");
        contents = new AnalysisContents(uniSymbol);
    }
    
    public void end_sec() throws SAXException {
        if (DEBUG) {
            System.err.println("end_sofic()");
        }
    }
    
    public void start_source(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("start_source: " + meta);
        }
        
        QuoteContract dataContract = new QuoteContract();
        
        dataContract.setActive(Boolean.parseBoolean(meta.getValue("active").trim()));
        dataContract.setServiceClassName(meta.getValue("class"));
        
        dataContract.setSymbol(meta.getValue("symbol"));
        dataContract.setSecType(Sec.Type.valueOf(meta.getValue("sectype")));
        dataContract.setExchange(meta.getValue("exchange"));
        dataContract.setPrimaryExchange(meta.getValue("primaryexchange"));
        dataContract.setCurrency(meta.getValue("currency"));
        
        dataContract.setDateFormatString(meta.getValue("dateformat"));
        
        Frequency freq = new Frequency(
                Unit.valueOf(meta.getValue("unit")),
                Integer.parseInt(meta.getValue("nunits").trim()));
        dataContract.setFreq(freq);
        
        dataContract.setRefreshable(Boolean.parseBoolean(meta.getValue("refreshable").trim()));
        dataContract.setRefreshInterval(Integer.parseInt(meta.getValue("refreshinterval").trim()));
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        try {
            calendar.setTime(sdf.parse(meta.getValue("begdate").trim()));
            dataContract.setBeginDate(calendar.getTime());
            
            calendar.setTime(sdf.parse(meta.getValue("enddate").trim()));
            dataContract.setEndDate(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        dataContract.setUrlString(meta.getValue("url"));
        
        contents.addDescriptor(dataContract);
    }
    
    public void start_sources(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("start_sources: " + meta);
        }
    }
    
    public void end_sources() throws SAXException {
        if (DEBUG) {
            System.err.println("end_sources()");
        }
        
    }
    
    public void start_layer(final Attributes meta) throws SAXException {
        if (DEBUG) {
            System.err.println("start_layer: " + meta);
        }
        drawingDescriptor = new DrawingDescriptor();
        drawingDescriptor.setServiceClassName(meta.getValue("name"));
        Frequency freq = new Frequency(
                Unit.valueOf(meta.getValue("unit")),
                Integer.parseInt(meta.getValue("nunits").trim()));
        drawingDescriptor.setFreq(freq);
        
        handledChartMapPoints = new HashMap();
    }
    
    public void end_layer() throws SAXException {
        if (DEBUG) {
            System.err.println("end_layer()");
        }
        drawingDescriptor.setHandledChartMapPoints(handledChartMapPoints);
        contents.addDescriptor(drawingDescriptor);
    }
    
    public AnalysisContents getContents() {
        return contents;
    }
    
    public void javac() {
        
    }
    
}
