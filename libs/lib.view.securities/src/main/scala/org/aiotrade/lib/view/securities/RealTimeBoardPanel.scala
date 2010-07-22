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
import java.awt.Dimension
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Point
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.ResourceBundle
import java.util.logging.Logger
import javax.swing.Box
import javax.swing.CellRendererPane
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.basic.BasicTableUI
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.securities.model.Execution
import org.aiotrade.lib.securities.model.ExecutionEvent
import org.aiotrade.lib.securities.model.Executions
import org.aiotrade.lib.securities.model.MarketDepth
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Secs
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.securities.model.TickerEvent
import org.aiotrade.lib.securities.model.Tickers
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.util.actors.Reactor
import org.aiotrade.lib.util.swing.GBC
import org.aiotrade.lib.util.swing.plaf.AIOScrollPaneStyleBorder
import org.aiotrade.lib.util.swing.table.AttributiveCellRenderer
import org.aiotrade.lib.util.swing.table.AttributiveCellTableModel
import org.aiotrade.lib.util.swing.table.DefaultCellAttribute
import org.aiotrade.lib.util.swing.table.MultiSpanCellTable
import scala.collection.mutable.WeakHashMap

/**
 *
 * @author Caoyuan Deng
 */
object RealTimeBoardPanel {
  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.view.securities.Bundle")
  private val NUMBER_FORMAT = NumberFormat.getInstance
  private val DIM = new Dimension(230, 100000)

  private val instanceRefs = WeakHashMap[RealTimeBoardPanel, AnyRef]()
  def instances = instanceRefs.keys

  def instanceOf(sec: Sec, contents: AnalysisContents): RealTimeBoardPanel = {
    instances find {_.sec eq sec} getOrElse new RealTimeBoardPanel(sec, contents)
  }

  private val log = Logger.getLogger(this.getClass.getSimpleName)
}

import RealTimeBoardPanel._
class RealTimeBoardPanel private (val sec: Sec, contents: AnalysisContents) extends JPanel with Reactor {
  instanceRefs.put(this, null)
  log.info("Instances of " + this.getClass.getSimpleName + " is " + instances.size)

  private val tickerContract: TickerContract = sec.tickerContract
  private val tickerPane = new JScrollPane
  private val chartPane = new JPanel
  private val symbol = new ValueCell
  private val sname = new ValueCell
  private val dayChange = new ValueCell
  private val dayHigh = new ValueCell
  private val dayLow = new ValueCell
  private val dayOpen = new ValueCell
  private val dayVolume = new ValueCell
  private val lastPrice = new ValueCell
  private val dayPercent = new ValueCell
  private val prevClose = new ValueCell

  private val numberStrs = Array("①", "②", "③", "④", "⑤")
  private val timeZone = sec.exchange.timeZone
  private val exchgCal = Calendar.getInstance(timeZone)
  private val sdf: SimpleDateFormat = new SimpleDateFormat("HH:mm:ss")
  sdf.setTimeZone(timeZone)

  private val (tickers, executions) = Quotes1d.lastDailyQuoteOf(sec) match {
    case Some(lastDailyQuote) =>
      (Tickers.tickersOf(lastDailyQuote), new ArrayList[Execution] ++ Executions.executionsOfDay(lastDailyQuote))
    case None =>
      (Nil, new ArrayList[Execution])
  }

  private val prevTicker: Ticker = new Ticker
  private var infoModel: AttributiveCellTableModel = _
  private var depthModel: AttributiveCellTableModel = _
  private var infoCellAttr: DefaultCellAttribute = _
  private var depthCellAttr: DefaultCellAttribute = _
  private var infoTable: JTable = _
  private var depthTable: JTable = _
  private var executionTable: JTable = _
  private var executionModel: AbstractTableModel = _
  initComponents

  private val rtSer = sec.serOf(TFreq.ONE_MIN).get
  private val controller = ChartingController(rtSer, contents)
  private val viewContainer = controller.createChartViewContainer(classOf[RealTimeChartViewContainer], this)
  private val tabbedPane = new JTabbedPane(SwingConstants.BOTTOM)
  tabbedPane.setFocusable(false)

  chartPane.setLayout(new BorderLayout)
  chartPane.add(viewContainer, BorderLayout.CENTER)

  setFocusable(false)
  chartPane.setFocusable(false)
  viewContainer.setFocusable(false)

  reactions += {
    case TickerEvent(src: Sec, ticker: Ticker) =>
      // symbol.value = if (src.secInfo != null) src.secInfo.uniSymbol else ticker.symbol
      // @Note ticker.time may only correct to minute, so tickers in same minute may has same time
      if (ticker.isValueChanged(prevTicker)) {
        updateInfoTable(ticker)
        updateDepthTable(ticker.marketDepth)
        prevTicker.copyFrom(ticker)
        repaint()
      }
    case ExecutionEvent(prevClose, execution) =>
      updateExecutionTable(prevClose, execution)
      repaint()
  }

  // use last ticker to update info/depth table
  if (!tickers.isEmpty) {
    val ticker = tickers.last
    updateInfoTable(ticker)
    updateDepthTable(ticker.marketDepth)
    prevTicker.copyFrom(ticker)
  }
  scrollToLastRow(executionTable)

  def watch {
    listenTo(sec)
  }

  def unWatch {
    deafTo(sec)
  }

  private def initComponents {
    setFocusable(false)
    setPreferredSize(DIM)

    // --- info table

    val infoModelData = Array(
      Array(symbol,                         "",         "",                            sname),
      Array(BUNDLE.getString("lastPrice"),  lastPrice,  BUNDLE.getString("dayVolume"), dayVolume),
      Array(BUNDLE.getString("dayChange"),  dayChange,  BUNDLE.getString("dayHigh"),   dayHigh),
      Array(BUNDLE.getString("dayPercent"), dayPercent, BUNDLE.getString("dayLow"),    dayLow),
      Array(BUNDLE.getString("prevClose"),  prevClose,  BUNDLE.getString("dayOpen"),   dayOpen)
    )
    ValueCell.setRowCol(infoModelData)
    infoModel = AttributiveCellTableModel(
      infoModelData,
      Array("A", "B", "C", "D")
    )

    infoCellAttr = infoModel.cellAttribute.asInstanceOf[DefaultCellAttribute]
    // combine cells
    infoCellAttr.combine(Array(0), Array(0, 1))
    //infoCellAttr.combine(Array(0), Array(2, 3))
    //infoCellAttr.combine(Array(1), Array(0, 1, 2, 3))
    
    symbol.value = sec.uniSymbol
    sname.value = Secs.company(sec) map (_.shortName) getOrElse ""

    for (cell <- Array(lastPrice, dayChange, dayPercent, prevClose, dayVolume, dayHigh, dayLow, dayOpen)) {
      infoCellAttr.setHorizontalAlignment(SwingConstants.TRAILING, cell.row, cell.col)
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

    // --- execution table

    depthModel = AttributiveCellTableModel(
      Array(
        Array("A", null, null, null, null),
        Array("A", null, null, null, null),
        Array("A", null, null, null, null),
        Array("A", null, null, null, null),
        Array("A", null, null, null, null),
        Array("B", null, null, null, null),
        Array("B", null, null, null, null),
        Array("B", null, null, null, null),
        Array("B", null, null, null, null),
        Array("B", null, null, null, null)
      ),
      Array(
        BUNDLE.getString("askBid"), BUNDLE.getString("price"), BUNDLE.getString("size"), BUNDLE.getString("price"), BUNDLE.getString("size")
      )
    )

    val level = 5
    for (i <- 0 until level) {
      val bidIdx = level - 1 - i
      val bidRow = i
      depthModel.setValueAt(BUNDLE.getString("bid") + numberStrs(bidIdx), bidRow, 0)
      val askIdx = i
      val askRow = level + i
      depthModel.setValueAt(BUNDLE.getString("ask") + numberStrs(askIdx), askRow, 0)
    }

    depthCellAttr = depthModel.cellAttribute.asInstanceOf[DefaultCellAttribute]

    for (i <- 0 to 9) {
      for (j <- 1 to 5) {
        depthCellAttr.setHorizontalAlignment(SwingConstants.TRAILING, i, j)
      }
    }
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
    val depthNameCol = depthTable.getColumnModel.getColumn(0)
    depthNameCol.setPreferredWidth(50)

    // --- execution table
    
    executionModel = new AbstractTableModel {
      private val columnNames = Array[String](
        BUNDLE.getString("time"), BUNDLE.getString("price"), BUNDLE.getString("size")
      )

      def getRowCount: Int = executions.size
      def getColumnCount: Int = columnNames.length

      def getValueAt(row: Int, col: Int): Object = {
        val execution = executions(row)
        col match {
          case 0 => sdf.format(execution.time)
          case 1 => "%5.2f"  format execution.price
          case 2 => "%10.2f" format execution.volume
          case _ => null
        }
      }

      override def getColumnName(col: Int) = columnNames(col)
    }

    executionTable = new JTable(executionModel)
    executionTable.setDefaultRenderer(classOf[Object], new TrendSensitiveCellRenderer)
    executionTable.setFocusable(false)
    executionTable.setCellSelectionEnabled(false)
    executionTable.setShowHorizontalLines(false)
    executionTable.setShowVerticalLines(false)
    executionTable.setForeground(Color.WHITE)
    executionTable.setBackground(LookFeel().backgroundColor)
    executionTable.setFillsViewportHeight(true)
    val tickerHeader = executionTable.getTableHeader
    if (tickerHeader != null) {
      tickerHeader.setForeground(Color.WHITE)
      tickerHeader.setBackground(LookFeel().backgroundColor)
    }

    // --- set column width
    var columnModel = infoTable.getColumnModel
    columnModel.getColumn(0).setMaxWidth(35)
    columnModel.getColumn(2).setMaxWidth(35)

    columnModel = depthTable.getColumnModel
    columnModel.getColumn(0).setMinWidth(12)
    columnModel.getColumn(1).setMinWidth(35)

    columnModel = executionTable.getColumnModel
    columnModel.getColumn(0).setMinWidth(22)
    columnModel.getColumn(1).setMinWidth(30)

    /* @Note Border of JScrollPane may not be set by #setBorder, at least in Metal L&F: */
    UIManager.put("ScrollPane.border", classOf[AIOScrollPaneStyleBorder].getName)
    tickerPane.setBackground(LookFeel().backgroundColor)
    tickerPane.setViewportView(executionTable)
    //tickerPane.getVerticalScrollBar.setUI(new BasicScrollBarUI)

    val infoBox = Box.createVerticalBox
    infoBox.add(infoTable)

    // put fix size components to box
    val box = Box.createVerticalBox
    box.setBackground(LookFeel().backgroundColor)
    box.add(infoBox)
    box.add(depthTable)

    setLayout(new GridBagLayout)
    add(box,        GBC(0, 0).setFill(GridBagConstraints.BOTH).setWeight(100,   0))
    add(tickerPane, GBC(0, 1).setFill(GridBagConstraints.BOTH).setWeight(100, 100))
    add(chartPane,  GBC(0, 2).setFill(GridBagConstraints.BOTH).setWeight(100, 100))
  }

  private def updateInfoTable(ticker: Ticker) {
    val neuColor = LookFeel().getNeutralColor
    val posColor = LookFeel().getPositiveColor
    val negColor = LookFeel().getNegativeColor

    exchgCal.setTimeInMillis(System.currentTimeMillis)
    val now = exchgCal.getTime

    lastPrice.value   = "%8.2f"    format ticker.lastPrice
    prevClose.value   = "%8.2f"    format ticker.prevClose
    dayOpen.value     = "%8.2f"    format ticker.dayOpen
    dayHigh.value     = "%8.2f"    format ticker.dayHigh
    dayLow.value      = "%8.2f"    format ticker.dayLow
    dayChange.value   = "%+8.2f"   format ticker.dayChange
    dayPercent.value  = "%+3.2f%%" format ticker.changeInPercent
    dayVolume.value   = ticker.dayVolume.toString

    def setInfoCellColorByPrevCls(value: Double, cell: ValueCell) {
      val bgColor = LookFeel().backgroundColor
      val fgColor = (
        if (value > ticker.prevClose) posColor
        else if (value < ticker.prevClose) negColor
        else neuColor
      )
      infoCellAttr.setForeground(fgColor, cell.row, cell.col)
      infoCellAttr.setBackground(bgColor, cell.row, cell.col)
    }

    def setInfoCellColorByZero(value: Double, cell: ValueCell) {
      val bgColor = LookFeel().backgroundColor
      val fgColor = (
        if (value > 0) posColor
        else if (value < 0) negColor
        else neuColor
      )
      infoCellAttr.setForeground(fgColor, cell.row, cell.col)
      infoCellAttr.setBackground(bgColor, cell.row, cell.col)
    }

    setInfoCellColorByPrevCls(ticker.dayOpen, dayOpen)
    setInfoCellColorByPrevCls(ticker.dayLow, dayLow)
    setInfoCellColorByPrevCls(ticker.dayHigh, dayHigh)
    setInfoCellColorByPrevCls(ticker.lastPrice, lastPrice)
    setInfoCellColorByZero(ticker.dayChange, dayChange)
    setInfoCellColorByZero(ticker.dayChange, dayPercent)
  }
  
  private def updateDepthTable(marketDepth: MarketDepth) {
    val depth = marketDepth.depth
    var i = 0
    while (i < depth) {
      val bidIdx = depth - 1 - i
      val bidRow = i
      depthModel.setValueAt("%8.2f" format marketDepth.askPrice(bidIdx), bidRow, 1)
      depthModel.setValueAt(marketDepth.askSize(bidIdx).toInt.toString,  bidRow, 2)
      
      val askIdx = i
      val askRow = depth + i
      depthModel.setValueAt("%8.2f" format marketDepth.bidPrice(askIdx), askRow, 1)
      depthModel.setValueAt(marketDepth.bidSize(askIdx).toInt.toString,  askRow, 2)

      i += 1
    }
  }

  private def updateExecutionTable(prevClose: Double, execution: Execution) {
    // update last execution row in depth table
    val neuColor = LookFeel().getNeutralColor
    val posColor = LookFeel().getPositiveColor
    val negColor = LookFeel().getNegativeColor

    val bgColor = LookFeel().backgroundColor
    val fgColor = (
      if (execution.price > prevClose) posColor
      else if (execution.price < prevClose) negColor
      else neuColor
    )

    // update execution table
    executions += execution
    executionModel.fireTableDataChanged
    scrollToLastRow(executionTable)
  }

  private def scrollToLastRow(table: JTable) {
    if (table.getRowCount < 1) return
    
    // wrap in EDT to wait enough time to get rowCount updated
    SwingUtilities.invokeLater(new Runnable {
        def run {
          showCell(table, table.getRowCount - 1, 0)
        }
      })
  }

  private def showCell(table: JTable, row: Int, column: Int) {
    val rect = table.getCellRect(row, column, true)
    table.scrollRectToVisible(rect)
    table.clearSelection
    table.setRowSelectionInterval(row, row)
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
                                               hasFocus: Boolean, row: Int, column: Int): Component = {

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
            if (row - 1 >= 0) {
              try {
                var dValue = NUMBER_FORMAT.parse(value.toString.trim).doubleValue
                val prevValue = table.getValueAt(row - 1, column)
                if (prevValue != null) {
                  val prevDValue = NUMBER_FORMAT.parse(prevValue.toString.trim).doubleValue
                  if (dValue > prevDValue) {
                    setForeground(LookFeel().getPositiveBgColor)
                    setBackground(LookFeel().backgroundColor)
                  } else if (dValue < prevDValue) {
                    setForeground(LookFeel().getNegativeBgColor)
                    setBackground(LookFeel().backgroundColor)
                  } else {
                    setForeground(LookFeel().getNeutralBgColor)
                    setBackground(LookFeel().backgroundColor)
                  }
                }
              } catch {case ex: ParseException => ex.printStackTrace}
            }
          case 2 => // Size
            setHorizontalAlignment(SwingConstants.TRAILING)
        }

        setText(value.toString)
      }

      this
    }
  }

  def realTimeChartViewContainer: Option[ChartViewContainer] = {
    if (viewContainer == null) None else Some(viewContainer)
  }

  private def test {
//    executions += ((0, 1234f, 1))
//    executions += ((1, 1234f, 1))
//    executions += ((2, 1234f, 1))
//    executions += ((3, 1234f, 1))
//    executions += ((4, 1234f, 1))
    showCell(executionTable, executionTable.getRowCount - 1, 0)
  }
}

object ValueCell {
  def setRowCol(modelData: Array[Array[Object]]) {
    for (i <- 0 until modelData.length; rows = modelData(i);
         j <- 0 until rows.length; cell = rows(j)
    ) {
      cell match {
        case x: ValueCell =>
          x.row = i
          x.col = j
        case _ =>
      }
    }
  }
}

class ValueCell(var row: Int, var col: Int) {
  var value: String = _

  def this() = this(0, 0)

  override def toString = {
    value
  }
}
