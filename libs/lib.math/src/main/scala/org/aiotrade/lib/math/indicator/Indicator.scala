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
package org.aiotrade.lib.math.indicator

import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.util.actors.Event

/**
 *
 * @author Caoyuan Deng
 */
object Indicator {
  private val FAC_DECIMAL_FORMAT = new DecimalFormat("0.###")

  private val idToIndicator = new ConcurrentHashMap[Id[_], Indicator]

  def indicatorOf[T <: Indicator](clazz: Class[T], baseSer: BaseTSer, args: Any*): T = {
    val id = Id(clazz, baseSer, args: _*)
    idToIndicator.get(id) match {
      case null =>
        /** if got none from idToIndicator, try to create new one */
        try {
          val indicator = clazz.newInstance
          /** don't forget to call set(baseSer, args) immediatley */
          indicator.set(baseSer) // @todo, setFactors
          idToIndicator.putIfAbsent(id, indicator)
          indicator
        } catch {
          case ex: IllegalAccessException => ex.printStackTrace; null.asInstanceOf[T]
          case ex: InstantiationException => ex.printStackTrace; null.asInstanceOf[T]
        }
      case x => x.asInstanceOf[T]
    }
  }

  def displayName(ser: TSer): String = ser match {
    case x: Indicator => displayName(ser.shortDescription, x.factors)
    case _ => ser.shortDescription
  }

  def displayName(name: String, factors: Array[Factor]): String = {
    if (factors.length == 0) name
    else factors map {x => FAC_DECIMAL_FORMAT.format(x.value)} mkString(name + "(", ",", ")")
  }
}

case class ComputeFrom(time: Long) extends Event

trait Indicator extends TSer {

  val Plot = org.aiotrade.lib.math.indicator.Plot

  reactions += {
    case ComputeFrom(time) => computeFrom(time)
  }

  def set(baseSer: BaseTSer)
  def createNewInstance(baseSer: BaseTSer): Indicator

  def baseSer: BaseTSer
  def baseSer_=(baseSer: BaseTSer)

  /**
   * @param time to be computed from
   */
  def computeFrom(time: Long)
  def computedTime: Long

  def factors: Array[Factor]
  def factors_=(factors: Array[Factor])
  def factorValues_=(values: Array[Double])

  def dispose

}
