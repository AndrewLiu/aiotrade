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
package org.aiotrade.lib.math.timeseries;



/**
 * QuoteItem class
 * 
 * @author Caoyuan Deng
 */
public class QuoteItem extends DefaultItem {
    protected QuoteItem(QuoteSer ser, long time) {
        super(ser, time);
    }
    
    public QuoteSer getSer() {
        return (QuoteSer)super.getSer();
    }
    
    public final float getVolume() {
        return super.getFloat(getSer().getVolume());
    }
    
    public final float getOpen() {
        return super.getFloat(getSer().getOpen());
    }
    
    public final float getHigh() {
        return super.getFloat(getSer().getHigh());
    }
    
    public final float getLow() {
        return super.getFloat(getSer().getLow());
    }
    
    public final float getClose() {
        return super.getFloat(getSer().getClose());
    }
    
    public final float getClose_Adj() {
        return super.getFloat(getSer().getClose_Adj());
    }
    
    public final float getClose_Ori() {
        return super.getFloat(getSer().getClose_Ori());
    }
    
    public final void setOpen(float open) {
        super.setFloat(getSer().getOpen(), open);
    }
    
    public final void setHigh(float high) {
        super.setFloat(getSer().getHigh(), high);
    }
    
    public final void setLow(float low) {
        super.setFloat(getSer().getLow(), low);
    }
    
    public final void setClose(float close) {
        super.setFloat(getSer().getClose(), close);
    }
    
    public final void setVolume(float volume) {
        super.setFloat(getSer().getVolume(), volume);
    }
    
    public final void setClose_Ori(float close_ori) {
        super.setFloat(getSer().getClose_Ori(), close_ori);
    }
    
    public final void setClose_Adj(float close_adj) {
        super.setFloat(getSer().getClose_Adj(), close_adj);
    }
}