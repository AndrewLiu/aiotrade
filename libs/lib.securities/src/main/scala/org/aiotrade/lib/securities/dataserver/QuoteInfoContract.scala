/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.TFreq

class QuoteInfoContract extends DataContract[QuoteInfoDataServer] {

  serviceClassName = null
  freq = TFreq.ONE_MIN
  refreshable = true

  override def displayName = {
    "QuoteInfo Data Contract[" + srcSymbol + "]"
  }

  /**
   * @param none args are needed
   */
  override def createServiceInstance(args: Any*): Option[QuoteInfoDataServer] = {
    lookupServiceTemplate(classOf[QuoteInfoDataServer], "DataServers") match {
      case Some(x) => x.createNewInstance.asInstanceOf[Option[QuoteInfoDataServer]]
      case None => None
    }
  }
}
