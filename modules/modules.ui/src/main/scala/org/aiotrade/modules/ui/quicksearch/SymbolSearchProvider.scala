/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.modules.ui.quicksearch

import java.net.MalformedURLException
import java.net.URL
import java.util.logging.Logger
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.pinyin.PinYin
import org.aiotrade.lib.util.swing.action.ViewAction
import org.aiotrade.modules.ui.nodes.SymbolNodes
import org.aiotrade.spi.quicksearch.SearchProvider
import org.aiotrade.spi.quicksearch.SearchRequest
import org.aiotrade.spi.quicksearch.SearchResponse
import org.openide.awt.HtmlBrowser.URLDisplayer
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashMap


class SymbolSearchProvider extends SearchProvider {
  private val log = Logger.getLogger(this.getClass.getName)

  private val url = "http://finance.yahoo.com/q?s="

  private val textToSecs = new HashMap[String, Set[Sec]]

  initMap
  
  private def initMap {
    for ((symbol, sec) <- Exchange.uniSymbolToSec) addSec(symbol, sec)
    log.fine("Search map: " + textToSecs)
  }

  def addSec(symbol: String, sec: Sec) {
    textToSecs.put(symbol.toUpperCase, Set(sec))
    PinYin.getFirstSpells(sec.name) foreach {spell =>
      textToSecs.get(spell) match {
        case Some(xs) => textToSecs.put(spell, xs + sec)
        case None => textToSecs.put(spell, Set(sec))
      }
    }
  }

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
    val input = request.text.toUpperCase
    var addedSecs = TreeSet[Sec]()

    for ((text, secs) <- textToSecs if text.startsWith(input); sec <- secs) {
      addedSecs += sec
    }

    addedSecs foreach {sec =>
      val uniSymbol = sec.uniSymbol
      val name = uniSymbol + " (" + sec.name + ")"
      if (!response.addResult(new FoundResult(uniSymbol), name)) return
    }
  }

  private class FoundResult(symbol: String) extends Runnable {
    def run {
      try {
        SymbolNodes.findSymbolNode(symbol) match {
          case Some(x) => x.getLookup.lookup(classOf[ViewAction]).execute
          case None => URLDisplayer.getDefault.showURL(new URL(url + symbol))
        }
      } catch {case ex: MalformedURLException =>}
    }
  }

}
