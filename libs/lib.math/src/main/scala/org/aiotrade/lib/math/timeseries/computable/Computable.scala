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

import java.text.DecimalFormat
import org.aiotrade.lib.math.timeseries.TSer

/**
 *
 * @author Caoyuan Deng
 */
object Computable {
  private val FAC_DECIMAL_FORMAT = new DecimalFormat("0.###")

  def displayName(ser: TSer): String = ser match {
    case x: Computable => displayName(ser.shortDescription, x.factors)
    case _ => ser.shortDescription
  }

  def displayName(name: String, factors: Array[Factor]): String = {
    factors map {x => FAC_DECIMAL_FORMAT.format(x.value)} mkString("(", ",", ")")
  }
}

case class ComputeFrom(time: Long)
trait Computable extends TSer {

  val Plot = org.aiotrade.lib.math.timeseries.plottable.Plot

  actorActions += {
    case ComputeFrom(time) => computeFrom(time)
  }

  def init(baseSer: TSer)

  def baseSer: TSer
  def baseSer_=(baseSer: TSer)

  /**
   * @param time to be computed from
   */
  def computeFrom(time: Long)
  def computedTime: Long

  def factors: Array[Factor]
  def factors_=(factors: Array[Factor])
  def factorValues_=(values: Array[Number])

  def dispose

  def createNewInstance(baseSer: TSer): Computable
}
