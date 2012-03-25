package org.aiotrade.lib.trading

import org.aiotrade.lib.util.actors.Publisher

/**
 * @author Caoyuan Deng
 */
package backtest {
  trait Param {
    def shortDescription: String = toString
  }
  
  object NoParam extends Param {
    override val shortDescription = ""
    override def toString = "P()"
  }
  
  case class RoundStarted(params: Param)
  case class RoundFinished(params: Param)

  case class ReportData(name: String, id: Int, time: Long, value: Double)
  
  object ReportPublisher extends Publisher
}