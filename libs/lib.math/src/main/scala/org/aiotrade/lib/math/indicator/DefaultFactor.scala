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

/**
 * Class for defining indicator's DefaultFactor
 *
 *
 * @author Caoyuan Deng
 * @Notice
 * If you use Factor in indicator, please considerate AbstractIndicator#InnerFactor first
 * which will be added to Indicator's factors automatically when new it.
 */
class DefaultFactor(private var _name: String,
                    private var _value: Double,
                    private var _step: Double,
                    private var _minValue: Double,
                    private var _maxValue: Double
) extends Factor {

  def this(name: String, value: Double) = {
    this(name, value, 1.0, Double.MinValue, Double.MaxValue)
  }
    
  def this(name: String, value: Double, step: Double) = {
    this(name, value, step, Double.MinValue, Double.MaxValue)
  }

  def name = _name
  def name_=(name: String) = {
    this._name = name
  }
  
  def value = _value
  def value_=(value: Double) = {
    this._value = value
  }

  def step = _step
  def step_=(step: Double) = {
    this._step = step
  }
    
  def maxValue = _maxValue
  def maxValue_=(maxValue: Double) {
    this._maxValue = maxValue
  }
    
  def minValue = _minValue
  def minValue_=(minValue: Double) = {
    this._minValue = minValue
  }
}


