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
import java.awt.Container
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Point
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.ResourceBundle
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.CellRendererPane
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.plaf.basic.BasicTableUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.ChartingControllerFactory
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents

import org.aiotrade.lib.securities.Sec
import org.aiotrade.lib.securities.Ticker
import org.aiotrade.lib.securities.TickerObserver
import org.aiotrade.lib.securities.TickerSnapshot
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.util.Observable
import org.aiotrade.lib.util.swing.GBC
import org.aiotrade.lib.util.swing.plaf.AIOScrollPaneStyleBorder
import org.aiotrade.lib.util.swing.table.AttributiveCellRenderer
import org.aiotrade.lib.util.swing.table.AttributiveCellTableModel
import org.aiotrade.lib.util.swing.table.DefaultCellAttribute
import org.aiotrade.lib.util.swing.table.MultiSpanCellTable

/**
 *
 * @author Caoyuan Deng
 */
object RealTimeBoardPanel {
  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.view.securities.Bundle")
  private val NUMBER_FORMAT = NumberFormat.getInstance
}

class RealTimeBoardPanel(sec: Sec, contents: AnalysisContents) extends JPanel with TickerObserver {
  import RealTimeBoardPanel._

  private val tickerContract: TickerContract = sec.tickerContract
  private val tickerPane = new JScrollPane
  private val chartPane = new JPanel
  private val symbol = new ValueCell
  private val sname = new ValueCell
  private val currentTime = new ValueCell
  private val dayChange = new ValueCell
  private val dayHigh = new ValueCell
  private val dayLow = new ValueCell
  private val dayOpen = new ValueCell
  private val dayVolume = new ValueCell
  private val lastPrice = new ValueCell
  private val dayPercent = new ValueCell
  private val prevClose = new ValueCell

  private val numbers = Array("①", "②", "③", "④", "⑤")
  private val timeZone = sec.market.timeZone
  private val marketCal = Calendar.getInstance(timeZone)
  private val sdf: SimpleDateFormat = new SimpleDateFormat("HH:mm:ss")
  sdf.setTimeZone(timeZone)

  private var prevTicker: Ticker = _
  private var infoModel: AttributiveCellTableModel = _
  private var depthModel: AttributiveCellTableModel = _
  private var tickerModel: AttributiveCellTableModel = _
  private var infoCellAttr: DefaultCellAttribute = _
  private var depthCellAttr: DefaultCellAttribute = _
  private var tickerCellAttr: DefaultCellAttribute = _
  private var infoTable: JTable = _
  private var depthTable: JTable = _
  private var tickerTable: JTable = _

  initComponents

  private val controller = ChartingControllerFactory.createInstance(sec.tickerSer, contents)
  private val viewContainer = controller.createChartViewContainer(classOf[RealTimeChartViewContainer], this).get
  private val tabbedPane = new JTabbedPane(SwingConstants.BOTTOM)
  tabbedPane.setFocusable(false)

  chartPane.setLayout(new BorderLayout)
  chartPane.add(viewContainer, BorderLayout.CENTER)

  private def initComponents {
    setFocusable(false)

    val infoModelData = Array(
      Array(BUNDLE.getString("lastPrice"),  lastPrice,  BUNDLE.getString("dayVolume"), dayVolume),
      Array(BUNDLE.getString("dayChange"),  dayChange,  BUNDLE.getString("dayHigh"),   dayHigh),
      Array(BUNDLE.getString("dayPercent"), dayPercent, BUNDLE.getString("dayLow"),    dayLow),
      Array(BUNDLE.getString("prevClose"),  prevClose,  BUNDLE.getString("dayOpen"),   dayOpen)
    )
    ValueCell.setRowColumn(infoModelData)
    infoModel = AttributiveCellTableModel(
      infoModelData,
      Array("A", "B", "C", "D")
    )

    infoCellAttr = infoModel.cellAttribute.asInstanceOf[DefaultCellAttribute]
    /* Code for combining cells
     infoCellAttr.combine(new int[]{0}, new int[]{0, 1});
     infoCellAttr.combine(new int[]{1}, new int[]{0, 1, 2, 3});
     */

    symbol.value = sec.uniSymbol
    if (tickerContract != null) {
      sname.value = tickerContract.shortName
    }

    for (cell <- Array(lastPrice, dayChange, dayPercent, prevClose, dayVolume, dayHigh, dayLow, dayOpen)) {
      infoCellAttr.setHorizontalAlignment(SwingConstants.TRAILING, cell.row, cell.column)
    }

    infoTable = new MultiSpanCellTable(infoModel)
    infoTable.setDefaultRenderer(classOf[Object], new AttributiveCellRenderer)
    infoTable.setFocusable(false)
    infoTable.setCellSelectionEnabled(false)
    infoTable.setShowHorizontalLines(false)
    infoTable.setShowVerticalLines(false)
    infoTable.setBorder(new AIOScrollPaneStyleBorder(LookFeel().heavyBackgroundColor))
    infoTable.setForeground(Color.WHITE)
    infoTable.setBackground(LookFeel().heavyBackgroundColor)

    depthModel = AttributiveCellTableModel(
      Array(
        Array("卖⑤", null, null),
        Array("卖④", null, null),
        Array("卖③", null, null),
        Array("卖②", null, null),
        Array("卖①", null, null),
        Array("成交", null, null),
        Array("买①", null, null),
        Array("买②", null, null),
        Array("买③", null, null),
        Array("买④", null, null),
        Array("买⑤", null, null)
      ),
      Array(
        BUNDLE.getString("askBid"), BUNDLE.getString("price"), BUNDLE.getString("size")
      )
    )

    val depth = 5
    val dealRow = 5
    depthModel.setValueAt(BUNDLE.getString("deal"), dealRow, 0)
    for (i <- 0 until depth) {
      val askIdx = depth - 1 - i
      val askRow = i
      depthModel.setValueAt(BUNDLE.getString("bid") + numbers(askIdx), askRow, 0)
      val bidIdx = i
      val bidRow = depth + 1 + i
      depthModel.setValueAt(BUNDLE.getString("ask") + numbers(bidIdx), bidRow, 0)
    }

    depthCellAttr = depthModel.cellAttribute.asInstanceOf[DefaultCellAttribute]

    for (i <- 0 until 11) {
      for (j <- 1 until 3) {
        depthCellAttr.setHorizontalAlignment(SwingConstants.TRAILING, i, j)
      }
    }
    depthCellAttr.setHorizontalAlignment(SwingConstants.LEADING, 5, 0)
//        for (int j = 0; j < 3; j++) {
//            depthCellAttr.setBackground(Color.gray, 5, j);
//        }

    depthTable = new MultiSpanCellTable(depthModel)
    depthTable.setDefaultRenderer(classOf[Object], new AttributiveCellRenderer)
    depthTable.setTableHeader(null)
    depthTable.setFocusable(false)
    depthTable.setCellSelectionEnabled(false)
    depthTable.setShowHorizontalLines(false)
    depthTable.setShowVerticalLines(false)
    depthTable.setBorder(new AIOScrollPaneStyleBorder(LookFeel().borderColor))
    depthTable.setForeground(Color.WHITE)
    depthTable.setBackground(LookFeel().infoBackgroundColor)
    val depthHeader = depthTable.getTableHeader
    if (depthHeader != null) {
      depthHeader.setForeground(Color.WHITE)
      depthHeader.setBackground(LookFeel().backgroundColor)
    }

    tickerModel = AttributiveCellTableModel( //new DefaultTableModel(
      Array(
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null)
      ),
      Array(
        BUNDLE.getString("time"), BUNDLE.getString("price"), BUNDLE.getString("size")
      )
    )

    tickerCellAttr = tickerModel.cellAttribute.asInstanceOf[DefaultCellAttribute]

    //tickerTable = new JTable(tickerModel)
    //tickerTable.setDefaultRenderer(classOf[Object], new TrendSensitiveCellRenderer)
    tickerTable = new MultiSpanCellTable(tickerModel)
    tickerTable.setDefaultRenderer(classOf[Object], new AttributiveCellRenderer)
    tickerTable.setFocusable(false)
    tickerTable.setCellSelectionEnabled(false)
    tickerTable.setShowHorizontalLines(false)
    tickerTable.setShowVerticalLines(false)
    tickerTable.setForeground(Color.WHITE)
    tickerTable.setBackground(LookFeel().backgroundColor)
    tickerTable.setFillsViewportHeight(true)
    val tickerHeader = tickerTable.getTableHeader
    if (tickerHeader != null) {
      tickerHeader.setForeground(Color.WHITE)
      tickerHeader.setBackground(LookFeel().backgroundColor)
    }

    var columeModel = infoTable.getColumnModel
    columeModel.getColumn(0).setMaxWidth(35)
    columeModel.getColumn(2).setMaxWidth(35)

    columeModel = depthTable.getColumnModel
    columeModel.getColumn(0).setMinWidth(12)
    columeModel.getColumn(1).setMinWidth(35)

    columeModel = tickerTable.getColumnModel
    columeModel.getColumn(0).setMinWidth(22)
    columeModel.getColumn(1).setMinWidth(30)


    /* @Note Border of JScrollPane may not be set by #setBorder, at least in Metal L&F: */
    UIManager.put("ScrollPane.border", classOf[AIOScrollPaneStyleBorder].getName)
    tickerPane.setBackground(LookFeel().backgroundColor)
    tickerPane.setViewportView(tickerTable)

    // put infoTable to a box to simple the insets setting:
    val infoBox = new Box(BoxLayout.Y_AXIS) {
      // box does not paint anything, override paintComponent to get background:
      override protected def paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.setColor(getBackground)
        val rect = getBounds()
        g.fillRect(rect.x, rect.y, rect.width, rect.height)
      }
    }
    infoBox.add(infoTable)

    // put fix size components to box
    val box = Box.createVerticalBox
    box.setBackground(LookFeel().backgroundColor)
    box.add(infoBox)
    box.add(depthTable)

    setLayout(new GridBagLayout)
    add(box,        new GBC(0, 0).setFill(GridBagConstraints.BOTH).setWeight(100,   0))
    add(tickerPane, new GBC(0, 1).setFill(GridBagConstraints.BOTH).setWeight(100, 100))
    add(chartPane,  new GBC(0, 2).setFill(GridBagConstraints.BOTH).setWeight(100, 100))
  }

  def update(tickerSnapshot: Observable) {
    val ts = tickerSnapshot.asInstanceOf[TickerSnapshot]
    val neutralColor  = LookFeel().getNeutralColor
    val positiveColor = LookFeel().getPositiveColor
    val negativeColor = LookFeel().getNegativeColor
    symbol.value = ts.symbol

    val snapshotTicker = ts.ticker

    val currentSize =
      if (prevTicker != null) {
        (snapshotTicker(Ticker.DAY_VOLUME) - prevTicker(Ticker.DAY_VOLUME)).toInt
      } else 0

    val depth = snapshotTicker.depth
    val dealRow = 5
    depthModel.setValueAt("%8.2f" format snapshotTicker(Ticker.LAST_PRICE), dealRow, 1)
    depthModel.setValueAt(if (prevTicker == null) "-" else currentSize,     dealRow, 2)
    for (i <- 0 until depth) {
      val askIdx = depth - 1 - i
      val askRow = i
      depthModel.setValueAt("%8.2f" format snapshotTicker.askPrice(askIdx), askRow, 1)
      depthModel.setValueAt(snapshotTicker.askSize(askIdx).toInt.toString,  askRow, 2)
      val bidIdx = i
      val bidRow = depth + 1 + i
      depthModel.setValueAt("%8.2f" format snapshotTicker.bidPrice(bidIdx), bidRow, 1)
      depthModel.setValueAt(snapshotTicker.bidSize(bidIdx).toInt.toString,  bidRow, 2)
    }

    marketCal.setTimeInMillis(snapshotTicker.time)
    val lastTradeTime = marketCal.getTime
    currentTime.value = sdf.format(lastTradeTime)
    lastPrice.value   = "%8.2f"    format snapshotTicker(Ticker.LAST_PRICE)
    prevClose.value   = "%8.2f"    format snapshotTicker(Ticker.PREV_CLOSE)
    dayOpen.value     = "%8.2f"    format snapshotTicker(Ticker.DAY_OPEN)
    dayHigh.value     = "%8.2f"    format snapshotTicker(Ticker.DAY_HIGH)
    dayLow.value      = "%8.2f"    format snapshotTicker(Ticker.DAY_LOW)
    dayChange.value   = "%+8.2f"   format snapshotTicker(Ticker.DAY_CHANGE)
    dayPercent.value  = "%+3.2f%%" format snapshotTicker.changeInPercent
    dayVolume.value   = snapshotTicker(Ticker.DAY_VOLUME).toString

    var bgColor = LookFeel().backgroundColor
    var fgColor = neutralColor
    if (snapshotTicker(Ticker.DAY_CHANGE) > 0) {
      fgColor = positiveColor
    } else if (snapshotTicker(Ticker.DAY_CHANGE) < 0) {
      fgColor = negativeColor
    }
    infoCellAttr.setForeground(fgColor, dayChange.row,  dayChange.column)
    infoCellAttr.setForeground(fgColor, dayPercent.row, dayPercent.column)
    infoCellAttr.setBackground(bgColor, dayChange.row,  dayChange.column)
    infoCellAttr.setBackground(bgColor, dayPercent.row, dayPercent.column)

    /**
     * Sometimes, DataUpdatedEvent is fired by other symbols' new ticker,
     * so assert here again.
     * @see UpdateServer.class in AbstractTickerDataServer.class and YahooTickerDataServer.class
     */
    if (prevTicker != null && snapshotTicker.isDayVolumeChanged(prevTicker)) {
      bgColor = LookFeel().backgroundColor
      fgColor = neutralColor
      snapshotTicker.compareLastCloseTo(prevTicker) match {
        case  1 =>
          fgColor = positiveColor
        case -1 =>
          fgColor = negativeColor
        case _ =>
      }

    }
    infoCellAttr  setForeground (fgColor, lastPrice.row, lastPrice.column)
    infoCellAttr  setBackground (bgColor, lastPrice.row, lastPrice.column)
    depthCellAttr setForeground (fgColor, dealRow, 1) // last deal
    depthCellAttr setBackground (bgColor, dealRow, 1) // last deal

    val tickerRow = Array(
      sdf.format(lastTradeTime),
      "%5.2f" format snapshotTicker(Ticker.LAST_PRICE),
      if (prevTicker == null) "-" else currentSize
    )
    tickerModel.insertRow(0, tickerRow.asInstanceOf[Array[Object]])

    if (prevTicker == null) {
      prevTicker = new Ticker
    }
    prevTicker.copy(snapshotTicker)

    repaint()
  }

  private def showCell(table: JTable, row: Int, column: Int) {
    val rect = table.getCellRect(row, column, true)
    table.scrollRectToVisible(rect)
    table.clearSelection
    table.setRowSelectionInterval(row, row)
    /* notify the model */
    table.getModel.asInstanceOf[DefaultTableModel].fireTableDataChanged
  }

  class CustomTableUI extends BasicTableUI {
    override def installUI(c: JComponent) {
      super.installUI(c)

      table.remove(rendererPane)
      rendererPane = createCustomCellRendererPane
      table.add(rendererPane)
    }

    /**
     * Creates a custom {@link CellRendererPane} that sets the renderer component to
     * be non-opaque if the associated row isn't selected. This custom
     * {@code CellRendererPane} is needed because a table UI delegate has no prepare
     * renderer like {@link JTable} has.
     */
    private def createCustomCellRendererPane: CellRendererPane = new CellRendererPane {
      override def paintComponent(graphics: Graphics, component: Component, 
                                  container: Container, x: Int, y: Int, w: Int, h: Int,
                                  shouldValidate: Boolean) {
        // figure out what row we're rendering a cell for.
        val rowAtPoint = table.rowAtPoint(new Point(x, y))
        val isSelected = table.isRowSelected(rowAtPoint)
        // if the component to render is a JComponent, add our tweaks.
        if (component.isInstanceOf [JComponent]) {
          val jcomponent = component.asInstanceOf[JComponent]
          jcomponent.setOpaque(isSelected)
        }

        super.paintComponent(graphics, component, container, x, y, w, h, shouldValidate)
      }
    }
  }
  
  class TrendSensitiveCellRenderer extends DefaultTableCellRenderer {

    setForeground(Color.WHITE)
    setBackground(LookFeel().backgroundColor)
    setOpaque(true)

    override def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                               hasFocus: Boolean, row: Int, column: Int
    ): Component = {

      /** Beacuse this will be a sinleton for all cells, so, should clear it first */
      setForeground(Color.WHITE)
      setBackground(LookFeel().backgroundColor)
      setText(null)

      if (value != null) {
        column match {
          case 0 => // Time
            setHorizontalAlignment(SwingConstants.LEADING)
          case 1 => // Price
            setHorizontalAlignment(SwingConstants.TRAILING)
            if (row + 1 < table.getRowCount) {
              try {
                var floatValue = NUMBER_FORMAT.parse(value.toString.trim).floatValue
                val prevValue = table.getValueAt(row + 1, column)
                if (prevValue != null) {
                  val prevFloatValue = NUMBER_FORMAT.parse(prevValue.toString.trim).floatValue
                  if (floatValue > prevFloatValue) {
                    setForeground(Color.WHITE)
                    setBackground(LookFeel().getPositiveBgColor)
                  } else if (floatValue < prevFloatValue) {
                    setForeground(Color.WHITE)
                    setBackground(LookFeel().getNegativeBgColor)
                  } else {
                    setForeground(Color.BLACK)
                    setBackground(LookFeel().getNeutralBgColor)
                  }
                }
              } catch {case ex: ParseException => ex.printStackTrace}
            }
          case 2 => // Size
            setHorizontalAlignment(SwingConstants.TRAILING)
        }

        setText(value.toString)
      }

      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
      this
    }
  }

  def realTimeChartViewContainer: Option[ChartViewContainer] = {
    if (viewContainer == null) None else Some(viewContainer)
  }

  private def test {
    tickerModel.addRow(Array[Object]("00:01", "12334",     "1"))
    tickerModel.addRow(Array[Object]("00:02", "12333",  "1234"))
    tickerModel.addRow(Array[Object]("00:03", "12335", "12345"))
    tickerModel.addRow(Array[Object]("00:04", "12334",   "123"))
    tickerModel.addRow(Array[Object]("00:05", "12334",   "123"))
    showCell(tickerTable, tickerTable.getRowCount - 1, 0)
  }
}

object ValueCell {
  def setRowColumn(modelData: Array[Array[Object]]) {
    for (i <- 0 until modelData.length) {
      val rows = modelData(i)
      for (j <- 0 until rows.length) {
        rows(j) match {
          case cell: ValueCell =>
            cell.row = i
            cell.column = j
          case _ =>
        }
      }
    }
  }
}

class ValueCell(var row: Int, var column: Int) {
  var value: String = _

  def this() = this(0, 0)

  override def toString = {
    value
  }
}
