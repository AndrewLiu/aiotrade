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

import java.util.Calendar
import org.aiotrade.lib.collection.ArrayList
import scala.collection.mutable

/**
 * @author Caoyuan Deng
 */
object TimeQueue {
  // --- simple test
  def main(args: Array[String]) {
    val tq = new TimeQueue[Int](2)

    val cal = Calendar.getInstance
    cal.setTimeInMillis(0)
    cal.set(1990, 0, 1)
    for (i <- 0 until 10) {
      cal.add(Calendar.DAY_OF_MONTH, 1)
      tq.put("a", cal.getTimeInMillis, i)
      tq.put("b", cal.getTimeInMillis, i)
    }

    println(tq._dayToXs(0))
    println(tq._dayToXs(1))
    val ok = {
      tq._dayToXs(0) == (7314, Map("a" -> ArrayList(8), "b" -> ArrayList(8))) &&
      tq._dayToXs(1) == (7315, Map("a" -> ArrayList(9), "b" -> ArrayList(9)))
    }
    println(ok)
    assert(ok, "Error.")
  }
}

import TimeQueue._
final class TimeQueue[V: Manifest](fixedSize: Int) {

  private val lastIdx = fixedSize - 1
  private var _dayToXs = new Array[(Int, mutable.HashMap[String, ArrayList[V]])](fixedSize)

  val length = fixedSize
  def apply(i: Int): (Int, collection.Map[String, ArrayList[V]]) = _dayToXs synchronized {_dayToXs(i)}

  def put(key: String, time: Long, value: V): Unit = _dayToXs synchronized  {
    val day = daysFrom1970(time)

    var willAdd = true
    var lastKeyToXs = _dayToXs(lastIdx) match {
      case null => null
      case (dayMax, keyToXs) =>
        if (day == dayMax) {
          keyToXs
        } else if (day > dayMax) {
          val newDayToXs = new Array[(Int, mutable.HashMap[String, ArrayList[V]])](fixedSize)
          System.arraycopy(_dayToXs, 1, newDayToXs, 0, lastIdx)
          _dayToXs = newDayToXs
          _dayToXs(lastIdx) = null
          null
        } else {
          willAdd = false
          null
        }
    }

    if (willAdd) {
      if (lastKeyToXs == null) {
        val newXs = new ArrayList[V]
        newXs += value
        val newMap = mutable.HashMap[String, ArrayList[V]](key -> newXs)
        _dayToXs(lastIdx) = (day, newMap)
      } else {
        val xs = lastKeyToXs.get(key).getOrElse(new ArrayList[V]())
        lastKeyToXs.put(key, xs += value)
      }
    }
  }

  private def daysFrom1970(time: Long): Int = {
    (time / TUnit.Day.interval).toInt
  }

}
