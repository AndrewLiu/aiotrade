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

import org.aiotrade.lib.math.timeseries.plottable.Plot


/**
 * This is a horizotal view of DefaultSeries. Is' a reference of one of
 * the field vars.
 *
 * @author Caoyuan Deng
 */
abstract class AbstractVar[@specialized V: Manifest](var name: String, var plot: Plot) extends Var[V] {

  val nullValue = getNullValue[V]

  val LAYER_NOT_SET = -1

  var layer = LAYER_NOT_SET
    
  def this() = {
    this("", Plot.None)
  }
    
  def this(name: String) = {
    this(name, Plot.None)
  }

  def addNullValue(time: Long): Boolean = {
    add(time, nullValue)
  }

  def toDoubleArray: Array[Double] = {
    val length = size
    val result = new Array[double](length)
        
    if (length > 0 && apply(0).isInstanceOf[Number]) {
      var i = 0
      while (i < length) {
        result(i) = apply(i).asInstanceOf[Number].doubleValue
        i += 1
      }
    }
        
    result
  }
    
  override def toString = name

  private def getNullValue[T](implicit m: Manifest[T]): T = {
    val value = m.toString match {
      case "Byte"    => Byte   MinValue   // -128 ~ 127
      case "Short"   => Short  MinValue   // -32768 ~ 32767
      case "Char"    => Char   MinValue   // 0(\u0000) ~ 65535(\uffff)
      case "Int"     => Int    MinValue   // -2,147,483,648 ~ 2,147,483,647
      case "Long"    => Long   MinValue   // -9,223,372,036,854,775,808 ~ 9,223,372,036,854,775,807
      case "Float"   => Float  NaN
      case "Double"  => Double NaN
      case "Boolean" => false
      case _ => null
    }
    value.asInstanceOf[T]
  }

}