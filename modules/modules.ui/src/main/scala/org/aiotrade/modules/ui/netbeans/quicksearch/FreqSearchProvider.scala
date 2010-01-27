/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.modules.ui.netbeans.quicksearch

import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.modules.ui.netbeans.windows.AnalysisChartTopComponent
import org.aiotrade.modules.ui.netbeans.windows.RealTimeChartTopComponent
import org.aiotrade.spi.quicksearch.SearchProvider
import org.aiotrade.spi.quicksearch.SearchRequest
import org.aiotrade.spi.quicksearch.SearchResponse


class FreqSearchProvider extends SearchProvider {

  private val freqs = Array(TFreq.ONE_MIN, TFreq.DAILY, TFreq.WEEKLY, TFreq.MONTHLY, TFreq.ONE_YEAR)
  private val nameTofreq = freqs map (x => (x.shortDescription -> x)) toMap

  /**
   * Method is called by infrastructure when search operation was requested.
   * Implementors should evaluate given request and fill response object with
   * apropriate results
   *
   * @param request Search request object that contains information what to search for
   * @param response Search response object that stores search results.
   *    Note that it's important to react to return value of SearchResponse.addResult(...) method
   *    and stop computation if false value is returned.
   */
  def evaluate(request: SearchRequest, response: SearchResponse) {

    for ((name, freq) <- nameTofreq if name.toLowerCase.startsWith(request.text.toLowerCase)) {
      if (!response.addResult(new FoundResult(freq), name)) {
        return
      }
    }
  }

  private class FoundResult(freq: TFreq) extends Runnable {

    def run {
      val analysisTc_? = AnalysisChartTopComponent.selected
      val realtimeTc_? = RealTimeChartTopComponent.selected

      val contents = analysisTc_? match {
        case Some(x) => x.contents
        case None => realtimeTc_? match {
            case Some(x) => x.contents
            case None => return
          }
      }

      val tc = freq match {
        case TFreq.ONE_MIN => RealTimeChartTopComponent(contents)
        case TFreq.DAILY   => AnalysisChartTopComponent(contents)
        case _ => return // @Todo
      }
      tc.requestActive
    }
  }

}
