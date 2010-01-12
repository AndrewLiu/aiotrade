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


import java.util.{Calendar}
import org.aiotrade.lib.util.serialization.BeansDocument
import org.w3c.dom.Element
import org.aiotrade.lib.math.timeseries.TUnit._

/**
 * Class combining Unit and nUnits.
 * Try to implement a Primitive-like type.
 * Use modifies to define a lightweight class.
 *
 * This class is better to be treated as a <b>value</b> class, so, using:
 *   <code>freq = anotherFreq.clone()</code>
 * instead of:
 *   <code>freq = anotherFreq</code>
 * is always more safe.
 *
 * @author Caoyuan Deng
 */
class TFreq(val unit: TUnit, val nUnits: Int) extends Cloneable with Ordered[TFreq] {

  val interval = unit.getInterval * nUnits

  def getUnit: TUnit = unit

  def getNUnits: Int = nUnits

  /**
   * return interval in milliseconds
   */
  def getInterval: Long = interval

  def nextTime(fromTime: Long): Long = unit.timeAfterNUnits(fromTime, nUnits)
    
  def previousTime(fromTime: Long): Long = unit.timeAfterNUnits(fromTime, -nUnits)

  def timeAfterNFreqs(fromTime: Long, nFreqs: Int): Long = unit.timeAfterNUnits(fromTime, nUnits * nFreqs)

  def nFreqsBetween(fromTime: Long, toTime: Long): Int = unit.nUnitsBetween(fromTime, toTime) / nUnits

  /**
   * round time to freq's begin 0
   * @param time time in milliseconds from the epoch (1 January 1970 0:00 UTC)
   * @param cal Calendar instance with proper timeZone set, <b>cal is not thread safe</b>
   */
  def round(time: Long, cal: Calendar): Long = {
    cal.setTimeInMillis(time)
    val offsetToLocalZeroOfDay = cal.getTimeZone.getRawOffset - cal.get(Calendar.DST_OFFSET)
    ((time + offsetToLocalZeroOfDay) / interval) * interval - offsetToLocalZeroOfDay
  }

  /**
   * round time to freq's begin 0
   * @param timeA time in milliseconds from the epoch (1 January 1970 0:00 UTC)
   * @param timeB time in milliseconds from the epoch (1 January 1970 0:00 UTC)
   * @param cal Calendar instance with proper timeZone set, <b>cal is not thread safe</b>
   */
  def sameInterval(timeA: Long, timeB: Long, cal: Calendar): Boolean = {
    round(timeA, cal) == round(timeB, cal)
  }

  def getName: String = {
    if (nUnits == 1) {
      unit match {
        case Hour =>
          return "Hourly"
        case Day =>
          return "Daily"
        case Week =>
          return "Weekly"
        case Month =>
          return "Monthly"
        case Year =>
          return "Yearly"
        case _ =>
      }
    }

    val sb = new StringBuilder(10).append(nUnits).append(unit.getCompactDescription)
    if (nUnits > 1) {
      sb.append("s")
    }

    sb.toString
  }

  override def equals(o: Any): Boolean = {
    o match {
      case x: TFreq =>
        if (x.unit == this.unit && x.nUnits == this.nUnits) {
          true
        } else false
      case _ => false
    }
  }

  override def clone: TFreq = {
    try {
      super.clone.asInstanceOf[TFreq]
    } catch {case ex:CloneNotSupportedException => ex.printStackTrace; null}
  }

  override def compare(another: TFreq): Int = {
    if (this.unit.ordinal < another.unit.ordinal) {
      -1
    } else if (this.unit.ordinal > another.unit.ordinal) {
      1
    } else {
      if (this.nUnits < another.nUnits) -1 else {if (this.nUnits == another.nUnits) 0 else 1}
    }
  }

  override def hashCode: Int = {
    /** should let the equaled frequencies have the same hashCode, just like a Primitive type */
    interval.asInstanceOf[Int]
    /*- Reserve
     return unit.hashCode() * nUnits
     */
  }

  override def toString: String = getName

  def writeToBean(doc: BeansDocument): Element = {
    val bean = doc.createBean(this)
    
    doc.valueConstructorArgOfBean(bean, 0, getUnit)
    doc.valueConstructorArgOfBean(bean, 1, getNUnits)
    
    bean
  }
    
  def writeToJava(id: String): String = {
    "todo"//JavaDocument.create(id, classOf[Frequency], getUnit, getNUnits)
  }
}

object TFreq {
  val PREDEFINED = Set(ONE_MIN,
                       TWO_MINS,
                       THREE_MINS,
                       FOUR_MINS,
                       FIVE_MINS,
                       FIFTEEN_MINS,
                       THIRTY_MINS,
                       DAILY,
                       TWO_DAYS,
                       THREE_DAYS,
                       FOUR_DAYS,
                       FIVE_DAYS,
                       WEEKLY,
                       MONTHLY)

  val SELF_DEFINED = new TFreq(TUnit.Second, 0)
  val ONE_SEC = new TFreq(TUnit.Second, 1)
  val TWO_SECS = new TFreq(TUnit.Second, 2)
  val THREE_SECS = new TFreq(TUnit.Second, 3)
  val FOUR_SECS = new TFreq(TUnit.Second, 3)
  val FIVE_SECS = new TFreq(TUnit.Second, 5)
  val FIFTEEN_SECS = new TFreq(TUnit.Second, 15)
  val THIRTY_SECS = new TFreq(TUnit.Second, 30)
  val ONE_MIN = new TFreq(TUnit.Minute, 1)
  val TWO_MINS = new TFreq(TUnit.Minute, 2)
  val THREE_MINS = new TFreq(TUnit.Minute, 3)
  val FOUR_MINS = new TFreq(TUnit.Minute, 3)
  val FIVE_MINS = new TFreq(TUnit.Minute, 5)
  val FIFTEEN_MINS = new TFreq(TUnit.Minute, 15)
  val THIRTY_MINS = new TFreq(TUnit.Minute, 30)
  val ONE_HOUR = new TFreq(TUnit.Hour, 1)
  val DAILY = new TFreq(TUnit.Day, 1)
  val TWO_DAYS = new TFreq(TUnit.Day, 2)
  val THREE_DAYS = new TFreq(TUnit.Day, 3)
  val FOUR_DAYS = new TFreq(TUnit.Day, 4)
  val FIVE_DAYS = new TFreq(TUnit.Day, 5)
  val WEEKLY = new TFreq(TUnit.Week, 1)
  val MONTHLY = new TFreq(TUnit.Month, 1)
  val THREE_MONTHS = new TFreq(TUnit.Month, 3)
  val ONE_YEAR = new TFreq(TUnit.Year, 1)
}
