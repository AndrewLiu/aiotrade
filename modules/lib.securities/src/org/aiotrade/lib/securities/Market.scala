/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.lib.securities

import java.util.{Calendar,TimeZone}

/**
 *
 * @author dcaoyuan
 */
object Market {
  val NYSE = new Market(TimeZone.getTimeZone("America/New_York"), 9, 30, 15, 00)  // New York
  val SHSE = new Market(TimeZone.getTimeZone("Asia/Shanghai"), 9, 30, 15, 0) // Shanghai
  val SZSE = new Market(TimeZone.getTimeZone("Asia/Shanghai"), 9, 30, 15, 0) // Shenzhen
  val LDSE = new Market(TimeZone.getTimeZone("UTC"), 9, 30, 15, 0) // London
}

class Market(val timeZone:TimeZone, openHour:Int, openMin:int, closeHour:Int, closeMin:int) {

  private val cal = Calendar.getInstance(timeZone)
  val openTimeOfDay :Long = (openHour * 60 + openMin) * 60 * 1000

  def this(openHour:Int, openMin:int, closeHour:Int, closeMin:int) {
    this(TimeZone.getTimeZone("UTC"), openHour, openMin, closeHour, closeMin)
  }

  def openTime(time:Long) :Long = {
    cal.clear
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, openHour)
    cal.set(Calendar.MINUTE, openMin)
    cal.getTimeInMillis
  }

  def closeTime(time:Long) :Long = {
    cal.clear
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, closeHour)
    cal.set(Calendar.MINUTE, closeMin)
    cal.getTimeInMillis
  }

  override def toString :String = {
    timeZone.getDisplayName
  }
}
