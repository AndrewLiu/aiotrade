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
package org.aiotrade.lib.math.vector;

import java.util.Random;
import java.util.StringTokenizer;

/**
 * Default implement of Vec.
 *
 * @author Caoyuan Deng
 */
public class DefaultVec implements Vec {
    
    public static final String ITEM_SEPARATOR = " ";
    
    private double[] values;
    
    /**
     * Create a zero values <code>DefaultVec</code>.
     */
    public DefaultVec() {
        values = new double[0];
    }
    
    /**
     * Creates a <code>DefaultVec</code> whose values are copied from
     * <code>source</code>.
     *
     *
     * @param source   the array from which values are copied
     */
    public DefaultVec(double[] source) {
        this.values = source;
    }
    
    /**
     * Create a <code>DefaultVec</code> of the desired dimension and initialized to zero.
     *
     * @param dimension   the dimension of the new <code>DefaultVec</code>
     */
    public DefaultVec(int dimension) {
        this.values = new double[dimension];
    }
    
    /**
     * Create a <code>DefaultVec</code> whose values are copied from
     * <code>source</code>.
     *
     * @param source   the <code>DefaultVec</code> to be used as source
     */
    public DefaultVec(Vec source) {
        this(source.toDoubleArray());
    }
    
    public void add(double value) {
        int size = (values == null) ? 0 : values.length;
        
        double[] newValues = new double[size + 1];
        
        if (size > 0) {
            System.arraycopy(values, 0, newValues, 0, size);
        }
        newValues[newValues.length - 1] = value;
        
        values = newValues;
    }
    
    public double[] toDoubleArray() {
        return values;
    }
    
    public void checkDimensionEquality(Vec comp) {
        if (comp.dimension() != this.dimension()) {
            throw new ArrayIndexOutOfBoundsException(
                    "Doing operations with DefaultVec instances of different sizes.");
        }
    }
    
    public DefaultVec clone() {
        return new DefaultVec(this);
    }
    
    public double metric(Vec other) {
        return this.minus(other).normTwo();
    }
    
    public boolean equals(Vec other) {
        if (dimension() != other.dimension()) {
            return false;
        }
        
        for (int i = 0, n = dimension(); i < n; i++) {
            if (get(i) != other.get(i)) {
                return false;
            }
        }
        
        return true;
    }
    
    public double get(int dimensionIdx) {
        return values[dimensionIdx];
    }
    
    public void set(int dimensionIdx, double value) {
        values[dimensionIdx] = value;
    }
    
    public void setAll(double value) {
        for (int i = 0, n = dimension(); i < n; i++) {
            values[i] = value;
        }
    }
    
    public void copy(Vec src) {
        checkDimensionEquality(src);
        System.arraycopy(src.toDoubleArray(), 0, values, 0, values.length);
    }
    
    public void copy(Vec src, int srcPos, int destPos, int length) {
        System.arraycopy(src.toDoubleArray(), srcPos, values, destPos, length);
    }
    
    
    public void setValues(double[] values) {
        this.values = values;
    }
    
    public int dimension() {
        return values.length;
    }
    
    public Vec plus(Vec operand) {
        checkDimensionEquality(operand);
        
        Vec result = new DefaultVec(dimension());
        
        for (int i = 0, n = dimension(); i < n; i++) {
            result.set(i, get(i) + operand.get(i));
        }
        
        return result;
    }
    
    public Vec plus(double operand) {
        Vec result = new DefaultVec(dimension());
        
        for (int i = 0, n = dimension(); i < n; i++) {
            result.set(i, get(i) + operand);
        }
        
        return result;
    }
    
    public Vec minus(Vec operand) {
        checkDimensionEquality(operand);
        
        Vec result = new DefaultVec(dimension());
        
        for (int i = 0, n = dimension(); i < n; i++) {
            result.set(i, get(i) - operand.get(i));
        }
        
        return result;
    }
    
    public double innerProduct(Vec operand) {
        checkDimensionEquality(operand);
        
        double result = 0;
        
        for (int i = 0, n = dimension(); i < n; i++) {
            result += get(i) * operand.get(i);
        }
        
        return result;
    }
    
    public double square() {
        double result = 0;
        
        for (int i = 0, n = dimension(); i < n; i++) {
            double value = get(i);
            result += value * value;
        }
        
        return result;
    }
    
    
    public Vec times(double operand) {
        Vec result = new DefaultVec(dimension());
        
        for (int i = 0, n = dimension(); i < n; i++) {
            result.set(i, get(i) * operand);
        }
        
        return result;
    }
    
    public double normOne() {
        double result = 0.0;
        
        for (int i = 0, n = dimension(); i < n; i++) {
            result += Math.abs(get(i));
        }
        
        return result;
    }
    
    public double normTwo() {
        double result = 0.0;
        
        for (int i = 0, n = dimension(); i < n; i++) {
            result += Math.pow(get(i), 2.0);
        }
        result = Math.sqrt(result);
        
        return result;
    }
    
    public boolean checkValidation() {
        boolean b = true;
        
        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i])) {
                b = false;
                break;
            }
        }
        
        return b;
    }
    
    public String toString() {
        String result = "[";
        
        for (int i = 0, n = dimension(); i < n; i++) {
            result = result + String.valueOf(get(i)) + ITEM_SEPARATOR;
        }
        
        return result.trim() + "]";
    }
    
    /**
     * Parses a String into a <code>DefaultVec</code>.
     * Elements are separated by <code>DefaultVec.ITEM_SEPARATOR</code>
     *
     * @param str   the String to parse
     * @return the resulting <code>DefaultVec</code>
     * @see DefaultVec#ITEM_SEPARATOR
     */
    public static Vec parseVec(String str) {
        StringTokenizer st = new StringTokenizer(str, ITEM_SEPARATOR);
        
        int dimension = st.countTokens();
        
        Vec result = new DefaultVec(dimension);
        
        for (int i = 0; i < dimension; i++) {
            result.set(i, Double.parseDouble(st.nextToken()));
        }
        
        return result;
    }
    
    public void randomize(double min, double max) {
        Random source = new Random(System.currentTimeMillis() + Runtime.getRuntime().freeMemory());
        
        for (int i = 0, n = dimension(); i < n; i++) {
            /**
             * @NOTICE
             * source.nextDouble() returns a pseudorandom value between 0.0 and 1.0
             */
            set(i, source.nextDouble() * (max - min) + min);
        }
    }
    
    
}