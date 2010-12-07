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
import scala.collection.mutable.HashMap


class SymbolSearchProvider extends SearchProvider {
  private val log = Logger.getLogger(this.getClass.getName)

  private val url = "http://finance.yahoo.com/q?s="

  private val textToSymbols = new HashMap[String, List[String]]

  initMap
  
  private def initMap {
    for ((symbol, sec) <- Exchange.uniSymbolToSec) addSec(symbol, sec)
    log.fine("Search map: " + textToSymbols)
  }

  def addSec(symbol: String, sec: Sec) {
    textToSymbols.put(symbol.toUpperCase, List(symbol))
    PinYin.getFirstSpells(sec.name) foreach {spell =>
      textToSymbols.get(spell) match {
        case Some(xs) => textToSymbols.put(spell, symbol :: xs)
        case None => textToSymbols.put(spell, List(symbol))
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
    for ((text, symbols) <- textToSymbols if text.startsWith(input);
         symbol <- symbols
    ) {
      val name = Exchange.secOf(symbol) match {
        case Some(x) => symbol + " (" + x.name + ")"
        case _ => symbol
      }
      
      if (!response.addResult(new FoundResult(symbol), name)) return
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
