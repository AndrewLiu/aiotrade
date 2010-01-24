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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Comparator
import java.util.Locale
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.RowSorter
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.securities.Ticker
import org.aiotrade.lib.securities.TickerObserver
import org.aiotrade.lib.securities.TickerSnapshot
import org.aiotrade.lib.util.Observable
import scala.collection.mutable.HashMap


/**
 *
 * @author  Caoyuan Deng
 */
class RealTimeWatchListPanel extends JPanel with TickerObserver {

  private val SYMBOL = "Symbol"
  private val TIME = "Time"
  private val LAST_PRICE = "Last"
  private val DAY_VOLUME = "Volume"
  private val PREV_CLOSE = "Prev. cls"
  private val DAY_CHANGE = "Change"
  private val PERCENT = "Percent"
  private val DAY_HIGH = "High"
  private val DAY_LOW = "Low"
  private val DAY_OPEN = "Open"
  private val colNameToCol = Map[String, Int](
    SYMBOL     -> 0,
    TIME       -> 1,
    LAST_PRICE -> 2,
    DAY_VOLUME -> 3,
    PREV_CLOSE -> 4,
    DAY_CHANGE -> 5,
    PERCENT    -> 6,
    DAY_HIGH   -> 7,
    DAY_LOW    -> 8,
    DAY_OPEN   -> 9
  )

  private val colNames = {
    val names = new Array[String](colNameToCol.size)
    for ((name, col) <- colNameToCol) {
      names(col) = name
    }
    
    names
  }
  
  private val symbolToInWatching = new HashMap[String, Boolean]
  private val symbolToPrevTicker = new HashMap[String, Ticker]
  private val symbolToColColors  = new HashMap[String, HashMap[String, Color]]

  private val scrollPane = new JScrollPane
  private val table = new JTable
  private val tableModel: WatchListTableModel = new WatchListTableModel(colNames , 0)
  private val df = new SimpleDateFormat("hh:mm", Locale.US)
  private val cal = Calendar.getInstance
  private val bgColorSelected = new Color(169, 178, 202)

  initComponents

  private def initComponents {
    table.setFont(LookFeel().defaultFont)
    table.setModel(
      new DefaultTableModel(
        Array(Array[Object]()),
        Array[Object]()
      )
    )
    table.setModel(tableModel)
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
    val sorter = new TableRowSorter(tableModel)
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
    for (col <- 1 until tableModel.getColumnCount) {
      sorter.setComparator(col, comparator)
    }
    // default sort order and precedence
    val sortKeys = new java.util.ArrayList[RowSorter.SortKey]
    sortKeys.add(new RowSorter.SortKey(colOfName(PERCENT), javax.swing.SortOrder.DESCENDING))
    sorter.setSortKeys(sortKeys)

    table.setRowSorter(sorter)

    scrollPane.setViewportView(table)
    scrollPane.setBackground(LookFeel().backgroundColor)
    
    setLayout(new BorderLayout)
    add(BorderLayout.CENTER, scrollPane)
  }

  class WatchListTableModel(columnNames: Array[String], rowCount: Int) extends DefaultTableModel(columnNames.asInstanceOf[Array[Object]], rowCount) {

    private val types = Array(
      classOf[String], classOf[String], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object]
    )
    private val canEdit = Array(
      false, false, false, false, false, false, false, false, false, false
    )

    override def getColumnClass(columnIndex: Int): Class[_] = {
      types(columnIndex)
    }

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = {
      canEdit(columnIndex)
    }
  }

  def update(tickerSnapshot: Observable) {
    val ts = tickerSnapshot.asInstanceOf[TickerSnapshot]

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
          updateByTicker(ts)
        }
      })
  }

  private def updateByTicker(ts: TickerSnapshot) {
    val symbol = ts.symbol
    val ticker = ts.ticker

    val prevTicker = symbolToPrevTicker.get(symbol) getOrElse {
      val x = new Ticker
      symbolToPrevTicker += (symbol -> x)
      x
    }
    
    val inWatching = symbolToInWatching.get(symbol) getOrElse {false}
    if (!inWatching) {
      return
    }

    /**
     * @Note
     * Should set columeColors[] before addRow() or setValue() of table to
     * make the color effects take place at once.
     */
    symbolToColColors.get(symbol) match {
      case Some(colNameToColor) =>
        if (ticker.isDayVolumeGrown(prevTicker)) {
          setColColorsByTicker(colNameToColor, ticker, prevTicker, inWatching)

          val rowData = composeRowData(symbol, ticker)
          val row = rowOfSymbol(symbol)
          for (i <- 0 until rowData.length) {
            table.setValueAt(rowData(i), row, i)
          }
        }
      case None =>
        val colNameToColor = new HashMap[String, Color]
        for (name <- colNameToCol.keysIterator) {
          colNameToColor += (name -> LookFeel().nameColor)
        }
        symbolToColColors += (symbol -> colNameToColor)

        setColColorsByTicker(colNameToColor, ticker, null, inWatching)

        val rowData = composeRowData(symbol, ticker)
        tableModel.addRow(rowData)
    }

    prevTicker.copyFrom(ticker)
  }

  private val SWITCH_COLOR_A = LookFeel().nameColor
  private val SWITCH_COLOR_B = new Color(128, 192, 192) //Color.CYAN;

  private def setColColorsByTicker(colNameToColor: HashMap[String, Color], ticker: Ticker, prevTicker: Ticker, inWatching: Boolean) {
    val fgColor = if (inWatching) LookFeel().nameColor else Color.GRAY.brighter

    for (name <- colNameToColor.keysIterator) {
      colNameToColor += (name -> fgColor)
    }

    val neutralColor  = LookFeel().getNeutralBgColor
    val positiveColor = LookFeel().getPositiveBgColor
    val negativeColor = LookFeel().getNegativeBgColor

    if (inWatching) {
      /** color of volume should be recorded for switching between two colors */
      colNameToColor += (DAY_VOLUME -> fgColor)
    }

    if (ticker != null) {
      if (ticker(Ticker.DAY_CHANGE) > 0) {
        colNameToColor += (DAY_CHANGE -> positiveColor)
        colNameToColor += (PERCENT    -> positiveColor)
      } else if (ticker(Ticker.DAY_CHANGE) < 0) {
        colNameToColor += (DAY_CHANGE -> negativeColor)
        colNameToColor += (PERCENT    -> negativeColor)
      } else {
        colNameToColor += (DAY_CHANGE -> neutralColor)
        colNameToColor += (PERCENT    -> neutralColor)
      }

      def setColorByPrevClose(tickerField: Int, columnName: String) {
        if (ticker(tickerField) > ticker(Ticker.PREV_CLOSE)) {
          colNameToColor += (columnName -> positiveColor)
        } else if (ticker(tickerField) < ticker(Ticker.PREV_CLOSE)) {
          colNameToColor += (columnName -> negativeColor)
        } else {
          colNameToColor += (columnName -> neutralColor)
        }
      }

      setColorByPrevClose(Ticker.DAY_OPEN,   DAY_OPEN)
      setColorByPrevClose(Ticker.DAY_HIGH,   DAY_HIGH)
      setColorByPrevClose(Ticker.DAY_LOW,    DAY_LOW)
      setColorByPrevClose(Ticker.LAST_PRICE, LAST_PRICE)

      if (prevTicker != null) {
        if (ticker.isDayVolumeChanged(prevTicker)) {
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
          if (colNameToColor(DAY_VOLUME).equals(SWITCH_COLOR_A)) {
            colNameToColor(DAY_VOLUME) = SWITCH_COLOR_B
          } else {
            colNameToColor(DAY_VOLUME) = SWITCH_COLOR_A
          }
        }
      }
    }

  }

  private val rowData = new Array[Object](table.getColumnCount)

  private def composeRowData(symbol: String, ticker: Ticker): Array[Object] = {
    cal.setTimeInMillis(ticker.time)
    val lastTradeTime = cal.getTime

    rowData(colOfName(SYMBOL)) = symbol
    rowData(colOfName(TIME)) = df format lastTradeTime
    rowData(colOfName(LAST_PRICE)) = "%5.2f"   format ticker(Ticker.LAST_PRICE)
    rowData(colOfName(DAY_VOLUME)) = "%5.2f"   format ticker(Ticker.DAY_VOLUME)
    rowData(colOfName(PREV_CLOSE)) = "%5.2f"   format ticker(Ticker.PREV_CLOSE)
    rowData(colOfName(DAY_CHANGE)) = "%5.2f"   format ticker(Ticker.DAY_CHANGE)
    rowData(colOfName(PERCENT))    = "%3.2f%%" format ticker.changeInPercent
    rowData(colOfName(DAY_HIGH))   = "%5.2f"   format ticker(Ticker.DAY_HIGH)
    rowData(colOfName(DAY_LOW))    = "%5.2f"   format ticker(Ticker.DAY_LOW)
    rowData(colOfName(DAY_OPEN))   = "%5.2f"   format ticker(Ticker.DAY_OPEN)

    rowData
  }

  def watch(symbol: String) {
    symbolToInWatching += (symbol -> true)

    for (lastTicker <- symbolToPrevTicker.get(symbol);
         colNameToColors <- symbolToColColors.get(symbol)
    ) {
      setColColorsByTicker(colNameToColors, lastTicker, null, true)
      repaint()
    }
  }

  def unWatch(symbol: String) {
    symbolToInWatching += (symbol -> false)

    for (lastTicker <- symbolToPrevTicker.get(symbol);
         colNameToColors <- symbolToColColors.get(symbol)
    ) {
      setColColorsByTicker(colNameToColors, lastTicker, null, false)
      repaint()
    }
  }

  def clearAllWatch {
    symbolToInWatching.clear
    symbolToPrevTicker.clear
    symbolToColColors.clear
  }

  def getWatchListTable: JTable = {
    table
  }

  def colOfName(colName: String): Int = {
    colNameToCol(colName)
  }

  def rowOfSymbol(symbol: String): Int = {
    val colOfSymbol = colOfName(SYMBOL)
    var row = 0
    val nRows = table.getRowCount
    while (row < nRows) {
      if (table.getValueAt(row, colOfSymbol) == symbol) {
        return row
      }
      row += 1
    }

    -1
  }

  def symbolAtRow(row: Int): String = {
    if (row >= 0 && row < table.getRowCount) {
      table.getValueAt(row, colOfName(SYMBOL)).toString
    } else null
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
      val colNameToColor = symbolToColColors(symbol)

      val colName = table.getColumnName(col)
      if (isSelected && colName == SYMBOL) {
        setBackground(bgColorSelected)
      } else {
        setBackground(LookFeel().backgroundColor)
      }

      setForeground(colNameToColor(colName))
      
      setText(null)

      if (value != null) {
        colName match {
          case SYMBOL =>
            setHorizontalAlignment(SwingConstants.LEADING)
          case TIME =>
            setHorizontalAlignment(SwingConstants.CENTER)
          case LAST_PRICE =>
            setHorizontalAlignment(SwingConstants.TRAILING)
            setForeground(colNameToColor(colName))
          case DAY_VOLUME =>
            setHorizontalAlignment(SwingConstants.TRAILING)
            setForeground(colNameToColor(colName))
          case PREV_CLOSE =>
            setHorizontalAlignment(SwingConstants.TRAILING)
          case DAY_CHANGE =>
            setHorizontalAlignment(SwingConstants.TRAILING)
            setForeground(colNameToColor(colName))
          case PERCENT =>
            setHorizontalAlignment(SwingConstants.TRAILING)
            setForeground(colNameToColor(colName))
          case DAY_OPEN =>
            setHorizontalAlignment(SwingConstants.TRAILING)
          case _ =>
            setHorizontalAlignment(SwingConstants.TRAILING)
        }
      }
      setText(value.toString)

      this
    }
  }

}
