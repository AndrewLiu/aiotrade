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

import javax.swing.event.ChangeEvent;
import org.aiotrade.util.CallBack;


/**
 *
 * @author Caoyuan Deng
 */
public class SerChangeEvent extends ChangeEvent {
    private String symbol;
    
    private long begTime;
    private long endTime;
    
    private Type type;
    
    /** it could be anything */
    private Object lastObject;
    
    private CallBack callBack;
    
    public enum Type {
        RefreshInLoading,
        FinishedLoading,
        Updated,
        FinishedComputing,
        Clear
    };
    
    public SerChangeEvent(Ser source, Type type, String symbol, long begTime, long endTime) {
        this(source, type, symbol, begTime, endTime, null);
    }
    
    
    public SerChangeEvent(Ser source, Type type, String symbol, long begTime, long endTime, Object lastObject) {
        this(source, type, symbol, begTime, endTime, lastObject, null);
    }

    public SerChangeEvent(Ser source, Type type, String symbol, long begTime, long endTime, CallBack callBack) {
        this(source, type, symbol, begTime, endTime, null, callBack);
    }

    public SerChangeEvent(Ser source, Type type, String symbol, long begTime, long endTime, Object lastObject, CallBack callBack) {
        super(source);
        this.type = type;
        this.symbol = symbol;
        this.begTime = begTime;
        this.endTime = endTime;
        this.lastObject = lastObject;
        this.callBack = callBack;
    }
    
    
    public Ser getSource() {
        assert (source instanceof Ser) : "Source should be Series";
        
        return (Ser)source;
    }
    
    public void setSource(Ser series) {
        source = series;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }
    
    public long getBeginTime() {
        return begTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * @return object the event carries (It can be any thing other than a SerItem) 
     */
    public Object getLastObject() {
        return lastObject;
    }
    
    public CallBack getCallBack() {
        return callBack;
    }
    
    public void callBack() {
        if (callBack != null) {
            callBack.callBack();
        }
    }
    
}

