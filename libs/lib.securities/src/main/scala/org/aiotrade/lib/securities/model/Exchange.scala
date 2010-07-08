package org.aiotrade.lib.securities.model

import java.util.logging.Logger
import java.util.{Calendar, TimeZone, ResourceBundle, Timer, TimerTask}
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.actors.Event

import ru.circumflex.orm.Table
import ru.circumflex.orm._

import scala.actors.Actor
import scala.actors.Actor._

object Exchanges extends Table[Exchange] {
  val log = Logger.getLogger(this.getClass.getSimpleName)

  val code = "code" VARCHAR(4)
  val name = "name" VARCHAR(10)
  val fullName = "fullName" VARCHAR(30)
  val timeZoneStr = "timeZoneStr" VARCHAR(30)
  val openCloseHMs = "openCloseHMs" SERIALIZED(classOf[Array[Int]], 100)

  def closeDates = inverse(ExchangeCloseDates.exchange)
  def secs = inverse(Secs.exchange)

  INDEX(getClass.getSimpleName + "_code_idx", code.name)


  // --- helper methods
  def secsOf(exchange: Exchange): Seq[Sec] = {
    val exchangeId = Exchanges.idOf(exchange)
    val secs = (SELECT (Secs.*, SecInfos.*) FROM (Secs JOIN SecInfos) WHERE (Secs.exchange.field EQ exchangeId) list) map (_._1)
    log.info("Secs number of " + exchange.code + "(id=" + exchangeId + ") is " + secs.size)
    secs
  }
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

  lazy val N  = withCode("N" ).get
  lazy val SS = withCode("SS").get
  lazy val SZ = withCode("SZ").get
  lazy val L  = withCode("L" ).get

  lazy val allExchanges = Exchanges.all()
  lazy val codeToExchange = allExchanges map (x => (x.code -> x)) toMap

  lazy val exchangeToSecs = {
    allExchanges map (x => (x -> Exchanges.secsOf(x))) toMap
  }

  lazy val uniSymbolToSec = {
    exchangeToSecs map (_._2) flatMap {secs =>
      secs filter (_.secInfo != null) map (x => x.secInfo.uniSymbol -> x)
    } toMap
  }
  
  def withCode(code: String): Option[Exchange] = codeToExchange.get(code)

  def exchangeOf(uniSymbol: String): Exchange = {
    uniSymbol.toUpperCase.split('.') match {
      case Array(symbol) => N
      case Array(symbol, "L" ) => L
      case Array(symbol, "SS") => SS
      case Array(symbol, "SZ") => SZ
      case _ => SZ
    }
  }

  def secsOf(exchange: Exchange): Seq[Sec] = exchangeToSecs.get(exchange) getOrElse Nil

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

  lazy val uniSymbolToLastTicker = {
    val map = new HashMap[String, LightTicker]
    for ((sec, ticker) <- Tickers.lastTickersOf(this) if sec != null) {
      val symbol = sec.uniSymbol
      ticker.symbol = symbol
      map.put(symbol, ticker)
    }
    map
  }

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

  lazy val nMinutes: Int = {
    val openInMillis = open.getTimeInMillis
    val closeInMills = close.getTimeInMillis
    ((closeInMills - openInMillis) / TUnit.Minute.interval).toInt + 1
  }

  def symbols = _symbols
  def symbols_=(symbols: List[String]) {
    _symbols = symbols
  }

  override def toString: String = {
    code + " -> " + timeZone.getDisplayName
  }

}

