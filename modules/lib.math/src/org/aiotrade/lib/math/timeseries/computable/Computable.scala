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
package org.aiotrade.lib.math.timeseries.computable

import scala.collection.mutable.ArrayBuffer
import java.text.DecimalFormat
import org.aiotrade.lib.math.timeseries.Ser
import scala.actors.Actor._

/**
 *
 * @author Caoyuan Deng
 */
case class ComputeFrom(time: Long)
trait Computable {

  // ----- actor's implementation
  val computableActor = actor {
    loop {
      react {
        case ComputeFrom(time) => computeFrom(time)
      }
    }
  }
  // ----- end of actor's implementation

  /**
   * @param time to be computed from
   */
  def computeFrom(time: Long): Unit
  def computedTime: Long
    
  def factors: ArrayBuffer[Factor]
  def factors_=(factors: ArrayBuffer[Factor]): Unit
  def factors_=(values: Array[Number]): Unit
    
  def dispose: Unit
}

object Computable {
  private val FAC_DECIMAL_FORMAT = new DecimalFormat("0.###")

  def displayName(ser: Ser): String = ser match {
    case x: Computable => displayName(ser.shortDescription, x.factors)
    case _ => ser.shortDescription
  }

  def displayName(name: String, factors: ArrayBuffer[Factor]): String = {
    val buffer = new StringBuffer(name)

    val size = factors.size
    for (i <- 0 until size) {
      if (i == 0) {
        buffer.append(" (")
      }
      buffer.append(FAC_DECIMAL_FORMAT.format(factors(i).value))
      if (i < size - 1) {
        buffer.append(", ")
      } else {
        buffer.append(")")
      }
    }

    buffer.toString
  }

}


