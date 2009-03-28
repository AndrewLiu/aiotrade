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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Caoyuan Deng
 */
public class ObservableHelper implements Observable {

    private boolean changed;
    private Collection<WeakReference<Observer>> observerRefs;

    public ObservableHelper() {
        this.observerRefs = new ArrayList<WeakReference<Observer>>();
    }

    public synchronized void addObserver(Observer observer) {
        assert observer != null : "Can't add a null observer!";
        boolean contained = false;
        for (WeakReference<Observer> ref : observerRefs) {
            if (ref.get() == observer) {
                contained = true;
                break;
            }
        }

        if (!contained) {
            observerRefs.add(new WeakReference<Observer>(observer));
        }
    }

    public synchronized void deleteObserver(Observer observer) {
        WeakReference<Observer> theRef = null;
        for (WeakReference<Observer> ref : observerRefs) {
            if (ref.get() == observer) {
                theRef = ref;
                break;
            }

        }

        observerRefs.remove(theRef);
    }

    /**
     * for use of sub-class 
     */
    public void notifyObservers() {
        notifyObservers(this);
    }

    /**
     * for use of wrap class 
     */
    @SuppressWarnings("unchecked")
    public synchronized void notifyObservers(Observable source) {
        if (changed) {
            /** must clone the observers in case deleteObserver is called */
            WeakReference[] clone = observerRefs.toArray(new WeakReference[observerRefs.size()]);
            clearChanged();
            for (WeakReference<Observer> ref : clone) {
                ref.get().update(source);
            }
        }
    }

    public synchronized void deleteObservers() {
        observerRefs.clear();
    }

    protected synchronized void setChanged() {
        changed = true;
    }

    protected synchronized void clearChanged() {
        changed = false;
    }

    public synchronized boolean hasChanged() {
        return changed;
    }

    public synchronized int countObservers() {
        return observerRefs.size();
    }

    // helper:
    public synchronized void printObservers() {
        System.out.println("Observer of " + this + " :");
        for (WeakReference<Observer> observerRef : observerRefs) {
            System.out.println(observerRef.get());
        }
    }
}
