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
package org.aiotrade.lib.charting.view.pane

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.text.DecimalFormat
import java.util.Calendar
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.MouseInputAdapter
import org.aiotrade.lib.math.timeseries.computable.Computable
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.plottable.Plot
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.chart.Chart
import org.aiotrade.lib.charting.chart.CursorChart
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartValidityObserver
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.ReferCursorObserver
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.charting.view.WithQuoteChart
import org.aiotrade.lib.charting.widget.Label
import org.aiotrade.lib.securities.QuoteItem
import org.aiotrade.lib.util.awt.AWTUtil
import org.aiotrade.lib.util.swing.AIOAutoHideComponent
import org.aiotrade.lib.util.swing.AIOCloseButton
import org.aiotrade.lib.util.swing.action.EditAction
import org.aiotrade.lib.util.swing.action.HideAction
import scala.collection.mutable.HashMap


/**
 * GlassPane overlaps mainChartPane, and is not opaque, thus we should carefully
 * define the contents of it, try the best to avoid add components on it, since
 * when the tranparent components change size, bounds, text etc will cause the
 * components repaint(), and cause the overlapped mainChartPane repaint() in chain.
 * That's why we here use a lot of none component-based lightweight textSegments,
 * pathSegments to draw texts, paths directly on GlassPane. Otherwise, should
 * carefully design container layout manage of the labels, especially do not call
 * any set property methods of labels in paint() routine.
 *
 * @author Caoyuan Deng
 */
class GlassPane(view: ChartView, datumPlane: DatumPlane) extends {
  private val TRANSPARENT_COLOR = new Color(0, 0, 0, 0)
  private val BUTTON_SIZE = 12
  private val BUTTON_DIMENSION = new Dimension(BUTTON_SIZE, BUTTON_SIZE)
  private val MONEY_DECIMAL_FORMAT = new DecimalFormat("0.###")
} with Pane(view, datumPlane) with WithCursorChart {

  private val overlappingSersToCloseButton = new HashMap[TSer, AIOCloseButton]
  private val overlappingSersToNameLabel = new HashMap[TSer, JLabel]
  private val selectedSerVarsToValueLabel = new HashMap[TVar[_], JLabel]
  private var selected: Boolean = _
  private var instantValueLabel: JLabel = _
  private var usingInstantTitleValue: Boolean = _


  setOpaque(false)
  setRenderStrategy(RenderStrategy.NoneBuffer)

  private val titlePanel = new JPanel
  titlePanel.setOpaque(false)
  titlePanel.setPreferredSize(new Dimension(10, view.TITLE_HEIGHT_PER_LINE))

  setLayout(new BorderLayout)
  add(titlePanel, BorderLayout.NORTH)

  private var selectedSer = getView.getMainSer

  private val closeButton = createCloseButton(getView.getMainSer)
  private val nameLabel = createNameLabel(getView.getMainSer)

  /** Container should be JComponent instead of JPanel to show selectedMark ? */
  private val pinnedMark = new PinnedMark
  pinnedMark.setPreferredSize(BUTTON_DIMENSION)

  titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0))
  titlePanel.add(closeButton)
  titlePanel.add(nameLabel)
  //titlePanel.add(pinnedMark);

  val paneMouseListener = new PaneMouseInputAdapter
  addMouseListener(paneMouseListener)
  addMouseMotionListener(paneMouseListener)

  view.getController.addObserver(this, new ReferCursorObserver[ChartingController] {

      def update(controller: ChartingController) {
        if (!isUsingInstantTitleValue) {
          updateSelectedSerVarValues
        }
      }
    }.asInstanceOf[ReferCursorObserver[Any]])

  view.getController.addObserver(this, new ChartValidityObserver[ChartingController] {

      def update(controller: ChartingController) {
        updateMainName
        updateOverlappingNames
        if (!isUsingInstantTitleValue) {
          updateSelectedSerVarValues
        }
      }
    }.asInstanceOf[ChartValidityObserver[Any]])

  view.addObserver(this, new ChartValidityObserver[ChartView] {

      def update(view: ChartView) {
        updateMainName
        updateOverlappingNames
        if (!isUsingInstantTitleValue) {
          updateSelectedSerVarValues
        }
      }
    }.asInstanceOf[ChartValidityObserver[Any]])

  /**
   * @todo updateTitle() when
   * comparing chart added
   */
  

  private def createCloseButton(ser: TSer): AIOCloseButton = {
    val button = new AIOCloseButton
    button.setOpaque(false)
    button.setForeground(LookFeel().axisColor)
    button.setFocusable(false);
    button.setPreferredSize(BUTTON_DIMENSION)
    button.setMaximumSize(BUTTON_DIMENSION)
    button.setMinimumSize(BUTTON_DIMENSION)
    button.setVisible(true)

    button.addActionListener(new ActionListener {

        def actionPerformed(e: ActionEvent) {
          if (getView.getParent.isInstanceOf[ChartViewContainer]) {
            if (ser == getSelectedSer) {
              if (ser != getView.getMainSer) {
                setSelectedSer(getView.getMainSer)
              } else {
                setSelectedSer(null)
              }
            }
            val contents = getView.getController.getContents
            contents.lookupDescriptor(classOf[IndicatorDescriptor],
                                      ser.getClass.getName,
                                      ser.freq
            ) foreach {descriptor =>
              descriptor.lookupAction(classOf[HideAction]) foreach {action =>
                action.execute
              }
            }
          }
        }
      })

    return button
  }

  private def createNameLabel(ser: TSer): JLabel = {
    val label = new JLabel
    label.setOpaque(false)
    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    label.setHorizontalAlignment(SwingConstants.CENTER)
    label.setPreferredSize(null) // null, let layer manager to decide the size
    label.setVisible(true)
    val mouseListener = new NameLabelMouseInputListener(ser, label)
    label.addMouseListener(mouseListener)
    label.addMouseMotionListener(mouseListener)

    label
  }

  private final class NameLabelMouseInputListener(ser: TSer, label: JLabel) extends MouseInputAdapter {

    private var rolloverEffectSet: Boolean = _

    override def mouseClicked(e: MouseEvent) {
      setSelectedSer(ser)
      setSelected(true)
      if (e.getClickCount == 2) {
        val contents = getView.getController.getContents
        contents.lookupDescriptor(classOf[IndicatorDescriptor],
                                  ser.getClass.getName,
                                  ser.freq
        ) foreach {descriptor =>
          descriptor.lookupAction(classOf[EditAction]) foreach {action =>
            action.execute
          }
        }
      }
    }

    override def mouseMoved(e: MouseEvent) {
      if (!rolloverEffectSet) {
        /** @todo */
        label.setBackground(LookFeel().borderColor)
        rolloverEffectSet = true
        label.repaint()
      }
    }

    override def mouseExited(e: MouseEvent) {
      /** @todo */
      label.setBackground(LookFeel().backgroundColor)
      rolloverEffectSet = false
      label.repaint()
    }
  }

  override protected def plotPane {
  }

  def isUsingInstantTitleValue: Boolean = {
    return usingInstantTitleValue
  }

  def setUsingInstantTitleValue(b: Boolean) {
    this.usingInstantTitleValue = b
  }

  private def updateMainName {
    closeButton.setForeground(LookFeel().axisColor)
    closeButton.setBackground(LookFeel().backgroundColor)
    if (getSelectedSer == getView.getMainSer) {
      closeButton.setChosen(true)
    } else {
      closeButton.setChosen(false)
    }

    nameLabel.setForeground(LookFeel().nameColor)
    nameLabel.setBackground(LookFeel().backgroundColor)
    nameLabel.setFont(LookFeel().axisFont)
    nameLabel.setText(Computable.displayName(getView.getMainSer))

    titlePanel.revalidate
    titlePanel.repaint()

    /** name of comparing quote */
    //        if ( view instanceof AnalysisQuoteChartView) {
    //            final FontMetrics fm = getFontMetrics(getFont());
    //            int xPositionLine2 = MARK_SIZE + 1;
    //            Map<QuoteCompareIndicator, QuoteChart> quoteCompareSerChartMap = ((AnalysisQuoteChartView)view).getQuoteCompareSerChartMap();
    //            for (Ser ser : quoteCompareSerChartMap.keySet()) {
    //                String comparingQuoteName = ser.getShortDescription() + "  ";
    //                Color color = quoteCompareSerChartMap.get(ser).getForeground();
    //                TextSegment text2 = new TextSegment(comparingQuoteName, xPositionLine2, ChartView.TITLE_HEIGHT_PER_LINE * 2, color, null);
    //                addSegment(text2);
    //                xPositionLine2 += fm.stringWidth(comparingQuoteName);
    //            }
    //        }
  }

  final private def updateOverlappingNames {
    var begIdx = 2
    val overlappingSers = view.getOverlappingSers
    for (ser <- overlappingSers) {
      var label = overlappingSersToNameLabel.get(ser).getOrElse(null)
      var button = overlappingSersToCloseButton.get(ser) match {
        case Some(x) => 
          begIdx += 2
          x
        case None =>
          val buttonx = createCloseButton(ser)
          label = createNameLabel(ser)

          titlePanel.add(buttonx, begIdx)
          begIdx += 1
          titlePanel.add(label, begIdx)
          begIdx += 1
          overlappingSersToCloseButton.put(ser, buttonx)
          overlappingSersToNameLabel.put(ser, label)
          buttonx
      }

      button.setForeground(LookFeel().axisColor)
      button.setBackground(LookFeel().backgroundColor)
      if (getSelectedSer == ser) {
        button.setChosen(true)
      } else {
        button.setChosen(false)
      }

      label.setForeground(LookFeel().nameColor)
      label.setBackground(LookFeel().backgroundColor)
      label.setFont(LookFeel().axisFont)
      label.setText(Computable.displayName(ser))
    }

    /** remove unused ser's buttons and labels */
    val toBeRemoved = overlappingSersToCloseButton.keysIterator filter {ser => !overlappingSers.contains(ser)}
    for (ser <- toBeRemoved) {
      val button = overlappingSersToCloseButton(ser)
      val label = overlappingSersToNameLabel(ser)
      AWTUtil.removeAllAWTListenersOf(button)
      AWTUtil.removeAllAWTListenersOf(label)
      titlePanel.remove(button)
      titlePanel.remove(label)
      overlappingSersToCloseButton.remove(ser)
      overlappingSersToNameLabel.remove(ser)
    }

    titlePanel.revalidate
    titlePanel.repaint()
  }

  /**
   * update name and valueStr of all the vars in this view's selected ser.
   * all those vars with var.getPlot() != Plot.None will be shown with value.
   */
  private def updateSelectedSerVarValues {
    val ser = getSelectedSer
    if (ser == null) {
      return;
    }

    val referTime = getView.getController.getReferCursorTime
    val item = ser(referTime);
    if (item != null) {
      val serVars = ser.vars
      for (v <- serVars if v.plot != Plot.None) {
        val vStr = new StringBuilder().append(" ").append(v.name).append(": ").append(MONEY_DECIMAL_FORMAT.format(item.getFloat(v)))

        /** lookup this var's chart and use chart's color if possible */
        var chartOfVar: Chart = null
        val chartToVars = getView.getChartMapVars(ser)
        val keys = chartToVars.keysIterator
        while (keys.hasNext && chartOfVar != null) {
          val chart = keys.next
          chartToVars.get(chart) foreach {vars =>
            if (vars exists {_ == v}) {
              chartOfVar = chart
            }
          }
        }
        val color = if (chartOfVar == null) LookFeel().nameColor else chartOfVar.getForeground

        val valueLabel = selectedSerVarsToValueLabel.get(v) getOrElse {
          val valueLabelx = new JLabel
          valueLabelx.setOpaque(false)
          valueLabelx.setHorizontalAlignment(SwingConstants.LEADING)
          valueLabelx.setPreferredSize(null) // null, let the UI delegate to decide the size

          titlePanel.add(valueLabelx)
          selectedSerVarsToValueLabel.put(v, valueLabelx)
          valueLabelx
        }

        valueLabel.setForeground(color)
        valueLabel.setBackground(LookFeel().backgroundColor)
        valueLabel.setFont(LookFeel().axisFont)
        valueLabel.setText(vStr.toString)
      }

      /** remove unused vars and their labels */
      val toBeRemoved = selectedSerVarsToValueLabel.keysIterator filter {v => !serVars.contains(v) || v.plot == Plot.None}
      for (v <- toBeRemoved) {
        val label = selectedSerVarsToValueLabel(v)
        // label maybe null? not init yet?
        if (label != null) {
          AWTUtil.removeAllAWTListenersOf(label)
          titlePanel.remove(label)
        }
        selectedSerVarsToValueLabel.remove(v)
      }
    }

    titlePanel.revalidate
    titlePanel.repaint()
  }

  def updateInstantValue(valueStr: String, color: Color) {
    if (instantValueLabel == null) {
      instantValueLabel = new JLabel
      instantValueLabel.setOpaque(false)
      instantValueLabel.setHorizontalAlignment(SwingConstants.LEADING)
      instantValueLabel.setPreferredSize(null) // null, let the UI delegate to decide the size

      titlePanel.add(instantValueLabel)
    }

    instantValueLabel.setForeground(color)
    instantValueLabel.setBackground(LookFeel().backgroundColor)
    instantValueLabel.setFont(LookFeel().axisFont)
    instantValueLabel.setText(valueStr)
  }

  def isSelected: Boolean = {
    this.selected
  }

  def setSelected(b: Boolean) {
    val oldValue = isSelected
    this.selected = b
    if (isSelected != oldValue) {
      /** todo: still need this? */
    }
  }

  final private def setSelectedSer(selectedSer: TSer) {
    val oldValue = getSelectedSer
    this.selectedSer = selectedSer
    if (getSelectedSer != oldValue) {
      updateMainName
      updateOverlappingNames
      if (!isUsingInstantTitleValue) {
        updateSelectedSerVarValues
      }
    }
  }

  final private def getSelectedSer: TSer = {
    selectedSer
  }

  def setInteractive(b: Boolean) {
    closeButton.setVisible(b)
  }

  def setPinned(b: Boolean) {
    pinnedMark.setAutoHidden(!b)
  }

  /**
   * @NOTICE
   * This will be and only be called when I have mouse motion listener
   */
  override protected def processMouseMotionEvent(e: MouseEvent) {
    /** fire to my listeners */
    super.processMouseMotionEvent(e)

    forwardMouseEventToWhoMayBeCoveredByMe(e)
  }

  /**
   * !NOTICE
   * This will be and only be called when I have mouse listener
   */
  override protected def processMouseEvent(e: MouseEvent) {
    /** fire to my listeners */
    super.processMouseEvent(e)

    forwardMouseEventToWhoMayBeCoveredByMe(e)
  }

  private def forwardMouseEventToWhoMayBeCoveredByMe(e: MouseEvent) {
    forwardMouseEvent(this, getView.getMainChartPane, e)
    forwardMouseEvent(this, getView.getParent, e)

    if (getView.isInstanceOf[WithDrawingPane]) {
      val drawingPane = getView.asInstanceOf[WithDrawingPane].getSelectedDrawing
      if (drawingPane != null) {
        forwardMouseEvent(this, drawingPane, e)
        if (drawingPane.getSelectedHandledChart != null) {
          setCursor(drawingPane.getSelectedHandledChart.getCursor)
        } else {
          /**
           * @credit from msayag@users.sourceforge.net
           * set to default cursor what ever, especilly when a handledChart
           * was just deleted.
           */
          setCursor(Cursor.getDefaultCursor)
        }
      }
    }
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    getView.getController.removeObserversOf(this)
    getView.removeObserversOf(this)

    AWTUtil.removeAllAWTListenersOf(nameLabel)
    AWTUtil.removeAllAWTListenersOf(this)

    super.finalize
  }

  class PaneMouseInputAdapter extends MouseInputAdapter {

    override def mouseClicked(e: MouseEvent) {
      val activeComponent = getActiveComponentAt(e)
      if (activeComponent == null) {
        return
      }

      if (!(getView.getParent.isInstanceOf[ChartViewContainer])) {
        return
      }
      val viewContainer = view.getParent.asInstanceOf[ChartViewContainer]

      if (activeComponent == titlePanel) {

        if (e.getClickCount == 1) {

          if (viewContainer.isInteractive) {
            viewContainer.setSelectedView(getView)
          } else {
            if (viewContainer.isPinned) {
              viewContainer.unPin
            } else {
              viewContainer.pin
            }
          }

        } else if (e.getClickCount == 2) {

          getView.popupToDesktop

        }

      } else if (activeComponent == pinnedMark) {

        if (getView.isPinned) {
          getView.unPin
        } else {
          getView.pin
        }

      }
    }

    override def mouseMoved(e: MouseEvent) {
      getActiveComponentAt(e)
    }

    override def mouseExited(e: MouseEvent) {
      getActiveComponentAt(e)
    }

    /**
     * Decide which componet is active and return it.
     * @return actived component or <code>null</code>
     */
    private def getActiveComponentAt(e: MouseEvent): Component = {
      val p = e.getPoint

      if (pinnedMark.contains(p)) {
        pinnedMark.setHidden(false)
        return pinnedMark
      } else {
        pinnedMark.setHidden(true)
      }

      if (titlePanel.contains(p)) {
        return titlePanel
      }

      return null
    }

    override def mouseDragged(e: MouseEvent) {
    }
  }

  /**
   * Inner pinned mark class
   */
  private class PinnedMark extends AIOAutoHideComponent {

    setOpaque(false)
    setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE))

    setCursor(Cursor.getDefaultCursor)
    
    override protected def paintComponent(g0: Graphics) {
      if (isHidden) {
        return
      }

      val g = g0.asInstanceOf[Graphics2D]
      g.setColor(LookFeel().axisColor)
      val w = getWidth - 3
      val h = getHeight - 3

      if (!autoHidden) {
        /** pinned, draw pinned mark (an filled circle) */
        g.fillOval(2, 2, w, h)
      } else {
        if (!hidden) {
          /** draw to pin mark (an filled circle) */
          g.fillOval(2, 2, w, h)
        }
      }
    }
  }

  /**
   * implement WithCursorChart
   * ----------------------------------------------------
   */
  def createCursorChartInstance(datumPlane: DatumPlane): CursorChart = {
    new MyCursorChart
  }

  private class MyCursorChart extends CursorChart {

    private val cal = Calendar.getInstance

    protected def plotReferCursor {
      val h = GlassPane.this.getHeight
      val w = GlassPane.this.getWidth

      /** plot cross' vertical line */
      if (isCursorCrossVisible) {
        cursorPath.moveTo(x, 0)
        cursorPath.lineTo(x, h)
      }

      if (getView.isInstanceOf[WithQuoteChart]) {
        val quoteSer = GlassPane.this.getView.asInstanceOf[WithQuoteChart].getQuoteSer
        val item = quoteSer.itemOfRow(referRow).asInstanceOf[QuoteItem]
        if (item != null) {
          val y = if (isAutoReferCursorValue) yv(item.close) else yv(getReferCursorValue)

          /** plot cross' horizonal line */
          if (isCursorCrossVisible) {
            cursorPath.moveTo(0, y)
            cursorPath.lineTo(w, y)
          }
        }
      }

    }

    protected def plotMouseCursor {
      val w = GlassPane.this.getWidth
      val h = GlassPane.this.getHeight

      val mainChartPane = GlassPane.this.getView.getMainChartPane

      /** plot vertical line */
      if (isCursorCrossVisible) {
        cursorPath.moveTo(x, 0)
        cursorPath.lineTo(x, h)
      }

      var y: Float = 0f
      if (GlassPane.this.getView.isInstanceOf[WithQuoteChart]) {
        cal.setTimeInMillis(mouseTime)

        val quoteSer = GlassPane.this.getView.asInstanceOf[WithQuoteChart].getQuoteSer
        var item = quoteSer.itemOfRow(mouseRow).asInstanceOf[QuoteItem]
        val vMouse = if (item == null) 0 else item.close

        if (mainChartPane.isMouseEntered) {
          y = mainChartPane.getYMouse
        } else {
          y = if (item == null) 0 else mainChartPane.yv(item.close)
        }


        /** plot horizonal line */
        if (isCursorCrossVisible) {
          cursorPath.moveTo(0, y)
          cursorPath.lineTo(w, y)
        }

        val vDisplay = mainChartPane.vy(y)

        val str = /** normal QuoteChartView ? */
          if (isAutoReferCursorValue) {
            item = quoteSer.itemOfRow(referRow).asInstanceOf[QuoteItem]
            val vRefer = if (item == null) 0f else item.close

            val period = br(mouseRow) - br(referRow)
            val percent = if (vRefer == 0) 0f else 100 * (mainChartPane.vy(y) - vRefer) / vRefer

            var volumeSum = 0f
            val rowBeg = Math.min(referRow, mouseRow)
            val rowEnd = Math.max(referRow, mouseRow)
            var i = rowBeg
            while (i <= rowEnd) {
              item = quoteSer.itemOfRow(i).asInstanceOf[QuoteItem]
              if (item != null) {
                volumeSum += item.volume
              }
              i += 1
            }

            new StringBuilder(20).append("P: ").append(period).append("  ").append("%+3.2f".format(percent)).append("%").append("  V: ").append("%5.0f".format(volumeSum)).toString
          } else { /** else, usually RealtimeQuoteChartView */
            val vRefer = GlassPane.this.getReferCursorValue
            val percent = if (vRefer == 0) 0f else 100 * (mainChartPane.vy(y) - vRefer) / vRefer

            new StringBuilder(20).append(MONEY_DECIMAL_FORMAT.format(vDisplay)).append("  ").append("%+3.2f".format(percent)).append("%").toString
          }

        val label = addChild(new Label)
        label.setForeground(laf.nameColor)
        label.setFont(laf.axisFont)

        val fm = getFontMetrics(label.getFont)
        label.model.set(w - fm.stringWidth(str) - (BUTTON_SIZE + 1), view.TITLE_HEIGHT_PER_LINE - 2, str)
        label.plot
      } else { /** indicator view */
        if (mainChartPane.isMouseEntered) {
          y = mainChartPane.getYMouse

          /** plot horizonal line */
          if (isCursorCrossVisible) {
            cursorPath.moveTo(0, y)
            cursorPath.lineTo(w, y)
          }
        }
      }

    }
  }
}
