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

import org.aiotrade.lib.util.collection.ArrayList
import scala.actors.Actor._

/**
 * Time Series
 *
 *
 * @author Caoyuan Deng
 */
case class AddAll[@specialized V <: TVal](values: Array[V])
trait TSer {

  // ----- actor's implementation
  //    val serActor = actor {
  //        loop {
  //            receive { // this actor will possess timestampslog's lock, which should be attached to same thread, so use receive here
  //                case AddAll(values) => this ++ values
  //            }
  //        }
  //    }
  // ----- end of actor's implementation

  def init(freq: TFreq): Unit
    
  def timestamps: TStamps
  def attach(timestamps: TStamps)

  def freq: TFreq

  def vars: Seq[TVar[_]]
  def items: ArrayList[TItem]

  def exists(time: Long): Boolean
  def itemOf(time: Long): TItem
    
  def lastOccurredTime: Long
    
  def size: Int
    
  def indexOfOccurredTime(time: Long): Int
    
  /** public clear(long fromTime) instead of clear(int fromIndex) to avoid bad usage */
  def clear(fromTime: Long): Unit

  def ++=[T <: TVal](values: Array[T]): TSer
    
  def createItemOrClearIt(time: Long): TItem
    
  def shortDescription: String
  def shortDescription_=(description: String): Unit
    
  def addSerChangeListener(listener: SerChangeListener): Unit
  def removeSerChangeListener(listener: SerChangeListener): Unit
  def fireSerChangeEvent(evt: SerChangeEvent): Unit
    
  def inLoading: Boolean
  def inLoading_=(b: Boolean): Unit
  def loaded: Boolean
  def loaded_=(b: Boolean): Unit

  def validate: Unit
}

import java.util.EventListener
trait SerChangeListener extends EventListener {
  def serChanged(evt: SerChangeEvent): Unit
}


import javax.swing.event.ChangeEvent
object SerChangeEvent {
  abstract class Type
  object Type {
    case object RefreshInLoading extends Type
    case object FinishedLoading  extends Type
    case object Updated extends Type
    case object FinishedComputing extends Type
    case object Clear extends Type
    case object None extends Type
  }
  type CallBack = () => Unit
}

import org.aiotrade.lib.math.timeseries.SerChangeEvent._
class SerChangeEvent(var _source: TSer,
                     var tpe: Type,
                     val symbol: String,
                     val beginTime: Long,
                     val endTime: Long,
                     val lastObject: AnyRef, // object the event carries (It can be any thing other than a SerItem)
                     var callBack: CallBack) extends ChangeEvent(_source) {


  def this(source: TSer, tpe: Type, symbol: String, beginTime: Long, endTime: Long) = {
    this(source, tpe, symbol, beginTime, endTime, null, null)
  }

  def this(source: TSer, tpe: Type, symbol: String, beginTime: Long, endTime: Long, lastObject: AnyRef) = {
    this(source, tpe, symbol, beginTime, endTime, lastObject, null)
  }

  def this(source: TSer, tpe: Type, symbol: String, beginTime: Long, endTime: Long, callBack: CallBack) = {
    this(source, tpe, symbol, beginTime, endTime, null, callBack)
  }

  override def getSource: TSer = {
    assert(source.isInstanceOf[TSer], "Source should be TSer")

    source.asInstanceOf[TSer]
  }

  def doCallBack: Unit = {
    if (callBack != null) {
      callBack()
    }
  }

}


