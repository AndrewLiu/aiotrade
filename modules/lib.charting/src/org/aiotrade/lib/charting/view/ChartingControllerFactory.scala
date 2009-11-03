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
package org.aiotrade.lib.charting.view

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseWheelListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JFrame
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.math.timeseries.MasterTSer
import org.aiotrade.lib.math.timeseries.SerChangeEvent
import org.aiotrade.lib.math.timeseries.SerChangeListener
import javax.swing.WindowConstants
import org.aiotrade.lib.util.ChangeObserver
import org.aiotrade.lib.util.ChangeObservable
import org.aiotrade.lib.util.ChangeObservableHelper
import org.aiotrade.lib.util.collection.ArrayList
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet


/**
 * Only DefaultChartingController will be provided in this factory
 *
 * @author Caoyuan Deng
 */
object ChartingControllerFactory {
  /** a static map to know how many controllers are bound with each MasterSer */
  private val sersWithcontrollers = new HashMap[MasterTSer, HashSet[ChartingController]]
  private var cursorAccelerated = false

  def createInstance(masterSer: MasterTSer, contents: AnalysisContents): ChartingController = {
    val controllers = sersWithcontrollers.get(masterSer) getOrElse {
      val controllersx = new HashSet[ChartingController]
      sersWithcontrollers += (masterSer -> controllersx)
      controllersx
    }

    val controller = new DefaultChartingController(masterSer, contents)
    controllers.add(controller)

    controller
  }

  def isCursorAccelerated: Boolean = {
    cursorAccelerated
  }

  def setCursorAccelerated(b: Boolean) {
    cursorAccelerated = b
  }

  object DefaultChartingController {
    /**
     * min spacing between rightRow and left / right edge, if want more, such as:
     *     minSpacing = (nBars * 0.168).intValue
     */
    private val MIN_RIGHT_SPACING = 2
    private val MIN_LEFT_SPACING = 0

    /** BASIC_BAR_WIDTH = 6 */
    private val BAR_WIDTHS_ARRAY = Array(
      0.00025f, 0.0005f, 0.001f, 0.025f, 0.05f, 0.1f, 0.25f, 0.5f, 1f, 2f, 4f, 6f, 10f, 20f
    )
  }

  /**
   * DefaultChartingController that implements ChartingController
   */
  private class DefaultChartingController(masterSer: MasterTSer, contents: AnalysisContents) extends ChartingController {
    import DefaultChartingController._
    private val popupViews = new HashSet[ChartView]
    private var viewContainer: ChartViewContainer = _
    private var wBarIdx = 11
    /** pixels per bar (bar width in pixels) */
    private var wBar = BAR_WIDTHS_ARRAY(wBarIdx)
    private var referCursorRow: Int = _
    private var mouseCursorRow: Int = _
    private var rightSideRow: Int = _
    private var lastOccurredRowOfMasterSer: Int = _
    private var autoScrollToNewData = true
    private var mouseEnteredAnyChartPane: Boolean = _
    private var cursorCrossLineVisible = true
    private var mySerChangeListener: MasterSerChangeListener = _
    private val observableHelper = new ChangeObservableHelper

    private def internal_setChartViewContainer(viewContainer: ChartViewContainer) {
      this.viewContainer = viewContainer

      internal_initCursorRow

      if (mySerChangeListener == null) {
        mySerChangeListener = new MasterSerChangeListener
        masterSer.addSerChangeListener(mySerChangeListener)
      }

      addKeyMouseListenersTo(viewContainer)
    }

    private def internal_initCursorRow {
      /**
       * masterSer may have finished computing at this time, to adjust
       * the cursor to proper row, update it here.
       * @NOTICE
       * don't set row directly, instead, use setCursorByRow(row, row);
       */
      val row = getMasterSer.lastOccurredRow
      setCursorByRow(row, row, true)

      mouseCursorRow = getReferCursorRow
    }

    private def addKeyMouseListenersTo(component: JComponent) {
      component.setFocusable(true);
      component.addKeyListener(new ViewKeyAdapter);
      component.addMouseWheelListener(new ViewMouseWheelListener)
    }

    private def removeKeyMouseListenersFrom(component: JComponent) {
      /** use a list to avoid concurrent issue */
      val toBeRemoved = new ArrayList[AnyRef]

      val ls = component.getKeyListeners.iterator
      while (ls.hasNext) {
        toBeRemoved += ls.next
      }
      for (l <- toBeRemoved) {
        component.removeKeyListener(l.asInstanceOf[KeyListener])
      }

      toBeRemoved.clear
      val ls2 = component.getMouseWheelListeners.iterator
      while (ls2.hasNext) {
        toBeRemoved += ls.next
      }
      for (l <- toBeRemoved) {
        component.removeMouseWheelListener(l.asInstanceOf[MouseWheelListener])
      }
    }

    def getMasterSer: MasterTSer = {
      masterSer
    }

    def getContents: AnalysisContents = {
      contents
    }

    def setCursorCrossLineVisible(b: Boolean) {
      this.cursorCrossLineVisible = b
    }

    def isCursorCrossLineVisible: Boolean = {
      cursorCrossLineVisible
    }

    def isMouseEnteredAnyChartPane: Boolean = {
      mouseEnteredAnyChartPane
    }

    def setMouseEnteredAnyChartPane(b: Boolean) {
      val oldValue = this.mouseEnteredAnyChartPane
      this.mouseEnteredAnyChartPane = b

      if (!mouseEnteredAnyChartPane) {
        /** this cleanups mouse cursor */
        if (this.mouseEnteredAnyChartPane != oldValue) {
          notifyObserversChanged(classOf[MouseCursorObserver[Any]])
          updateViews
        }
      }

    }

    def setAutoScrollToNewData(autoScrollToNewData: Boolean) {
      this.autoScrollToNewData = autoScrollToNewData
    }

    def getWBar: Float = {
      wBar
    }

    def growWBar(increment: Int) {
      wBarIdx += increment
      if (wBarIdx < 0) {
        wBarIdx = 0
      } else if (wBarIdx > BAR_WIDTHS_ARRAY.length - 1) {
        wBarIdx = BAR_WIDTHS_ARRAY.length - 1
      }

      internal_setWBar(BAR_WIDTHS_ARRAY(wBarIdx))
      updateViews
    }

    def setWBarByNBars(wViewPort: Int, nBars: Int) {
      if (nBars < 0) {
        return
      }

      /** decide wBar according to wViewPort. Do not use integer divide here */
      var newWBar = wViewPort.toFloat / nBars.toFloat

      /** adjust xfactorIdx to nearest */
      if (newWBar < BAR_WIDTHS_ARRAY(0)) {
        /** avoid too small xfactor */
        newWBar = BAR_WIDTHS_ARRAY(0)

        wBarIdx = 0
      } else if (newWBar > BAR_WIDTHS_ARRAY(BAR_WIDTHS_ARRAY.length - 1)) {
        wBarIdx = BAR_WIDTHS_ARRAY.length - 1
      } else {
        var i = 0
        val n = BAR_WIDTHS_ARRAY.length - 1
        var break = false
        while (i < n && !break) {
          if (newWBar > BAR_WIDTHS_ARRAY(i) && newWBar < BAR_WIDTHS_ARRAY(i + 1)) {
            /** which one is the nearest ? */
            wBarIdx = if (Math.abs(BAR_WIDTHS_ARRAY(i) - newWBar) < Math.abs(BAR_WIDTHS_ARRAY(i + 1) - newWBar)) i else i + 1
            break = true
          }
          i += 1
        }
      }

      internal_setWBar(newWBar)
      updateViews
    }

    def isOnCalendarMode: Boolean = {
      getMasterSer.isOnCalendarMode
    }

    def setOnCalendarMode(b: Boolean) {
      if (isOnCalendarMode != b) {
        val referCursorTime = getReferCursorTime
        val rightCursorTime = getRightSideTime

        if (b == true) {
          getMasterSer.setOnCalendarMode
        } else {
          getMasterSer.setOnOccurredMode
        }

        internal_setReferCursorByTime(referCursorTime)
        internal_setRightCursorByTime(rightCursorTime)

        notifyObserversChanged(classOf[ChartValidityObserver[Any]])
        updateViews
      }
    }

    def setCursorByRow(referRow: Int, rightRow: Int, updateViews: Boolean) {
      /** set right cursor row first and directly */
      internal_setRightSideRow(rightRow)

      val oldValue = getReferCursorRow
      scrollReferCursor(referRow - oldValue, updateViews)
    }

    def setReferCursorByRow(row: Int, updateViews: Boolean) {
      val increment = row - getReferCursorRow
      scrollReferCursor(increment, updateViews)
    }

    def scrollReferCursor(increment: Int, updateViews: Boolean) {
      var referRow = getReferCursorRow
      val rightRow = getRightSideRow

      referRow += increment

      val rightSpacing = rightRow - referRow
      if (rightSpacing >= MIN_RIGHT_SPACING) {
        /** right spacing is enough, check left spacing: */
        val nBars = viewContainer.getMasterView.getNBars
        val leftRow = rightRow - nBars + 1
        val leftSpacing = referRow - leftRow
        if (leftSpacing < MIN_LEFT_SPACING) {
          internal_setRightSideRow(rightRow + leftSpacing - MIN_LEFT_SPACING)
        }
      } else {
        internal_setRightSideRow(rightRow + MIN_RIGHT_SPACING - rightSpacing)

      }

      internal_setReferCursorRow(referRow)
      if (updateViews) {
        updateViews
      }
    }

    /** keep refer cursor stay on same x of screen, and scroll charts left or right by bar */
    def scrollChartsHorizontallyByBar(increment: Int) {
      val rightRow = getRightSideRow
      internal_setRightSideRow(rightRow + increment)

      scrollReferCursor(increment, true)
    }

    def scrollReferCursorToLeftSide {
      val rightRow = getRightSideRow
      val nBars = viewContainer.getMasterView.getNBars

      val leftRow = rightRow - nBars + MIN_LEFT_SPACING
      setReferCursorByRow(leftRow, true)
    }

    def setMouseCursorByRow(row: Int) {
      internal_setMouseCursorRow(row)
    }

    def updateViews {
      if (viewContainer != null) {
        viewContainer.repaint()
      }

      /**
       * as repaint() may be called by awt in instance's initialization, before
       * popupViewSet is created, so, check null.
       */
      if (popupViews != null) {
        for (view <- popupViews) {
          view.repaint()
        }
      }
    }

    def addObserver(owner: AnyRef, observer: ChangeObserver[Any]) {
      observableHelper.addObserver(owner, observer)
    }

    def removeObserver(observer: ChangeObserver[Any]) {
      observableHelper.removeObserver(observer)
    }

    def removeObserversOf(owner: Object) {
      observableHelper.removeObserversOf(owner)
    }

    /**
     * Changed cases for ChartValidityObserver:
     *   rightSideRow
     *   referCursorRow
     *   wBar
     *   onCalendarMode
     * Change cases for MouseCursorObserver:
     *   mosueCursor
     *   mouseEnteredAnyChartPane
     */
    def notifyObserversChanged(observerType: Class[_ <: ChangeObserver[Any]]) {
      observableHelper.notifyObserversChanged(this, observerType)
    }

    final def getReferCursorRow: Int = {
      referCursorRow
    }

    final def getReferCursorTime: Long = {
      getMasterSer.timeOfRow(referCursorRow)
    }

    final def getRightSideRow: Int = {
      rightSideRow
    }

    final def getRightSideTime: Long = {
      getMasterSer.timeOfRow(rightSideRow)
    }

    final def getLeftSideRow: Int = {
      val rightRow = getRightSideRow
      val nBars = viewContainer.getMasterView.getNBars

      rightRow - nBars + MIN_LEFT_SPACING
    }

    final def getLeftSideTime: Long = {
      getMasterSer.timeOfRow(getLeftSideRow)
    }

    final def getMouseCursorRow: Int = {
      mouseCursorRow
    }

    final def getMouseCursorTime: Long = {
      getMasterSer.timeOfRow(mouseCursorRow)
    }

    /**
     * @NOTICE
     * =======================================================================
     * as we don't like referCursor and rightCursor being set directly by others,
     * the following setter methods are named internal_setXXX, and are private.
     */
    private def internal_setWBar(wBar: Float) {
      val oldValue = this.wBar
      this.wBar = wBar
      if (this.wBar != oldValue) {
        notifyObserversChanged(classOf[ChartValidityObserver[Any]])
      }
    }

    private def internal_setReferCursorRow(row: Int) {
      val oldValue = this.referCursorRow
      this.referCursorRow = row
      /** remember the lastRow for decision if need update cursor, see changeCursorByRow() */
      this.lastOccurredRowOfMasterSer = masterSer.lastOccurredRow
      if (this.referCursorRow != oldValue) {
        notifyObserversChanged(classOf[ReferCursorObserver[Any]])
        notifyObserversChanged(classOf[ChartValidityObserver[Any]])
      }
    }

    private def internal_setRightSideRow(row: Int) {
      val oldValue = this.rightSideRow
      this.rightSideRow = row
      if (this.rightSideRow != oldValue) {
        notifyObserversChanged(classOf[ChartValidityObserver[Any]])
      }
    }

    private def internal_setReferCursorByTime(time: Long) {
      internal_setReferCursorRow(getMasterSer.rowOfTime(time))
    }

    private def internal_setRightCursorByTime(time: Long) {
      internal_setRightSideRow(getMasterSer.rowOfTime(time))
    }

    private def internal_setMouseCursorRow(row: Int) {
      val oldValue = this.mouseCursorRow
      this.mouseCursorRow = row

      /**
       * even mouseCursor row not changed, the mouse's y may has been changed,
       * so, notify observers without comparing the oldValue and newValue.
       */
      notifyObserversChanged(classOf[MouseCursorObserver[Any]])
    }

    def isCursorAccelerated: Boolean = {
      cursorAccelerated
    }

    def setCursorAccelerated(b: Boolean) {
      cursorAccelerated = b
    }

    def popupViewToDesktop(view: ChartView, dimendion: Dimension, alwaysOnTop: Boolean, joint: Boolean) {
      val popupView = view

      popupViews.add(popupView)
      addKeyMouseListenersTo(popupView)

      val w = dimendion.width
      val h = dimendion.height
      val frame = new JFrame//new JDialog (), true);
      frame.setAlwaysOnTop(alwaysOnTop)
      frame.setTitle(popupView.getMainSer.shortDescription)
      frame.add(popupView, BorderLayout.CENTER)
      val screenSize = Toolkit.getDefaultToolkit.getScreenSize
      frame.setBounds((screenSize.width - w) / 2, (screenSize.height - h) / 2, w, h)
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      frame.addWindowListener(new WindowAdapter {

          override
          def windowClosed(e: WindowEvent) {
            removeKeyMouseListenersFrom(popupView)
            popupViews.remove(popupView)
          }
        })

      frame.setVisible(true)
    }

    /**
     * Factory method to create ChartViewContainer instance, got the relations
     * between ChartViewContainer and Controller ready.
     */
    def createChartViewContainer[T <: ChartViewContainer](clazz: Class[T], focusableParent: Component): T  = {
      var instance: T = null
      try {
        instance = clazz.newInstance
      } catch {
        case ex: InstantiationException => ex.printStackTrace
        case ex: IllegalAccessException => ex.printStackTrace
      }

      if (instance != null) {
        instance.init(focusableParent, this)
        /**
         * @NOTICE
         * Always call internal_setChartViewContainer(instance) next to
         * instance.init(focusableParent, this), since the internal_initCursorRow()
         * procedure needs the children of chartViewContainer ready.
         */
        internal_setChartViewContainer(instance)
      }

      instance
    }

    @throws(classOf[Throwable])
    override protected def finalize {
      if (mySerChangeListener != null) {
        masterSer.removeSerChangeListener(mySerChangeListener)
      }

      super.finalize
    }

    /**
     * listen to masterSer and process loading, update events to check if need to update cursor
     */
    class MasterSerChangeListener extends SerChangeListener {

      def serChanged(evt: SerChangeEvent) {
        if (!autoScrollToNewData) {
          return
        }

        /** this method only process loading, update events to check if need to update cursor */
        evt.tpe match {
          case SerChangeEvent.Type.FinishedLoading | SerChangeEvent.Type.RefreshInLoading | SerChangeEvent.Type.Updated =>
            val masterView = viewContainer.getMasterView
            if (masterView.isInstanceOf[WithDrawingPane]) {
              val drawing = masterView.asInstanceOf[WithDrawingPane].getSelectedDrawing
              if (drawing != null && drawing.isInDrawing) {
                return
              }
            }

            val oldReferRow = getReferCursorRow
            if (oldReferRow == lastOccurredRowOfMasterSer || lastOccurredRowOfMasterSer <= 0) {
              /** refresh only when the old lastRow is extratly oldReferRow, or prev lastRow <= 0 */
              val lastTime = Math.max(evt.endTime, masterSer.lastOccurredTime)
              val rightRow = masterSer.rowOfTime(lastTime)
              val referRow = rightRow

              setCursorByRow(referRow, rightRow, true)
            }

            notifyObserversChanged(classOf[ChartValidityObserver[Any]])
            
          case _ =>
        }
      }
    }

    private def internal_getCorrespondingChartView(e: InputEvent): ChartView = {
      var correspondingChartView: ChartView = null

      e.getSource match {
        case source: ChartViewContainer =>
          correspondingChartView = source.getMasterView
        case source: ChartView =>
          correspondingChartView = source
        case _ => null
      }

      correspondingChartView
    }

    /**
     * =============================================================
     * Bellow is the private listener classes for key and mouse:
     */
    class ViewKeyAdapter extends KeyAdapter {

      private val LEFT = -1
      private val RIGHT = 1

      override
      def keyPressed(e: KeyEvent) {
        val view = internal_getCorrespondingChartView(e)
        if (view == null || !view.isInteractive) {
          return
        }

        val fastSteps = (view.getNBars * 0.168f).intValue

        e.getKeyCode match {
          case KeyEvent.VK_LEFT =>
            if (e.isControlDown()) {
              moveCursorInDirection(fastSteps, LEFT)
            } else {
              moveChartsInDirection(fastSteps, LEFT)
            }
          case KeyEvent.VK_RIGHT =>
            if (e.isControlDown) {
              moveCursorInDirection(fastSteps, RIGHT)
            } else {
              moveChartsInDirection(fastSteps, RIGHT)
            }
          case KeyEvent.VK_UP =>
            growWBar(1)
          case KeyEvent.VK_DOWN =>
            growWBar(-1)
          case _ =>
        }

      }

      override def keyReleased(e: KeyEvent) {

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          setCursorAccelerated(!isCursorAccelerated)
          /*-
           * let action to process this
           setCursorAccelerated(!isCursorAccelerated());
           */
        }

      }

      override def keyTyped(e: KeyEvent) {
      }

      private def moveCursorInDirection(fastSteps: Int, DIRECTION: Int) {
        var steps = if (isCursorAccelerated) fastSteps else 1
        steps *= DIRECTION

        scrollReferCursor(steps, true)
      }

      private def moveChartsInDirection(fastSteps: Int, DIRECTION: Int) {
        var steps = if (isCursorAccelerated) fastSteps else 1
        steps *= DIRECTION

        scrollChartsHorizontallyByBar(steps)
      }
    }

    class ViewMouseWheelListener extends MouseWheelListener {

      def mouseWheelMoved(e: java.awt.event.MouseWheelEvent) {
        val view = internal_getCorrespondingChartView(e)
        if (view == null || !view.isInteractive) {
          return
        }

        val fastSteps = (view.getNBars * 0.168f).intValue

        if (e.isShiftDown) {
          /** zoom in / zoom out */
          growWBar(e.getWheelRotation)
        } else if (e.isControlDown) {
          if (!view.isInteractive) {
            return
          }

          val unitsToScroll = if (isCursorAccelerated) e.getWheelRotation * fastSteps else e.getWheelRotation
          /** move refer cursor left / right */
          scrollReferCursor(unitsToScroll, true)
        } else {
          if (!view.isInteractive) {
            return
          }

          val unitsToScroll = if (isCursorAccelerated) e.getWheelRotation * fastSteps else e.getWheelRotation
          /** keep referCursor stay same x in screen, and move */
          scrollChartsHorizontallyByBar(unitsToScroll)
        }
      }
    }
  }
}
