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

import org.aiotrade.lib.math.indicator.Plottable
import org.aiotrade.lib.collection.ArrayList


/**
 * A horizontal view of Ser. Is' a reference of one of the field vars.
 * V is the type of value
 *
 * @author Caoyuan Deng
 */
trait TVar[V] extends Plottable {
  def name: String
  def name_=(name: String)

  def timestamps: TStamps

  def put(time: Long, value: V): Boolean
  def apply(time: Long): V
  def update(time: Long, value: V)
    
  def apply(idx: Int): V
  def update(idx: Int, value: V)

  def float(time: Long): Float
  def float(idx: Int): Float
  
  def clear(fromIdx: Int)
    
  def size: Int

  def toArray(fromTime: Long, toTime: Long): (Array[V])
  def toArrayWithTime(fromTime: Long, toTime: Long): (Array[Long], Array[V])
  def toDoubleArray: Array[Double]
    
  def values: ArrayList[V]

  val NullVal: V

  private val _hashCode = System.identityHashCode(this)
  override def hashCode: Int = _hashCode
}
