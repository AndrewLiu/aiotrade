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

import org.aiotrade.lib.util.serialization.JavaDocument

/**
 * Class for defining indicator's DefaultFactor
 *
 *
 * @author Caoyuan Deng
 * @Notice
 * If you use Factor in indicator, please considerate AbstractIndicator#InnerFactor first
 * which will be added to Indicator's factors automatically when new it.
 */
class DefaultFactor(namex:String, valuex:Number, stepx:Number, minValuex:Number, maxValuex:Number) extends AbstractFactor(namex) {
    
  private var _value :Float = valuex.floatValue
  private var _step :float = if (stepx == null) 1.0f else stepx.floatValue
  private var _minValue :Float = if (minValuex == null) -Float.MaxValue else minValuex.floatValue
  private var _maxValue :Float = if (maxValuex == null)  Float.MaxValue else maxValuex.floatValue
    
    
  def this(name:String, value:Number) = {
    this(name, value, null, null, null)
  }
    
  def this(name:String, value:Number, step:Number) = {
    this(name, value, step, null, null)
  }
    
  def value :Float = _value
  def value_=(value:Number) = {
    this._value = value.floatValue
  }

  def step :Float = _step
  def step_=(step:Number) = {
    this._step = step.floatValue
  }
    
  def maxValue :Float = _maxValue
  def maxValue_=(maxValue:Number) :Unit = {
    this._maxValue = maxValue.floatValue
  }
    
  def minValue :Float = _minValue
  def minValue_=(minValue:Number) = {
    this._minValue = minValue.floatValue
  }
    
  def writeToJava(id:String) :String = {
    JavaDocument.set(id, "setName", name) +
    JavaDocument.set(id, "setValue", value) +
    JavaDocument.set(id, "setStep", step) +
    JavaDocument.set(id, "setMinValue", minValue) +
    JavaDocument.set(id, "setMaxValue", maxValue)
  }
    
}


