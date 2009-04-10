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


import java.util.List
import scala.collection.Set

/**
 * Time Series
 *
 *
 * @author Caoyuan Deng
 */
trait Ser {
    
    def init(freq:Frequency) :Unit
    
    def timestamps :Timestamps
    
    def varSet :Set[Var[Any]]
    
    def getItem(time:Long) :SerItem
    
    def freq :Frequency
    
    def lastOccurredTime :Long
    
    def itemList :List[SerItem]
    
    def size :Int
    
    def indexOfOccurredTime(time:Long) :Int
    
    /** public clear(long fromTime) instead of clear(int fromIndex) to avoid bad usage */
    def clear(fromTime:Long) :Unit
    
    def createItemOrClearIt(time:Long): SerItem
    
    def shortDescription_=(description:String) :Unit
    
    def shortDescription :String
    
    def addSerChangeListener(listener:SerChangeListener) :Unit
    
    def removeSerChangeListener(listener:SerChangeListener) :Unit
    
    def fireSerChangeEvent(evt:SerChangeEvent) :Unit
    
    def loaded :Boolean
    
    def loaded_=(b:Boolean)
}


