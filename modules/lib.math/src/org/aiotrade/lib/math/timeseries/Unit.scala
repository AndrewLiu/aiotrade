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

import java.text.DateFormat
import java.text.FieldPosition
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 *
 * @author Caoyuan Deng
 *
 * @credits:
 *     stebridev@users.sourceforge.net - fix case of Week : beginTimeOfUnitThatInclude(long)
 */
object Unit extends Enumeration {
  // Let type of enum's value the same as enum itself
  type Unit = V

  def V(name:String) = new V(name)

  val Second = V("Second")
  val Minute = V("Minute")
  val Hour   = V("Hour")
  val Day    = V("Day")
  val Week   = V("Week")
  val Month  = V("Month")
  val Year   = V("Year")

  /**
   * the unit(interval) of each Unit
   */
  private val ONE_SECOND :Int  = 1000
  private val ONE_MINUTE :Int  = 60 * ONE_SECOND
  private val ONE_HOUR   :Int  = 60 * ONE_MINUTE
  private val ONE_DAY    :Long = 24 * ONE_HOUR
  private val ONE_WEEK   :Long =  7 * ONE_DAY
  private val ONE_MONTH  :Long = 30 * ONE_DAY
  private val ONE_Year   :Long = (365.24 * ONE_DAY).asInstanceOf[Long]

  /**
   *
   *
   *
   * @NOTICE: Should avoid declaring Calendar instance as static, it's not thread-safe
   * see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6231579
   *
   * As Unit is enum, which actually is a kind of singleton, so delcare a
   * none static Calendar instance here also means an almost static instance,
   * so, if we declare class scope instance of Calendar in enum, we should also
   * synchronized each method that uses this instance or declare the cal
   * instance as volatile to share this instance by threads.
   */
  private val cal = Calendar.getInstance()
    
  class V(name:String) extends Val(name) {
        
    def ordinal = id

    def getInterval :Long = {
      this match {
        case Second => ONE_SECOND
        case Minute => ONE_MINUTE
        case Hour   => ONE_HOUR
        case Day    => ONE_DAY
        case Week   => ONE_WEEK
        case Month  => ONE_MONTH
        case Year   => ONE_Year
        case _      => ONE_DAY
      }
    }

    /**
     * round time to unit's begin 0
     * @param time time in milliseconds from the epoch (1 January 1970 0:00 UTC)
     */
    def round(time:Long) :Long = {
      //return (time + offsetToUTC / getInterval()) * getInterval() - offsetToUTC
      (time / getInterval) * getInterval
    }

    def getShortDescription = {
      this match {
        case Second => "s"
        case Minute => "m"
        case Hour   =>"h"
        case Day    => "D"
        case Week   =>"W"
        case Month  => "M"
        case _      => "d"
      }
    }

    def getCompactDescription :String = {
      this match {
        case Second => "Sec"
        case Minute => "Min"
        case Hour   => "Hour"
        case Day    => "Day"
        case Week   => "Week"
        case Month  => "Month"
        case _      => "Day"
      }
    }

    def getLongDescription :String = {
      this match {
        case Second => "Second"
        case Minute => "Minute"
        case Hour   => "Hourly"
        case Day    => "Daily"
        case Week   => "Weekly"
        case Month  => "Monthly"
        case _      => "Daily"
      }
    }

    def nUnitsBetween(fromTime:Long, toTime:Long) :Int = {
      this match {
        case Week  => nWeeksBetween(fromTime, toTime)
        case Month => nMonthsBetween(fromTime, toTime)
        case _     => ((toTime - fromTime) / getInterval).asInstanceOf[Int]
      }

    }

    private def nWeeksBetween(fromTime:Long, toTime:Long) :Int = {
      val between = ((toTime - fromTime) / ONE_WEEK).asInstanceOf[Int]

      /**
       * If between >= 1, between should be correct.
       * Otherwise, the days between fromTime and toTime is <= 6,
       * we should consider it as following:
       */
      if (Math.abs(between) < 1) {
        cal.setTimeInMillis(fromTime)
        val weekOfYearA = cal.get(Calendar.WEEK_OF_YEAR)

        cal.setTimeInMillis(toTime)
        val weekOfYearB = cal.get(Calendar.WEEK_OF_YEAR)

        /** if is in same week, between = 0, else between = 1 */
        if (weekOfYearA == weekOfYearB) 0 else {if (between > 0) 1 else -1}
      } else {
        between
      }
    }

    private def nMonthsBetween(fromTime:Long, toTime:Long):Int = {
      cal.setTimeInMillis(fromTime)
      val monthOfYearA = cal.get(Calendar.MONTH)
      val yearA = cal.get(Calendar.YEAR)

      cal.setTimeInMillis(toTime)
      val monthOfYearB = cal.get(Calendar.MONTH)
      val yearB = cal.get(Calendar.YEAR)

      /** here we assume each year has 12 months */
      (yearB * 12 + monthOfYearB) - (yearA * 12 + monthOfYearA)
    }

    def timeAfterNUnits(fromTime:Long, nUnits:Int) :Long  = {
      this match {
        case Week  => timeAfterNWeeks(fromTime, nUnits)
        case Month => timeAfterNMonths(fromTime, nUnits)
        case _     => fromTime + nUnits * getInterval
      }
    }

    /** snapped to first day of the week */
    private def timeAfterNWeeks(fromTime:Long, nWeeks:Int) :Long = {
      cal.setTimeInMillis(fromTime)

      /** set the time to first day of this week */
      val firstDayOfWeek = cal.getFirstDayOfWeek()
      cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)

      cal.add(Calendar.WEEK_OF_YEAR, nWeeks)

      cal.getTimeInMillis
    }

    /** snapped to 1st day of the month */
    private def timeAfterNMonths(fromTime:Long, nMonths:Int) :Long = {
      cal.setTimeInMillis(fromTime)

      /** set the time to this month's 1st day */
      val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
      cal.add(Calendar.DAY_OF_YEAR, -(dayOfMonth - 1))

      cal.add(Calendar.MONTH, nMonths)

      cal.getTimeInMillis
    }

    def beginTimeOfUnitThatInclude(time:Long, cal:Calendar):Long = {
      cal.setTimeInMillis(time)

      this match {
        case Day =>
          /** set the time to today's 0:00 by clear hour, minute, second etc. */
          val year  = cal.get(Calendar.YEAR)
          val month = cal.get(Calendar.MONTH)
          val date  = cal.get(Calendar.DAY_OF_MONTH)
          cal.clear()
          cal.set(year, month, date)
        case Week =>
          /**
           * set the time to this week's first day of one week
           *     int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
           *     calendar.add(Calendar.DAY_OF_YEAR, -(dayOfWeek - Calendar.SUNDAY))
           *
           * From stebridev@users.sourceforge.net:
           * In some place of the world the first day of month is Monday,
           * not Sunday like in the United States. For example Sunday 15
           * of August of 2004 is the week 33 in Italy and not week 34
           * like in US, while Thursday 19 of August is in the week 34 in
           * boot Italy and US.
           */
          val firstDayOfWeek = cal.getFirstDayOfWeek()
          cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        case Month =>
          /** set the time to this month's 1st day */
          val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
          cal.add(Calendar.DAY_OF_YEAR, -(dayOfMonth - 1))
        case _ =>
      }

      cal.getTimeInMillis
    }

    def formatNormalDate(date:Date, timeZone:TimeZone) :String = {
      val df = this match {
        case Second => DateFormat.getTimeInstance(DateFormat.MEDIUM)
        case Minute => DateFormat.getTimeInstance(DateFormat.SHORT)
        case Hour   => DateFormat.getTimeInstance(DateFormat.MEDIUM)
        case Day    => DateFormat.getDateInstance(DateFormat.SHORT)
        case Week   => DateFormat.getDateInstance(DateFormat.SHORT)
        case _      => DateFormat.getDateInstance(DateFormat.SHORT)
      }

      df.setTimeZone(timeZone)
      df.format(date)
    }

    def formatStrideDate(date:Date, timeZone:TimeZone) :String = {
      val df = this match {
        case Second => DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        case Minute => DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        case Hour   => DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        case Day    => DateFormat.getDateInstance(DateFormat.SHORT)
        case Week   => DateFormat.getDateInstance(DateFormat.SHORT)
        case _      => DateFormat.getDateInstance(DateFormat.SHORT)
      }

      val buffer = new StringBuffer
      df.setTimeZone(timeZone)
      df.format(date, buffer, new FieldPosition(DateFormat.MONTH_FIELD))
      buffer.toString()
    }
  }

}
