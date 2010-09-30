package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.TFreq


class QuoteInfoHisContract extends DataContract[QuoteInfoHisDataServer] {
  serviceClassName = null
  freq = TFreq.DAILY
  refreshable = false

  override def displayName = {
    "QuoteInfo His Data Contract[" + srcSymbol + "]"
  }

  /**
   * @param none args are needed
   */
  override def createServiceInstance(args: Any*): Option[QuoteInfoHisDataServer] = {
    lookupServiceTemplate(classOf[QuoteInfoHisDataServer], "DataServers") match {
      case Some(x) => x.createNewInstance.asInstanceOf[Option[QuoteInfoHisDataServer]]
      case None => None
    }
  }
}
