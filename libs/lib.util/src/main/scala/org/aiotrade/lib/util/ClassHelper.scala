/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.util

/**
 * 
 * @author Caoyuan Deng
 */
object ClassHelper {
  
  // --- classes
  
  val JByteClass = classOf[java.lang.Byte]
  val JShortClass = classOf[java.lang.Short]
  val JIntegerClass = classOf[java.lang.Integer]
  val JLongClass = classOf[java.lang.Long]
  val JFloatClass = classOf[java.lang.Float]
  val JDoubleClass = classOf[java.lang.Double]
  val JBooleanClass = classOf[java.lang.Boolean]
  
  val ByteClass = classOf[Byte]
  val ShortClass = classOf[Short]
  val IntClass = classOf[Int]
  val LongClass = classOf[Long]
  val FloatClass = classOf[Float]
  val DoubleClass = classOf[Double]
  val BooleanClass = classOf[Boolean]
  
  val StringClass = classOf[String]


  val SeqClass = classOf[collection.Seq[_]]

  val JCollectionClass = classOf[java.util.Collection[_]]
  val JListClass = classOf[java.util.List[_]]
  
  val BigDecimalClass = classOf[java.math.BigDecimal]
  val BigIntegerClass = classOf[java.math.BigInteger]
  val DateClass = classOf[java.util.Date]
  val SqlDateClass = classOf[java.sql.Date]
  val SqlTimeClass = classOf[java.sql.Time]
  val SqlTimestampClass = classOf[java.sql.Timestamp]

  
  // --- helpers
  
  private val TupleNameRegex = """scala\.Tuple(\d\d?)""".r
  def isTuple(v: AnyRef): Boolean = {
    v.getClass.getName match {
      case TupleNameRegex(count) => val i = count.toInt; i >= 1 && i < 23
      case _ => false
    }
  }
  
  def isInstance(t: Class[_], v: Any): Boolean = {
    t match {
      case ByteClass => JByteClass.isInstance(v) || ByteClass.isInstance(v)
      case ShortClass => JShortClass.isInstance(v) || ShortClass.isInstance(v)
      case IntClass => JIntegerClass.isInstance(v) || IntClass.isInstance(v)
      case LongClass => JLongClass.isInstance(v) || LongClass.isInstance(v)
      case FloatClass => JFloatClass.isInstance(v) || FloatClass.isInstance(v)
      case DoubleClass => JDoubleClass.isInstance(v) || DoubleClass.isInstance(v)
      case BooleanClass => JBooleanClass.isInstance(v) || BooleanClass.isInstance(v)
      case _ => t.isInstance(v)
    }
  }
}
