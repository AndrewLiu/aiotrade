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
package org.aiotrade.lib.math.timeseries

/**
 *
 * @author Caoyuan Deng
 */
import org.aiotrade.lib.math.timeseries.datasource.SerProvider

class DefaultBaseTSer(_serProvider: SerProvider, $freq: TFreq) extends DefaultTSer($freq) with BaseTSer {
  private var _isOnCalendarMode = false
    
  attach(TStampsFactory.createInstance(INIT_CAPACITY))

  def this() = this(null, TFreq.DAILY)

  def serProvider = _serProvider

  /*-
   * !NOTICE
   * This should be the only place to create an Item from outside, because it's
   * a bit complex to finish an item creating procedure, the procedure contains
   * at least 3 steps:
   * 1. create a clear holder, which with clear = true, and idx to be set
   *    later by holders;
   * 2. add the time to timestamps properly.
   * @see #internal_addClearItemAndNullVarValuesToList_And_Filltimestamps__InTimeOrder(long, SerItem)
   * 3. add null value to vars at the proper idx.
   * @see #internal_addTime_addClearItem_addNullVarValues()
   *
   * So we do not try to provide other public methods such as addItem() that can
   * add item from outside, you should use this method to create a new (a clear)
   * item and return it, or just clear it, if it has be there.
   * And that why define some motheds signature begin with internal_, becuase
   * you'd better never think to open these methods to protected or public.
   */
  def createOrClear(time: Long) {
    /**
     * @NOTE:
     * Should only get index from timestamps which has the proper
     * position <-> time <-> item mapping
     */
    val idx = timestamps.indexOfOccurredTime(time)
    if (idx >= 0 && idx < holders.size) {
      // existed, clear it
      vars foreach {x => x(idx) = x.NullVal}
      holders(idx) = 0
    } else {
      // create a new one, add placeholder
      val holder = createItem(time)
      internal_addItem_fillTimestamps_InTimeOrder(time, holder)
    }
  }

  /*_ @Todo removed synchronized for internal_apply and internal_addClearItem_fillTimestamps_InTimeOrder, synchronized would cause deadlock:
   [java] "AWT-EventQueue-0" prio=6 tid=0x0000000101891800 nid=0x132013000 waiting for monitor entry [0x0000000132011000]
   [java]    java.lang.Thread.State: BLOCKED (on object monitor)
   [java] 	at org.aiotrade.lib.math.timeseries.DefaultTSer.internal_apply(DefaultTSer.scala:115)
   [java] 	- waiting to lock <0x00000001070295d8> (a org.aiotrade.lib.securities.QuoteSer)
   [java] 	at org.aiotrade.lib.math.timeseries.DefaultTSer(DefaultTSer.scala:354)
   [java] 	at org.aiotrade.lib.math.timeseries.DefaultBaseTSer.itemOfRow(DefaultBaseTSer.scala:56)
   [java] 	at org.aiotrade.lib.charting.view.pane.AxisYPane.org$aiotrade$lib$charting$view$pane$AxisYPane$$updateReferCursorLabel(AxisYPane.scala:167)
   [java] 	at org.aiotrade.lib.charting.view.pane.AxisYPane.syncWithView(AxisYPane.scala:187)
   [java] 	at org.aiotrade.lib.charting.view.ChartView.postPaintComponent(ChartView.scala:294)
   [java] 	at org.aiotrade.lib.charting.view.ChartView.paintComponent(ChartView.scala:231)
   [java] 	at javax.swing.JComponent.paint(JComponent.java:1029)
   [java] 	at javax.swing.JComponent._paintImmediately(JComponent.java:5098)
   [java] 	at javax.swing.JComponent.paintImmediately(JComponent.java:4882)
   [java] 	at javax.swing.RepaintManager.paintDirtyRegions(RepaintManager.java:829)
   [java] 	at javax.swing.RepaintManager.paintDirtyRegions(RepaintManager.java:714)
   [java] 	at javax.swing.RepaintManager.seqPaintDirtyRegions(RepaintManager.java:694)
   [java] 	at javax.swing.SystemEventQueueUtilities$ComponentWorkRequest.run(SystemEventQueueUtilities.java:128)
   [java] 	at java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:209)
   [java] 	at java.awt.EventQueue.dispatchEvent(EventQueue.java:633)
   [java] 	at java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:296)
   [java] 	at java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:211)
   [java] 	at java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:201)
   [java] 	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:196)
   [java] 	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:188)
   [java] 	at java.awt.EventDispatchThread.run(EventDispatchThread.java:122)

   [java] "ForkJoinPool-1-worker-2" daemon prio=5 tid=0x000000010195f800 nid=0x131d0a000 waiting for monitor entry [0x0000000131d08000]
   [java]    java.lang.Thread.State: BLOCKED (on object monitor)
   [java] 	at org.aiotrade.lib.math.timeseries.DefaultTSer.internal_apply(DefaultTSer.scala:109)
   [java] 	- waiting to lock <0x00000001070295d8> (a org.aiotrade.lib.securities.QuoteSer)
   [java] 	at org.aiotrade.lib.math.timeseries.DefaultTSer(DefaultTSer.scala:354)
   [java] 	at org.aiotrade.lib.math.timeseries.DefaultBaseTSer.itemOfRow(DefaultBaseTSer.scala:56)
   [java] 	at org.aiotrade.lib.charting.view.pane.AxisYPane.org$aiotrade$lib$charting$view$pane$AxisYPane$$updateReferCursorLabel(AxisYPane.scala:167)
   [java] 	at org.aiotrade.lib.charting.view.pane.AxisYPane$$anon$3.update(AxisYPane.scala:96)
   [java] 	at org.aiotrade.lib.charting.view.pane.AxisYPane$$anon$3.update(AxisYPane.scala:93)
   [java] 	at org.aiotrade.lib.util.ChangeObservableHelper$$anonfun$notifyObserversChanged$2.apply(ChangeObservableHelper.scala:85)
   [java] 	at org.aiotrade.lib.util.ChangeObservableHelper$$anonfun$notifyObserversChanged$2.apply(ChangeObservableHelper.scala:84)
   [java] 	at scala.collection.Iterator$class.foreach(Iterator.scala:542)
   [java] 	at scala.collection.Iterator$$anon$20.foreach(Iterator.scala:365)
   [java] 	at org.aiotrade.lib.util.ChangeObservableHelper.notifyObserversChanged(ChangeObservableHelper.scala:84)
   [java] 	at org.aiotrade.lib.charting.view.ChartView.notifyObserversChanged(ChartView.scala:157)
   [java] 	at org.aiotrade.lib.charting.view.ChartView.updateView(ChartView.scala:562)
   [java] 	at org.aiotrade.lib.charting.view.ChartView$MySerChangeListener.serChanged(ChartView.scala:597)
   [java] 	at org.aiotrade.lib.math.timeseries.AbstractTSer.fireTSerEvent(AbstractTSer.scala:68)
   [java] 	at org.aiotrade.lib.math.timeseries.computable.ComputableHelper.postComputeFrom(ComputableHelper.scala:198)
   [java] 	at org.aiotrade.lib.indicator.AbstractIndicator.computeFrom(AbstractIndicator.scala:304)
   [java] 	at org.aiotrade.lib.math.timeseries.computable.Computable$$anonfun$1$$anonfun$apply$2$$anonfun$apply$1.apply(Computable.scala:49)
   [java] 	at org.aiotrade.lib.math.timeseries.computable.Computable$$anonfun$1$$anonfun$apply$2$$anonfun$apply$1.apply(Computable.scala:48)
   [java] 	at scala.actors.Reaction$$anonfun$$init$$1.apply(Reaction.scala:33)
   [java] 	at scala.actors.Reaction$$anonfun$$init$$1.apply(Reaction.scala:29)
   [java] 	at scala.actors.ReactorTask.run(ReactorTask.scala:33)
   [java] 	at scala.actors.scheduler.ForkJoinScheduler$$anon$1.compute(ForkJoinScheduler.scala:111)
   [java] 	at scala.concurrent.forkjoin.RecursiveAction.exec(Unknown Source)
   [java] 	at scala.concurrent.forkjoin.ForkJoinTask.quietlyExec(Unknown Source)
   [java] 	at scala.concurrent.forkjoin.ForkJoinWorkerThread.mainLoop(Unknown Source)
   [java] 	at scala.concurrent.forkjoin.ForkJoinWorkerThread.run(Unknown Source)
   */

  /**
   * Add a clear item and corresponding time in time order,
   * should process time position (add time to timestamps orderly).
   * Support inserting time/clearItem pair in random order
   *
   * @param time
   * @param clearItem
   */
  private def internal_addItem_fillTimestamps_InTimeOrder(time: Long, holder: Holder) {
    // @Note: writeLock timestamps only when insert/append it
    val lastOccurredTime = timestamps.lastOccurredTime
    if (time < lastOccurredTime) {
      val existIdx = timestamps.indexOfOccurredTime(time)
      if (existIdx >= 0) {
        vars foreach {x => x.put(time, x.NullVal)}
        // as timestamps includes this time, we just always put in a none-null item
        holders.insert(existIdx, holder)
      } else {
        val idx = timestamps.indexOfNearestOccurredTimeBehind(time)
        assert(idx >= 0,  "Since itemTime < lastOccurredTime, the idx=" + idx + " should be >= 0")

        // (time at idx) > itemTime, insert this new item at the same idx, so the followed elems will be pushed behind
        try {
          timestamps.writeLock.lock

          // should add timestamps first
          timestamps.insert(idx, time)
          timestamps.log.logInsert(1, idx)

          vars foreach {x => x.put(time, x.NullVal)}
          holders.insert(idx, holder)

        } finally {
          timestamps.writeLock.unlock
        }
      }
    } else if (time > lastOccurredTime) {
      // time > lastOccurredTime, just append it behind the last:
      try {
        timestamps.writeLock.lock

        /** should append timestamps first */
        timestamps += time
        timestamps.log.logAppend(1)

        vars foreach {x => x.put(time, x.NullVal)}
        holders += holder

      } finally {
        timestamps.writeLock.unlock
      }
    } else {
      // time == lastOccurredTime, keep same time and append vars and holders.
      val existIdx = timestamps.indexOfOccurredTime(time)
      if (existIdx >= 0) {
        vars foreach {x => x.put(time, x.NullVal)}
        holders += holder
      } else {
        assert(false,
               "As it's an adding action, we should not reach here! " +
               "Check your code, you are probably from createOrClear(long), " +
               "Does timestamps.indexOfOccurredTime(itemTime) = " + timestamps.indexOfOccurredTime(time) +
               " return -1 ?")
        // to avoid concurrent conflict, just do nothing here.
      }
    }
  }

  /**
   * To use this method, should define proper assignValue(value)
   */
  override def ++=[V <: TVal](values: Array[V]): TSer = synchronized {
    if (values.length < 1) return this
    
    try {
      timestamps.writeLock.lock

      var frTime = Long.MaxValue
      var toTime = Long.MinValue

      val shouldReverse = !isAscending(values)

      val size = values.length
      var i = if (shouldReverse) size - 1 else 0
      while (i >= 0 && i < size) {
        val value = values(i)
        val time = value.time
        createOrClear(time)
        assignValue(value)

        frTime = math.min(frTime, time)
        toTime = math.max(toTime, time)

        if (shouldReverse) {
          /** the recent quote's index is more in quotes, thus the order in timePositions[] is opposed to quotes */
          i -= 1
        } else {
          /** the recent quote's index is less in quotes, thus the order in timePositions[] is the same as quotes */
          i += 1
        }
      }

      publish(TSerEvent.Updated(this, shortDescription, frTime, toTime))

    } finally {
      timestamps.writeLock.unlock
    }

    logger.fine("TimestampsLog: " + timestamps.log)
    this
  }

  def isOnCalendarMode = _isOnCalendarMode
  def toOnCalendarMode {
    this._isOnCalendarMode = true
  }
  def toOnOccurredMode {
    this._isOnCalendarMode = false
  }
        
  def indexOfTime(time: Long): Int = activeTimestamps.indexOfOccurredTime(time)
  def timeOfIndex(idx: Int): Long = activeTimestamps(idx)

  def rowOfTime(time: Long): Int = activeTimestamps.rowOfTime(time, freq)
  def timeOfRow(row: Int): Long = activeTimestamps.timeOfRow(row, freq)
  def lastOccurredRow: Int = activeTimestamps.lastRow(freq)
    
  override def size: Int = activeTimestamps.sizeOf(freq)

  private def activeTimestamps: TStamps = if (_isOnCalendarMode) timestamps.asOnCalendar else timestamps
}






