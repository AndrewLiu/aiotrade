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
package org.aiotrade.lib.math.vector;

import java.util.Random
import java.util.StringTokenizer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.util.collection.ArrayList

/**
 * Sparse implement of Vec. It do not store 0 valued elements.
 *
 * This class should keep elements index sorted.
 *
 * Create a <code>SparseVec</code> whose items are copied from
 * <code>source</code>.
 *
 * @param source   the array from which items are copied
 *
 * @author Caoyuan Deng
 */
class SparseVec(src: Array[VecItem]) extends Vec {
  import SparseVec._

  private var items: Array[VecItem] = src
  private var _dimension: Int = _
    
  /**
   * Create a zero items <code>SparseVec</code>.
   */
  def this() {
    this(new Array[VecItem](0))
  }
    
  /**
   * Create a <code>SparseVec</code> of the desired dimension initialized to zero.
   *
   * @param dimension   the dimension of the new <code>SparseVec</code>
   */
  def this(dimension: Int) {
    this(new Array[VecItem](0))
    this.dimension = dimension
  }
    
  /**
   * Create a <code>SparseVec</code> whose items are copied from
   * <code>src</code>.
   *
   * @param src   the <code>Vec</code> to be used as src
   */
  def this(src:Vec) {
    this(null.asInstanceOf[Array[VecItem]])
    copy(src)
  }
    
  def dimension: Int = _dimension
  def dimension_=(dimension: Int): Unit = {
    this._dimension = dimension
  }

  def setTo(src: Array[VecItem]): Unit = {
    this.items = src;
  }
    
  def add(value: Double): Unit = {
    assert(false, "SparseVec do not support this method, because we should make sure the elements is index sorted")
  }
    
  def toDoubleArray: Array[Double] = {
    val values = new Array[double](dimension)
        
    /** as all values has been initialed to 0 , we only need to: */
    for (item <- items) {
      values(item.index) = item.value
    }
        
    values
  }
    
  def checkDimensionEquality(comp: Vec): Unit = {
    if (comp.dimension != this.dimension) {
      throw new ArrayIndexOutOfBoundsException("Doing operations with SparseVec instances of different sizes.");
    }
  }

  override def clone: SparseVec = {
    new SparseVec(this)
  }
    
  def metric(other: Vec): Double = {
    this.minus(other).normTwo
  }
    
  def equals(another: Vec): Boolean = {
    if (dimension != another.dimension) {
      return false
    }

    another match {
      case x: SparseVec =>
        val itemsA = this.items
        val itemsB = x.items
        val lenA = itemsA.length
        val lenB = itemsB.length
        var idxA = 0
        var idxB = 0
        while (idxA < lenA && idxB < lenB) {
          val itemA = itemsA(idxA)
          val itemB = itemsB(idxB)
                
          if (itemA.index == itemB.index) {
            if (itemA.value != itemB.value) {
              return false
            }
            idxA += 1
            idxB += 1
          } else if (itemA.index > itemB.index) {
            idxB +=1
          } else {
            idxA +=1
          }
        }
            
      case _ =>
            
        for (i <- 0 until dimension) {
          if (apply(i) != another(i)) {
            return false
          }
        }
            
    }
        
    true
  }
    
  def getItemByPosition(position: Int): VecItem = {
    items(position)
  }
    
  def apply(dimensionIdx: Int): Double = {
    for (item <- items) {
      if (item.index == dimensionIdx) {
        return item.value
      }
    }
        
    return 0d
  }
    
  def update(dimensionIdx: Int, value: Double): Unit = {
    val item = getItem(dimensionIdx)
        
    if (item != null) {
            
      item.value = value
            
    } else {
            
      val newItems = new Array[VecItem](items.length + 1)
            
      var added = false
      for (i <- 0 until newItems.length) {
        if (items(i).index < dimensionIdx) {
          newItems(i) = items(i)
        } else {
          if (!added) {
            newItems(i) = new VecItem(dimensionIdx, value)
            added = true
          } else {
            newItems(i) = items(i - 1)
          }
        }
      }
            
      this.items = newItems
    }
  }
    
  def getItem(dimensionIdx: Int): VecItem = {
        
    for (i <- 0 until items.length) {
      if (items(i).index == dimensionIdx) {
        return items(i)
      }
    }
        
    return null
  }
    
  def setAll(value: Double): Unit = {
    if (value == 0) {
            
      items = null
            
    } else {
            
      items = new Array[VecItem](dimension)
      for (i <- 0 until dimension) {
        items(i).index = i
        items(i).value = value
      }
            
    }
  }
    
  def copy(src:Vec): Unit = {
    checkDimensionEquality(src)

    items = src match {
      case x:SparseVec =>


        val srcItems = x.items
        val newItems = new Array[VecItem](srcItems.length)
        System.arraycopy(srcItems, 0, newItems, 0, srcItems.length)

        newItems
      case _ =>

        val itemBuf = new ArrayList[VecItem]
        for (i <- 0 until src.dimension) {
          val value = src.apply(i)
          if (value != 0) {
            itemBuf += new VecItem(i, value)
          }
        }

        itemBuf.toArray
    }
  }
    
  def copy(src: Vec, srcPos: Int, destPos: Int, length: Int): Unit = {
    /** todo */
    //System.arraycopy(src.toDoubleArray(), srcPos, items, destPos, length);
  }
    
    
  def setValues(values: Array[Double]): Unit ={
    if (dimension != values.length) {
      throw new ArrayIndexOutOfBoundsException("Doing operations with source of different sizes.");
    }
        
    val newItems = new Array[VecItem](dimension)
    for (i <- 0 until dimension) {
      val value = values(i)
      if (value != 0) {
        newItems(i).index = i
        newItems(i).value = value
      }
    }
        
    items = newItems
  }
    

    
  def plus(operand: Vec): Vec = {
    checkDimensionEquality(operand)
        
    val result = new SparseVec(dimension)
        
    for (i <- 0 until dimension) {
      val value = apply(i) + operand(i)
      if (value != 0) {
        result(i) = value
      }
    }
        
    result
  }
    
  def minus(operand: Vec): Vec = {
    checkDimensionEquality(operand)
        
    val result = new SparseVec(dimension)
        
    for (i <- 0 until operand.dimension) {
      val value = apply(i) - operand(i)
      if (value != 0) {
        result(i) = value
      }
    }
        
    result
  }
    
  def innerProduct(operand:Vec): Double = {
    checkDimensionEquality(operand)
        
    var result = 0d

    operand match {
      case x: SparseVec =>
        /** A quick algorithm in case of both are SparseVec */
        val itemsA = this.items
        val itemsB = x.items
        val lenA = itemsA.length
        val lenB = itemsB.length
        var idxA = 0
        var idxB = 0
        while (idxA < lenA && idxB < lenB) {
          val itemA = itemsA(idxA)
          val itemB = itemsB(idxB)
                
          if (itemA.index == itemB.index) {
            result += itemA.value * itemB.value;
            idxA += 1
            idxB +=1
          } else if (itemA.index > itemB.index) {
            idxB += 1
          } else {
            idxA += 1
          }
        }
            
      case _ =>
            
        /** for inner product, we only need compute with those value != 0 */
        for (i <- 0 until items.length) {
          val item = items(i)
          result += item.value * operand(item.index)
        }
            
    }
        
    result
  }
    
  def square: Double = {
    var result = 0d
        
    for (i <- 0 until items.length) {
      val value = items(i).value
      result += value * value
    }
        
    result
  }
    
  def plus(operand: Double): Vec = {
    val result = new SparseVec(this)
        
    for (i <- 0 until items.length) {
      result.items(i).value = items(i).value + operand
    }
        
    result
  }
    
    
  def times(operand: Double): Vec = {
    val result = new SparseVec(this)
        
    for (i <- 0 until items.length) {
      result.items(i).value = items(i).value * operand
    }
        
    result
  }
    
  def compactSize: Int = {
    items.length
  }
    
  def compactData :Array[VecItem] = {
    items
  }
    
  def normOne: Double = {
    var result = 0d
        
    /** for norm1 operation, we only need compute with those data.value != 0 */
    for (i <- 0 until items.length) {
      result += Math.abs(items(i).value)
    }
        
    return result;
  }
    
  def normTwo: Double = {
    var result = 0d
        
    /** for norm2 operation, we only need compute with those data.value != 0 */
    for (i <- 0 until items.length) {
      result += Math.pow(items(i).value, 2)
    }
    result = Math.sqrt(result)
        
    result
  }
    
  def checkValidation: Boolean = {
    for (i <- 0 until items.length) {
      if (items(i).value == Null.Float) {
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


  override def toString: String = {
    val result = new StringBuffer()
        
    result.append("[")
    for (i <- 0 until dimension) {
      result.append(apply(i)).append(ITEM_SEPARATOR)
    }
    result.append("]")
        
    result.toString
  }
    
    
}

object SparseVec {
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
