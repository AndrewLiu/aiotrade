package org.aiotrade.lib.trading

import java.util.Calendar
import java.util.Date
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq

/**
 * 
 * @author Caoyuan Deng
 */
class Benchmark(freq: TFreq) {
  case class Profit(time: Long, nav: Double, accRate: Double, periodRate: Double, riskFreeRate: Double) {
    val periodRateForSharpe = periodRate - riskFreeRate
    
    override 
    def toString = {
      "%1$tY.%1$tm.%1$td \t %2$ 8.2f \t %3$ 8.2f%% \t %4$ 8.2f%% \t %5$ 8.2f%% \t %6$ 8.2f%%".format(
        new Date(time), nav, accRate * 100, periodRate * 100, riskFreeRate * 100, periodRateForSharpe * 100
      )
    }
  }
  
  var tradeCount: Int = _
  var tradeFromTime: Long = Long.MinValue
  var tradeToTime: Long = Long.MinValue
  var tradePeriod: Int = _
  
  var times = new ArrayList[Long]
  var equities = new ArrayList[Double]()
  
  var initialEquity = Double.NaN
  var profitRatio = 0.0
  var annualizedProfitRatio = 0.0
  private var lastEquity = 0.0
  private var maxEquity = Double.MinValue
  private var maxDrawdownEquity = Double.MaxValue
  var maxDrawdownRatio = Double.MinValue
  
  var weeklyProfits: Array[Profit] = Array()
  var monthlyProfits: Array[Profit] = Array()
  var rrr: Double = _
  var sharpeRatioOnWeeks: Double = _
  var sharpeRatioOnMonths: Double = _
  
  var weeklyRiskFreeRate = 0.0 // 0.003
  var monthlyRiskFreeRate = 0.0 // 0.003
  
  var reportDayOfWeek = Calendar.SATURDAY
  var reportDayOfMonth = 26
  
  def at(time: Long, equity: Double) {
    times += time
    equities += equity
    
    if (tradeFromTime == Long.MinValue) tradeFromTime = time
    tradeToTime = time
    
    if (initialEquity.isNaN) initialEquity = equity

    lastEquity = equity
    profitRatio = equity / initialEquity - 1
    calcMaxDropdown(equity)
  }
  
  def report: String = {
    tradePeriod = daysBetween(tradeFromTime, tradeToTime)
    annualizedProfitRatio = math.pow(1 + profitRatio, 365.24 / tradePeriod) - 1
    rrr = annualizedProfitRatio / maxDrawdownRatio
    
    val navs = toNavs(initialEquity, equities)
    weeklyProfits = calcPeriodicReturns(times.toArray, navs)(getWeeklyReportTime)(weeklyRiskFreeRate)
    monthlyProfits = calcPeriodicReturns(times.toArray, navs)(getMonthlyReportTime)(monthlyRiskFreeRate)
    sharpeRatioOnWeeks = math.sqrt(52) * calcSharpeRatio(weeklyProfits)
    sharpeRatioOnMonths = math.sqrt(12) * calcSharpeRatio(monthlyProfits)
    
    toString
  }
  
  private def calcMaxDropdown(equity: Double) {
    if (equity > maxEquity) {
      maxEquity = math.max(equity, maxEquity)
      maxDrawdownEquity = equity
    } else if (equity < maxEquity) {
      maxDrawdownEquity = math.min(equity, maxDrawdownEquity)
      maxDrawdownRatio = math.max((maxEquity - maxDrawdownEquity) / maxEquity, maxDrawdownRatio)
    }
  }
  
  private def calcSharpeRatio(xs: Array[Profit]) = {
    if (xs.length > 0) {
      var sum = 0.0
      var i = 0
      while (i < xs.length) {
        val x = xs(i)
        sum += x.periodRateForSharpe
        i += 1
      }
      val average = sum / xs.length
      var devSum = 0.0
      i = 0
      while (i < xs.length) {
        val x = xs(i).periodRateForSharpe - average
        devSum += x * x
        i += 1
      }
      val stdDev = math.sqrt(devSum / xs.length)
      average / stdDev
    } else {
      0.0
    }
  }
  
  private def toNavs(initalEquity: Double, equities: ArrayList[Double]) = {
    val navs = Array.ofDim[Double](equities.length)
    var i = 0
    while (i < equities.length) {
      navs(i) = equities(i) / initialEquity
      i += 1
    }
    navs
  }
  
  /**
   * navs Net Asset Value
   */
  private def calcPeriodicReturns(times: Array[Long], navs: Array[Double])(getPeriodicReportTimeFun: (Calendar, Int) => Long)(riskFreeRate: Double) = {
    val reportTimes = new ArrayList[Long]()
    val reportNavs = new ArrayList[Double]()
    val now = Calendar.getInstance
    var prevNav = 1.0
    var prevTime = 0L
    var reportTime = Long.MaxValue
    var i = 0
    while (i < times.length) {
      val time = times(i)
      val nav = navs(i)
      now.setTimeInMillis(time)
      if (reportTime == Long.MaxValue) {
        reportTime = getPeriodicReportTimeFun(now, reportDayOfWeek)
      }
      
      var reported = false
      if (time > reportTime) {
        if (prevTime <= reportTime) {
          reportTimes += reportTime
          reportNavs += prevNav
          reported = true
        }
        reportTime = getPeriodicReportTimeFun(now, reportDayOfWeek)
      }
      
      if (!reported && i == times.length - 1) {
        reportTimes += getPeriodicReportTimeFun(now, reportDayOfWeek)
        reportNavs += nav
      }
      
      prevTime = time
      prevNav = nav
      i += 1
    }
    
    if (reportTimes.length > 0) {
      val profits = new ArrayList[Profit]
 
      var i = 0
      while (i < reportTimes.length) {
        val time = reportTimes(i)
        val nav = reportNavs(i)
        val accRate = nav - 1
        val periodRate = if (i > 0) nav / profits(i - 1).nav - 1 else nav / 1.0 - 1
        profits += Profit(time, nav, accRate, periodRate, riskFreeRate)
        i += 1
      }
      
      profits.toArray
    } else {
      Array[Profit]()
    }
  } 
  
  /**
   * @param the current date calendar
   * @param the day of month of settlement, last day of each month if -1 
   */
  private def getWeeklyReportTime(now: Calendar, _reportDayOfWeek: Int = -1) = {
    val reportDayOfWeek = if (_reportDayOfWeek == -1) {
      Calendar.SATURDAY
    } else _reportDayOfWeek
    
    if (now.get(Calendar.DAY_OF_WEEK) > reportDayOfWeek) {
      now.add(Calendar.WEEK_OF_YEAR, 1) // will report in next week
    }
    
    now.set(Calendar.DAY_OF_WEEK, reportDayOfWeek)
    now.getTimeInMillis
  }

  /**
   * @param the current date calendar
   * @param the day of month of settlement, last day of each month if -1 
   */
  private def getMonthlyReportTime(now: Calendar, _reportDayOfMonth: Int = -1) = {
    val reportDayOfMonth = if (_reportDayOfMonth == -1) {
      now.getActualMaximum(Calendar.DAY_OF_MONTH)
    } else _reportDayOfMonth
    
    if (now.get(Calendar.DAY_OF_MONTH) > reportDayOfMonth) {
      now.add(Calendar.MONTH, 1) // will report in next month
    }
    
    now.set(Calendar.DAY_OF_MONTH, reportDayOfMonth)
    now.getTimeInMillis
  }

  override 
  def toString = {
    val aboutWeekly  = aboutPeriodRate(weeklyProfits)
    val aboutMonthly = aboutPeriodRate(monthlyProfits)
    ;
    """
================ Benchmark Report ================
Trade period           : %1$tY.%1$tm.%1$td --- %2$tY.%2$tm.%2$td (%3$s calendar days, %4$s trading periods)
Initial equity         : %5$.0f
Final equity           : %6$.0f  
Total Return           : %7$.2f%%
Annualized Return      : %8$.2f%% 
Max Drawdown           : %9$.2f%%
RRR                    : %10$5.2f
Sharpe Ratio on Weeks  : %11$5.2f  (%12$s weeks)
Sharpe Ratio on Months : %13$5.2f  (%14$s months)

================ Weekly Return ================
Date                  nav       acc-return   period-return       riskfree    sharpe-return
%15$s
Average: %16$ 5.2f%%  Stdev: %17$ 5.2f%%  Win: %18$5.2f%%  Loss: %19$5.2f%%  Tie: %20$5.2f%%

================ Monthly Return ================
Date                  nav       acc-return   period-return       rf-return   sharpe-return
%21$s
Average: %22$ 5.2f%%  Stdev: %23$ 5.2f%%  Win: %24$5.2f%%  Loss: %25$5.2f%%  Tie: %26$5.2f%%
    """.format(
      tradeFromTime, tradeToTime, tradePeriod, times.length,
      initialEquity,
      lastEquity,
      profitRatio * 100,
      annualizedProfitRatio * 100,
      maxDrawdownRatio * 100,
      rrr,
      sharpeRatioOnWeeks, weeklyProfits.length,
      sharpeRatioOnMonths, monthlyProfits.length,
      weeklyProfits.mkString("\n"),
      aboutWeekly._1, aboutWeekly._2, aboutWeekly._3, aboutWeekly._4, aboutWeekly._5,
      monthlyProfits.mkString("\n"),
      aboutMonthly._1, aboutMonthly._2, aboutMonthly._3, aboutMonthly._4, aboutMonthly._5
    )
  }
  
  private def aboutPeriodRate(profits: Array[Profit]) = {
    val len = profits.length.toDouble
    var sum = 0.0
    var win = 0
    var loss = 0
    var tie = 0
    var i = 0
    while (i < len) {
      val periodRate = profits(i).periodRate
      sum += periodRate
      if (periodRate > 0) win += 1 
      else if (periodRate < 0) loss += 1 
      else tie += 1
      i += 1
    }
    
    val average = if (len > 0) sum / len else 0.0
    var devSum = 0.0
    i = 0
    while (i < len) {
      val x = profits(i).periodRate - average
      devSum += x * x
      i += 1
    }
    val stdDev = math.sqrt(devSum / len)

    if (len > 0) {
      (average * 100, stdDev * 100, win / len * 100, loss / len * 100, tie / len * 100)
    } else {
      (0.0, 0.0, 0.0, 0.0, 0.0)
    }
  }
  
  /**
   * @Note time2 > time1
   */
  private def daysBetween(time1: Long, time2: Long): Int = {
    val cal1 = Calendar.getInstance
    val cal2 = Calendar.getInstance
    cal1.setTimeInMillis(time1)
    cal2.setTimeInMillis(time2)
    
    val years = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR)
    var days = cal2.get(Calendar.DAY_OF_YEAR) - cal1.get(Calendar.DAY_OF_YEAR)
    var i = 0
    while (i < years) {
      cal1.set(Calendar.YEAR, cal1.get(Calendar.YEAR) + 1)
      days += cal1.getActualMaximum(Calendar.DAY_OF_YEAR)
      i += 1
    }
    days
  }
}

