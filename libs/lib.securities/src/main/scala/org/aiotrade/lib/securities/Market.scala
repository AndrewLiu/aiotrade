/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.lib.securities

import java.util.{Calendar,TimeZone}
import scala.collection.immutable.TreeMap

/**
 *
 * @author dcaoyuan
 */
object Market {
  val NYSE = new Market("NYSE", TimeZone.getTimeZone("America/New_York"), 9, 30, 15, 00)  // New York
  val SHSE = new Market("SHSE", TimeZone.getTimeZone("Asia/Shanghai"), 9, 30, 15, 0) // Shanghai
  val SZSE = new Market("SZSE", TimeZone.getTimeZone("Asia/Shanghai"), 9, 30, 15, 0) // Shenzhen
  val LDSE = new Market("LDSE", TimeZone.getTimeZone("UTC"), 9, 30, 15, 0) // London

  def allMarkets = List(NYSE, SHSE, SZSE, LDSE)

  def symbolsOf(market: Market): List[String] = {
    market.code match {
      case "NYSE" =>
        List("GOOG", "JAVA", "YHOO")
      case "SHSE" =>
        shseSymbols
      case "SZSE" =>
        List("000001.SZ")
      case "LDSE" =>
        List("BP.L")
    }
  }

  lazy val shseSymbols = shseSymToName.keySet map (_ + ".SS") toList

  lazy val shseSymToName = TreeMap(
    "600000" -> "浦发银行",
    "600001" -> "邯郸钢铁",
    "600002" -> "齐鲁石化",
    "600003" -> "东北高速",
    "600004" -> "白云机场",
    "600005" -> "武钢股份",
    "600006" -> "东风汽车",
    "600007" -> "中国国贸",
    "600008" -> "首创股份",
    "600009" -> "上海机场",
    "600010" -> "钢联股份",
    "600011" -> "华能国际",
    "600012" -> "皖通高速",
    "600015" -> "华夏银行",
    "600016" -> "民生银行",
    "600018" -> "上港集箱",
    "600019" -> "宝钢股份",
    "600020" -> "中原高速",
    "600021" -> "上海电力",
    "600026" -> "中海发展",
    "600028" -> "中国石化",
    "600029" -> "南方航空",
    "600030" -> "中信证券",
    "600031" -> "三一重工",
    "600033" -> "福建高速",
    "600036" -> "招商银行",
    "600037" -> "歌华有线",
    "600038" -> "哈飞股份",
    "600039" -> "四川路桥"
  )

}

class Market(val code: String, val timeZone: TimeZone, openHour: Int, openMin: Int, closeHour: Int, closeMin: Int) {

  val openTimeOfDay: Long = (openHour * 60 + openMin) * 60 * 1000

  private var _symbols = List[String]()

  def this(openHour: Int, openMin: Int, closeHour: Int, closeMin: Int) {
    this("NYSE", TimeZone.getTimeZone("UTC"), openHour, openMin, closeHour, closeMin)
  }

  def openTime(time: Long): Long = {
    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, openHour)
    cal.set(Calendar.MINUTE, openMin)
    cal.getTimeInMillis
  }

  def closeTime(time: Long): Long = {
    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, closeHour)
    cal.set(Calendar.MINUTE, closeMin)
    cal.getTimeInMillis
  }

  def symbols: List[String] = _symbols
  def symbols_=(symbols: List[String]) {
    _symbols = symbols
  }

  override def toString: String = {
    code + " " + timeZone.getDisplayName
  }
}
