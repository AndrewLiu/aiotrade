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
package org.aiotrade.lib.math.timeseries

import java.util.concurrent.locks.{Lock,ReentrantReadWriteLock}
import javax.swing.event.EventListenerList

/**
 *
 * @author Caoyuan Deng
 */
abstract class AbstractSer(var freq:Frequency) extends Ser {
    private val readWriteLock = new ReentrantReadWriteLock
    val readLock  :Lock = readWriteLock.readLock
    val writeLock :Lock = readWriteLock.writeLock
    
    private val serChangeListenerList = new EventListenerList
        
    var inLoading :Boolean = false
    private var _loaded :Boolean = false

    def this() = {
        this(Frequency.DAILY)
    }
    
    def init(freq:Frequency) :Unit = {
        this.freq = freq.clone
    }
        
    def addSerChangeListener(listener:SerChangeListener) :Unit = {
        serChangeListenerList.add(classOf[SerChangeListener], listener)
    }
    
    def removeSerChangeListener(listener:SerChangeListener) :Unit = {
        serChangeListenerList.remove(classOf[SerChangeListener], listener)
    }
    
    def fireSerChangeEvent(evt:SerChangeEvent) :Unit = {
        val listeners = serChangeListenerList.getListenerList
        /** Each listener occupies two elements - the first is the listener class */
        var i = 0
        while (i < listeners.length) {
            if (listeners(i) == classOf[SerChangeListener]) {
                listeners(i + 1).asInstanceOf[SerChangeListener].serChanged(evt)
            }
            i += 2
        }
    }
    
    def loaded :Boolean = _loaded
    def loaded_=(b:Boolean) :Unit = {
        inLoading = false
        _loaded = false
    }

    override
    def toString :String = {
        this.getClass.getSimpleName + "(" + freq + ")"
    }
}

