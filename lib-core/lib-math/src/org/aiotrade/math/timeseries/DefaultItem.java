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
package org.aiotrade.math.timeseries;


/**
 * DefaultItem
 * -----------------------------------------------------------------------
 * ********* I don't think we can simplify it more ************
 *
 * A vertical view of Ser at a time point.
 *
 * It's more like a virtual SerItem that has all of the values contained in
 * DefaultSer's varSet except an private int idx and boolean clear. The idx
 * should be properly managed by the container.
 *
 * This is a class that implements correctly most SerItem's abstract methods,
 * but you sould keep your eyes on getFloat(var) to consider if it works.
 * and should also override DefaultSer#createClearItemOrClearIt(long) to
 * return the subclass's instance
 * 
 * @author Caoyuan Deng
 */
public class DefaultItem implements SerItem {
    /** ser of timeseries's item is immutable */
    private final Ser ser;
    /** time of timeseries's item is immutable */
    private final long time;
    
    private boolean clear = true;
    
    /**
     * Declare this protected default construct to prevent public DefaultItem()
     * call, and as it's protected, the subclass of DefaultSer can override
     * method: protected SerItem createItem(long time).
     *
     *
     * @see DefaultSer#createItem(long time)
     */
    protected DefaultItem(Ser ser, long time) {
        this.ser = ser;
        this.time = time;
    }
    
    public Ser getSer() {
        return ser;
    }
    
    public final int getIndex() {
        return ser.timestamps().indexOfOccurredTime(time);
    }
    
    public final boolean isClear() {
        return clear;
    }
    
    public final void clear() {
        this.clear = true;
    }
    
    public final long getTime() {
        return time;
    }
    
    public final <T> T get(Var<T> var) {
        return var.getByTime(time);
    }
    
    public final <T> void set(Var<T> var, final T value) {
        var.setByTime(time, value);
        this.clear = false;
    }
    
    
    /**
     * !NOTICE:
     * This is the best try implement. If Object is Number, this may works,
     * Otherwise, should sub class it.
     */
    public float getFloat(Var<?> var) {
        final Object o = var.getByTime(time);
        if (o != null) {
            if (o instanceof Number) {
                return ((Number) o).floatValue();
            }  else {
                assert false : "Why you get here(DefaultItem.getFloat(Var<?> var)) ? " +
                        time + " Check your code and give me Float instead of float: " +
                        o.getClass().getCanonicalName();
                return Float.NaN;
            }
        }
        
        return Float.NaN;
    }
    
    public <T extends Number> void setFloat(Var<T> var, final T number) {
        var.setByTime(time, number);
        this.clear = false;
    }
    
}