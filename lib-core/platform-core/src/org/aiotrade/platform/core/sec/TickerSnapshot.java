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
package org.aiotrade.platform.core.sec;

import org.aiotrade.lib.util.ObservableHelper;

/**
 * We use composite pattern here, wrap a ticker instead of inheriting it. So we
 * can inherit ObservableHelper, and apply org.aiotrade.util.observer to it's observers.
 *
 * @author Caoyuan Deng
 */
public final class TickerSnapshot extends ObservableHelper {

    private final Ticker ticker;
    private String symbol;
    private String fullName;

    public TickerSnapshot() {
        super();
        ticker = new Ticker();
    }

    public final String getSymbol() {
        return symbol;
    }

    public final void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public final String getFullName() {
        return fullName;
    }

    public final void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public final Ticker readTicker() {
        return ticker;
    }

    public final void setTime(long time) {
        ticker.setTime(time);
    }

    public final void setAskPrice(int idx, float value) {
        if (ticker.getAskPrice(idx) != value) {
            setChanged();
        }
        ticker.setAskPrice(idx, value);
    }

    public final void setAskSize(int idx, float value) {
        if (ticker.getAskSize(idx) != value) {
            setChanged();
        }
        ticker.setAskSize(idx, value);
    }

    public final void setBidPrice(int idx, float value) {
        if (ticker.getBidPrice(idx) != value) {
            setChanged();
        }
        ticker.setBidPrice(idx, value);
    }

    public final void setBidSize(int idx, float value) {
        if (ticker.getBidSize(idx) != value) {
            setChanged();
        }
        ticker.setBidSize(idx, value);
    }

    public final void set(int field, float value) {
        if (ticker.get(field) != value) {
            setChanged();
        }
        ticker.set(field, value);
    }

    public final float get(int field) {
        return ticker.get(field);
    }

    public final void copy(Ticker another) {
        if (ticker.isValueChanged(another)) {
            ticker.copy(another);
            setChanged();
        }
    }
}

