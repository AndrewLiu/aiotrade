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
case class SignalsEvent(source: SignalIndicator, signals: Array[Signal]) extends Event

case class SubSignalEvent(uniSymbol: String, name: String, freq: String, signal: Signal) extends Event
case class SubSignalsEvent(uniSymbol: String, name: String, freq: String, signals: Array[Signal]) extends Event

object Signal extends Publisher {  
  def importFrom(v: (Long, List[Any])): Signal = {
    v match {
      case (time: Long, List(kindId: Byte, text: String, id: Int)) =>
        if (Kind.isSign(kindId)) {
          Sign(time, Direction.withId(kindId), id, text)
        } else {
          Mark(time, Position.withId(kindId), id, text)
        }
      case (time: Long, List(kindId: Byte, id: Int)) =>
        if (Kind.isSign(kindId)) {
          Sign(time, Direction.withId(kindId), id)
        } else {
          Mark(time, Position.withId(kindId), id)
        }
      case (time: Long, List(kindId: Double, id: Double)) =>
        if (Kind.isSign(kindId.toByte)) {
          Sign(time, Direction.withId(kindId.toByte), id.toInt)
        } else {
          Mark(time, Position.withId(kindId.toByte), id.toInt)
        }
      case _ => null
    }
  }
}

case class Sign(time: Long, kind: Direction, id: Int = 0, text: String = null, color: Color = null) extends Signal
case class Mark(time: Long, kind: Position,  id: Int = 0, text: String = null, color: Color = null) extends Signal

abstract class Signal {
  def time: Long
  def kind: Kind
  def id: Int
  def text: String
  def color: Color

  def isTextSignal = text != null

  def export: (Long, List[Any]) = {
    if (text != null) {
      (time, List[Any](kind.id, text, id))
    } else {
      (time, List[Any](kind.id, id))
    }
  }

  override def hashCode: Int = {
    var h = 17
    h = 37 * h + (time ^ (time >>> 32)).toInt
    h = 37 * h + kind.hashCode
    h = 37 * h + id
    if (text  != null) h = 37 * h + text.hashCode
    if (color != null) h = 37 * h + color.hashCode
    h
  }
  
  override def equals(o: Any): Boolean = {
    o match {
      case x: Signal => x.time == time && x.kind == kind && x.id == id && x.text == text && x.color == color
      case _ => false
    }
  }
}
