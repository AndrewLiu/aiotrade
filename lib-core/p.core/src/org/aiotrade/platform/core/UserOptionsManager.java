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

import java.net.InetSocketAddress;
import java.net.Proxy;
import org.aiotrade.platform.core.dataserver.QuoteContract;

/**
 *
 * @author Caoyuan Deng
 */
public class UserOptionsManager {
    
    private static boolean optionsLoaded = false;
    
    /** wont persistent */
    private static QuoteContract currentPreferredQuoteContract;
    
    /**
     * @NOTICE
     * proxy == null means using system proxies
     */
    private static Proxy proxy = null;
    
    public static boolean assertLoaded() {
        if (!optionsLoaded) {
            PersistenceManager.getDefault().restoreProperties();
            optionsLoaded = true;
        }
        return optionsLoaded;
    }
    
    /** setOptionsLoaded(false) will force reload options */
    public static void setOptionsLoaded(boolean b) {
        optionsLoaded = b;
    }
    
    public static Proxy getProxy() {
        assertLoaded();
        return proxy;
    }
    
    /**
     * use ProxySelector class to detect the proxy settings. If there is a
     * Direct connection to Internet the Proxy type will be DIRECT else it will
     * return the host and port.
     */
    public static void setProxy(Proxy _proxy) {
        proxy = _proxy;
        
        if (_proxy == null) {
            /** means use system proxies */
            System.setProperty("java.net.useSystemProxies", "true");
            System.clearProperty("http.proxyHost");
        } else {
            System.setProperty("java.net.useSystemProxies", "false");
            switch (_proxy.type()) {
                case DIRECT:
                    System.clearProperty("http.proxyHost");
                    break;
                case HTTP:
                    InetSocketAddress addr = (InetSocketAddress)proxy.address();
                    if (addr != null) {
                        System.setProperty("http.proxyHost", addr.getHostName());
                        System.setProperty("http.proxyPort", String.valueOf(addr.getPort()));
                    }
                    break;
                default:
                    System.setProperty("java.net.useSystemProxies", "true");
            }
        }
        
    }

    public static QuoteContract getCurrentPreferredQuoteContract() {
        return currentPreferredQuoteContract;
    }
    
    public static void setCurrentPreferredQuoteContract(QuoteContract quoteContract) {
        currentPreferredQuoteContract = quoteContract;
    }
    

}


