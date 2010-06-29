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
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Comparator
import java.util.Locale
import java.util.ResourceBundle
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
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.securities.model.TickerEvent
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable.HashMap

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

  private val SYMBOL     = "Symbol"
  private val TIME       = "Time"
  private val LAST_PRICE = "Last"
  private val DAY_VOLUME = "Volume"
  private val PREV_CLOSE = "PrevCls"
  private val DAY_CHANGE = "Change"
  private val PERCENT    = "Percent"
  private val DAY_HIGH   = "High"
  private val DAY_LOW    = "Low"
  private val DAY_OPEN   = "Open"

  private val colKeys = Array[String](
    SYMBOL,
    LAST_PRICE,
    DAY_VOLUME,
    PREV_CLOSE,
    DAY_CHANGE,
    PERCENT,
    DAY_HIGH,
    DAY_LOW,
    DAY_OPEN
  )

  private val lastTickers = new ArrayList[Ticker]
  private class Info {
    var inWatching = false
    val prevTicker = new Ticker
    val colKeyToColor = HashMap[String, Color]()
    for (key <- colKeys) {
      colKeyToColor(key) = LookFeel().nameColor
    }
  }
  private val symbolToInfo = new HashMap[String, Info]

  val table = new JTable
  private val model = new WatchListTableModel
  private val df = new SimpleDateFormat("hh:mm", Locale.US)
  private val cal = Calendar.getInstance
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
    case TickerEvent(sec: Sec, ticker: Ticker) =>
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
            updateByTicker(sec.uniSymbol, ticker)
          }
        })
  }

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
    header.setForeground(Color.WHITE)
    header.setBackground(LookFeel().backgroundColor)
    header.setReorderingAllowed(true)
    header.getDefaultRenderer.asInstanceOf[DefaultTableCellRenderer].setHorizontalAlignment(SwingConstants.CENTER)

    // --- sorter
    table.setAutoCreateRowSorter(false)
    val sorter = new TableRowSorter(model)
    val comparator = new Comparator[Object] {
      def compare(o1: Object, o2: Object): Int = {
        (o1, o2) match {
          case (s1: String, s2: String) =>
            val idx1 = s1.indexOf('%')
            val s11 = if (idx1 > 0) s1.substring(0, idx1) else s1
            val idx2 = s2.indexOf('%')
            val s12 = if (idx2 > 0) s2.substring(0, idx2) else s2
            try {
              val d1 = s11.toDouble
              val d2 = s12.toDouble
              if (d1 > d2) 1 else if (d1 < d2) -1 else 0
            } catch {case _ => s1 compareTo s2}
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
      classOf[String], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object]
    )

    def getRowCount: Int = lastTickers.size
    def getColumnCount: Int = colNames.length

    def getValueAt(row: Int, col: Int): Object = {
      val ticker = lastTickers(row)

      colKeys(col) match {
        case SYMBOL => ticker.symbol
        case LAST_PRICE => "%5.2f"   format ticker.lastPrice
        case DAY_VOLUME => "%5.2f"   format ticker.dayVolume
        case PREV_CLOSE => "%5.2f"   format ticker.prevClose
        case DAY_CHANGE => "%5.2f"   format ticker.dayChange
        case PERCENT    => "%3.2f%%" format ticker.changeInPercent
        case DAY_HIGH   => "%5.2f"   format ticker.dayHigh
        case DAY_LOW    => "%5.2f"   format ticker.dayLow
        case DAY_OPEN   => "%5.2f"   format ticker.dayOpen
        case _ => null
      }
    }

    override def getColumnName(col: Int) = colNames(col)

    override def getColumnClass(columnIndex: Int): Class[_] = {
      types(columnIndex)
    }

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
  }

  private def updateByTicker(symbol: String, ticker: Ticker) {
    val (info, dayFirst) = symbolToInfo.get(symbol) match {
      case Some(x) => (x, false)
      case None =>
        val x = new Info
        symbolToInfo.put(symbol, x)
        (x, true)
    }
    
    val inWatching = info.inWatching
    if (!inWatching) {
      return
    }

    val idx = lastTickers.findIndexOf(_.symbol == symbol)
    if (idx < 0) {
      lastTickers += ticker
    } else {
      lastTickers(idx) = ticker
    }
    
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
    
    if (updated) {
      table.getRowSorter.asInstanceOf[TableRowSorter[_]].sort // force to re-sort all rows
    }
  }

  private val SWITCH_COLOR_A = LookFeel().nameColor
  private val SWITCH_COLOR_B = new Color(128, 192, 192) //Color.CYAN;

  private def setColColorsByTicker(info: Info, ticker: Ticker) {
    val fgColor = if (info.inWatching) LookFeel().nameColor else Color.GRAY.brighter

    val colKeyToColor = info.colKeyToColor
    for (key <- colKeyToColor.keysIterator) {
      colKeyToColor(key) = fgColor
    }

    val neutralColor  = LookFeel().getNeutralBgColor
    val positiveColor = LookFeel().getPositiveBgColor
    val negativeColor = LookFeel().getNegativeBgColor

    if (info.inWatching) {
      /** color of volume should be recorded for switching between two colors */
      colKeyToColor(DAY_VOLUME) = fgColor
    }

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

      def setColorByPrevClose(value: Float, columnName: String) {
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
    listenTo(sec)

    val symbol = sec.uniSymbol
    symbolToInfo.get(symbol) match {
      case Some(x) =>
        x.inWatching = true
        setColColorsByTicker(x, x.prevTicker)
        repaint()
      case None =>
        val x = new Info
        x.inWatching = true
        symbolToInfo.put(symbol, x)
    }
  }

  def unWatch(sec: Sec) {
    deafTo(sec)

    val symbol = sec.uniSymbol
    symbolToInfo.get(symbol) match {
      case Some(x) =>
        x.inWatching = false
        setColColorsByTicker(x, x.prevTicker)
        repaint()
      case None =>
    }
  }

  def clearAllWatch {
    symbolToInfo.clear
  }

  def symbolAtRow(row: Int): String = {
    val symbol = table.getValueAt(row, colKeys.findIndexOf(_ == SYMBOL)).asInstanceOf[String]
    symbol
  }

  class TrendSensitiveCellRenderer extends JLabel with TableCellRenderer {

    setOpaque(true)

    def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                      hasFocus: Boolean, row: Int, col: Int): Component = {

      /**
       * @Note
       * Here we should use table.getColumeName(column) which is
       * not the same as tableModel.getColumeName(column).
       * Especially: after you draged and moved the table colume, the
       * column index of table will change, but the column index
       * of tableModel will remain the same.
       */
      val symbol = symbolAtRow(row)
      val colKeyToColor = symbolToInfo(symbol).colKeyToColor

      val colKey = colKeys(col)
      if (isSelected) {
        setBackground(bgColorSelected)
      } else {
        setBackground(LookFeel().backgroundColor)
      }

      setForeground(colKeyToColor(colKey))
      
      setText(null)

      if (value != null) {
        colKey match {
          case SYMBOL => setHorizontalAlignment(SwingConstants.LEADING)
          case _      => setHorizontalAlignment(SwingConstants.TRAILING)
        }

        setText(value.toString)
      }

      this
    }
  }

}
