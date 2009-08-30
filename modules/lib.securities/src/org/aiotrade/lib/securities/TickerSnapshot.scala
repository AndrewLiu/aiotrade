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
package org.aiotrade.lib.securities

import org.aiotrade.lib.util.ObservableHelper

/**
 * We use composite pattern here, wrap a ticker instead of inheriting it. So we
 * can inherit ObservableHelper, and apply org.aiotrade.util.observer to it's observers.
 *
 * @author Caoyuan Deng
 */
class TickerSnapshot extends ObservableHelper {

  val ticker = new Ticker
  var symbol: String = _
  var fullName: String = _

  def time: Long = {
    ticker.time
  }

  def time_=(time: Long): Unit = {
    ticker.time = time
  }

  def apply(field: Int): Float = {
    ticker(field)
  }

  def update(field: Int, value: Float): Unit = {
    if (ticker(field) != value) {
      setChanged
    }
    ticker(field) = value
  }

  def setAskPrice(idx: Int, value: Float): Unit = {
    if (ticker.askPrice(idx) != value) {
      setChanged
    }
    ticker.setAskPrice(idx, value)
  }

  def setAskSize(idx: Int, value: Float): Unit = {
    if (ticker.askSize(idx) != value) {
      setChanged
    }
    ticker.setAskSize(idx, value)
  }

  def setBidPrice(idx: Int, value: Float): Unit = {
    if (ticker.bidPrice(idx) != value) {
      setChanged
    }
    ticker.setBidPrice(idx, value)
  }

  def setBidSize(idx: Int, value: Float): Unit = {
    if (ticker.bidSize(idx) != value) {
      setChanged
    }
    ticker.setBidSize(idx, value)
  }

  def copy(another: Ticker): Unit = {
    if (ticker.isValueChanged(another)) {
      ticker.copy(another)
      setChanged
    }
  }
}

