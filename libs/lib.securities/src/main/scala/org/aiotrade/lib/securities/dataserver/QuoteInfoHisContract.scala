package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.TFreq


class QuoteInfoHisContract extends DataContract[QuoteInfoHisDataServer] {
  serviceClassName = null
  freq = TFreq.DAILY
  isRefreshable = false

  override def displayName = {
    "QuoteInfo His Data Contract[" + srcSymbol + "]"
  }

}
