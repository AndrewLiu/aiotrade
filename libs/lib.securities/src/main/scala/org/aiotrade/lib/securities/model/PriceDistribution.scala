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
  def time_=(time: Long) {
    this._time = time
  }

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag
  def flag_=(flag: Int) {
    this._flag = flag
  }

  var price: Double =_
  var volumeUp: Double =_
  var volumeDown: Double =_

  def copyFrom(another: PriceDistribution) {
    this.sec = another.sec
    this.time = another.time
    this.price = another.price
    this.volumeUp = another.volumeUp
    this.volumeDown = another.volumeDown
  }
}

class PriceCollection extends BelongsToSec with TVal with Flag  {
  val cal = Calendar.getInstance
  private var map = mutable.Map[Double, PriceDistribution]()

  var isTransient = true

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {
    this._time = time
  }

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag
  def flag_=(flag: Int) {
    this._flag = flag
  }

  def get(price: Double) = map.get(price)
    
  def put(price: Double, pd: PriceDistribution) = {
    if (map.isEmpty){
      this.time = pd.time
    }

    if (TFreq.DAILY.round(this.time, cal) == TFreq.DAILY.round(pd.time, cal)){
      map.put(price, pd)

      if (this.closed_?) pd.closed_! else pd.unclosed_!
    }
  }

  def keys() = map.keys

  def values() = map.values

  def isEmpty() = map.isEmpty

  override def closed_! = {
    super.closed_!
    this.map.keys.foreach{key =>
      this.map.get(key).getOrElse(null).closed_!
    }
  }

  override def unclosed_! = {
    super.unclosed_!
    this.map.keys.foreach{key =>
      this.map.get(key).getOrElse(null).unclosed_!
    }
  }

  override def justOpen_! = {
    super.justOpen_!
    this.map.keys.foreach{key =>
      this.map.get(key).getOrElse(null).justOpen_!
    }
  }

  override def unjustOpen_! = {
    super.unjustOpen_!
    this.map.keys.foreach{key =>
      this.map.get(key).getOrElse(null).unjustOpen_!
    }
  }


  override def fromMe_! = {
    super.fromMe_!
    this.map.keys.foreach{key =>
      this.map.get(key).getOrElse(null).fromMe_!
    }
  }

  override def unfromMe_! = {
    super.unfromMe_!
    this.map.keys.foreach{key =>
      this.map.get(key).getOrElse(null).unfromMe_!
    }
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
  def dailyDistribuOf(sec: Sec, date: Long): PriceCollection ={
    dailyCache.get(sec -> date) match {
      case Some(map) => map
      case None =>
        val map = new PriceCollection

        dailyCache.put(sec -> date, map)

        (SELECT (this.*) FROM this WHERE ((this.time EQ date) AND (this.sec.field EQ Secs.idOf(sec))) list
        ) foreach {x => map.put(x.price, x)}

        map.isTransient = map.isEmpty
        if (map.isTransient){
          sec.exchange.addNewPriceDistribution(TFreq.DAILY, map)
        }
        map
    }
  }

  def dailyDistribuOf_ignoreCache(sec: Sec, date: Long): PriceCollection ={
    val map = new PriceCollection
    (SELECT (this.*) FROM this WHERE ((this.time EQ date) AND (this.sec.field EQ Secs.idOf(sec))) list
    ) foreach {x => map.put(x.price, x)}

    map.isTransient = map.isEmpty
    if (map.isTransient){
      sec.exchange.addNewPriceDistribution(TFreq.DAILY, map)
    }
    map
  }


  def dailyDistribuOf(sec: Sec): mutable.Map[Long, PriceCollection] = {
    seqToMap(
      (SELECT (this.*) FROM (this) WHERE (
          this.sec.field EQ Secs.idOf(sec)
        ) ORDER_BY (this.time) list
      ))
  }

  def closedDistribuOf(sec: Sec): mutable.Map[Long, PriceCollection] = {
    val map = mutable.Map[Long, PriceCollection]()
    (SELECT (this.*) FROM (this) WHERE (
        this.sec.field EQ Secs.idOf(sec)
      ) ORDER_BY (this.time) list
    ) foreach {x =>
      if (x.closed_?){
        map.get(x.time) match{
          case Some(m) => m.isTransient = false; m.put(x.price, x)
          case None =>
            val m = new PriceCollection
            map.put(x.time, m)
            m.put(x.price, x)
        }
      }
    }
    map
  }

  def closedDistribuOf__filterByDB(sec: Sec): mutable.Map[Long, PriceCollection]= {
    seqToMap(
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
      ) ORDER_BY (this.time) list
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
        case Some(m) => m.isTransient = false; m.put(x.price, x)
        case None =>
          val m = new PriceCollection
          m.isTransient = false;
          map.put(x.time, m)
          m.put(x.price, x)
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

  def saveBatch(atSameTime: Long, pds: ArrayList[PriceCollection]) {
    if (pds.isEmpty) return

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

    pds.foreach{pc =>
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
