package org.aiotrade.lib.model.securities

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import ru.circumflex.orm.Table

object Ticker extends Table[Ticker] {
  val quote = "quote_id" REFERENCES(Quote1d)

  val time = "time" BIGINT

  val prevClose   = "prevClose"    FLOAT(12, 2)
  val latestPrice = "lastestPrice" FLOAT(12, 2)

  val dayOpen   = "dayOprn"   FLOAT(12, 2)
  val dayHigh   = "dayHigh"   FLOAT(12, 2)
  val dayLow    = "dayLow"    FLOAT(12, 2)
  val dayVolume = "dayVolume" FLOAT(12, 2)
  val dayAmount = "dayAmount" FLOAT(12, 2)

  val dayChange = "dayChange" FLOAT(12, 2)

  val bidAsks = "bidAsks" VARBINARY(200)
}

class Ticker {
  var quote: Quote = _
  
  var time: Long = _

  var prevClose: Float  = _
  var latestPrice: Float  = _

  var dayOpen   : Float  = _
  var dayHigh   : Float  = _
  var dayLow    : Float  = _
  var dayVolume : Float  = _
  var dayAmount : Float  = _

  var dayChange : Float  = _

  /**
   * Array[Float](depth) readObject
   */
  var bidAsks: Array[Byte] = Array()

  // ----- helpers
  
  def encodeBidAsks(values: Array[Float]) = {
    val baos = new ByteArrayOutputStream
    val dos = new ObjectOutputStream(baos)
    try {
      dos.writeObject(values)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  def decodeBidAsks: Array[Float] = {
    val bais = new ByteArrayInputStream(bidAsks)
    val dis = new ObjectInputStream(bais)
    try {
      dis.readObject.asInstanceOf[Array[Float]]
    } catch {case ioe: IOException => Array[Float]()}
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