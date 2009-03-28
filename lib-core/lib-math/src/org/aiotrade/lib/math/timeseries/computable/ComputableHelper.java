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
package org.aiotrade.lib.math.timeseries.computable;

import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.SerChangeEvent;
import org.aiotrade.lib.math.timeseries.SerChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.aiotrade.lib.util.CallBack;

/**
 * A helper class to implement most of the Computable methods, it can be used
 * by indicator etc.
 *
 * @author Caoyuan Deng
 */
public class ComputableHelper {
    private final static DecimalFormat OPT_DECIMAL_FORMAT = new DecimalFormat("0.###");
    
    /**
     * opts of this instance, such as period long, period short etc,
     * it's 'final' to avoid being replaced somewhere.
     */
    private final List<Opt> opts = new ArrayList<Opt>();
    
    /**
     * base series to compute resultSer
     */
    private Ser baseSer;
    /** result series to be computed: */
    private Indicator resultSer;
    
    private SerChangeListener baseSerChangeListener;
    
    private CallBack baseSerChangeEventCallBack;
    
    public ComputableHelper() {
        /** do nothing: opts should has been initialized in instance initialization procedure */
    }
    
    public ComputableHelper(Ser baseSer, Indicator resultSer) {
        init(baseSer, resultSer);
    }
    
    public void init(Ser baseSer, Indicator resultSer) {
        this.baseSer = baseSer;
        this.resultSer = resultSer;
        
        addBaseSerChangeListener();
    }
    
    private void addBaseSerChangeListener() {
        /**
         * The series is a result computed from baseSeries, so
         * should follow the baseSeries' data changing:
         * 1. In case of series is the same as baseSeries, should repond
         *    to FinishingComputing event of baseSeries.
         * 2. In case of series is not the same as baseSeries, should repond
         *    to FinishedLoading, RefreshInLoading and Updated event of baseSeries.
         */
        if (resultSer == baseSer) {
            
            baseSerChangeListener = new SerChangeListener() {
                public void serChanged(SerChangeEvent evt) {
                    final long fromTime = evt.getBeginTime();
                    switch (evt.getType()) {
                        case FinishedLoading:
                        case RefreshInLoading:
                        case Updated:
                            /**
                             * only responds to those events fired by outside for baseSer,
                             * such as loaded from a data server etc.
                             */
                            /** call back */
                            resultSer.computeFrom(fromTime);
                            break;
                            
                        default:
                    }
                    
                    /** process event's callback, remember it to forwarded it in postCompute() late */
                    baseSerChangeEventCallBack = evt.getCallBack();
                }
            };
            
        } else {
            
            baseSerChangeListener = new SerChangeListener() {
                public void serChanged(SerChangeEvent evt) {
                    final long begTime = evt.getBeginTime();
                    switch (evt.getType()) {
                        case FinishedLoading:
                        case RefreshInLoading:
                        case Updated:
                        case FinishedComputing:
                            /**
                             * If the resultSer is the same as baseSer (such as QuoteSer), 
                             * the baseSer will fire an event when compute() finished, 
                             * then run to here, this may cause a dead loop. So, added 
                             * FinishedComputing event to diff from Updated(caused by outside)
                             */
                            /** call back */
                            resultSer.computeFrom(begTime);
                            break;
                        case Clear:
                            resultSer.clear(begTime);
                            break;
                        default:
                    }
                    
                    /** remember event's callback to be forwarded in postCompute() */
                    baseSerChangeEventCallBack = evt.getCallBack();
                }
            };
            
        }
        
        baseSer.addSerChangeListener(baseSerChangeListener);
    }
    
    /**
     * preComputeFrom() will set and backup the context before computeFrom(long begTime):
     * begTime, begIdx etc.
     *
     *
     * @return begIdx
     */
    private long begTime;
    public int preComputeFrom(final long begTime) {
        assert this.baseSer != null : "base series not set!";
        
        this.begTime = begTime;
        final long computedTime = resultSer.getComputedTime();
        
        int begIdx;
        if (begTime < computedTime || begTime == 0) {
            begIdx = 0;
        } else {
            /** if computedTime < begTime, re-compute from the minimal one */
            this.begTime = Math.min(computedTime, begTime);
            
            /** indexOfTime always return physical index, so don't worry about isOncalendarTime() */
            begIdx = this.baseSer.indexOfOccurredTime(begTime);
            begIdx = begIdx < 0 ?
                0 : begIdx;
        }
        
        /**
         * should re-compute series except it's also the baseSer:
         * @TODO
         * Do we really need clear it from begTime, or just from computed time after computing?
         */
        //        if (resultSer != baseSer) {
        //            /** in case of resultSer == baseSer, do this will also clear baseSer */
        //            resultSer.clear(begTime);
        //        }
        
        return begIdx;
    }
    
    public void postComputeFrom() {
        /** construct resultSer's change event, forward baseSerChangeEventCallBack */
        resultSer.fireSerChangeEvent(new SerChangeEvent(
                resultSer,
                SerChangeEvent.Type.FinishedComputing,
                null,
                begTime,
                resultSer.getComputedTime(),
                baseSerChangeEventCallBack));
    }
    
    public final void addOpt(final Opt opt) {
        /** add opt change listener to this opt */
        addOptChangeListener(opt);
        
        opts.add(opt);
    }
    
    private void addOptChangeListener(Opt opt) {
        opt.addOptChangeListener(new OptChangeListener() {
            public void optChanged(OptChangeEvent evt) {
                /**
                 * As any one of opt in opt changed will fire change events
                 * for each opt in opt, we only need respond to the first
                 * one.
                 * @see fireOptChangeEvents();
                 */
                if (evt.getSource().equals(opts.get(0))) {
                    /** call back */
                    resultSer.computeFrom(0);
                }
            }
        });
    }
    
    public List<Opt> getOpts() {
        return opts;
    }
    
    /**
     *
     *
     * @return if any value of opts changed, return true, else return false
     */
    public void setOpts(final List<Opt> opts) {
        if (opts != null) {
            Number[] values = new Number[opts.size()];
            for (int i = 0, n = opts.size(); i < n; i++) {
                values[i] = opts.get(i).value();
            }
            setOpts(values);
        }
    }
    
    /**
     *
     *
     * @return if any value of opts changed, return true, else return false
     */
    public void setOpts(final Number[] values) {
        boolean valueChanged = false;
        if (values != null) {
            if (this.opts.size() == values.length) {
                for (int i = 0, n = values.length; i < n; i++) {
                    final Opt myOpt = this.opts.get(i);
                    final Number inValue = values[i];
                    /** check if changed happens before set myOpt */
                    if ((myOpt.value() - inValue.floatValue()) != 0) {
                        valueChanged = true;
                    }
                    myOpt.setValue(inValue);
                }
            }
        }
        
        if (valueChanged) {
            fireOptsChangeEvents();
        }
    }
    
    private void fireOptsChangeEvents() {
        for (Opt opt : opts) {
            opt.fireOptChangeEvent(new OptChangeEvent(opt));
        }
    }
    
    public void replaceOpt(Opt oldOpt, Opt newOpt) {
        int idxOld = -1;
        for (int i = 0, size = opts.size(); i < size; i++) {
            final Opt opt = opts.get(i);
            if (opt.equals(oldOpt)) {
                idxOld = i;
                break;
            }
        }
        
        if (idxOld != -1) {
            addOptChangeListener(newOpt);
            
            opts.set(idxOld, newOpt);
        }
    }

    public static String getDisplayName(Ser ser) {
        if (ser instanceof Computable) {
            return getDisplayName(ser.getShortDescription(), ((Computable)ser).getOpts());
        } else {
            return ser.getShortDescription();
        }
    }
    
    public static String getDisplayName(final String name, final List<Opt> opts) {
        StringBuffer buffer = new StringBuffer(name);
        
        final int size = opts.size();
        for (int i = 0; i < size; i++) {
            if (i == 0) {
                buffer.append(" (");
            }
            buffer.append(OPT_DECIMAL_FORMAT.format(opts.get(i).value()));
            if (i < size - 1) {
                buffer.append(", ");
            } else {
                buffer.append(")");
            }
        }
        
        return buffer.toString();
    }
    
    public void dispose() {
        if (baseSerChangeListener != null) {
            baseSer.removeSerChangeListener(baseSerChangeListener);
        }
    }
    
}



