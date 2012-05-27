package org.aiotrade.lib.trading.backtest

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import org.aiotrade.lib.math.indicator.SignalIndicator
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.trading.Account
import org.aiotrade.lib.trading.BaseTradingService
import org.aiotrade.lib.trading.Broker
import org.aiotrade.lib.trading.Param
import org.aiotrade.lib.trading.SecPicking
import org.aiotrade.lib.trading.StockAccount
import org.aiotrade.lib.trading.TradingRule
import org.aiotrade.lib.trading.Trigger
import org.aiotrade.lib.util.ValidTime
import scala.collection.mutable
import scala.concurrent.SyncVar

class TradingService(_broker: Broker, _accounts: List[Account], _param: Param,
                     _referSer: securities.QuoteSer, _secPicking: SecPicking, _signalIndTemplates: SignalIndicator*)
extends BaseTradingService(_broker, _accounts, _param, _referSer, _secPicking, _signalIndTemplates: _*) {

  private case class Go(fromTime: Long, toTime: Long)
  private val done = new SyncVar[Boolean]()
  
  reactions += {
    case Go(fromTime, toTime) => doGo(fromTime, toTime)
  }

  /**
   * Main entrance for outside caller.
   * 
   * @Note we use publish(Go) to make sure doGo(...) happens only after all signals 
   *       were published (during initSignalIndicators).
   */ 
  def go(fromTime: Long, toTime: Long) {
    initSignalIndicators
    publish(Go(fromTime, toTime))
    // We should make this calling synchronized, so block here untill done
    done.get
  }
  
  private def doGo(fromTime: Long, toTime: Long) {
    val fromIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
    val toIdx = timestamps.indexOfNearestOccurredTimeBefore(toTime)
    println("Backtest from %s to %s, referIdx: from %s to %s, total referPeriods: %s".format(new Date(timestamps(fromIdx)), new Date(timestamps(toIdx)), fromIdx, toIdx, timestamps.length))
    
    var i = fromIdx
    while (i <= toIdx) {
      go(i)

      i += 1
    }
    
    // release resources. @Todo any better way? We cannot guarrantee that only backtesing is using Function.idToFunctions
    deafTo(Signal)
    done.set(true)
    org.aiotrade.lib.math.indicator.Function.releaseAll
  }
}


/**
 * An example of backtest trading service
 * 
 * @author Caoyuan Deng
 */
object TradingService {
  
  def createIndicator[T <: SignalIndicator](signalClass: Class[T], factors: Array[Double]): T = {
    val ind = signalClass.newInstance.asInstanceOf[T]
    ind.factorValues = factors
    ind
  }
  
  private def init = {
    val category = "008011"
    val CSI300Code = "399300.SZ"
    val secs = securities.getSecsOfSector(category, CSI300Code)
    val referSec = Exchange.secOf("000001.SS").get
    val referSer = securities.loadSers(secs, referSec, TFreq.DAILY)
    val goodSecs = secs filter {_.serOf(TFreq.DAILY).get.size > 0}
    println("Number of good secs: " + goodSecs.length)
    (goodSecs, referSer)
  }

  /**
   * Simple test
   */
  def main(args: Array[String]) {
    import org.aiotrade.lib.indicator.basic.signal._

    case class TestParam(faster: Int, slow: Int, signal: Int) extends Param {
      override def shortDescription = List(faster, slow, signal).mkString("_")
    }
    
    val df = new SimpleDateFormat("yyyy.MM.dd")
    val fromTime = df.parse("2011.04.03").getTime
    val toTime = df.parse("2012.04.03").getTime
    
    val imageFileDir = System.getProperty("user.home") + File.separator + "backtest"
    val chartReport = new ChartReport(imageFileDir)
    
    val (secs, referSer) = init
    
    val secPicking = new SecPicking()
    secPicking ++= secs map (ValidTime(_, 0, 0))
    
    for {
      fasterPeriod <- List(5, 8, 12)
      slowPeriod <- List(26, 30, 55) if slowPeriod > fasterPeriod
      signalPeriod <- List(5, 9)
      param = TestParam(fasterPeriod, slowPeriod, signalPeriod)
    } {
      val broker = new PaperBroker("Backtest")
      val tradingRule = new TradingRule()
      val account = new StockAccount("Backtest", 10000000.0, tradingRule)
    
      val indTemplate = createIndicator(classOf[MACDSignal], Array(fasterPeriod, slowPeriod, signalPeriod))
    
      val tradingService = new TradingService(broker, List(account), param, referSer, secPicking, indTemplate) {
        override 
        def at(idx: Int) {
          val triggers = scanTriggers(idx)
          for (Trigger(sec, position, triggerTime, side) <- triggers) {
            side match {
              case Side.EnterLong =>
                buy (sec) after (1)
              
              case Side.ExitLong =>
                sell (sec) after (1)
              
              case Side.CutLoss => 
                sell (sec) quantity (position.quantity) after (1)
              
              case Side.TakeProfit =>
                sell (sec) quantity (position.quantity) after (1)
              
              case _ =>
            }
          }
        }
      }
    
      chartReport.roundStarted(List(param))
      tradingService.go(fromTime, toTime)
      chartReport.roundFinished
      System.gc
    }
    
    println("Done!")
  }
}
