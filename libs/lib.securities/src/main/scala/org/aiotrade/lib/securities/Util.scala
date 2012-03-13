package org.aiotrade.lib.securities


import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Secs
import org.aiotrade.lib.securities.model.Sectors
import org.aiotrade.lib.securities.model.SectorSecs
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import ru.circumflex.orm._

/**
 * 
 * @author Caoyuan Deng
 */
object Util {
  private val log = Logger.getLogger(this.getClass.getName)
  
  def getSecsOfSector(category: String) = {
    val sectors = SELECT (Sectors.*) FROM (AVRO(Sectors)) list()
    sectors foreach {x => log.info("%s, category=%s, id=%s".format(x.name, x.category, Sectors.idOf(x)))}
    val sector = sectors.find(_.category == category).get
    val sectorId = Sectors.idOf(sector)
    val secsHolder = Exchange.uniSymbolToSec
    val secs = SELECT (SectorSecs.*) FROM (AVRO(SectorSecs)) list() filter {x => 
      (x.sector eq sector) && (x.validTo == 0)
    } map {x => x.sec}
    secs.toSet.toArray
  }

  /**
   * Load all sers of secs from persistence and return referSer
   * 
   * @return referSer
   */
  def loadSers(secs: Array[Sec], referSec: Sec, freq: TFreq): QuoteSer = {
    val referSer = loadSer(referSec, freq, false)

    val t0 = System.currentTimeMillis
    var i = 0
    while (i < secs.length) {
      val sec = secs(i)
      loadSer(sec, freq)
      i += 1
      log.info("Loaded %s, %s of %s.".format(sec.uniSymbol, i, secs.length))
    }
    log.info("Loaded sers in %s s".format((System.currentTimeMillis - t0) / 1000))
    
    referSer
  }
  
  /**
   * Load ser of sec from persistence and return ser
   * 
   * @return referSer
   */
  def loadSer(sec: Sec, freq: TFreq, doAdjust: Boolean = true): QuoteSer = {
    val ser = sec.serOf(freq).get
    sec.loadSerFromPersistence(ser, false)
    ser.isLoaded = true // should be called to let adjust go and release a rection later.
    if (doAdjust) {
      ser.adjust(true)
    }
    ser
  }
  
  def getReferTimesViaDB(toTime: Long, referTimePeriod: Int, freq: TFreq, referSec: Sec): Array[Long] = {
    val quotes = getQuoteTable(freq) match {
      case Some(q) =>
        (SELECT (q.*) FROM (q) WHERE (
            (q.sec.field EQ Secs.idOf(referSec)) AND (q.time LE toTime)
          ) ORDER_BY (q.time DESC) list()
        ) toArray
    
      case _ => Array()
    }
    
    val referTimes = new ArrayList[Long]()
    var i = 0
    var n = math.min(quotes.length, referTimePeriod)
    while (i < n) {
      referTimes += quotes(n - i - 1).time
      i += 1
    }
    referTimes.toArray
  }

  def getQuotes(sec: Sec, freq: TFreq, limit: Int): Array[Quote] = {
    getQuoteTable(freq) match {
      case Some(q) =>
        (SELECT (q.*) FROM (q) WHERE (
            q.sec.field EQ Secs.idOf(sec)
          ) ORDER_BY (q.time DESC) LIMIT(limit) list() reverse
        ) toArray
    
      case _ => Array()
    }
  }
  
  def getQuotes(sec: Sec, freq: TFreq, fromTime: Long, toTime: Long): Array[Quote] = {
    getQuoteTable(freq) match {
      case Some(q) =>
        (SELECT (q.*) FROM (q) WHERE (
            (q.sec.field EQ Secs.idOf(sec)) AND (q.time LE toTime) AND (q.time GE fromTime)
          ) ORDER_BY (q.time DESC) list() reverse
        ) toArray
    
      case _ => Array()
    }
  }
  
  def doAdjusting(sec: Sec, quotes: Array[Quote]) {
    val divs = Exchanges.dividendsOf(sec)
    if (divs.isEmpty) {
      return
    }
    
    var i = 0
    while (i < quotes.length) {
      val quote = quotes(i)
      val time = quote.time

      var h = quote.high
      var l = quote.low
      var o = quote.open
      var c = quote.close

      val divItr = divs.iterator
      while (divItr.hasNext) {
        val div = divItr.next
        if (time < div.dividendDate) {
          h = div.adjust(h)
          l = div.adjust(l)
          o = div.adjust(o)
          c = div.adjust(c)
        }
      }
      
      quote.high  = h
      quote.low   = l
      quote.open  = o
      quote.close = c
      
      i += 1
    }
  }

  def getQuoteTable(freq: TFreq) = freq match {
    case TFreq.DAILY => Some(Quotes1d)
    case TFreq.ONE_MIN => Some(Quotes1m)
    case _ => None
  }  
}
