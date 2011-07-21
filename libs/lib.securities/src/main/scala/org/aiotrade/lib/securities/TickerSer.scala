package org.aiotrade.lib.securities

import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.math.timeseries.{TVal, TSerEvent, DefaultBaseTSer, TFreq}
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.collection.ArrayList

class TickerSer($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {
  
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

  //Consuct a type to store the price -> size
  val priceToSize = collection.mutable.Map[Double, Int]()

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    
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
