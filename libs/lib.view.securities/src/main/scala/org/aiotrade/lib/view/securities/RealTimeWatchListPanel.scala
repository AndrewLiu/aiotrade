/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.view.securities

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Comparator
import java.util.ResourceBundle
import java.util.logging.Logger
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.RowSorter
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.dataserver.TickersEvent
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.LightTicker
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

/**
 *
 * @author  Caoyuan Deng
 */
object RealTimeWatchListPanel {
  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.view.securities.Bundle")

  val orig_pgup = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP.toChar)   // Fn + UP   in mac
  val orig_pgdn = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN.toChar) // Fn + DOWN in mac
  val meta_pgup = KeyStroke.getKeyStroke(KeyEvent.VK_UP.toChar,   InputEvent.META_MASK)
  val meta_pgdn = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN.toChar, InputEvent.META_MASK)
}

import RealTimeWatchListPanel._
class RealTimeWatchListPanel extends JPanel with Reactor {
  private val log = Logger.getLogger(this.getClass.getName)

  private val SYMBOL     = "Symbol"
  private val TIME       = "Time"
  private val LAST_PRICE = "Last"
  private val DAY_VOLUME = "Volume"
  private val DAY_AMOUNT = "Amount"
  private val PREV_CLOSE = "PrevCls"
  private val DAY_CHANGE = "Change"
  private val PERCENT    = "Percent"
  private val DAY_HIGH   = "High"
  private val DAY_LOW    = "Low"
  private val DAY_OPEN   = "Open"

  private val colKeys = Array[String](
    SYMBOL,
    TIME,
    LAST_PRICE,
    DAY_VOLUME,
    DAY_AMOUNT,
    PREV_CLOSE,
    DAY_CHANGE,
    PERCENT,
    DAY_HIGH,
    DAY_LOW,
    DAY_OPEN
  )

  private val uniSymbols = new ArrayList[String]
  private val watchingSymbols = new HashSet[String] // symbols will list in this pael

  private class Info {
    val prevTicker = new Ticker
    val colKeyToColor = HashMap[String, Color]()
    for (key <- colKeys) {
      colKeyToColor(key) = LookFeel().nameColor
    }
  }
  private val symbolToInfo = new HashMap[String, Info]

  val table = new JTable
  private val model = new WatchListTableModel
  private val df = new SimpleDateFormat("HH:mm:ss")
  private val cal = Calendar.getInstance
  private val priceDf = new DecimalFormat("0.000")

  private val bgColorSelected = new Color(56, 86, 111)//new Color(24, 24, 24) //new Color(169, 178, 202)

  initTable

  val scrollPane = new JScrollPane
  scrollPane.setViewportView(table)
  scrollPane.setBackground(LookFeel().backgroundColor)
  scrollPane.setFocusable(true)

  scrollPane.registerKeyboardAction(scrollPane.getActionMap.get("scrollUp"),   "pageup",   meta_pgup, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  scrollPane.registerKeyboardAction(scrollPane.getActionMap.get("scrollDown"), "pagedown", meta_pgdn, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)

  //scrollPane.getVerticalScrollBar.setUI(new BasicScrollBarUI)

  setLayout(new BorderLayout)
  add(BorderLayout.CENTER, scrollPane)

  reactions += {
    case TickersEvent(tickers) => updateByTickers(tickers)
  }

  listenTo(TickerServer)

  /** forward focus to scrollPane, so it can response UP/DOWN key event etc */
  override def requestFocusInWindow: Boolean = {
    scrollPane.requestFocusInWindow
  }

  private def initTable {
    table.setFont(LookFeel().defaultFont)
    table.setModel(
      new DefaultTableModel(
        Array[Array[Object]](),
        Array[Object]()
      )
    )
    table.setModel(model)
    table.setDefaultRenderer(classOf[Object], new TrendSensitiveCellRenderer)
    table.setBackground(LookFeel().backgroundColor)
    table.setGridColor(LookFeel().backgroundColor)
    table.setFillsViewportHeight(true)
    val header = table.getTableHeader
    header.setDefaultRenderer(new TableHeaderRenderer)
    header.setReorderingAllowed(true)

    // --- sorter
    table.setAutoCreateRowSorter(false)
    val sorter = new TableRowSorter(model)
    val comparator = new Comparator[Object] {
      def compare(o1: Object, o2: Object): Int = {
        (o1, o2) match {
          case (s1: String, s2: String) =>
            if (s1 == "-" && s2 == "-") 0
            else if (s1 == "-") -1
            else if (s2 == "-") 1
            else {
              val idx1 = s1.indexOf('%')
              val s11 = if (idx1 > 0) s1.substring(0, idx1) else s1
              val idx2 = s2.indexOf('%')
              val s12 = if (idx2 > 0) s2.substring(0, idx2) else s2
              try {
                val d1 = s11.toDouble
                val d2 = s12.toDouble
                if (d1 > d2) 1 else if (d1 < d2) -1 else 0
              } catch {case _ => s1 compareTo s2}
            }
          case _ => 0
        }
      }
    }
    for (col <- 1 until model.getColumnCount) {
      sorter.setComparator(col, comparator)
    }
    // default sort order and precedence
    val sortKeys = new java.util.ArrayList[RowSorter.SortKey]
    sortKeys.add(new RowSorter.SortKey(colKeys.findIndexOf(_ == PERCENT), javax.swing.SortOrder.DESCENDING))
    sorter.setSortKeys(sortKeys)
    // @Note sorter.setSortsOnUpdates(true) almost work except that the cells behind sort key
    // of selected row doesn't refresh, TableRowSorter.sort manually

    table.setRowSorter(sorter)
  }

  class WatchListTableModel extends AbstractTableModel {
    private val colNames = {
      val names = new Array[String](colKeys.length)
      var i = 0
      while (i < colKeys.length) {
        val key = colKeys(i)
        names(i) = BUNDLE.getString(colKeys(i))
        i += 1
      }
      names
    }

    private val types = Array(
      classOf[String], classOf[String], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object]
    )

    def getRowCount: Int = uniSymbols.size
    def getColumnCount: Int = colNames.length

    def getValueAt(row: Int, col: Int): Object = {
      val symbol = uniSymbols(row)
      val exchange = Exchange.exchangeOf(symbol)
      
      TickerServer.uniSymbolToLastTicker.get(symbol) match {
        case Some(ticker) =>
          colKeys(col) match {
            case SYMBOL => symbol
            case TIME =>
              val tz = exchange.timeZone
              val cal = Calendar.getInstance(tz)
              cal.setTimeInMillis(ticker.time)
              df.setTimeZone(tz)
              df format cal.getTime
            case LAST_PRICE => priceDf   format ticker.lastPrice
            case DAY_VOLUME => "%10.2f"  format ticker.dayVolume / 100.0
            case DAY_AMOUNT => "%10.2f"  format ticker.dayAmount / 10000.0
            case PREV_CLOSE => priceDf   format ticker.prevClose
            case DAY_CHANGE => priceDf   format ticker.dayChange
            case PERCENT    => "%3.2f%%" format ticker.changeInPercent
            case DAY_HIGH   => priceDf   format ticker.dayHigh
            case DAY_LOW    => priceDf   format ticker.dayLow
            case DAY_OPEN   => priceDf   format ticker.dayOpen
            case _ => null
          }
        case None =>
          colKeys(col) match {
            case SYMBOL => symbol
            case TIME => "_"
            case LAST_PRICE => "_"
            case DAY_VOLUME => "_"
            case DAY_AMOUNT => "_"
            case PREV_CLOSE => "_"
            case DAY_CHANGE => "_"
            case PERCENT    => "_"
            case DAY_HIGH   => "_"
            case DAY_LOW    => "_"
            case DAY_OPEN   => "_"
            case _ => null
          }
      }

    }

    override def getColumnName(col: Int) = colNames(col)

    override def getColumnClass(columnIndex: Int): Class[_] = {
      types(columnIndex)
    }

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
  }

  def updateByTickers(tickers: Array[LightTicker]) {
    /*
     * To avoid:
     java.lang.NullPointerException
     at javax.swing.DefaultRowSorter.convertRowIndexToModel(DefaultRowSorter.java:501)
     at javax.swing.JTable.convertRowIndexToModel(JTable.java:2620)
     at javax.swing.JTable.getValueAt(JTable.java:2695)
     at javax.swing.JTable.prepareRenderer(JTable.java:5712)
     at javax.swing.plaf.basic.BasicTableUI.paintCell(BasicTableUI.java:2072)
     * We should call addRow, removeRow, setValueAt etc in EventDispatchThread
     */
    SwingUtilities.invokeLater(new Runnable {
        def run {
          log.info("Batch updating, tickers: " + tickers.length)
          var updated = false
          var i = 0
          while (i < tickers.length) {
            val ticker = tickers(i)
            if (watchingSymbols.contains(ticker.symbol)) {
              updated = updateByTicker(ticker) | updated // don't use shortcut one: "||"
            }
            i += 1
          }

          if (updated) {
            table.getRowSorter.asInstanceOf[TableRowSorter[_]].sort // force to re-sort all rows
          }
        }
      })
  }

  private def updateByTicker(ticker: LightTicker): Boolean = {
    val symbol = ticker.symbol
    if (!uniSymbols.contains(symbol)) {
      uniSymbols += symbol
    }

    val (info, dayFirst) = symbolToInfo.get(symbol) match {
      case Some(x) => (x, false)
      case None =>
        val x = new Info
        symbolToInfo.put(symbol, x)
        (x, true)
    }

    if (ticker == null) return false

    val prevTicker = info.prevTicker
    /**
     * @Note
     * Should set columeColors[] before addRow() or setValue() of table to
     * make the color effects take place at once.
     */
    var updated = false
    val colKeyToColor = info.colKeyToColor
    if (dayFirst) {
      setColColorsByTicker(info, ticker)
      updated = true
    } else {
      if (ticker.isDayVolumeGrown(prevTicker)) {
        setColColorsByTicker(info, ticker)
        updated = true
      }
    }

    prevTicker.copyFrom(ticker)
    
    updated
  }

  private val SWITCH_COLOR_A = LookFeel().nameColor
  private val SWITCH_COLOR_B = new Color(128, 192, 192) //Color.CYAN;

  private def setColColorsByTicker(info: Info, ticker: LightTicker) {
    val fgColor = LookFeel().nameColor

    val colKeyToColor = info.colKeyToColor
    for (key <- colKeyToColor.keysIterator) {
      colKeyToColor(key) = fgColor
    }

    val neutralColor  = LookFeel().getNeutralBgColor
    val positiveColor = LookFeel().getPositiveBgColor
    val negativeColor = LookFeel().getNegativeBgColor

    /** color of volume should be recorded for switching between two colors */
    colKeyToColor(DAY_VOLUME) = fgColor

    if (ticker != null) {
      if (ticker.dayChange > 0) {
        colKeyToColor(DAY_CHANGE) = positiveColor
        colKeyToColor(PERCENT)    = positiveColor
      } else if (ticker.dayChange < 0) {
        colKeyToColor(DAY_CHANGE) = negativeColor
        colKeyToColor(PERCENT)    = negativeColor
      } else {
        colKeyToColor(DAY_CHANGE) = neutralColor
        colKeyToColor(PERCENT)    = neutralColor
      }

      def setColorByPrevClose(value: Double, columnName: String) {
        if (value > ticker.prevClose) {
          colKeyToColor(columnName) = positiveColor
        } else if (value < ticker.prevClose) {
          colKeyToColor(columnName) = negativeColor
        } else {
          colKeyToColor(columnName) = neutralColor
        }
      }

      setColorByPrevClose(ticker.dayOpen,   DAY_OPEN)
      setColorByPrevClose(ticker.dayHigh,   DAY_HIGH)
      setColorByPrevClose(ticker.dayLow,    DAY_LOW)
      setColorByPrevClose(ticker.lastPrice, LAST_PRICE)

      if (ticker.isDayVolumeChanged(info.prevTicker)) {
        /** lastPrice's color */
        /* ticker.compareLastCloseTo(prevTicker) match {
         case 1 =>
         symbolToColColor += (LAST_PRICE -> positiveColor)
         case 0 =>
         symbolToColColor += (LAST_PRICE -> neutralColor)
         case -1 =>
         symbolToColColor += (LAST_PRICE -> negativeColor)
         case _ =>
         } */

        /** volumes color switchs between two colors if ticker renewed */
        if (colKeyToColor(DAY_VOLUME) == SWITCH_COLOR_A) {
          colKeyToColor(DAY_VOLUME) = SWITCH_COLOR_B
        } else {
          colKeyToColor(DAY_VOLUME) = SWITCH_COLOR_A
        }
      }
    }
  }


  def watch(sec: Sec) {
    val uniSymbol = sec.uniSymbol
    watchingSymbols += uniSymbol
  }

  def unWatch(sec: Sec) {
    val uniSymbol = sec.uniSymbol
    watchingSymbols -= uniSymbol
  }

  def clearAllWatch {
    symbolToInfo.clear
  }

  def symbolAtRow(row: Int): String = {
    val symbol = table.getValueAt(row, colKeys.findIndexOf(_ == SYMBOL)).asInstanceOf[String]
    symbol
  }
  
  class TrendSensitiveCellRenderer extends JLabel with TableCellRenderer {
    private val defaultFont = new Font("Dialog", Font.PLAIN, 12)
    setOpaque(true)
    def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                      hasFocus: Boolean, row: Int, col: Int): Component = {

      setFont(defaultFont)
      
      /**
       * @Note
       * Here we should use table.getColumeName(column) which is
       * not the same as tableModel.getColumeName(column).
       * Especially: after you draged and moved the table colume, the
       * column index of table will change, but the column index
       * of tableModel will remain the same.
       */
      val symbol = symbolAtRow(row)
      symbolToInfo.get(symbol) match {
        case Some(info) =>
          val colKeyToColor = info.colKeyToColor

          val colKey = colKeys(col)
          if (isSelected) {
            setBackground(bgColorSelected)
          } else {
            setBackground(LookFeel().backgroundColor)
          }

          setForeground(colKeyToColor(colKey))

          if (value != null) {
            colKey match {
              case SYMBOL => setHorizontalAlignment(SwingConstants.LEADING)
              case _      => setHorizontalAlignment(SwingConstants.TRAILING)
            }

            setText(value.toString)
          } else {
            setText(null)
          }
        case None =>
      }

      this
    }
  }

}
