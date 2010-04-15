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

import org.aiotrade.lib.util.actors.ChainActor
import scala.swing.Publisher
import scala.swing.event.Event

/**
 * Time Series
 *
 *
 * @author Caoyuan Deng
 */
case class AddAll[V <: TVal](values: Array[V])
trait TSer extends Publisher with ChainActor {

//  ----- actor's implementation
//  val serActor = actor {
//    loop {
//      receive { // this actor will possess timestampslog's lock, which should be attached to same thread, so use receive here
//        case AddAll(values) => this ++ values
//      }
//    }
//  }
//  ----- end of actor's implementation

  def init(freq: TFreq)
    
  def timestamps: TStamps
  def attach(timestamps: TStamps)

  def freq: TFreq

  def vars: Seq[TVar[_]]

  def exists(time: Long): Boolean
    
  def lastOccurredTime: Long
    
  def size: Int
   
  def indexOfOccurredTime(time: Long): Int
    
  /** public clear(long fromTime) instead of clear(int fromIndex) to avoid bad usage */
  def clear(fromTime: Long)

  def ++=[T <: TVal](values: Array[T]): TSer
    
  def createOrClear(time: Long)
    
  def shortDescription: String
  def shortDescription_=(description: String)
    
  def inLoading: Boolean
  def inLoading_=(b: Boolean)
  def loaded: Boolean
  def loaded_=(b: Boolean)

  def validate

  // --- for charting
  
  def isOverlapping: Boolean
  def isOverlapping_=(b: Boolean)

  /**
   * horizonal _grids of this indicator used to draw grid
   */
  def grids: Array[Float]
}

object TSerEvent {
  case class RefreshInLoading(
    source: TSer,
    symbol: String,
    fromTime: Long,
    toTime: Long,
    lastObject: AnyRef = null,
    callback: Callback = () => {}) extends TSerEvent(source, symbol, fromTime, toTime, lastObject, callback)
  case class FinishedLoading(
    source: TSer,
    symbol: String,
    fromTime: Long,
    toTime: Long,
    lastObject: AnyRef = null,
    callback: Callback = () => {}) extends TSerEvent(source, symbol, fromTime, toTime, lastObject, callback)
  case class Updated(
    source: TSer,
    symbol: String,
    fromTime: Long,
    toTime: Long,
    lastObject: AnyRef = null,
    callback: Callback = () => {}) extends TSerEvent(source, symbol, fromTime, toTime, lastObject, callback)
  case class FinishedComputing(
    source: TSer,
    symbol: String,
    fromTime: Long,
    toTime: Long,
    lastObject: AnyRef = null,
    callback: Callback = () => {}) extends TSerEvent(source, symbol, fromTime, toTime, lastObject, callback)
  case class Clear(
    source: TSer,
    symbol: String,
    fromTime: Long,
    toTime: Long,
    lastObject: AnyRef = null,
    callback: Callback = () => {}) extends TSerEvent(source, symbol, fromTime, toTime, lastObject, callback)
  case class ToBeSet(
    source: TSer,
    symbol: String,
    fromTime: Long,
    toTime: Long,
    lastObject: AnyRef = null,
    callback: Callback = () => {}) extends TSerEvent(source, symbol, fromTime, toTime, lastObject, callback)
  case object None extends TSerEvent(null, null, 0, 0, null, () => {})

  type Callback = () => Unit

  def unapply(e: TSerEvent): Option[(TSer, String, Long, Long, AnyRef, Callback)] = {
    Some((e.source, e.symbol, e.fromTime, e.toTime, e.lastObject, e.callback))
  }
}

import TSerEvent._
abstract class TSerEvent(private val source: TSer,
                         private val symbol: String,
                         private val fromTime: Long,
                         private val toTime: Long,
                         private val lastObject: AnyRef, // object the event carries (it can be any thing other than a SerItem)
                         private val callback: Callback
) extends Event


