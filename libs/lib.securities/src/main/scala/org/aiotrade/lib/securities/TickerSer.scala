package org.aiotrade.lib.securities

import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.math.timeseries.{TVal, TSerEvent, DefaultBaseTSer, TFreq}
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.collection.ArrayList

/**
 * @author Guibin Zhang
 */
class TickerSer($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {

  val config = org.aiotrade.lib.util.config.Config()
  val openHisAccumBidAsk = config.getBool("dataserver.openHisAccumBidAsk", false)

  private var _shortDescription: String = "TickerSer." + $sec.uniSymbol
  var adjusted: Boolean = false

  var dayOpen: Double = 0D
  val dayHigh = TVar[Double]("DH", Plot.None)
  val dayLow = TVar[Double]("DL", Plot.None)
  val dayVolume = TVar[Double]("DV", Plot.None)
  val dayAmount = TVar[Double]("DM", Plot.None)

  val prevClose = TVar[Double]("PC", Plot.None)
  val lastPrice = TVar[Double]("LP", Plot.Line)
  val dayChange = TVar[Double]("DC", Plot.None)
  val bidPrice = TVar[Array[Double]]("BP", Plot.None)
  val bidSize = TVar[Array[Double]]("BS", Plot.None)
  val askPrice = TVar[Array[Double]]("AP", Plot.None)
  val askSize = TVar[Array[Double]]("AS", Plot.None)
  val bidOrders = TVar[ArrayList[Double]]("BO", Plot.None)
  val askOrders = TVar[ArrayList[Double]]("AO", Plot.None)

  val hisBids = new HisAccumPriceVolume
  val hisAsks = new HisAccumPriceVolume

  override def clear(frTime: Long) = {
    super.clear(frTime)
  }

  def clearHisAccumBidAdk = {
    //@TODO recompute the hisBids & hisAsks
    hisBids.clear
    hisAsks.clear
  }

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    val bidsToBePub = scala.collection.mutable.ListBuffer[HisAccumBid]()
    val asksToBePub = scala.collection.mutable.ListBuffer[HisAccumAsk]()

    tval match {
      case ticker: Ticker =>
        dayOpen = ticker.dayOpen
        dayHigh(time) = ticker.dayHigh
        dayLow(time) = ticker.dayLow
        dayVolume(time) = ticker.dayVolume
        dayAmount(time) = ticker.dayAmount

        prevClose(time) = ticker.prevClose
        lastPrice(time) = ticker.lastPrice
        dayChange(time) = ticker.dayChange

        val depth = ticker.depth
        if(depth > 0) {
          val bPrices = new Array[Double](depth)
          val bSizes = new Array[Double](depth)
          val aPrices = new Array[Double](depth)
          val aSizes = new Array[Double](depth)
          for(i <- 0 until depth) {
            bPrices(i) = ticker.bidPrice(i)
            bSizes(i) = ticker.bidSize(i)
            aPrices(i) = ticker.askPrice(i)
            aSizes(i) = ticker.askSize(i)
            
            if(openHisAccumBidAsk) {
              hisBids ++= (bPrices(i) -> bSizes(i))
              hisAsks ++= (aPrices(i) -> aSizes(i))

              bidsToBePub += HisAccumBid(bPrices(i), hisBids.volumeOf(bPrices(i)).get)
              asksToBePub += HisAccumAsk(aPrices(i), hisAsks.volumeOf(aPrices(i)).get)
            }
          }
          
          if(openHisAccumBidAsk) {
            if(bidsToBePub.size > 0) publish(bidsToBePub.toArray)
            if(asksToBePub.size > 0) publish(asksToBePub.toArray)
          }
          
          bidPrice(time) = bPrices
          bidSize(time) = bSizes
          askPrice(time) = aPrices
          askSize(time) = aSizes

          if(ticker.ordersExist_?) {
            bidOrders(time) = ticker.bidOrders
            askOrders(time) = ticker.askOrders
          }
        }
        
      case _ =>
    }
  }

  def valueOf(time: Long): Option[Ticker] = {
    if (exists(time)) {
      val ticker = new Ticker
      ticker.dayOpen = dayOpen
      ticker.dayHigh = dayHigh(time)
      ticker.dayLow = dayLow(time)
      ticker.dayVolume = dayVolume(time)
      ticker.dayAmount = dayAmount(time)

      ticker.prevClose = prevClose(time)
      ticker.lastPrice = lastPrice(time)
      ticker.dayChange = dayChange(time)

      if(bidPrice(time) != null && askPrice(time) != null) {
        val depth = bidPrice(time).length
        
        val bPrices = bidPrice(time)
        val bSizes = bidSize(time)
        val aPrices = askPrice(time)
        val aSizes = askSize(time)

        for(i <- 0 until depth) {
          ticker.setBidPrice(i, bPrices(i))
          ticker.setBidSize(i, bSizes(i))
          ticker.setAskPrice(i, aPrices(i))
          ticker.setAskSize(i, aSizes(i))
        }
      }

      if(bidOrders(time) != null && askOrders(time) != null) {
        ticker.bidOrders = bidOrders(time)
        ticker.askOrders = askOrders(time)
      }
      
      Some(ticker)
    } else None
  }

  

  def updateFrom(ticker: Ticker) {
    val time = ticker.time
    createOrClear(time)

    val bidsToBePub = scala.collection.mutable.ListBuffer[HisAccumBid]()
    val asksToBePub = scala.collection.mutable.ListBuffer[HisAccumAsk]()

    dayOpen = ticker.dayOpen
    dayHigh(time) = ticker.dayHigh
    dayLow(time) = ticker.dayLow
    dayVolume(time) = ticker.dayVolume
    dayAmount(time) = ticker.dayAmount

    prevClose(time) = ticker.prevClose
    lastPrice(time) = ticker.lastPrice
    dayChange(time) = ticker.dayChange

    val depth = ticker.depth
    if(depth > 0) {
      val bPrices = new Array[Double](depth)
      val bSizes = new Array[Double](depth)
      val aPrices = new Array[Double](depth)
      val aSizes = new Array[Double](depth)
      for(i <- 0 until depth) {
        bPrices(i) = ticker.bidPrice(i)
        bSizes(i) = ticker.bidSize(i)
        aPrices(i) = ticker.askPrice(i)
        aSizes(i) = ticker.askSize(i)

        if(openHisAccumBidAsk) {
          hisBids ++= (bPrices(i) -> bSizes(i))
          hisAsks ++= (aPrices(i) -> aSizes(i))

          bidsToBePub += HisAccumBid(bPrices(i), hisBids.volumeOf(bPrices(i)).get)
          asksToBePub += HisAccumAsk(aPrices(i), hisAsks.volumeOf(aPrices(i)).get)
        }
      }
      
      if(openHisAccumBidAsk) {
        if(bidsToBePub.size > 0) publish(bidsToBePub.toArray)
        if(asksToBePub.size > 0) publish(asksToBePub.toArray)
      }

      bidPrice(time) = bPrices
      bidSize(time) = bSizes
      askPrice(time) = aPrices
      askSize(time) = aSizes

      if(ticker.ordersExist_?) {
        bidOrders(time) = ticker.bidOrders
        askOrders(time) = ticker.askOrders
      }
    }

    publish(TSerEvent.Updated(this, $sec.uniSymbol, time, time))
  }
}


case class HisAccumBid(price: Double, volume: Double)
case class HisAccumAsk(price: Double, volume: Double)
class HisAccumPriceVolume {
  private val priceToVolume : java.util.NavigableMap[Double, Double] = new java.util.TreeMap[Double, Double]

  def clear = priceToVolume.clear

  /**
   * @param price -> volume
   */
  def ++= (pToV:(Double, Double)) {
    val volume = priceToVolume.get(pToV._1)
    if(volume > 0) priceToVolume.put(pToV._1, pToV._2 + volume)
    else priceToVolume.put(pToV._1, pToV._2)
  }

  def prices = priceToVolume.keySet

  def volumeOf(price: Double): Option[Double] = {
    val volume = priceToVolume.get(price)
    
    if(volume > 0) Some(volume)
    else None
  }

}