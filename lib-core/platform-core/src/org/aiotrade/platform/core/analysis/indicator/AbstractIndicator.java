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
package org.aiotrade.platform.core.analysis.indicator;

import java.util.List;
import org.aiotrade.platform.core.analysis.function.AbstractFunction;
import org.aiotrade.platform.core.analysis.function.Function;
import org.aiotrade.platform.core.analysis.function.ZIGZAGFunction;
import org.aiotrade.platform.core.analysis.function.ADXFunction;
import org.aiotrade.platform.core.analysis.function.ADXRFunction;
import org.aiotrade.platform.core.analysis.function.BOLLFunction;
import org.aiotrade.platform.core.analysis.function.CCIFunction;
import org.aiotrade.platform.core.analysis.function.DIFunction;
import org.aiotrade.platform.core.analysis.function.DMFunction;
import org.aiotrade.platform.core.analysis.function.DXFunction;
import org.aiotrade.platform.core.analysis.function.EMAFunction;
import org.aiotrade.platform.core.analysis.function.MACDFunction;
import org.aiotrade.platform.core.analysis.function.MAFunction;
import org.aiotrade.platform.core.analysis.function.MAXFunction;
import org.aiotrade.platform.core.analysis.function.MFIFunction;
import org.aiotrade.platform.core.analysis.function.MINFunction;
import org.aiotrade.platform.core.analysis.function.MTMFunction;
import org.aiotrade.platform.core.analysis.function.OBVFunction;
import org.aiotrade.platform.core.analysis.function.PROBMASSFunction;
import org.aiotrade.platform.core.analysis.function.ROCFunction;
import org.aiotrade.platform.core.analysis.function.RSIFunction;
import org.aiotrade.platform.core.analysis.function.SARFunction;
import org.aiotrade.platform.core.analysis.function.STDDEVFunction;
import org.aiotrade.platform.core.analysis.function.STOCHDFunction;
import org.aiotrade.platform.core.analysis.function.STOCHJFunction;
import org.aiotrade.platform.core.analysis.function.STOCHKFunction;
import org.aiotrade.platform.core.analysis.function.SUMFunction;
import org.aiotrade.platform.core.analysis.function.TRFunction;
import org.aiotrade.platform.core.analysis.function.WMSFunction;
import org.aiotrade.math.timeseries.computable.Indicator;
import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.math.timeseries.computable.Option;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.math.timeseries.DefaultSer;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;
import org.aiotrade.math.timeseries.computable.ComputableHelper;

/**
 *
 * @author Caoyuan Deng
 */
@IndicatorName("Abstract Indicator")
public abstract class AbstractIndicator extends DefaultSer implements Indicator {
    protected final static float NaN = Float.NaN;
    
    /** a static global session id */
    private static long sessionId;
    
    /**
     * !NOTICE
     * computableHelper should be created here, because it will be used to
     * inject DefaultOpt(s): new DefaultOpt() will call addOpt which delegated
     * by computableHelper.addOpt(..)
     */
    private ComputableHelper computableHelper = new ComputableHelper();
    
    /** some instance scope variables that can be set directly */
    protected boolean _overlapping = false;
    protected String _sname = "unkown";
    protected String _lname = "unkown";
    
    /**
     * horizonal _grids of this indicator used to draw grid
     */
    protected Float[] _grids;
    
    /** base series to compute this */
    protected Ser _baseSer;
    /** base series' item size */
    protected int _itemSize;
    
    /** To store values of open, high, low, close, volume: */
    protected Var<Float> O;
    protected Var<Float> H;
    protected Var<Float> L;
    protected Var<Float> C;
    protected Var<Float> V;
    
    /**
     * Make sure this null args contructor only be called and return instance to
     * NetBeans layer manager for register usage, so it just do nothing.
     */
    public AbstractIndicator() {
        /** do nothing: computableHelper should has been initialized in instance scope */
    }
    
    public AbstractIndicator(Ser baseSer) {
        init(baseSer);
    }
    
    /**
     * make sure this method will be called before this instance return to any others:
     * 1. via constructor (except the no-arg constructor)
     * 2. via createInstance
     */
    public void init(Ser baseSer) {
        super.init(baseSer.getFreq());
        this._baseSer = baseSer;
        
        this.computableHelper.init(baseSer, this);
        
        initPredefinedVarsOfBaseSer();
    }
    
    /** override this method to define your predefined vars */
    protected void initPredefinedVarsOfBaseSer() {
        if (_baseSer instanceof QuoteSer) {
            QuoteSer quoteSer = (QuoteSer)_baseSer;
            O = quoteSer.getOpen();
            H = quoteSer.getHigh();
            L = quoteSer.getLow();
            C = quoteSer.getClose();
            V = quoteSer.getVolume();
        }
    }
    
    protected final void addOpt(Opt opt) {
        computableHelper.addOpt(opt);
    }
    
    public List<Opt> getOpts() {
        return computableHelper.getOpts();
    }
    
    public void setOpts(final List<Opt> opts) {
        computableHelper.setOpts(opts);
    }
    
    public void setOpts(final Number[] values) {
        computableHelper.setOpts(values);
    }
    
    public Float[] getGrids() {
        return _grids;
    }
    
    public boolean isOverlapping() {
        return _overlapping;
    }
    
    public void setOverlapping(boolean b) {
        _overlapping = b;
    }
    
    protected static void setSessionId() {
        sessionId++;
    }
    
    public long getComputedTime() {
        return lastOccurredTime();
    }
    
    /**
     * !NOTICE
     * It's better to fire ser change events or opt change event instead of
     * call me directly. But, in case of baseSer has been loaded, there may
     * be no more ser change events fired, so when first create, call computeFrom(0)
     * is a safe maner.
     *
     * @TODO
     * Should this method synchronized?
     * As each seriesProvider has its own indicator instance, and indicator instance
     * usually called by chartview, that means, they are called usually in same
     * thread: awt.event.thread.
     *
     * @param begin time to be computed
     */
    public void computeFrom(final long begTime) {
        setSessionId();
        
        /**
         * get baseSer's itemList size via protected _itemSize here instead of by
         * indicator's subclass when begin computeCont, because we could not
         * sure if the baseSer's _itemSize size has been change by others
         * (DataServer etc.)
         */
        _itemSize = _baseSer.itemList().size();
        
        final int begIdx = computableHelper.preComputeFrom(begTime);
        /** fill with clear items from begIdx: */
        for (int i = begIdx; i < _itemSize; i++) {
            final long time = _baseSer.timestamps().get(i);
            
            /**
             * if baseSer is MasterSer, we'll use timeOfRow(idx) to get the time,
             * this enable returning a good time even idx < 0 or exceed itemList.size()
             * because it will trace back in *calendar* time.
             * @TODO
             */
            /*-
            long time = _baseSer instanceof MasterSer ?
                ((MasterSer)_baseSer).timeOfRow(i) :
                _baseSer.timeOfIndex(i);
             */
            
            /** 
             * we've fetch time from _baseSer, but not sure if this time has been 
             * added to my timestamps, so, do any way:
             */
            createItemOrClearIt(time);
        }
        
        computeCont(begIdx);
        
        computableHelper.postComputeFrom();
    }
    
    protected int preComputeFrom(final long begTime) {
        return computableHelper.preComputeFrom(begTime);
    }
    
    public void postComputeFrom() {
        computableHelper.postComputeFrom();
    }
    
    protected abstract void computeCont(int begIdx);
    
    protected String getLongDescription() {
        return _lname;
    }
    
    @Override
    public String getShortDescription() {
        return _sname;
    }
    
    @Override
    public void setShortDescription(String description) {
        this._sname = description;
    }
    
    @Override
    public String toString() {
        return getLongDescription() != null ?
            getShortDescription() + " - " + getLongDescription() : getShortDescription();
    }
    
    public final int compareTo(Indicator another) {
        if (this.toString().equalsIgnoreCase(another.toString())) {
            return this.hashCode() < another.hashCode() ? -1 : (this.hashCode() == another.hashCode() ? 0 : 1);
        } else {
            return this.toString().compareTo(another.toString());
        }
    }
    
    public Indicator createNewInstance(final Ser baseSer) {
        try {
            final Indicator instance = (Indicator)this.getClass().newInstance();
            instance.init(baseSer);
            
            return instance;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    /**
     * Define functions
     * --------------------------------------------------------------------
     */
    
    /**
     * Functions for test
     * ----------------------------------------------------------------------
     */
    
    protected final static boolean crossOver(int idx, Var<Float> var1, Var<Float> var2) {
        if (idx > 0) {
            if (var1.get(idx) >= var2.get(idx) &&
                    var1.get(idx - 1) < var2.get(idx - 1)) {
                return true;
            }
        }
        return false;
    }
    
    protected final static boolean crossOver(int idx, Var<Float> var, float value) {
        if (idx > 0) {
            if (var.get(idx) >= value &&
                    var.get(idx - 1) < value) {
                return true;
            }
        }
        return false;
    }
    
    protected final static boolean crossUnder(int idx, Var<Float> var1, Var<Float> var2) {
        if (idx > 0) {
            if (var1.get(idx) < var2.get(idx) &&
                    var1.get(idx - 1) >= var2.get(idx - 1)) {
                return true;
            }
        }
        return false;
    }
    
    protected final static boolean crossUnder(int idx, Var<Float> var, float value) {
        if (idx > 0) {
            if (var.get(idx) < value &&
                    var.get(idx - 1) >= value) {
                return true;
            }
        }
        return false;
    }
    
    protected final static boolean turnUp(int idx, Var<Float> var) {
        if (idx > 1) {
            if (var.get(idx) > var.get(idx - 1) &&
                    var.get(idx - 1) <= var.get(idx - 2)) {
                return true;
            }
        }
        return false;
    }
    
    protected final static boolean turnDown(int idx, Var<Float> var) {
        if (idx > 1) {
            if (var.get(idx) < var.get(idx - 1) &&
                    var.get(idx - 1) >= var.get(idx - 2)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * ---------------------------------------------------------------------
     * End of functions for test
     */
    
    
    
    
    /**
     * a helper function for keeping the same functin form as Function, don't be
     * puzzled by the name, it actully will return funcion instance
     */
    protected final static <T extends Function> T getInstance(Class<T> clazz,Ser baseSer, Object... args) {
        return AbstractFunction.getInstance(clazz, baseSer, args);
    }
    
    /**
     * Functions
     * ----------------------------------------------------------------------
     */
    
    protected final float sum(int idx, Var var, Opt period) {
        return getInstance(SUMFunction.class, _baseSer, var, period).getSum(sessionId, idx);
    }
    
    protected final float max(int idx, Var var, Opt period) {
        return getInstance(MAXFunction.class, _baseSer, var, period).getMax(sessionId, idx);
    }
    
    protected final float min(int idx, Var var, Opt period) {
        return getInstance(MINFunction.class, _baseSer, var, period).getMin(sessionId, idx);
    }
    
    protected final float ma(int idx, Var var, Opt period) {
        return getInstance(MAFunction.class, _baseSer, var, period).getMa(sessionId, idx);
    }
    
    protected final float ema(int idx, Var var, Opt period) {
        return getInstance(EMAFunction.class, _baseSer, var, period).getEma(sessionId, idx);
    }
    
    protected final float stdDev(int idx, Var var, Opt period) {
        return getInstance(STDDEVFunction.class, _baseSer, var, period).getStdDev(sessionId, idx);
    }
    
    protected final Float[][] probMass(int idx, Var<Float> var, Opt period, Opt nInterval) {
        return getInstance(PROBMASSFunction.class, _baseSer, var, null, period, nInterval).getProbMass(sessionId, idx);
    }
    
    protected final Float[][] probMass(int idx, Var<Float> var, Var<Float> weight, Opt period, Opt nInterval) {
        return getInstance(PROBMASSFunction.class, _baseSer, var, weight, period, nInterval).getProbMass(sessionId, idx);
    }
    
    protected final float tr(int idx) {
        return getInstance(TRFunction.class, _baseSer).getTr(sessionId, idx);
    }
    
    protected final float dmPlus(int idx) {
        return getInstance(DMFunction.class, _baseSer).getDmPlus(sessionId, idx);
    }
    
    protected final float dmMinus(int idx) {
        return getInstance(DMFunction.class, _baseSer).getDmMinus(sessionId, idx);
    }
    
    protected final float diPlus(int idx, Opt period) {
        return getInstance(DIFunction.class, _baseSer, period).getDiPlus(sessionId, idx);
    }
    
    protected final float diMinus(int idx, Opt period) {
        return getInstance(DIFunction.class, _baseSer, period).getDiMinus(sessionId, idx);
    }
    
    protected final float dx(int idx, Opt period) {
        return getInstance(DXFunction.class, _baseSer, period).getDx(sessionId, idx);
    }
    
    protected final float adx(int idx, Opt periodDi, Opt periodAdx) {
        return getInstance(ADXFunction.class, _baseSer, periodDi, periodAdx).getAdx(sessionId, idx);
    }
    
    protected final float adxr(int idx, Opt periodDi, Opt periodAdx) {
        return getInstance(ADXRFunction.class, _baseSer, periodDi, periodAdx).getAdxr(sessionId, idx);
    }
    
    protected final float bollMiddle(int idx, Var var, Opt period, Opt alpha) {
        return getInstance(BOLLFunction.class, _baseSer, var, period, alpha).getBollMiddle(sessionId, idx);
    }
    
    protected final float bollUpper(int idx, Var var, Opt period, Opt alpha) {
        return getInstance(BOLLFunction.class, _baseSer, var, period, alpha).getBollUpper(sessionId, idx);
    }
    
    protected final float bollLower(int idx, Var var, Opt period, Opt alpha) {
        return getInstance(BOLLFunction.class, _baseSer, var, period, alpha).getBollLower(sessionId, idx);
    }
    
    protected final float cci(int idx, Opt period, Opt alpha) {
        return getInstance(CCIFunction.class, _baseSer, period, alpha).getCci(sessionId, idx);
    }
    
    protected final float macd(int idx, Var var, Opt periodSlow, Opt periodFast) {
        return getInstance(MACDFunction.class, _baseSer, var, periodSlow, periodFast).getMacd(sessionId, idx);
    }
    
    protected final float mfi(int idx, Opt period) {
        return getInstance(MFIFunction.class, _baseSer, period).getMfi(sessionId, idx);
    }
    
    protected final float mtm(int idx, Var var, Opt period) {
        return getInstance(MTMFunction.class, _baseSer, var, period).getMtm(sessionId, idx);
    }
    
    protected final float obv(int idx) {
        return getInstance(OBVFunction.class, _baseSer).getObv(sessionId, idx);
    }
    
    protected final float roc(int idx, Var var, Opt period) {
        return getInstance(ROCFunction.class, _baseSer, var, period).getRoc(sessionId, idx);
    }
    
    protected final float rsi(int idx, Opt period) {
        return getInstance(RSIFunction.class, _baseSer, period).getRsi(sessionId, idx);
    }
    
    protected final float sar(int idx, Opt initial, Opt step, Opt maximum) {
        return getInstance(SARFunction.class, _baseSer, initial, step, maximum).getSar(sessionId, idx);
    }
    
    protected final Direction sarDirection(int idx, Opt initial, Opt step, Opt maximum) {
        return getInstance(SARFunction.class, _baseSer, initial, step, maximum).getSarDirection(sessionId, idx);
    }
    
    protected final float stochK(int idx, Opt period, Opt periodK) {
        return getInstance(STOCHKFunction.class, _baseSer, period, periodK).getStochK(sessionId, idx);
    }
    
    protected final float stochD(int idx, Opt period, Opt periodK, Opt periodD) {
        return getInstance(STOCHDFunction.class, _baseSer, period, periodK, periodD).getStochD(sessionId, idx);
    }
    
    protected final float stochJ(int idx, Opt period, Opt periodK, Opt periodD) {
        return getInstance(STOCHJFunction.class, _baseSer, period, periodK, periodD).getStochJ(sessionId, idx);
    }
    
    protected final float wms(int idx, Opt period) {
        return getInstance(WMSFunction.class, _baseSer, period).getWms(sessionId, idx);
    }
    
    protected final float zigzag(int idx, Opt percent) {
        return getInstance(ZIGZAGFunction.class, _baseSer, percent).getZigzag(sessionId, idx);
    }
    
    protected final float pseudoZigzag(int idx, Opt percent) {
        return getInstance(ZIGZAGFunction.class, _baseSer, percent).getPseudoZigzag(sessionId, idx);
    }
    
    protected final Direction zigzagDirection(int idx, Opt percent) {
        return getInstance(ZIGZAGFunction.class, _baseSer, percent).getZigzagDirection(sessionId, idx);
    }
    
    public void dispose() {
        computableHelper.dispose();
    }
    
    /**
     * ----------------------------------------------------------------------
     * End of Functions
     */
    
    
    /**
     * Inner DefaultOpt class that will be added to AbstractIndicator instance
     * automaticlly when new it.
     * DefaultOpt can only lives in AbstractIndicator
     *
     *
     * @see addOpt()
     * --------------------------------------------------------------------
     */
    public class DefaultOpt extends Option {
        
        public DefaultOpt(String name, Number value) {
            super(name, value);
            addOpt(this);
        }
        
        public DefaultOpt(String name, Number value, Number step) {
            super(name, value, step);
            addOpt(this);
        }
        
        public DefaultOpt(String name, Number value, Number step, Number minValue, Number maxValue) {
            super(name, value, step, minValue, maxValue);
            addOpt(this);
        }
        
    }
    
}