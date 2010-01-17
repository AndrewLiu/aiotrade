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
package org.aiotrade.lib.math.vector

import java.util.Random
import java.util.StringTokenizer
import org.aiotrade.lib.math.timeseries.Null

/**
 * Default implement of Vec.
 *
 * @author Caoyuan Deng
 */
class DefaultVec(source: Array[Double]) extends Vec {
  import DefaultVec._
    
  private var values: Array[Double] = source
    
  /**
   * Create a zero values <code>DefaultVec</code>.
   */
  def this() {
    this(new Array[Double](0))
  }
    
  /**
   * Create a <code>DefaultVec</code> of the desired dimension and initialized to zero.
   *
   * @param dimension   the dimension of the new <code>DefaultVec</code>
   */
  def this(dimension: Int) {
    this(new Array[Double](dimension))
  }
    
  /**
   * Create a <code>DefaultVec</code> whose values are copied from
   * <code>source</code>.
   *
   * @param source   the <code>DefaultVec</code> to be used as source
   */
  def this(source: Vec) {
    this(source.toDoubleArray)
  }
    
  def add(value: Double): Unit = {
    val size = if (values == null) 0 else values.length
        
    val newValues = new Array[Double](size + 1)
        
    if (size > 0) {
      System.arraycopy(values, 0, newValues, 0, size)
    }
    newValues(newValues.length - 1) = value
        
    values = newValues
  }
    
  def toDoubleArray: Array[Double] = {
    values
  }
    
  def checkDimensionEquality(comp: Vec): Unit = {
    if (comp.dimension != this.dimension) {
      throw new ArrayIndexOutOfBoundsException(
        "Doing operations with DefaultVec instances of different sizes.");
    }
  }

  override def clone: DefaultVec = {
    new DefaultVec(this)
  }
    
  def metric(other: Vec): Double = {
    this.minus(other).normTwo
  }
    
  def equals(other: Vec): Boolean = {
    if (dimension != other.dimension) {
      return false
    }

    var i = 0
    while (i < dimension) {
      if (apply(i) != other(i)) {
        return false
      }
      i += 1
    }
        
    true
  }
    
  def apply(dimensionIdx: Int): Double = {
    values(dimensionIdx)
  }
    
  def update(dimensionIdx: Int, value: Double): Unit = {
    values(dimensionIdx) = value
  }
    
  def setAll(value: Double): Unit = {
    for (i <- 0 until values.size) values(i) = value
  }
    
  def copy(src: Vec): Unit = {
    checkDimensionEquality(src)
    System.arraycopy(src.toDoubleArray, 0, values, 0, values.length)
  }
    
  def copy(src: Vec, srcPos: Int, destPos: Int, length: Int): Unit = {
    System.arraycopy(src.toDoubleArray, srcPos, values, destPos, length)
  }
    
    
  def setValues(values: Array[Double]): Unit = {
    this.values = values
  }
    
  def dimension: Int = {
    values.length
  }
    
  def plus(operand: Vec): Vec = {
    checkDimensionEquality(operand)

    val result = new DefaultVec(dimension)
        
    for (i <- 0 until dimension) {
      result(i) =  apply(i) + operand(i)
    }
        
    result
  }
    
  def plus(operand: Double): Vec = {
    val result = new DefaultVec(dimension)
        
    for (i <- 0 until dimension) {
      result(i) = apply(i) + operand
    }
        
    result
  }
    
  def minus(operand: Vec): Vec = {
    checkDimensionEquality(operand)
        
    val result = new DefaultVec(dimension)
        
    for (i <- 0 until dimension) {
      result(i) = apply(i) - operand(i)
    }
        
    result
  }
    
  def innerProduct(operand: Vec): Double = {
    checkDimensionEquality(operand)
        
    var result = 0d
        
    for (i <- 0 until dimension) {
      result += apply(i) * operand(i)
    }
        
    result
  }
    
  def square: Double = {
    var result = 0d
        
    for (i <- 0 until dimension) {
      val value = apply(i)
      result += value * value
    }
        
    result
  }
    
    
  def times(operand: Double): Vec = {
    val result = new DefaultVec(dimension)
        
    for (i <- 0 until dimension) {
      result(i) = apply(i) * operand
    }
        
    result
  }
    
  def normOne: Double = {
    var result = 0d
        
    for (i <- 0 until dimension) {
      result += math.abs(apply(i))
    }
        
    result
  }
    
  def normTwo: Double = {
    var result = 0d
        
    for (i <- 0 until dimension) {
      result += math.pow(apply(i), 2.0)
    }
    result = math.sqrt(result)
        
    result
  }
    
  def checkValidation: Boolean = {
    for (i <- 0 until dimension) {
      if (Null.is(values(i))) {
        return false
      }
    }
        
    true
  }

  def randomize(min: Double, max: Double): Unit = {
    val source = new Random(System.currentTimeMillis + Runtime.getRuntime.freeMemory)

    for (i <- 0 until dimension) {
      /**
       * @NOTICE
       * source.nextDouble() returns a pseudorandom value between 0.0 and 1.0
       */
      update(i, source.nextDouble * (max - min) + min)
    }
  }

  override def toString :String = {
    val result = new StringBuilder

    result.append("[")
    for (i <- 0 until dimension) {
      result.append(apply(i)).append(ITEM_SEPARATOR)
    }
    result.append("]")

    result.toString
  }
}

object DefaultVec {
  val ITEM_SEPARATOR = " "
    
  /**
   * Parses a String into a <code>DefaultVec</code>.
   * Elements are separated by <code>DefaultVec.ITEM_SEPARATOR</code>
   *
   * @param str   the String to parse
   * @return the resulting <code>DefaultVec</code>
   * @see DefaultVec#ITEM_SEPARATOR
   */
  def parseVec(str: String): Vec = {
    val st = new StringTokenizer(str, ITEM_SEPARATOR)

    val dimension = st.countTokens

    val result = new DefaultVec(dimension)

    for (i <- 0 until dimension) {
      result(i) = st.nextToken.toDouble
    }

    result
  }

}
