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
import org.aiotrade.lib.math.signal.Kind
import org.aiotrade.lib.math.signal.Direction
import org.aiotrade.lib.math.signal.Position
import org.aiotrade.lib.math.signal.Mark
import org.aiotrade.lib.math.signal.Sign
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.signal.SignalEvent
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.TSerEvent

/**
 * Abstract Signal Indicator
 *
 * @author Caoyuan Deng
 */
abstract class SignalIndicator($baseSer: BaseTSer) extends Indicator($baseSer) with org.aiotrade.lib.math.indicator.SignalIndicator {
  private val log = Logger.getLogger(this.getClass.getName)
  
  isOverlapping = true

  //val signalVar = new SparseTVar[Signal](SignalIndicator.SIGNAL_VAR_KEY, Plot.Signal)
  val signalVar = TVar[List[Signal]](SignalIndicator.SIGNAL_VAR_KEY, Plot.Signal)
    
  def this() = this(null)
  
  /**
   * @return (signal, is new one)
   */
  private def signal[T <: Signal](idx: Int, kind: Kind, id: Int, text: String, color: Color): (T, Boolean) = {
    val time = baseSer.timestamps(idx)

    val signal = kind match {
      case x: Direction => Sign(time, x, id, text, color)
      case x: Position  => Mark(time, x, id, text, color)
    }

    signalVar(idx) match {
      case null =>
        signalVar(idx) = List(signal)

        (signal.asInstanceOf[T], true)
      case xs =>
        val (existOnes, others) = xs partition (_ == signal)
        signalVar(idx) = signal :: others

        (signal.asInstanceOf[T], existOnes.isEmpty)
    }
  }

  protected def mark(idx: Int, position: Position): Mark = {
    mark(idx, position, 0, null, null)
  }

  protected def mark(idx: Int, position: Position, id: Int): Mark = {
    mark(idx, position, id, null, null)
  }

  protected def mark(idx: Int, position: Position, color: Color): Mark = {
    mark(idx, position, 0, null, color)
  }

  protected def mark(idx: Int, position: Position, id: Int, color: Color): Mark = {
    mark(idx, position, id, null, color)
  }

  protected def mark(idx: Int, position: Position, text: String): Mark = {
    mark(idx, position, 0, text, null)
  }

  protected def mark(idx: Int, position: Position, id: Int, text: String): Mark = {
    mark(idx, position, id, text, null)
  }

  protected def mark(idx: Int, position: Position, text: String, color: Color): Mark = {
    signal[Mark](idx, position, 0, text, color)._1
  }

  protected def mark(idx: Int, position: Position, id: Int = 0, text: String = null, color: Color = null): Mark = {
    signal[Mark](idx, position, id, text, color)._1
  }

  protected def sign(idx: Int, direction: Direction): Sign = {
    sign(idx, direction)
  }

  protected def sign(idx: Int, direction: Direction, id: Int): Sign = {
    sign(idx, direction, id)
  }

  protected def sign(idx: Int, direction: Direction, color: Color): Sign = {
    sign(idx, direction, 0, null, color)
  }

  protected def sign(idx: Int, direction: Direction, id: Int, color: Color): Sign = {
    sign(idx, direction, id, null, color)
  }

  protected def sign(idx: Int, direction: Direction, text: String): Sign = {
    sign(idx, direction, 0, text, null)
  }

  protected def sign(idx: Int, direction: Direction, id: Int, text: String): Sign = {
    sign(idx, direction, id, text, null)
  }

  protected def sign(idx: Int, direction: Direction, text: String, color: Color): Sign = {
    sign(idx, direction, 0, text, color)
  }

  protected def sign(idx: Int, direction: Direction, id: Int = 0, text: String = null, color: Color = null): Sign = {
    val (sign, isNewOne) = signal[Sign](idx, direction, id, text, color)
    if (isNewOne) {
      log.info("Signal sign: " + sign)
      Signal.publish(SignalEvent(this, sign))
    }
    sign
  }

  protected def remove(idx: Int) {
    signalVar(idx) = null
  }
    
  protected def remove(idx: Int, kind: Kind) {
    remove(idx, kind, 0, null, null)
  }

  protected def remove(idx: Int, id: Int) {
    remove(idx, null, 0, null, null)
  }

  protected def remove(idx: Int, text: String) {
    remove(idx, null, 0, text, null)
  }

  protected def remove(idx: Int, color: Color) {
    remove(idx, null, 0, null, color)
  }

  protected def remove(idx: Int, kind: Kind, id: Int) {
    remove(idx, kind, id, null, null)
  }

  protected def remove(idx: Int, kind: Kind, text: String) {
    remove(idx, kind, 0, text, null)
  }

  protected def remove(idx: Int, kind: Kind, color: Color) {
    remove(idx, kind, 0, null, color)
  }

  protected def remove(idx: Int, kind: Kind, id: Int, text: String) {
    remove(idx, kind, id, text, null)
  }

  protected def remove(idx: Int, kind: Kind, id: Int, color: Color) {
    remove(idx, kind, id, null, color)
  }

  protected def remove(idx: Int, kind: Kind, text: String, color: Color) {
    remove(idx, kind, 0, text, color)
  }

  protected def remove(idx: Int, kind: Kind = null, id: Int = 0, text: String = null, color: Color = null) {
    signalVar(idx) match {
      case null =>
      case xs => signalVar(idx) = xs filterNot {x =>
          x.id == id &&
          (if (kind  == null) true else x.kind  == kind) &&
          (if (text  == null) true else x.text  == text) &&
          (if (color == null) true else x.color == color)
        }
    }
  }

  protected def addSignal(signal: Signal) {
    val time = signal.time
    if (exists(time)) {
      signalVar(time) = signalVar(time) match {
        case null => List(signal)
        case xs =>
          val (existOnes, others) = xs partition (_ == signal)
          signal :: others
      }
    }

    publish(TSerEvent.Computed(this, null, time, time, null, null))
  }

  protected def addSignals(signals: Array[Signal]) {
    var frTime = Long.MaxValue
    var toTime = Long.MinValue
    var i = 0
    while (i < signals.length) {
      val signal = signals(i)
      val time = signal.time
      if (exists(time)) {
        signalVar(time) = signalVar(time) match {
          case null => List(signal)
          case xs =>
            val (existOnes, others) = xs partition (_ == signal)
            signal :: others
        }
      }

      frTime = math.min(frTime, time)
      toTime = math.max(toTime, time)

      i += 1
    }

    if (signals.length > 0) {
      publish(TSerEvent.Computed(this, null, frTime, toTime, null, null))
    }
  }

}

object SignalIndicator {
  val SIGNAL_VAR_KEY = "*"
}

