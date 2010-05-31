package org.aiotrade.lib.securities.model

import java.util.logging.Logger
import java.util.{Calendar, TimeZone, ResourceBundle, Timer, TimerTask}
import scala.collection.mutable.ListBuffer
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.actors.Event

import ru.circumflex.orm.Table
import ru.circumflex.orm._

import scala.actors.Actor
import scala.actors.Actor._

object Exchanges extends Table[Exchange] {
  val code = "code" VARCHAR(4)
  val name = "name" VARCHAR(10)
  val fullName = "fullName" VARCHAR(30)
  val timeZoneStr = "timeZoneStr" VARCHAR(30)
  val openCloseHMs = "openCloseHMs" SERIALIZED(classOf[Array[Int]], 100)

  def closeDates = inverse(ExchangeCloseDates.exchange)
  def secs = inverse(Secs.exchange)

  INDEX("code_idx", code.name)
}

object Exchange extends Publisher {
  val logger = Logger.getLogger(this.getClass.getSimpleName)

  case class Opened(exchange: Exchange) extends Event
  case class Closed(exchange: Exchange) extends Event
  /**
   * @Note should lazy call allExchanges during model running, so, don't start timer actor automatically
   */
  object exchangesActor extends Actor {
    def act {
      val timer = new Timer("Exchange Open/Close Timer")
      for (exchange <- allExchanges) {
        val preOpen = exchange.open
        preOpen.add(Calendar.MINUTE, -15)
        timer.schedule(new TimerTask {
            def run {
              // @todo process vacation here
              publish(Opened(exchange))
            }
          }, preOpen.getTime, ONE_DAY)

        val postClose = exchange.close
        postClose.add(Calendar.MINUTE, +15)
        timer.schedule(new TimerTask {
            def run {
              // @todo process vacation here
              publish(Closed(exchange))
            }
          }, postClose.getTime, ONE_DAY)
      }
    }
  }

  def startTimer = exchangesActor.start

  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.securities.model.Bundle")
  private val ONE_DAY = 24 * 60 * 60 * 1000

  lazy val N  = allExchanges.find(_.code == "N" ) getOrElse (throw new Exception("Cannot find exchange of N(New York)"))
  lazy val SS = allExchanges.find(_.code == "SS") getOrElse (throw new Exception("Cannot find exchange of SS(Shanghai)"))
  lazy val SZ = allExchanges.find(_.code == "SZ") getOrElse (throw new Exception("Cannot find exchange of SZ(Shenzhen)"))
  lazy val L  = allExchanges.find(_.code == "L" ) getOrElse (throw new Exception("Cannot find exchange of L(London)"))

  lazy val allExchanges = Exchanges.all()

  lazy val uniSymbolToSec =
    (allExchanges map (x => secsOf(x)) flatMap {secs =>
        secs filter (_.secInfo != null) map (sec => sec.secInfo.uniSymbol -> sec)
      }
    ).toMap

  def exchangeOf(uniSymbol: String): Exchange = {
    uniSymbol.toUpperCase.split('.') match {
      case Array(symbol) => N
      case Array(symbol, "L" ) => L
      case Array(symbol, "SS") => SS
      case Array(symbol, "SZ") => SZ
      case _ => N
    }
  }

  def secsOf(exchange: Exchange): Seq[Sec] = {
    val secs = (SELECT (Secs.*, SecInfos.*) FROM (Secs JOIN SecInfos) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange)) list) map (_._1)
    logger.info("Secs number of " + exchange.code + " is " + secs.size)
    secs
  }

  def symbolsOf(exchange: Exchange): Seq[String] = {
    val syms = ListBuffer[String]()
    secsOf(exchange) foreach {sec =>
      if (sec.secInfo == null)
        logger.warning("secInfo of sec " + sec + " is null")
      else
        syms += sec.secInfo.uniSymbol
    }
    logger.info("Syms number of " + exchange.code + " is " + syms.size)
    syms
  }

  def secOf(uniSymbol: String): Option[Sec] =
    uniSymbolToSec.get(uniSymbol)

  def apply(code: String, timeZoneStr: String, openCloseHMs: Array[Int]) = {
    val exchange = new Exchange
    exchange.code = code
    exchange.timeZoneStr = timeZoneStr
    exchange.openCloseHMs = openCloseHMs
    exchange
  }
}


import Exchange._
class Exchange {

  // --- database fields
  var code: String = "SS"
  var name: String = "??"
  var fullName: String = "???????"
  var timeZoneStr: String = "Asia/Shanghai"
  var openCloseHMs: Array[Int] = Array(9, 30, 11, 30, 13, 0, 15, 0)

  var closeDates: List[ExchangeCloseDate] = Nil
  var secs: List[Sec] = Nil
  // --- end database fields

  lazy val longDescription:  String = BUNDLE.getString(code + "_Long")
  lazy val shortDescription: String = BUNDLE.getString(code + "_Short")
  lazy val timeZone: TimeZone = TimeZone.getTimeZone(timeZoneStr)

  private lazy val openHour = openCloseHMs(0)
  private lazy val openMin = openCloseHMs(1)
  private lazy val closeHour = openCloseHMs(openCloseHMs.length - 2)
  private lazy val closeMin = openCloseHMs(openCloseHMs.length - 1)

  lazy val openTimeOfDay: Long = (openHour * 60 + openMin) * 60 * 1000

  private var _symbols = List[String]()

  def open: Calendar = {
    val cal = Calendar.getInstance(timeZone)
    cal.set(Calendar.HOUR_OF_DAY, openHour)
    cal.set(Calendar.MINUTE, openMin)
    cal
  }

  def close: Calendar = {
    val cal = Calendar.getInstance(timeZone)
    cal.set(Calendar.HOUR_OF_DAY, closeHour)
    cal.set(Calendar.MINUTE, closeMin)
    cal
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

  def symbols = _symbols
  def symbols_=(symbols: List[String]) {
    _symbols = symbols
  }

  override def toString: String = {
    code + " -> " + timeZone.getDisplayName
  }

}

