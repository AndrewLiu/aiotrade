package org.aiotrade.lib


package object backtest {
  trait Param {
    def shortDescription: String
  }
  
  object NoParam extends Param {
    val shortDescription = ""
  }
  
  case class RoundStarted(params: Param)
  case class RoundFinished(params: Param)

  case class ReportData(name: String, id: Int, time: Long, value: Double)
}