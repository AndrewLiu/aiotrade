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
package org.aiotrade.lib.chartview

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
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

  private val scrollPane = new JScrollPane
  private val table = new JTable
  private val SYMBOL = "Symbol"
  private val TIME = "Time"
  private val LAST_PRICE = "Last"
  private val DAY_VOLUME = "Volume"
  private val PREV_CLOSE = "Prev. cls"
  private val DAY_CHANGE = "Change"
  private val PERCENT = "Percent"
  private val DAY_HIGH = "High"
  private val DAY_LOW = "Low"
  private val DAY_OPON = "Open"
  private val COLUME_COUNT = 10
  private val columeNames = Array(
    SYMBOL,
    TIME,
    LAST_PRICE,
    DAY_VOLUME,
    PREV_CLOSE,
    DAY_CHANGE,
    PERCENT,
    DAY_HIGH,
    DAY_LOW,
    DAY_OPON
  )
  private val symbolToInWatching = new HashMap[String, Boolean]
  private val symbolToRow = new HashMap[String, Int]
  private val smbolToPreviousTicker = new HashMap[String, Ticker]
  private val rowToColColors = new HashMap[Int, HashMap[String, Color]]
  private val tableModel: WatchListTableModel = new WatchListTableModel(columeNames, 0)
  private val df = new SimpleDateFormat("hh:mm", Locale.US)
  private val cal = Calendar.getInstance
  private val bgColorSelected = new Color(169, 178, 202)

  initComponents

  private def initComponents {
    table.setFont(new Font("SansSerif", 0, 10))
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
    val header = table.getTableHeader
    header.setForeground(Color.WHITE)
    header.setBackground(LookFeel().backgroundColor)

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

  final private def getColumnIndex(columeName: String): Int = {
    var idx = -1
    val n = table.getColumnCount

    var i = 0
    var break = false
    while (i < n && !break) {
      if (table.getColumnName(i).equals(columeName)) {
        idx = i
        break = true
      }
      i += 1
    }

    idx
  }

  def update(tickerSnapshot: Observable) {
    val ts = tickerSnapshot.asInstanceOf[TickerSnapshot]
    val symbol = ts.symbol
    val snapshotTicker = ts.ticker

    var previousTicker = smbolToPreviousTicker.get(symbol) match {
      case Some(x) => x
      case None =>
        val previousTickerx = new Ticker
        smbolToPreviousTicker.put(symbol, previousTickerx)
        previousTickerx
    }
    
    val inWatching = symbolToInWatching.get(symbol) getOrElse false
    if (!inWatching) {
      return
    }

    /**
     * @NOTICE
     * Should set columeColors[] before addRow() or setValue() of table to
     * make the color effects take place at once.
     */
    symbolToRow.get(symbol) match {
      case Some(row) =>
        if (snapshotTicker.isDayVolumeGrown(previousTicker)) {
          val symbolToColColor = rowToColColors(row)
          setColColorsByTicker(symbolToColColor, snapshotTicker, previousTicker, inWatching)

          val rowData1 = composeRowData(symbol, snapshotTicker)
          for (i <- 0 until rowData1.length) {
            table.setValueAt(rowData1(i), row, i)
          }
        }
      case None =>
        val row = tableModel.getRowCount
        symbolToRow.put(symbol, row)

        val symbolToColColor = new HashMap[String, Color]
        for (name <- columeNames) {
          symbolToColColor += (name -> Color.WHITE)
        }
        rowToColColors += (row -> symbolToColColor)

        setColColorsByTicker(symbolToColColor, snapshotTicker, null, inWatching)

        tableModel.addRow(composeRowData(symbol, snapshotTicker))
    }

    previousTicker.copy(snapshotTicker)
  }

  val SWITCH_COLOR_A = Color.WHITE
  val SWITCH_COLOR_B = new Color(128, 192, 192) //Color.CYAN;

  private def setColColorsByTicker(symbolToColColor: HashMap[String, Color], ticker: Ticker, prevTicker: Ticker, inWatching: Boolean) {
    val bgColor = if (inWatching) Color.BLACK else Color.GRAY.brighter

    for (columeName <- symbolToColColor.keysIterator) {
      symbolToColColor += (columeName -> bgColor)
    }

    val neutralColor  = LookFeel().getNeutralBgColor
    val positiveColor = LookFeel().getPositiveBgColor
    val negativeColor = LookFeel().getNegativeBgColor

    if (inWatching) {
      /** color of volume should be remembered for switching between two colors */
      symbolToColColor += (DAY_VOLUME -> bgColor)
    }

    if (ticker != null) {
      if (ticker(Ticker.DAY_CHANGE) > 0) {
        symbolToColColor += (DAY_CHANGE -> positiveColor)
        symbolToColColor += (PERCENT -> positiveColor)
      } else if (ticker(Ticker.DAY_CHANGE) < 0) {
        symbolToColColor += (DAY_CHANGE -> negativeColor)
        symbolToColColor += (PERCENT -> negativeColor)
      } else {
        symbolToColColor += (DAY_CHANGE -> neutralColor)
        symbolToColColor += (PERCENT -> neutralColor)
      }

      if (prevTicker != null) {
        if (ticker.isDayVolumeChanged(prevTicker)) {
          /** lastPrice's color */
          ticker.compareLastCloseTo(prevTicker) match {
            case 1 =>
              symbolToColColor += (LAST_PRICE -> positiveColor)
            case 0 =>
              symbolToColColor += (LAST_PRICE -> neutralColor)
            case -1 =>
              symbolToColColor += (LAST_PRICE -> negativeColor)
            case _ =>
          }

          /** volumes color switchs between two colors if ticker renewed */
          if (symbolToColColor(DAY_VOLUME).equals(SWITCH_COLOR_A)) {
            symbolToColColor += (DAY_VOLUME -> SWITCH_COLOR_B)
          } else {
            symbolToColColor += (DAY_VOLUME -> SWITCH_COLOR_A)
          }
        }
      }
    }

  }

  private val rowData = new Array[Object](COLUME_COUNT)

  private def composeRowData(symbol: String, ticker: Ticker): Array[Object] = {
    cal.setTimeInMillis(ticker.time)

    rowData(getColumnIndex(SYMBOL)) = symbol
    rowData(getColumnIndex(TIME)) = df.format(cal.getTime)
    rowData(getColumnIndex(LAST_PRICE)) = "%5.2f"    format ticker(Ticker.LAST_PRICE)
    rowData(getColumnIndex(DAY_VOLUME)) = "%5.2f"    format ticker(Ticker.DAY_VOLUME)
    rowData(getColumnIndex(PREV_CLOSE)) = "%5.2f"    format ticker(Ticker.PREV_CLOSE)
    rowData(getColumnIndex(DAY_CHANGE)) = "%5.2f"    format ticker(Ticker.DAY_CHANGE)
    rowData(getColumnIndex(PERCENT))    = "%+3.2f%%" format ticker.changeInPercent
    rowData(getColumnIndex(DAY_HIGH))   = "%5.2f"    format ticker(Ticker.DAY_HIGH)
    rowData(getColumnIndex(DAY_LOW))    = "%5.2f"    format ticker(Ticker.DAY_LOW)
    rowData(getColumnIndex(DAY_OPON))   = "%5.2f"    format ticker(Ticker.DAY_OPEN)

    rowData
  }

  def watch(symbol: String) {
    symbolToInWatching += (symbol -> true)

    val row = symbolToRow.get(symbol) getOrElse {return}

    val lastTicker = smbolToPreviousTicker.get(symbol) getOrElse {return}
    rowToColColors.get(row) foreach {columeToColor =>
      setColColorsByTicker(columeToColor, lastTicker, null, true)
    }

    repaint()
  }

  def unWatch(symbol: String) {
    symbolToInWatching += (symbol -> false)

    val row = symbolToRow.get(symbol) getOrElse {return}

    val lastTicker = smbolToPreviousTicker.get(symbol) getOrElse {return}
    rowToColColors.get(row) foreach {columeToColor =>
      setColColorsByTicker(columeToColor, lastTicker, null, false)
    }

    repaint()
  }

  def clearAllWatch {
    symbolToInWatching.clear
    symbolToRow.clear
    smbolToPreviousTicker.clear
    rowToColColors.clear
  }

  def getWatchListTable: JTable = {
    table
  }

  def getSymbolAtRow(row: Int): String = {
    if (row < table.getRowCount && row >= 0) {
      table.getValueAt(row, getColumnIndex(SYMBOL)).toString
    } else null
  }

  class TrendSensitiveCellRenderer extends JLabel with TableCellRenderer {
    var symbol: String = _

    setOpaque(true)

    def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                      hasFocus: Boolean, row: Int, column: Int): Component = {

      /**
       * @NOTICE
       * Here we should use watchListTable.getColumeName(column) which is
       * not the same as watchListTableModel.getColumeName(column).
       * Especially: after you draged and moved the table colume, the
       * column index of watchListTable will change, but the column index
       * of watchListTableModel will remain the same.
       */
      val columnName = table.getColumnName(column)

      val symbolToColColor = rowToColColors(row)

      setBackground(symbolToColColor(columnName))
      if (isSelected && columnName == SYMBOL) {
        setBackground(bgColorSelected)
      }

      setForeground(Color.WHITE)

      setText(null)

      if (value != null) {
        columnName match {
          case SYMBOL =>
            setHorizontalAlignment(SwingConstants.LEADING)
          case TIME =>
            setHorizontalAlignment(SwingConstants.CENTER)
          case LAST_PRICE =>
            setHorizontalAlignment(SwingConstants.TRAILING)
            setBackground(symbolToColColor(columnName))
          case DAY_VOLUME =>
            setHorizontalAlignment(SwingConstants.TRAILING)
            setBackground(symbolToColColor(columnName))
          case PREV_CLOSE =>
            setHorizontalAlignment(SwingConstants.TRAILING)
          case DAY_CHANGE =>
            setHorizontalAlignment(SwingConstants.TRAILING)
            setBackground(symbolToColColor(columnName))
          case PERCENT =>
            setHorizontalAlignment(SwingConstants.TRAILING)
            setBackground(symbolToColColor(columnName))
          case DAY_OPON =>
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
