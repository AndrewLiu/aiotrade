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

import java.awt.Color
import java.util.logging.Logger
import org.aiotrade.lib.math.signal.Sign
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.signal.SignalEvent
import org.aiotrade.lib.math.timeseries.BaseTSer

/**
 * Abstract Signal Indicator
 *
 * @author Caoyuan Deng
 */
abstract class SignalIndicator($baseSer: BaseTSer) extends Indicator($baseSer) with org.aiotrade.lib.math.indicator.SignalIndicator {
  private val log = Logger.getLogger(this.getClass.getName)
  
  isOverlapping = true

  //val signalVar = new SparseTVar[Signal]("Signal", Plot.Signal)
  val signalVar = TVar[List[Signal]]("Signal", Plot.Signal)
    
  def this() = this(null)

  protected def mark(idx: Int, sign: Sign): Signal = {
    mark(idx, sign, null, null)
  }

  protected def mark(idx: Int, sign: Sign, color: Color): Signal = {
    mark(idx, sign, null, color)
  }

  protected def mark(idx: Int, sign: Sign, text: String): Signal = {
    mark(idx, sign, text, null)
  }

  protected def mark(idx: Int, sign: Sign, text: String, color: Color): Signal = {
    internal_mark(idx, sign, text, color)._1
  }

  /**
   * @return (signal, is new one)
   */
  protected def internal_mark(idx: Int, sign: Sign, text: String, color: Color): (Signal, Boolean) = {
    val time = baseSer.timestamps(idx)
        
    val signal = Signal(time, sign, text, color)
    
    signalVar(idx) match {
      case null => 
        signalVar(idx) = List(signal)
        (signal, true)
      case xs =>
        val (existOnes, others) = xs partition (_ == signal)
        signalVar(idx) = signal :: others
        (signal, existOnes.isEmpty)
    }
  }

  protected def signal(idx: Int, sign: Sign): Signal = {
    signal(idx, sign, null, null)
  }

  protected def signal(idx: Int, sign: Sign, color: Color): Signal = {
    signal(idx, sign, null, color)
  }

  protected def signal(idx: Int, sign: Sign, text: String): Signal = {
    signal(idx, sign, text, null)
  }

  protected def signal(idx: Int, sign: Sign, text: String, color: Color): Signal = {
    val (signal, isNewOne) = internal_mark(idx, sign, text, color)
    if (isNewOne) {
      log.info("Signal: " + signal)
      Signal.publish(SignalEvent(this, signal))
    }
    signal
  }

  protected def removeMarks(idx: Int) {
    signalVar(idx) = null
  }
    
  protected def removeMarks(idx: Int, sign: Sign) {
    removeMarks(idx, sign, null, null)
  }

  protected def removeMarks(idx: Int, sign: Sign, text: String) {
    removeMarks(idx, sign, text, null)
  }

  protected def removeMarks(idx: Int, sign: Sign, color: Color) {
    removeMarks(idx, sign, null, color)
  }

  protected def removeMarks(idx: Int, sign: Sign, text: String, color: Color) {
    signalVar(idx) match {
      case null =>
      case xs => signalVar(idx) = xs filterNot (x => x.sign == sign && x.text == text && x.color == color)
    }
  }

}

