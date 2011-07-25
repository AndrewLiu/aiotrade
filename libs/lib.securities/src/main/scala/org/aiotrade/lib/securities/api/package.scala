package org.aiotrade.lib.securities

import org.aiotrade.lib.avro.Evt
import org.aiotrade.lib.securities.dataserver.DepthSnap
import org.aiotrade.lib.securities.model.Execution
import org.aiotrade.lib.securities.model.MoneyFlow
import org.aiotrade.lib.securities.model.PriceCollection
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.SecDividend
import org.aiotrade.lib.securities.model.Ticker

package object api {

  // snapshot evts
  val TickerEvt     = Evt[Ticker](0, "ticker")
  val TickersEvt    = Evt[Array[Ticker]](1, "tickers")
  val ExecutionEvt  = Evt[(Double, Execution)](2, "prevClose, execution")
  val DepthSnapsEvt = Evt[Array[DepthSnap]](3)
  val DelimiterEvt  = Evt[Unit](9, "A delimiter to notice batch tickers got.")

  
  // update evts
  val QuoteEvt      = Evt[(String, Quote)](10, "freq, quote")
  val QuotesEvt     = Evt[(String, Array[Quote])](11)
  val MoneyFlowEvt  = Evt[(String, MoneyFlow)](20, "freq, moneyflow")
  val MoneyFlowsEvt = Evt[(String, Array[MoneyFlow])](21)
  val PriceDistributionEvt = Evt[PriceCollection](30)
  val PriceDistributionsEvt = Evt[Array[PriceCollection]](31)

  // ---
  val SecDividendAddedEvt   = Evt[SecDividend](6000)
  val SecDividendUpdatedEvt = Evt[SecDividend](6001)
  val SecDividendDeletedEvt = Evt[SecDividend](6002)
  
  
  /** Refer this last method to load this object and all vals in object */
  def load() = true 
}
