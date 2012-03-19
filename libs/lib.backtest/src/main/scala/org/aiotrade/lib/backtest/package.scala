package org.aiotrade.lib


package object backtest {
  trait Param {
    def shortDescription: String
  }
  
  case class RoundStarted(params: Param)
  case class RoundFinished(params: Param)

  case class ReportData(name: String, id: Int, time: Long, value: Double)
}