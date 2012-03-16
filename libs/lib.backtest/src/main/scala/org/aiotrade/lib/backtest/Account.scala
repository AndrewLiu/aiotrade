package org.aiotrade.lib.backtest

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.trading.Portfolio
import org.aiotrade.lib.util.actors.Publisher
import scala.collection.mutable

/**
 * 
 * @author Caoyuan Deng
 */
class Account(name: String, numPortfolios: Int) extends Publisher {
  case class TimedProfit(time: Long, profit: Double)

  private val reportDatas = new ArrayList[ReportData]()

  var referProfit = 1.0
  val continuousProfit = Array.fill(numPortfolios){1.0}
  val continuousProfits = Array.fill(numPortfolios){new ArrayList[TimedProfit]}
    
  def process(time: Long, portfolios: Array[Portfolio], referProfitRatio: Double, isUnderShort: Boolean): Array[ReportData] = {
    reportDatas.clear

    referProfit *= (1 + referProfitRatio)
    println("=== %s, referProfit % 3.2f%%, delta % 3.2f%%".format(name, referProfit * 100, referProfitRatio * 100))
    
    reportDatas += ReportData("Refer", 0, time, referProfit * 100 - 100)
    
    var i = 0
    while (i < portfolios.length) {
      val portfolio = portfolios(i)

      val stockProfitRatio = if (portfolio.profit.isNaN) 0.0 else portfolio.profit 
      val futureProfitRatio = referProfitRatio
      val newProfitRatio = if (isUnderShort) -futureProfitRatio else stockProfitRatio
      val newProfit = continuousProfit(i) * (1 + newProfitRatio)
          
      if (!newProfit.isNaN) {
        continuousProfit(i) = newProfit
        continuousProfits(i) += TimedProfit(time, newProfit)
        reportDatas += ReportData(name, i, time, newProfit * 100)
      }
      print("%s profit % 3.2f%%, delta % 3.2f%% -- ".format(i, newProfit * 100, newProfitRatio * 100))
      println(portfolio)

      i += 1
    }
    
    // returns immutable reportDatas, so we can reuse reportDatas in parallel safely.
    reportDatas.toArray
  }

  def reportAll {
    println("=== " + name + " ===")
    var i = 0
    while (i < continuousProfits.length) {
      val profits = continuousProfits(i)
      println(i)
      println(profits.map(_.time).mkString("(", ",", ")"))
      println(profits.map(_.profit).mkString("(", ",", ")"))
      i += 1
    }
  }
}
