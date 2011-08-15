/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable
import org.aiotrade.lib.math.timeseries.TVal
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import java.util.Calendar
import org.aiotrade.lib.math.timeseries.TFreq

@serializable
class PriceDistribution extends BelongsToSec with TVal with Flag {

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {this._time = time}

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag
  def flag_=(flag: Int) {this._flag = flag}

  var price: Double =_
  var volumeUp: Double =_
  var volumeDown: Double =_

  def copyFrom(another: PriceDistribution) {
    this.sec = another.sec
    this.time = another.time
    this.flag = another.flag
    this.price = another.price
    this.volumeUp = another.volumeUp
    this.volumeDown = another.volumeDown
  }

  override def toString() = {
    val sp = new StringBuffer
    sp.append("price:").append(price)
    sp.append(",volumeUp:").append(volumeUp)
    sp.append(",volumeDown:").append(volumeDown)
    sp.toString
  }
}

@serializable
class PriceCollection extends BelongsToSec with TVal with Flag  {
  @transient
  val cal = Calendar.getInstance
  private var map = mutable.Map[String, PriceDistribution]()

  var isTransient = true

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {this._time = time}

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag
  def flag_=(flag: Int) {this._flag = flag}

  def get(price: String) = map.get(price)
    
  def put(price: String, pd: PriceDistribution) = {
    if (map.isEmpty){
      this.time = pd.time
      this.sec = pd.sec
    }

    if (TFreq.DAILY.round(this.time, cal) == TFreq.DAILY.round(pd.time, cal)){
      map.put(price, pd)

      if (this.closed_?) pd.closed_! else pd.unclosed_!
    }
  }

  def keys = map.keys

  def values = map.values

  def isEmpty = map.isEmpty

  override def closed_! = {
    super.closed_!
    this.map.values foreach (_.closed_!)
  }

  override def unclosed_! = {
    super.unclosed_!
    this.map.values foreach (_.unclosed_!)
  }

  override def justOpen_! = {
    super.justOpen_!
    this.map.values foreach (_.justOpen_!)
  }

  override def unjustOpen_! = {
    super.unjustOpen_!
    this.map.values foreach (_.unjustOpen_!)
  }

  override def fromMe_! = {
    super.fromMe_!
    this.map.values foreach (_.fromMe_!)
  }

  override def unfromMe_! = {
    super.unfromMe_!
    this.map.values foreach (_.unfromMe_!)
  }
  
  override def toString() ={
    val sp = new StringBuffer
    sp.append("\nunisymbol:").append(uniSymbol)
    sp.append("\ntime:").append(time)
    this.map.values foreach {value => sp.append("\n").append(value.toString)}
    sp.toString
  }
}


object PriceDistributions  extends Table[PriceDistribution] {
  private val log = Logger.getLogger(this.getClass.getName)

  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()
  val price = "price" DOUBLE()
  val volumeUp = "volumeUp" BIGINT()
  val volumeDown = "volumeDown" BIGINT()
  val flag = "flag" INTEGER()

  private val dailyCache = mutable.Map[(Sec, Long), PriceCollection]()

  @deprecated
  def dailyDistribuOf(sec: Sec, date: Long): PriceCollection ={
    dailyCache.get(sec -> date) match {
      case Some(map) => map
      case None =>
        val map = new PriceCollection

        dailyCache.put(sec -> date, map)

        try{
          (SELECT (this.*) FROM this WHERE ((this.time EQ date) AND (this.sec.field EQ Secs.idOf(sec))) list
          ) foreach {x => map.put(x.price.toString, x)}
        }
        catch{
          case ex => log.log(Level.SEVERE, ex.getMessage, ex)
        }

        map.isTransient = map.isEmpty
        map.time = date
        map.sec = sec
        if (map.isTransient){
          map.unclosed_!
          map.justOpen_!
          map.fromMe_!
          sec.exchange.addNewPriceDistribution(TFreq.DAILY, map)
        }
        map
    }
  }

  @deprecated
  def dailyDistribuOf_ignoreCache(sec: Sec, date: Long): PriceCollection ={
    val map = new PriceCollection
    try{
      (SELECT (this.*) FROM this WHERE ((this.time EQ date) AND (this.sec.field EQ Secs.idOf(sec))) list
      ) foreach {x => map.put(x.price.toString, x)}
    }
    catch{
      case ex => log.log(Level.SEVERE, ex.getMessage, ex)
    }

    map.time = date
    map.sec = sec
    map.isTransient = map.isEmpty
    if (map.isTransient){
      map.unclosed_!
      map.justOpen_!
      map.fromMe_!
      sec.exchange.addNewPriceDistribution(TFreq.DAILY, map)
    }
    map
  }

  @deprecated
  def dailyDistribuOf(sec: Sec): mutable.Map[Long, PriceCollection] = {
    seqToMap(
      try{
        (SELECT (this.*) FROM (this) WHERE (
            this.sec.field EQ Secs.idOf(sec)
          ) ORDER_BY (this.time) list
        )
      }
      catch{
        case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
      })
  }

  def closedDistribuOf(sec: Sec): mutable.Map[Long, PriceCollection] = {
    val map = mutable.Map[Long, PriceCollection]()
    (try{
        SELECT (this.*) FROM (this) WHERE (
          this.sec.field EQ Secs.idOf(sec)
        ) ORDER_BY (this.time) list
      }
     catch{
        case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
      }
    ) foreach {x =>
      if (x.closed_?){
        map.get(x.time) match{
          case Some(m) => m.isTransient = false; m.put(x.price.toString, x)
          case None =>
            val m = new PriceCollection
            m.isTransient = false
            map.put(x.time, m)
            m.time = x.time
            m.sec = sec
            m.put(x.price.toString, x)
        }
      }
    }

    log.info("Load price collection from DB:" + map.size)
    map
  }

  @deprecated
  def closedDistribuOf__filterByDB(sec: Sec): mutable.Map[Long, PriceCollection]= {
    seqToMap(
      try{
        SELECT (this.*) FROM (this) WHERE (
          (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
        ) ORDER_BY (this.time) list
      }
      catch{
        case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
      }
    )
  }

  /**
   * Convert the data format
   * The Price Distribution's format is:
   *
   *  -------------------------------------------------------------------
   *  |    Security |    Date     |  Price  |  Volume up  | Volume down |
   *  -------------------------------------------------------------------
   *  |   600001.S  |  2011-01-01 |   9.05  |   100000    |     10000   |
   *  |             |             |   9.04  |   100000    |     10000   |
   *  |             |             |   9.03  |   100000    |     100000  |
   *  |             -----------------------------------------------------
   *  |             |  2011-01-02 |   9.04  |   100000    |     10000   |
   *  |             |             |   9.03  |   100000    |     100000  |
   *  -------------------------------------------------------------------
   *  |  600002.SS  |  2011-01-01 |   10.87 |   10000     |     10000   |
   *  |             |             |   10.88 |   10000     |     10000   |
   *  |                           ......                                |
   *  |                                                                 |
   *  -------------------------------------------------------------------
   */
  private def seqToMap(list: Seq[PriceDistribution]): mutable.Map[Long, PriceCollection] = {
    val map = mutable.Map[Long, PriceCollection]()
    list foreach {x =>
      map.get(x.time) match{
        case Some(m) => m.isTransient = false; m.put(x.price.toString, x)
        case None =>
          val m = new PriceCollection
          m.isTransient = false
          map.put(x.time, m)
          m.time = x.time
          m.sec = x.sec
          m.put(x.price.toString, x)
      }
    }
    map
  }

  def saveBatch(sec: Sec, sortedPDs: Seq[PriceCollection]) {
    if (sortedPDs.isEmpty) return

    val head = sortedPDs.head
    val last = sortedPDs.last
    val frTime = math.min(head.time, last.time)
    val toTime = math.max(head.time, last.time)
    val exists = mutable.Map[(Long, Double), PriceDistribution]()
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time GE frTime) AND (this.time LE toTime)
      ) ORDER_BY (this.time) list
    ) foreach {x => exists.put(x.time -> x.price, x)}

    val updates = new ArrayList[PriceDistribution]()
    val inserts = new ArrayList[PriceDistribution]()

    sortedPDs.foreach{pc =>
      val (u, i) = pc.values.partition(x => exists.contains(x.time -> x.price))
      updates ++= u
      inserts ++= i
    }

    for (x <- updates) {
      val existOne = exists(x.time -> x.price)
      existOne.copyFrom(x)
    }

    try {
      if (updates.length > 0) {
        this.updateBatch_!(updates.toArray)
      }
      if (inserts.length > 0) {
        this.insertBatch_!(inserts.toArray)
      }
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }

  def saveBatch(atSameTime: Long, pcs: Array[PriceCollection]) {
    if (pcs.isEmpty) return

    val exists = mutable.Map[(Sec, Double),PriceDistribution]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.time EQ atSameTime) AND (this.sec.field GT 0) AND (this.sec.field LT CRCLongId.MaxId )
      ) list()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res foreach {x => exists.put(x.sec -> x.price, x)}

    val updates = new ArrayList[PriceDistribution]()
    val inserts = new ArrayList[PriceDistribution]()

    pcs.foreach{pc =>
      val (u, i) = pc.values.partition(x => exists.contains(x.sec -> x.price))
      updates ++= u
      inserts ++= i
    }

    for (x <- updates) {
      val existOne = exists(x.sec -> x.price)
      existOne.copyFrom(x)
    }

    try {
      if (updates.length > 0) {
        this.updateBatch_!(updates.toArray)
      }
      if (inserts.length > 0) {
        this.insertBatch_!(inserts.toArray)
      }
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }
}
