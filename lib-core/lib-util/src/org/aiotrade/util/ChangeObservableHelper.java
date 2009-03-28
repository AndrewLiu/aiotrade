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
package org.aiotrade.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Do not implement observerMap list using WeakReference, otherwise, we can not
 * add anonymous observer by:
 * addObserver(new ChangeObserver<Subject> {
 *      public void update(Subject) {
 *      }
 * });
 * 
 * 
 * @author  Caoyuan Deng
 * @version 1.0, November 24, 2006, 5:09 PM
 * @since 1.0.4
 */
public class ChangeObservableHelper {
    private transient Map<ChangeObserver, Object> observerMapOwner = new HashMap<ChangeObserver, Object>();
    
    public ChangeObservableHelper() {
    }
    
    public Collection<ChangeObserver> getObservers() {
        return observerMapOwner.keySet();
    }
    
    public synchronized void addObserver(Object owner, ChangeObserver observer) {
        observerMapOwner.put(observer, owner);
    }
    
    public synchronized void removeObserver(ChangeObserver observer) {
        if (observer == null) {
            return;
        }
        
        if (observerMapOwner.keySet().contains(observer)) {
            observerMapOwner.remove(observer);
        }
    }
    
    public void removeObserversOf(Object owner) {
        List<ChangeObserver> toBeRemoved = new ArrayList<ChangeObserver>();
        for (ChangeObserver observer : observerMapOwner.keySet()) {
            if (observerMapOwner.get(observer) == owner) {
                toBeRemoved.add(observer);
            }
        }
        
        synchronized (this) {
            observerMapOwner.keySet().removeAll(toBeRemoved);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends ChangeObserver> void notifyObserversChanged(ChangeObservable subject, Class<T> observerType) {
        for (ChangeObserver observer : observerMapOwner.keySet()) {
            if (observerType.isInstance(observer)) {
                observer.update(subject);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends ChangeObserver> List<T> getObservers(Class<T> observerType) {
        List<T> result = new ArrayList<T>();
        int i = 0;
        for (ChangeObserver observer : observerMapOwner.keySet()) {
            if (observerType.isInstance(observer)) {
                result.add((T)observer);
            }
        }
        return result;
    }
    
    /**
     * Returns the total number of obervers.
     */
    public int getObserverCount() {
        return observerMapOwner.size();
    }
    
    private int getObserverCount(Class<?> observerType) {
        int count = 0;
        for (ChangeObserver observer : observerMapOwner.keySet()) {
            if (observerType.isInstance(observer)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * @param observer the observer to be added
     */
    protected synchronized <T extends ChangeObserver> void add(Object owner, T observer) {
        assert observer != null : "Do not add a null observer!";
        observerMapOwner.put(observer, owner);
    }
    
    /**
     * @param observer the observer to be removed
     */
    protected synchronized <T extends ChangeObserver> void remove(T observer) {
        if (observer == null) {
            return;
        }
        
        if (observerMapOwner.keySet().contains(observer)) {
            observerMapOwner.remove(observer);
        }
    }
    
    public String toString() {
        StringBuilder s = new StringBuilder("ChangeObserverList: ");
        s.append(observerMapOwner.size()).append(" observers: ");
        for (ChangeObserver observer : observerMapOwner.keySet()) {
            s.append(" type ").append(observer.getClass().getName());
            s.append(" observer ").append(observer);
        }
        
        return s.toString();
    }
}
