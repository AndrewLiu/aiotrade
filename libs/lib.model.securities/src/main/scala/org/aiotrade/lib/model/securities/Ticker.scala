package org.aiotrade.lib.model.securities

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Ticker extends Table[Ticker] with LongIdPK[Ticker] {
  val intraDay = longColumn("intraDay_id").references(IntraDay)
  val time = longColumn("time")
  
  val prevClose   = numericColumn("prevClose",  12, 2)
  val latestPrice = numericColumn("lastPrice",  12, 2)

  val dayOpen    = numericColumn("dayOpen",    12, 2)
  val dayHigh    = numericColumn("dayHigh",    12, 2)
  val dayLow     = numericColumn("dayLow",     12, 2)
  val dayVolume  = numericColumn("dayVolume",  18, 2)
  val dayAmount  = numericColumn("dayAmount",  18, 2)

  val dayChange  = numericColumn("dayChange",  12, 2)

  val mfGrade  = intColumn("grade")
  val mfVolume = numericColumn("volume", 18, 2)
  val mfAmount = numericColumn("amount", 18, 2)

  /**
   * Array[Float](depth) writeObject
   */
  val bidAsks = binaryColumn("bidAsks", 200)
}

class Ticker extends Record[Ticker](Ticker) {
  val id = field(Ticker.id)
  val intraDay = manyToOne(Ticker.intraDay)
  val time = field(Ticker.time)

  val prevClose    = field(Ticker.prevClose)
  val latestPrice  = field(Ticker.latestPrice)

  val dayOpen   = field(Ticker.dayOpen)
  val dayHigh   = field(Ticker.dayHigh)
  val dayLow    = field(Ticker.dayLow)
  val dayVolume = field(Ticker.dayVolume)
  val dayAmount = field(Ticker.dayAmount)

  val dayChange = field(Ticker.dayChange)

  /**
   * Money flow
   * 1 - super, 2 - grand, 3 - small
   */
  val mfGrade  = field(Ticker.mfGrade)
  val mfVolume = field(Ticker.mfVolume)
  val mfAmount = field(Ticker.mfAmount)

  /**
   * Array[Float](depth) readObject
   */
  val bidAsks = field(Ticker.bidAsks)


  // ----- helpers
  def setBidAsks(values: Array[Float]) {
    val baos = new ByteArrayOutputStream
    val dos = new ObjectOutputStream(baos)
    try {
      dos.writeObject(values)
    } catch {case ioe: IOException =>}

    bidAsks := baos.toByteArray
  }

  def getBidAsks: Array[Float] = {
    bidAsks.get match {
      case Some(bytes) =>
        val bais = new ByteArrayInputStream(bytes)
        val dis = new ObjectInputStream(bais)
        try {
          dis.readObject.asInstanceOf[Array[Float]]
        } catch {case ioe: IOException => Array[Float]()}
      case None => Array[Float]()
    }
  }

}


/**
 * Assume table BidAsk's structure:
 val idx = intColumn("idx")
 val isBid = booleanColumn("isBid")
 val price = numericColumn("price",  12, 2)
 val size = numericColumn("size", 12, 2)

 // Select latest ask_bid in each group of "isBid" and "idx"
 def latest = {
 "SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx)"
 }

 def latest(intraDayId: Long) = {
 "SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx AND intraDay_id = " + intraDayId + ") AND intraDay_id = " + intraDayId
 }

 */