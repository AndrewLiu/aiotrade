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
package org.aiotrade.lib.math.signal

import org.aiotrade.lib.math.indicator.SignalIndicator
import org.aiotrade.lib.util.actors.Event
import org.aiotrade.lib.util.actors.Publisher
import java.awt.Color

/**
 *
 * @author Caoyuan Deng
 */
case class SignalEvent(source: SignalIndicator, signal: Signal) extends Event

case class SubSignalEvent(uniSymbol: String, name: String, freq: String, signal: Signal) extends Event

object Signal extends Publisher {  
  def importFrom(vs: List[Any]): Signal = {
    vs match {
      case List(time: Long, id: Byte, text: String) =>
        Signal(time, Sign.withId(id), text)
      case List(time: Long, id: Byte) =>
        Signal(time, Sign.withId(id))
      case List(time: Double, id: Double) =>
        Signal(time.toLong, Sign.withId(id.toByte))
      case _ => null
    }
  }
}

case class Signal(time: Long, sign: Sign, text: String = null, color: Color = null) {
  def isTextSignal = text != null

  def export: List[Any] = {
    if (text != null) {
      List[Any](time, sign.id, text)
    } else {
      List[Any](time, sign.id)
    }
  }

  override def hashCode: Int = {
    var h = 17
    if (text  != null) h = 37 * h + text.hashCode
    if (color != null) h = 37 * h + color.hashCode
    h = 37 * h + sign.hashCode
    h = 37 * h + (time ^ (time >>> 32)).toInt
    h
  }
  
  override def equals(o: Any): Boolean = {
    o match {
      case x: Signal => time == x.time && sign == x.sign && text == x.text && color == x.color
      case _ => false
    }
  }
}
