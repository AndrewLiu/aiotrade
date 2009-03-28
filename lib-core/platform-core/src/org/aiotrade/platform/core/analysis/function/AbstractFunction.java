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
package org.aiotrade.platform.core.analysis.function;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.aiotrade.platform.core.analysis.indicator.Direction;
import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.math.timeseries.DefaultSer;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
public abstract class AbstractFunction extends DefaultSer implements FunctionSer {
    /**
     * @TODO
     * Concurrent issues: use function as key instead of ser?
     */
    private final static Map<Ser, Set<WeakReference<Function>>> serMapFunctions =
            new WeakHashMap<Ser, Set<WeakReference<Function>>>();
    public final static <T extends Function> T getInstance(
            final Class<T> type,
            final Ser baseSer,
            final Object... args) {
        
        /** get this baseSer's functionSet first, if none, create new one */
        Set<WeakReference<Function>> functionRefSet;
        synchronized (serMapFunctions) {
            functionRefSet = serMapFunctions.get(baseSer);
            if (functionRefSet == null) {
                functionRefSet = new HashSet<WeakReference<Function>>();
                serMapFunctions.put(baseSer, functionRefSet);
            }
        }
        
        synchronized (functionRefSet) {
            /** lookup in functionSet, if found, return it */
            for (WeakReference<Function> functionRef : functionRefSet) {
                final Function function = functionRef.get();
                if (type.isInstance(function) && function.idEquals(baseSer, args)) {
                    return (T)function;
                }
            }
            
            /** if none got from functionSet, try to create new one */
            T function = null;
            try {
                function = type.newInstance();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            }
            
            if (function != null) {
                /** don't forget to call set(baseSer, args) immediatley */
                function.set(baseSer, args);
                functionRefSet.add(new WeakReference(function));
            }
            
            return function;
        }
        
    }
    
    /**
     * Use computing session to avoid redundant computation on same idx of same
     * function instance by different callers.
     *
     * A session is a series of continuant computing usally called by Indicator
     * It may contains a couple of functions that being called during it.
     *
     * The sessionId is injected in by the caller.
     */
    private long sessionId = -Long.MAX_VALUE;
    private int computedIdx = -Integer.MAX_VALUE;
    
    /** base series to compute this. */
    protected Ser _baseSer;
    /** base series' item size */
    protected int _itemSize;
    
    /** To store values of open, high, low, close, volume: */
    protected Var<Float> O;
    protected Var<Float> H;
    protected Var<Float> L;
    protected Var<Float> C;
    protected Var<Float> V;
    
    public AbstractFunction() {
    }
    
    public void set(final Ser baseSer, final Object... args) {
        init(baseSer);
    }
    
    protected void init(final Ser baseSer) {
        super.init(baseSer.getFreq());
        this._baseSer = baseSer;
        
        initPredefinedVarsOfBaseSer();
    }
    
    /** override this method to define your own pre-defined vars if necessary */
    protected void initPredefinedVarsOfBaseSer() {
        if (_baseSer instanceof QuoteSer) {
            final QuoteSer quoteSer = (QuoteSer)_baseSer;
            O = quoteSer.getOpen();
            H = quoteSer.getHigh();
            L = quoteSer.getLow();
            C = quoteSer.getClose();
            V = quoteSer.getVolume();
        }
    }
    
    protected void setComputedIdx(final int computedIdx) {
        this.computedIdx = computedIdx;
    }
    
    protected int getComputedIdx() {
        return computedIdx;
    }
    
    /**
     * This method will compute from computedIdx <b>to</b> idx.
     *
     * and AbstractIndicator.compute(final long begTime) will compute <b>from</b>
     * begTime to last item
     *
     * @param sessionId, the sessionId usally is controlled by outside caller,
     *        such as an indicator
     * @param idx, the idx to be computed to
     */
    public void computeTo(final long sessionId, final int idx) {
        preComputeTo(sessionId, idx);
        
        /**
         * if in same session and idx has just been computed, do not do
         * redundance computation
         */
        if (this.sessionId == sessionId && idx <= computedIdx) {
            return;
        }
        
        this.sessionId = sessionId;
        
        /** computedIdx itself has been computed, so, compare computedIdx + 1 with idx */
        int begIdx = Math.min(computedIdx + 1, idx);
        if (begIdx < 0) {
            begIdx = 0;
        }
        
        /**
         * get baseSer's itemList size via protected _itemSize here instead of by
         * indicator's subclass when begin computeCont, because we could not
         * sure if the baseSer's _itemSize size has been change by others
         * (DataServer etc.)
         */
        _itemSize = _baseSer.itemList().size();
        
        final int endIdx = Math.min(idx, _itemSize - 1);
        /** fill with clear items from begIdx, then call computeSpot(i): */
        for (int i = begIdx; i <= endIdx; i++) {
            final long time = _baseSer.timestamps().get(i);
            createItemOrClearIt(time);
            
            computeSpot(i);
        }
        
        computedIdx = idx;
        
        postComputeTo(sessionId, idx);
    }
    
    /**
     * override this method to do something before computeTo, such as set computedIdx etc.
     */
    protected void preComputeTo(final long sessionId, final int idx) {
    }
    
    /**
     * override this method to do something post computeTo
     */
    protected void postComputeTo(final long sessionId, final int idx) {
    }
    
    /**
     * @param i, idx of spot
     */
    protected abstract void computeSpot(final int i);
    
    /**
     * Define functions
     * --------------------------------------------------------------------
     */
    
    /**
     * Functions of helper
     * ----------------------------------------------------------------------
     */
    
    protected int indexOfLastValidValue(final Var var) {
        final List<Float> values = var.values();
        for (int i = values.size() - 1; i > 0; i--) {
            final Float value = values.get(i);
            if (value != null && !Float.isNaN(value)) {
                return _baseSer.indexOfOccurredTime(timestamps().get(i));
            }
        }
        return -1;
    }
    
    /**
     * ---------------------------------------------------------------------
     * End of functions of helper
     */
    
    
    
    
    /**
     * Functions from FunctionSereis
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
    
    
    /**
     * ----------------------------------------------------------------------
     * End of Functions from FunctionSereis
     */
}
