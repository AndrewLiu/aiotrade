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
package org.aiotrade.platform.core;

import java.util.List;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.platform.core.PersistenceManager.I;
import org.aiotrade.platform.core.sec.Quote;
import org.aiotrade.platform.core.sec.QuotePool;
import org.aiotrade.platform.core.sec.TickerPool;
import org.aiotrade.lib.util.ServiceLoader;

/**
 *
 * @author Caoyuan Deng
 */
public class PersistenceManager {
    private static I i;

    public static I getDefault() {
        return i == null ? i = ServiceLoader.load(I.class).iterator().next() : i;
    }
    
    public static interface I extends org.aiotrade.lib.math.PersistenceManager.I {
        
        void saveQuotes(String symbol, Frequency freq, List<Quote> quotes, long sourceId);
        List<Quote> restoreQuotes(String symbol, Frequency freq);
        void deleteQuotes(String symbol, Frequency freq, long fromTime, long toTime);
        void dropAllQuoteTables(String symbol);
        
        void shutdown();
        
        QuotePool getQuotePool();
        TickerPool getTickerPool();
    }
    
}
