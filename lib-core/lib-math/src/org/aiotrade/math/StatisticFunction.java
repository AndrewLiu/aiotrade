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
package org.aiotrade.math;

import java.util.List;

/**
 *
 * @author Caoyuan Deng
 */
public class StatisticFunction {

    public final static int MAX = 0;
    public final static int MIN = 1;
    public final static int VALUE = 0;
    public final static int MASS = 1;

    public final static float sum(List<Float> values, int begIdx, int endIdx) {
        if (begIdx < 0 || endIdx >= values.size()) {
            return Float.NaN;
        }

        float sum = 0;
        for (int i = begIdx; i <= endIdx; i++) {
            final Float v = values.get(i);
            if (v != null) {  // @todo why this happens?
                sum += v;
            }
        }

        return sum;
    }

    public final static float isum(int idx, List<Float> values, int period, float prev) {
        final int lookbackIdx = lookback(idx, period);

        if (lookbackIdx < 0 || idx >= values.size()) {
            return Float.NaN;
        } else if (lookbackIdx == 0) {
            /** compute first availabe sum (in case of enough period first time) */
            return sum(values, 0, idx);
        } else {
            if (Float.isNaN(prev)) {
                /**
                 * although the 'values' size is enough, it may contains NaN
                 * element, thus cause the prevSum to be a NaN, we should
                 * precess this case by:
                 */
                return sum(values, lookbackIdx, idx);
            } else {
                return prev + values.get(idx) - values.get(lookbackIdx - 1);
            }
        }
    }

    public final static float ma(List<Float> values, int begIdx, int endIdx) {
        if (begIdx < 0 || endIdx >= values.size()) {
            return Float.NaN;
        }

        final float period = period(begIdx, endIdx);
        return sum(values, begIdx, endIdx) / period;
    }

    /**
     * ma(t + 1) = ma(t) + ( x(t) / N - x(t - n) / N )
     */
    public final static float ima(int idx, List<Float> values, int period, float prev) {
        final int lookbackIdx = lookback(idx, period);

        if (lookbackIdx < 0 || idx >= values.size()) {
            return Float.NaN;
        } else if (lookbackIdx == 0) {
            /** compute first availabe ma (in case of enough period first time) */
            return ma(values, 0, idx);
        } else {
            if (Float.isNaN(prev)) {
                /**
                 * although the 'values' size is enough, it may contains NaN
                 * element, thus cause the prevSum to be a NaN, we should
                 * precess this case by:
                 */
                return ma(values, lookbackIdx, idx);
            } else {
                return prev + (values.get(idx) - values.get(lookbackIdx - 1)) / (float) period;
            }
        }
    }

    public final float ema(List<Float> values, int begIdx, int endIdx) {
        if (begIdx < 0 || endIdx >= values.size()) {
            return Float.NaN;
        }

        final float period = period(begIdx, endIdx);
        float ema = 0;
        for (int i = begIdx, n = values.size(); i <= endIdx && i < n; i++) {
            ema += ((period - 1.0f) / (period + 1.0f)) * ema + (2.0f / (period + 1.0f)) * values.get(i);
        }

        return ema;
    }

    /**
     * ema(t + 1) = ema(t) + ( x(t) / N - ema(t) / N )
     *            = (1 - 1/N) * ema(t) + (1/N) * x(t)
     *            = (1 - a) * ema(t) + a * x(t)  // let a = 1/N
     */
    public final static float iema(int idx, List<Float> values, int period, float prev) {
        float value = values.get(idx);
        if (value == Float.NaN) {
            value = 0;
        }

        final float a = 1f / (float) period;
        return (1 - a) * prev + a * value;
        //return ((period - 1.0f) / (period + 1.0f)) * prevEma + (2.0f / (period + 1.0f)) * value;
    }

    public final static float max(List<Float> values, int begIdx, int endIdx) {
        return maxmin(values, begIdx, endIdx)[MAX];
    }

    public final static float imax(int idx, List<Float> values, int period, float prev) {
        final int lookbackIdx = lookback(idx, period);

        if (lookbackIdx < 0 || idx >= values.size()) {
            return Float.NaN;
        } else if (lookbackIdx == 0) {
            return max(values, 0, idx);
        } else {
            if (Float.isNaN(prev) || values.get(lookbackIdx - 1).equals(prev)) {
                return max(values, lookbackIdx, idx);
            } else {
                final float value = values.get(idx);
                return prev >= value ? prev : value;
            }
        }
    }

    public final static float min(List<Float> values, int begIdx, int endIdx) {
        return maxmin(values, begIdx, endIdx)[MIN];
    }

    public final static float imin(int idx, List<Float> values, int period, float prev) {
        final int lookbackIdx = lookback(idx, period);

        if (lookbackIdx < 0 || idx >= values.size()) {
            return Float.NaN;
        } else if (lookbackIdx == 0) {
            return min(values, 0, idx);
        } else {
            if (Float.isNaN(prev) || values.get(lookbackIdx - 1).equals(prev)) {
                return min(values, lookbackIdx, idx);
            } else {
                final float value = values.get(idx);
                return prev <= value ? prev : value;
            }
        }
    }

    public final static float[] maxmin(List<Float> values, int begIdx, int endIdx) {
        if (begIdx < 0) {
            return new float[]{Float.NaN, Float.NaN};
        }

        float max = -Float.MAX_VALUE;
        float min = +Float.MAX_VALUE;
        final int lastIdx = Math.min(endIdx, values.size() - 1);
        for (int i = begIdx; i <= lastIdx; i++) {
            final float value = values.get(i);
            max = max >= value ? max : value;
            min = min <= value ? min : value;
        }

        return new float[]{max, min};
    }

    public final static float[] maxmin(Float[] values, int begIdx, int endIdx) {
        if (begIdx < 0) {
            return new float[]{Float.NaN, Float.NaN};
        }

        float max = -Float.MAX_VALUE;
        float min = +Float.MAX_VALUE;
        int lastIdx = Math.min(endIdx, values.length - 1);
        for (int i = begIdx; i <= lastIdx; i++) {
            float value = values[i];
            max = (max >= value) ? max : value;
            min = (min <= value) ? min : value;
        }

        return new float[]{max, min};
    }

    /**
     * Standard Deviation
     */
    public final static float stdDev(List<Float> values, int begIdx, int endIdx) {
        if (begIdx < 0 || endIdx >= values.size()) {
            return Float.NaN;
        }

        final float ma = ma(values, begIdx, endIdx);
        final int lastIdx = Math.min(endIdx, values.size() - 1);
        float deviation_square_sum = 0;
        for (int i = begIdx; i <= lastIdx; i++) {
            final float deviation = values.get(i) - ma;
            deviation_square_sum += deviation * deviation;
        }

        final float period = period(begIdx, endIdx);
        return (float) Math.sqrt(deviation_square_sum / period);
    }

    /**
     * Probability Mass Function
     */
    public final static Float[][] probMass(List<Float> values, int begIdx, int endIdx, int nIntervals) {
        return probMass(values, null, begIdx, endIdx, nIntervals);
    }

    /**
     * Probability Mass Function
     */
    public final static Float[][] probMass(List<Float> values, List<Float> weights,
            int begIdx, int endIdx, int nIntervals) {

        if (nIntervals <= 0) {
            return null;
        }

        if (begIdx < 0) {
            begIdx = 0;
        }

        final float maxmin[] = maxmin(values, begIdx, endIdx);
        final float max = maxmin[MAX];
        final float min = maxmin[MIN];
        return probMass(values, weights, begIdx, endIdx, max, min, nIntervals);
    }

    /**
     * Probability Density Function
     */
    public final static Float[][] probMass(List<Float> values,
            int begIdx, int endIdx, double interval) {

        return probMass(values, null, begIdx, endIdx, interval);
    }

    /**
     * Probability Mass Function
     */
    public final static Float[][] probMass(List<Float> values, List<Float> weights,
            int begIdx, int endIdx, double interval) {

        if (interval <= 0) {
            return null;
        }

        if (begIdx < 0) {
            begIdx = 0;
        }

        final float maxmin[] = maxmin(values, begIdx, endIdx);
        final float max = maxmin[MAX];
        final float min = maxmin[MIN];
        final int nIntervals = (int) ((max - min) / interval) + 1;
        return probMass(values, weights, begIdx, endIdx, max, min, nIntervals);
    }

    /**
     * Probability Mass Function
     */
    private final static Float[][] probMass(List<Float> values, List<Float> weights,
            int begIdx, int endIdx, float max, float min, int nIntervals) {

        if (nIntervals <= 0) {
            return null;
        }

        if (begIdx < 0) {
            begIdx = 0;
        }

        final float interval = (max - min) / (float) (nIntervals - 1);
        final Float[][] mass = new Float[2][nIntervals];
        for (int i = 0; i < nIntervals; i++) {
            mass[VALUE][i] = min + i * interval;
            mass[MASS][i] = 0f;
        }

        final int lastIdx = Math.min(endIdx, values.size() - 1);
        float total = 0;
        for (int i = begIdx; i <= lastIdx; i++) {
            final float value = values.get(i);
            final float weight = weights == null ? 1 : weights.get(i).floatValue();
            if (value >= min && value <= max) {
                /** only calculate those between max and min */
                final int densityIdx = (int) ((value - min) / interval);
                mass[MASS][densityIdx] += weight;
            }

            total += weight;
        }

        for (int i = 0; i < nIntervals; i++) {
            mass[MASS][i] = mass[MASS][i] / total;
        }

        return mass;
    }

    /**
     * Probability Density Function
     */
    public final static Float[][] probMassWithTimeInfo(List<Float> values, List<Float> weights,
            int begIdx, int endIdx, float interval) {

        if (begIdx < 0 || interval <= 0) {
            return null;
        }

        final float maxmin[] = maxmin(values, begIdx, endIdx);
        final float max = maxmin[MAX];
        final float min = maxmin[MIN];
        final int nIntervals = (int) ((max - min) / interval) + 1;
        final int period = period(begIdx, endIdx);
        final Float[][] mass = new Float[2][nIntervals];
        for (int i = 0; i < nIntervals; i++) {
            mass[VALUE][i] = min + i * interval;
            mass[MASS][i] = 0f;
        }

        final int lastIdx = Math.min(endIdx, values.size() - 1);
        float total = 0;
        for (int i = begIdx; i <= lastIdx; i++) {
            final float value = values.get(i);
            final float weight = weights == null ? 1 : weights.get(i);
            if (value >= min && value <= max) {
                /** only calculate those between max and min */
                final int densityIdx = (int) ((value - min) / interval);
                mass[MASS][densityIdx] += weight;
            }

            total += weight;
        }

        for (int i = 0; i < nIntervals; i++) {
            mass[MASS][i] = mass[MASS][i] / total;
        }

        return mass;
    }

    private final static int period(int begIdx, int endIdx) {
        return endIdx - begIdx + 1;
    }

    private final static int lookback(int idx, int period) {
        return idx - period + 1;
    }
}

