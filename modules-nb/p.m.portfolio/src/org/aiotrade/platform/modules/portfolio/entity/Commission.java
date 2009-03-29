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
package org.aiotrade.platform.modules.portfolio.entity;

/**
 *
 * @author Caoyuan Deng
 */
public class Commission {
    
    private double fixed;
    private double variable;
    private double minimum;
    private double maximum = Float.MAX_VALUE;
    
    public Commission(double fixed, double variable, double minimum, double maximum) {
        this.fixed = fixed;
        this.variable = variable;
        this.minimum = minimum;
        if (maximum > 0) {
            this.maximum = maximum;
        }
    }
    
    public double getFixed() {
        return fixed;
    }
    
    public void setFixed(double fixed) {
        this.fixed = fixed;
    }
    
    public double getVariable() {
        return variable;
    }
    
    public void setVariable(double variable) {
        this.variable = variable;
    }
    
    public double getMaximum() {
        return maximum;
    }
    
    public void setMaximum(double maximum) {
        this.maximum = maximum;
    }
    
    public double getMinimum() {
        return minimum;
    }
    
    public void setMinimum(double minimum) {
        this.minimum = minimum;
    }
    
    public double computeExpenses(double amount) {
        double expenses = getFixed() + (amount * getVariable() / 100.0);
        
        if (expenses < getMinimum()) {
            expenses = getMinimum();
        }
        
        if (expenses > getMaximum()) {
            expenses = getMaximum();
        }
        
        return expenses;
    }
    
}
