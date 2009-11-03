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

import java.awt.Color
import java.util.Calendar
import org.aiotrade.lib.util.collection.ArrayList
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.computable.SpotComputable
import org.aiotrade.lib.math.timeseries.plottable.Plot


/**
 * This is the default data container, which is a time sorted data contianer.
 *
 * This container has one copy of 'vars' (without null value) for both
 * compact and natural, and with two time positions:
 *   'timestamps', and
 *   'calendarTimes'
 * the 'timestamps' is actaully with the same idx correspinding to 'vars'
 *
 * We here implement two dimention views:
 * 1. view values as a Item, crossing varList at any time point.
 * 2. view values as a array of var list.
 *
 *
 * This class implemets all interface of ser and partly MasterSer.
 * So you can use it as full series, but don't use those methods of MasterSeries
 * except you sub class this.
 *
 * @author Caoyuan Deng
 */
class DefaultTSer(freq: TFreq) extends AbstractTSer(freq) {
  val logger = Logger.getLogger(this.getClass.getName)

  private val INIT_CAPACITY = 400

  val items = new ArrayList[TItem]//(INIT_CAPACITY)// this will cause timestamps' lock deadlock?
  /**
   * Each var element of array is a Var that contains a sequence of values for one field of SerItem.
   * @Note: Don't use scala's HashSet or HashMap to store Var, these classes seems won't get all of them stored
   */
  val vars = new ArrayList[TVar[Any]]

  /**
   * we implement occurred timestamps and items in density mode instead of spare
   * mode, to avoid getItem(time) return null even in case of timestamps has been
   * filled. DefaultItem is a lightweight virtual class, don't worry about the
   * memory occupied.
   *
   * Should only get index from timestamps which has the proper mapping of :
   * position <-> time <-> item
   */
  private var _timestamps = TStampsFactory.createInstance(INIT_CAPACITY)

  private var tsLog = timestamps.log
  private var tsLogCheckedCursor = 0
  private var tsLogCheckedSize = 0

  private var description = ""

  def this() = this(TFreq.DAILY)

  def timestamps: TStamps = _timestamps
  
  protected def attach(timestamps: TStamps): Unit = {
    this._timestamps = timestamps
    this.tsLog = timestamps.log
  }

  /**
   * used only by InnerVar's constructor and AbstractIndicator's functions
   */
  protected def addVar(v: TVar[Any]): Unit = {
    vars += v
  }

  /**
   * This should be the only interface to fetch item, what ever by time or by row.
   */
  private def internal_getItem(time: Long): TItem = synchronized {
    /**
     * @NOTE:
     * Should only get index from timestamps which has the proper
     * position <-> time <-> item mapping
     */
    val idx = timestamps.indexOfOccurredTime(time)
    if (idx >= 0 && idx < items.size) {
      items(idx)
    } else null
  }

  /**
   * Add a clear item and corresponding time in time order,
   * should process time position (add time to timestamps orderly).
   * Support inserting time/clearItem pair in random order
   *
   * @param time
   * @param clearItem
   */
  private def internal_addClearItem_fillTimestamps_InTimeOrder(itemTime: Long, clearItem: TItem): Unit = synchronized {
    // @Note: writeLock timestamps only when insert/append it
    val lastOccurredTime = timestamps.lastOccurredTime
    if (itemTime < lastOccurredTime) {
      val existIdx = timestamps.indexOfOccurredTime(itemTime)
      if (existIdx >= 0) {
        vars foreach {_.addNullValue(itemTime)}
        // * as timestamps includes this time, we just always put in a none-null item
        items.insert(existIdx, clearItem)
      } else {
        val idx = timestamps.indexOfNearestOccurredTimeBehind(itemTime)
        assert(idx >= 0,  "Since itemTime < lastOccurredTime, the idx=" + idx + " should be >= 0")

        // * (time at idx) > itemTime, insert this new item at the same idx, so the followed elems will be pushed behind
        try {
          _timestamps.writeLock.lock

          // * should add timestamps first
          _timestamps.insert(idx, itemTime)
          tsLog.logInsert(1, idx)

          vars foreach {_.addNullValue(itemTime)}

          // * as timestamps includes this time, we just always put in a none-null item
          items.insert(idx, clearItem)
        } finally {
          _timestamps.writeLock.unlock
        }
      }
    } else if (itemTime > lastOccurredTime) {
      // * time > lastOccurredTime, just append it behind the last:
      try {
        _timestamps.writeLock.lock

        /** should add timestamps first */
        _timestamps += itemTime
        tsLog.logAppend(1)

        vars foreach {_.addNullValue(itemTime)}

        /** as timestamps includes this time, we just always put in a none-null item  */
        items += clearItem
      } finally {
        _timestamps.writeLock.unlock
      }
    } else {
      // * time == lastOccurredTime, keep same time and update item to clear.
      val existIdx = timestamps.indexOfOccurredTime(itemTime)
      if (existIdx >= 0) {
        vars foreach {_.addNullValue(itemTime)}
        // * as timestamps includes this time, we just always put in a none-null item
        items += clearItem
      } else {
        assert(false,
               "As it's an adding action, we should not reach here! " +
               "Check your code, you are probably from createItemOrClearIt(long), " +
               "Does timestamps.indexOfOccurredTime(itemTime) = " + timestamps.indexOfOccurredTime(itemTime) +
               " return -1 ?")
        // * to avoid concurrent conflict, just do nothing here.
      }
    }
  }

  def ++=[V <: TVal](values: Array[V]): TSer = {
    var begTime = +Long.MaxValue
    var endTime = -Long.MaxValue
    try {
      _timestamps.writeLock.lock

      val shouldReverse = !isAscending(values)

      val size = values.length
      var i = if (shouldReverse) size - 1 else 0
      while (i >= 0 && i < size) {
        val value = values(i)
        val item = createItemOrClearIt(value.time)
        item.assignValue(value)

        if (shouldReverse) {
          /** the recent quote's index is more in quotes, thus the order in timePositions[] is opposed to quotes */
          i -= 1
        } else {
          /** the recent quote's index is less in quotes, thus the order in timePositions[] is same as quotes */
          i += 1
        }

        val itemTime = item.time
        begTime = Math.min(begTime, itemTime)
        endTime = Math.max(endTime, itemTime)
      }
    } finally {
      _timestamps.writeLock.unlock
    }
    println("TimestampsLog: " + tsLog)
    val evt = new SerChangeEvent(this, SerChangeEvent.Type.Updated, shortDescription, begTime, endTime)
    fireSerChangeEvent(evt)
    
    this
  }

  protected def createItem(time: Long): TItem = {
    new DefaultTItem(this, time)
  }

  def shortDescription :String = description
  def shortDescription_=(description: String): Unit = {
    this.description = description
  }

  /**
   * @Note:
   * This function is not thread safe, since tsLogCheckedCursor and tsLogCheckedSize
   * should be atomic accessed/modified during function's running scope so.
   * Should avoid to enter here by multiple actors concurrently
   */
  def validate: Unit = {
    try {
      timestamps.readLock.lock

      val tlog = timestamps.log
      val tlogCursor = tlog.logCursor
      var checkingCursor = tsLogCheckedCursor
      while (tlogCursor > -1 && checkingCursor <= tlogCursor) {
        val cursorMoved = if (checkingCursor != tsLogCheckedCursor) {
          // * Is checking a new log, should reset tsLogCheckedSize
          tsLogCheckedSize = 0
          true
        } else false

        val tlogFlag = tlog(checkingCursor)
        val tlogCurrSize = tlog.checkSize(tlogFlag)
        if (!cursorMoved && tlogCurrSize == tsLogCheckedSize) {
          // * same log with same size, actually nothing changed
        } else {
          tlog.checkKind(tlogFlag) match {
            case TStampsLog.INSERT =>
              val begIdx = tlog.insertIndexOfLog(checkingCursor)

              val begIdx1 = if (!cursorMoved) {
                // * if insert log is a merged one, means the inserts were continually happening one behind one
                begIdx + tsLogCheckedSize
              } else begIdx
                                    
              val insertSize = if (!cursorMoved) {
                tlogCurrSize - tsLogCheckedSize
              } else tlogCurrSize

              val newItems = new Array[TItem](insertSize)
              var i = 0
              while (i < insertSize) {
                val time = timestamps(begIdx1 + i)
                vars foreach {_.addNullValue(time)}
                newItems(0) = createItem(time)
                i += 1
              }
              items.insertAll(begIdx1, newItems)
              logger.info(shortDescription + "(" + freq + ") Log check: cursor=" + checkingCursor + ", insertSize=" + insertSize + ", begIdx=" + begIdx1 + " => newSize=" + items.size)
                            
            case TStampsLog.APPEND =>
              val begIdx = items.size

              val appendSize = if (!cursorMoved) {
                tlogCurrSize - tsLogCheckedSize
              } else tlogCurrSize

              val newItems = new Array[TItem](appendSize)
              var i = 0
              while (i < appendSize) {
                val time = timestamps(begIdx)
                vars foreach {_.addNullValue(time)}
                newItems(i) = createItem(time)
                i += 1
              }
              items ++= newItems
              logger.info(shortDescription + "(" + freq + ") Log check: cursor=" + checkingCursor + ", appendSize=" + appendSize + ", begIdx=" + begIdx + " => newSize=" + items.size)

            case x => assert(false, "Unknown log type: " + x)
          }
        }
                
        tsLogCheckedCursor = checkingCursor
        tsLogCheckedSize = tlogCurrSize
        checkingCursor = tlog.nextCursor(checkingCursor)
      }

      assert(timestamps.size == items.size,
             "Timestamps size=" + timestamps.size + " vs items size=" + items.size +
             ", checkedCursor=" + tsLogCheckedCursor +
             ", log=" + tlog)
    } finally {
      timestamps.readLock.unlock
    }

  }

  def clear(fromTime: Long): Unit = synchronized {
    try {
      _timestamps.writeLock.lock
            
      val fromIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
      if (fromIdx < 0) {
        return
      }

      vars foreach {_.clear(fromIdx)}

      for (i <- timestamps.size - 1 to fromIdx) {
        _timestamps.remove(i)
      }

      for (i <- items.size - 1 to fromIdx) {
        items.remove(i)
      }
    } finally {
      _timestamps.writeLock.unlock
    }

    fireSerChangeEvent(new SerChangeEvent(this,
                                          SerChangeEvent.Type.Clear,
                                          shortDescription,
                                          fromTime,
                                          Long.MaxValue))
  }

  def getItem(time: Long): TItem = {
    var item = internal_getItem(time)
    this match {
      case x: SpotComputable =>
        if (item == null || (item != null && item.isClear)) {
          /** re-get one from calculator */
          item = x.computeItem(time)
        }
      case _ =>
    }

    item
  }

  /*-
   * !NOTICE
   * This should be the only place to create an Item from outside, because it's
   * a bit complex to finish an item creating procedure, the procedure contains
   * at least 3 steps:
   * 1. create a clear item instance, which with clear = true, and idx to be set
   *    later by ItemList;
   * @see #ItemList#add()
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
  def createItemOrClearIt(time: Long): TItem = {
    internal_getItem(time) match {
      case null =>
        // * item == null means timestamps.indexOfOccurredTime(time) is not in valid range
        val item = createItem(time)
        internal_addClearItem_fillTimestamps_InTimeOrder(time, item)
        item
      case item =>
        item.clear
        item
    }
  }

  def size: Int = items.size

  def indexOfOccurredTime(time: Long): Int = timestamps.indexOfOccurredTime(time)

  def lastOccurredTime: Long = timestamps.lastOccurredTime

  override def toString: String = {
    val sb = new StringBuilder(20)
    sb.append(this.getClass.getSimpleName).append("(").append(freq)
    if (timestamps.size > 0) {
      val start = timestamps(0)
      val end = timestamps(size - 1)
      val cal = Calendar.getInstance
      cal.setTimeInMillis(start)
      sb.append(", ").append(cal.getTime)
      cal.setTimeInMillis(end)
      sb.append(" - ").append(cal.getTime).append(")")
    }
    sb.toString
  }

  /** Ser may be used as the HashMap key, for efficient reason, we define equals and hashCode method as it: */
  override def equals(a: Any) = a match {
    case x: TSer => this.getClass == x.getClass && this.hashCode == x.hashCode
    case _ => false
  }

  private val _hashCode = System.identityHashCode(this)
  override def hashCode: Int = _hashCode

  object TVar {
    def apply[V: Manifest]() = new InnerTVar[V]("", Plot.None)
    def apply[V: Manifest](name: String) = new InnerTVar[V](name, Plot.None)
    def apply[V: Manifest](name: String, plot: Plot) = new InnerTVar[V](name, plot)
  }
  
  protected class InnerTVar[V: Manifest](name: String, plot: Plot
  ) extends AbstractInnerTVar[V](name, plot) {

    var values = new ArrayList[V](INIT_CAPACITY)

    def add(time: Long, value: V): Boolean = {
      val idx = timestamps.indexOfOccurredTime(time)
      if (idx >= 0) {
        if (idx == values.size) {
          values += value
        } else {
          values.insert(idx, value)
        }
        true
      } else {
        assert(false, "Add timestamps first before add an element! " + ": " + "idx=" + idx + ", time=" + time)
        false
      }
    }

    def getByTime(time: Long): V = {
      val idx = timestamps.indexOfOccurredTime(time)
      values(idx)
    }

    def setByTime(time: Long, value: V): V = {
      val idx = timestamps.indexOfOccurredTime(time)
      values(idx) = value
      value
    }

    def validate: Unit = {
      val newValues = new ArrayList[V](INIT_CAPACITY)

      var i = 0
      var j = 0
      while (i < timestamps.size) {
        val time = timestamps(i)
        var break = false
        var v = null.asInstanceOf[V]
        while (j < items.size && !break) {
          val vtime = items(j).time
          if (vtime == time) {
            // found existed value
            v = values(j)
            j += 1
            break = true
          } else if (vtime > time) {
            // not existed value
            v = null.asInstanceOf[V]
            break = true
          } else {
            j += 1
          }
        }
        newValues += v
        i += 1
      }
      values = newValues
    }

    /**
     * All those instances of DefaultVar or extended class will be equals if
     * they have the same values, this prevent the duplicated manage of values.
     * @See AbstractIndicator.injectVarsToSer()
     */
    override def equals(a: Any): Boolean = a match {
      case x: InnerTVar[_] => this.values == x.values
      case _ => false
    }
  }

  protected class SparseTVar[@specialized V: Manifest](name: String, plot: Plot
  ) extends AbstractInnerTVar[V](name, plot) {

    val values = new TStampedMapBasedList[V](timestamps)

    def add(time: Long, value: V): Boolean = {
      val idx = timestamps.indexOfOccurredTime(time)
      if (idx >= 0) {
        values.add(time, value)
        true
      } else {
        assert(false, "Add timestamps first before add an element! " + ": " + "idx=" + idx + ", time=" + time)
        false
      }
    }

    def getByTime(time: Long): V = values.getByTime(time)

    def setByTime(time: Long, value: V): V = values.setByTime(time, value)

    def validate {}

    /**
     * All those instances of SparseVar or extended class will be equals if
     * they have the same values, this prevent the duplicated manage of values.
     * @See AbstractIndicator.injectVarsToSer()
     */
    override def equals(o: Any): Boolean = {
      o match {
        case x: SparseTVar[_] => this.values == x.values
        case _ => false
      }
    }
  }

  /**
   * Define inner Var class
   * -----------------------------------------------------------------------
   * Horizontal view of DefaultSer. Is' a reference of one of the field vars.
   *
   * Inner Var can only live with DefaultSer.
   *
   * We define it as inner class of DefaultSer, to avoid bad usage, especially
   * when its values is also managed by DefaultSer. We should make sure the
   * operation on values, including add, delete actions will be consistant by
   * cooperating with DefaultSer.
   */
  abstract class AbstractInnerTVar[V: Manifest](name: String, plot: Plot
  ) extends AbstractTVar[V](name, plot) {
    addVar(this.asInstanceOf[TVar[Any]])

    private val colors = new TStampedMapBasedList[Color](timestamps)
    
    /**
     * This method will never return null, return a nullValue at least.
     */
    override def apply(idx: Int): V = {
      if (idx >= 0 && idx < values.size) {
        values(idx) match {
          case null => nullValue
          case value => value
        }
      } else nullValue
    }

    override def update(idx: Int, value: V): Unit = {
      if (idx >= 0 && idx < values.size) {
        values(idx) = value
      } else {
        assert(false, "AbstractInnerVar.update(index, value): this index's value of Var not inited yet: " +
               "idx=" + idx + ", value size=" + values.size + ", timestamps size=" + timestamps.size)
      }
    }
        
    private def checkValidationAt(idx: Int): Int = {
      assert(idx >= 0, "Out of bounds: idx=" + idx)

      val time = timestamps(idx)
      if (idx < items.size) {
        val itime = items(idx).time
        if (itime != time) {
          val idx1 = idx - 1
          if (idx1 >= 0) {
            checkValidationAt(idx1)
          } else {
            idx
          }
        } else {
          -1 // it's good
        }
      } else {
        idx
      }
    }

    /**
     * Clear values that >= fromIdx
     */
    def clear(fromIdx: Int): Unit = {
      if (fromIdx < 0) {
        return
      }
      var i = values.size - 1
      while (i >= fromIdx) {
        values.remove(i)
        i += 1
      }
    }

    def setColor(idx: Int, color: Color): Unit = {
      colors(idx) = color
    }

    def getColor(idx: Int): Color = colors(idx)

    def size: Int = values.size
  }
}




