package org.aiotrade.lib.backtest

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.trading.Portfolio
import scala.collection.mutable

/**
 * 
 * @author Caoyuan Deng
 */
class Account(name: String, numPortfolios: Int) {
  case class TimedProfit(time: Long, profit: Double)

  private var reportDatas: List[ReportData] = Nil

  var referProfit = 1.0
  val continuousProfit = Array.fill(numPortfolios){1.0}
  val continuousProfits = Array.fill(numPortfolios){new ArrayList[TimedProfit]}
    
  def process(time: Long, portfolios: Array[Portfolio], referProfitRatio: Double): List[ReportData] = {
    referProfit *= (1 + referProfitRatio)
    println("=== %s, referProfit % 3.2f%%, delta % 3.2f%%".format(name, referProfit * 100, referProfitRatio * 100))
    
    reportDatas = Nil
    
    var i = 0
    while (i < portfolios.length) {
      val portfolio = portfolios(i)

      val periodProfitRatio = if (portfolio.profit.isNaN) 0.0 else portfolio.profit 
      val newProfit = continuousProfit(i) * (1 + periodProfitRatio)
          
      val arbitragerProfit = newProfit - referProfit
      
      if (!newProfit.isNaN) {
        continuousProfit(i) = newProfit
        continuousProfits(i) += TimedProfit(time, newProfit)
        reportDatas ::= ReportData(name, i, time, arbitragerProfit * 100)
      }
      print("%s profit % 3.2f%%, wins % 3.2f%% -- ".format(i, newProfit * 100, arbitragerProfit * 100))
      println(portfolio)

      i += 1
    }
    
    reportDatas
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
