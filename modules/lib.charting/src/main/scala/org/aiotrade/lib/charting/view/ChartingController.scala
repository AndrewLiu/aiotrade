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

import java.awt.Component
import java.awt.Dimension
import org.aiotrade.lib.math.timeseries.MasterTSer
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.util.ChangeObservable

/**
 * Each MasterSer can have more than one ChartingController instances.
 *
 * A ChartingController instance keeps the 1-1 relation with:
 *   the MasterSer,
 *   the AnalysisDescriptor, and
 *   a ChartViewContainer
 * Thus, ChartingController couples MasterSer-AnalysisDescriptor-ChartViewContainer
 * together from outside.
 *
 * A ChartView's container can be any Component even without a ChartViewContainer,
 * but should reference back to a controller. All ChartViews shares the same
 * controller will have the same cursor behaves.
 *
 * @author Caoyuan Deng
 */
trait ChartingController extends ChangeObservable {

  def masterSer: MasterTSer

  def contents: AnalysisContents

  def isCursorCrossLineVisible: Boolean
  def isCursorCrossLineVisible_=(b: Boolean)

  def isMouseEnteredAnyChartPane: Boolean
  def isMouseEnteredAnyChartPane_=(b: Boolean)

  def wBar: Float

  def growWBar(increment: Int)

  def setWBarByNBars(wViewPort: Int, nBars: Int)

  def isOnCalendarMode: Boolean
  def isOnCalendarMode_=(b: Boolean)

  def setCursorByRow(referRow: Int, rightRow: Int, willUpdateViews: Boolean)

  def setReferCursorByRow(Row: Int, willUpdateViews: Boolean)

  def scrollReferCursor(increment: Int, willUpdateViews: Boolean)

  /** keep refer cursor stay on same x of screen, and scroll charts left or right by bar */
  def scrollChartsHorizontallyByBar(increment: Int)

  def scrollReferCursorToLeftSide

  def setMouseCursorByRow(row: Int)

  def isAutoScrollToNewData: Boolean
  def isAutoScrollToNewData_=(b: Boolean)

  def updateViews

  def popupViewToDesktop(view: ChartView, dimension: Dimension, alwaysOnTop: Boolean, joint: Boolean)

  /**
   * ======================================================
   * Bellow is the methods for cursor etc:
   */
  def referCursorRow: Int
  def referCursorTime: Long

  def rightSideRow: Int
  def rightSideTime: Long

  def leftSideRow: Int
  def leftSideTime: Long

  def mouseCursorRow: Int
  def mouseCursorTime: Long

  def isCursorAccelerated: Boolean
  def isCursorAccelerated_=(b: Boolean)

  /**
   * Factory method to create ChartViewContainer instance
   */
  def createChartViewContainer[T <: ChartViewContainer](clazz: Class[T], focusableParent: Component): Option[T]
}
