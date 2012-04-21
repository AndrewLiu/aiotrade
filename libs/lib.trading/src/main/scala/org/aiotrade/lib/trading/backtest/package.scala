package org.aiotrade.lib.trading

import org.aiotrade.lib.util.actors.Publisher

/**
 * @author Caoyuan Deng
 */
package backtest {
  trait Param extends Publisher {
    /** Used in the image title */
    def titleDescription: String = toString
    /** Used in the image file name */
    def shortDescription: String = toString
  }
  
  object NoParam extends Param {
    override val shortDescription = ""
    override def toString = "P()"
  }
  
  case object RemoveTabs
  case class AddTab(param: Param)

  case class ReportData(name: String, id: Int, time: Long, value: Double)
}