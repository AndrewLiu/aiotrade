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
package org.aiotrade.lib.indicator

import org.aiotrade.lib.math.timeseries.Null
import java.awt.Color
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.util.Sign
import org.aiotrade.lib.math.util.Signal

/**
 * Abstract Signal Indicator
 *
 * @author Caoyuan Deng
 */
abstract class SignalIndicator($baseSer: BaseTSer) extends Indicator($baseSer) {
    
  isOverlapping = true

  //val signalVar = new SparseTVar[Signal]("Signal", Plot.Signal)
  val signalVar = TVar[List[Signal]]("Signal", Plot.Signal)
    
  def this() = this(null)

  protected def signal(idx: Int, sign: Sign) {
    signal(idx, sign, null, null)
  }

  protected def signal(idx: Int, sign: Sign, color: Color) {
    signal(idx, sign, null, color)
  }

  protected def signal(idx: Int, sign: Sign, text: String) {
    signal(idx, sign, text, null)
  }

  protected def signal(idx: Int, sign: Sign, text: String, color: Color) {
    // remove posible duplicte signals
    removeSignals(idx, sign, text, color)
    
    val time = baseSer.timestamps(idx)
        
    // appoint a value for this sign as the drawing position
    val value = sign match {
      case Sign.EnterLong  => L(idx)
      case Sign.ExitLong   => H(idx)
      case Sign.EnterShort => H(idx)
      case Sign.ExitShort  => L(idx)
      case _ => Null.Float
    }

    val signal = Signal(idx, time, value, sign, text, color)
    signalVar(idx) = signalVar(idx) match {
      case null => List(signal)
      case xs => signal :: xs
    }
  }

  protected def removeSignals(idx: Int) {
    signalVar(idx) = null
  }
    
  protected def removeSignals(idx: Int, sign: Sign) {
    removeSignals(idx, sign, null, null)
  }

  protected def removeSignals(idx: Int, sign: Sign, text: String) {
    removeSignals(idx, sign, text, null)
  }

  protected def removeSignals(idx: Int, sign: Sign, color: Color) {
    removeSignals(idx, sign, null, color)
  }

  protected def removeSignals(idx: Int, sign: Sign, text: String, color: Color) {
    signalVar(idx) match {
      case null =>
      case xs => signalVar(idx) = xs filterNot (x=> x.sign == sign && x.text == text && x.color == color)
    }
  }

}

