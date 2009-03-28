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

/**
 * A vector of real numbers.
 *
 * @author Caoyuan Deng
 */
public interface Vec {
    
    /**
     * Element-wise addition.
     *
     * @param vec   the <code>Vec</code> to plus
     * @return the result of the operation
     */
    public Vec plus(Vec operand);
    
    /**
     * Scalar addition.
     *
     * @param operand   the amount to plus
     * @return the result of the sum
     */
    public Vec plus(double operand);
    
    public Vec minus(Vec operand);
    
    /**
     * Appends an element to the <code>Vec</code>.
     * @Notice
     * that is an inefficient operation and should be rarely used.
     *
     *
     * @param value   the value of the element to add
     */
    public void add(double value);
    
    /**
     * The elements of this <code>Vec</code> as a <code>double[]</code>.
     *
     * @return the <code>double[]</code>
     */
    public double[] toDoubleArray();
    
    /**
     * Compute an Euclidean metric (or distance) from this <code>Vec</code> to
     * another.
     *
     * @param other   the <code>Vec</code> to measure the metric (or distance) with
     * @return the metric
     */
    public double metric(Vec other);
    
    /**
     * Compute the inner product of two <code>Vec</code>s.
     *
     * @param operand   the other <code>Vec</code>
     * @return the inner product
     */
    public double innerProduct(Vec operand);
    
    /**
     * <Xi dot Xi> the inner product of this vec itself
     */
    public double square();
    
    /**
     * Scalar multipication.
     *
     * @param operand   the amount to times
     * @return the resulting <code>Vec</code>
     */
    public Vec times(double operand);
    
    /**
     * Compute a 1-norm (sum of absolute values) of the <code>Vec</code>.
     * norm (or length)
     *
     * @return the norm
     */
    public double normOne();
    
    /**
     * Compute a 2-norm (square root of the sum of the squared values) of the
     * <code>Vec</code>.
     *
     *
     * @return the norm
     */
    public double normTwo();
    
    /**
     * Returns the <i>idx </i>-nary element of the <code>Vec</code>.
     *
     * @param dimensionIdx   the index of the desired element
     * @return the value of the element
     */
    public double get(int dimensionIdx);
    
    /**
     * Sets element of index <code>i</code> to <code>value</code>.
     *
     * @param dimensionIdx   index of the element to set
     * @param value    the value to set
     */
    public void set(int dimensionIdx, double value);
    
    /**
     * Sets all <code>Vec</code> elements to <code>value</code>.
     *
     * @param value   the value to set
     */
    public void setAll(double value);
    
    /**
     * Sets elements to the ones of <code>orig</code>.
     *
     * @param orig   the <code>Vec</code> with the elements to set
     */
    public void copy(Vec orig);
    
    public void copy(Vec src, int srcPos, int destPos, int length);
    
    public void setValues(double[] values);
    
    /**
     * @return the dimension of this <code>Vec</code>
     */
    public int dimension();
    
    
    /**
     * Randomizes this <code>Vec</code> with values bounded by
     * <code>min</code> and <code>max</code>.
     *
     * @param min   lower bound
     * @param max   upper bound
     */
    public void randomize(double min, double max);
    
    /**
     * Checks if a <code>Vec</code> has equal dimension of this <code>Vec</code>.
     *
     * @param comp   <code>Vec</code> to test with
     */
    public void checkDimensionEquality(Vec comp);
    
    public boolean checkValidation();
    
    public Vec clone();
}
