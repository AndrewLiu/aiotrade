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
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.computable.SpotComputable
import org.aiotrade.lib.math.timeseries.plottable.Plot
import scala.collection.mutable.ArrayBuffer


/**
 * This is the default data container, which is a time sorted data contianer.
 *
 * This container has one copy of 'vars' (without null value) for both
 * compact and natural, and with two time positions:
 *   'timestamps', and
 *   'calendarTimes'
 * the 'timestamps' is actaully with the same idx correspinding to 'vars'
 *
 *
 * This class implemets all interface of ser and partly MasterSer.
 * So you can use it as full series, but don't use those methods of MasterSeries
 * except you sub class this.
 *
 * @author Caoyuan Deng
 */
class DefaultTSer(afreq: TFreq) extends AbstractTSer(afreq) {
  protected val logger = Logger.getLogger(getClass.getName)
  logger.setLevel(Level.INFO)

  protected val INIT_CAPACITY = 100

  /**
   * a place holder plus flags
   */
  protected type Holder = Byte
  val holders = new ArrayBuffer[Holder]//(INIT_CAPACITY)// this will cause timestamps' lock deadlock?
  /**
   * Each var element of array is a Var that contains a sequence of values for one field of SerItem.
   * @Note: Don't use scala's HashSet or HashMap to store Var, these classes seems won't get all of them stored
   */
  val vars = new ArrayBuffer[TVar[Any]]

  /**
   * we implement occurred timestamps and items in density mode instead of spare
   * mode, to avoid itemOf(time) return null even in case of timestamps has been
   * filled. DefaultItem is a lightweight virtual class, don't worry about the
   * memory occupied.
   *
   * Should only get index from timestamps which has the proper mapping of :
   * position <-> time <-> item
   */
  private var _timestamps: TStamps = _

  private var tsLogCheckedCursor = 0
  private var tsLogCheckedSize = 0

  private var description = ""

  def this() = this(TFreq.DAILY)

  def timestamps: TStamps = _timestamps
  def attach(timestamps: TStamps) {
    this._timestamps = timestamps
  }

  /**
   * used only by InnerVar's constructor and AbstractIndicator's functions
   */
  protected def addVar(v: TVar[Any]): Unit = {
    vars += v
  }

  /**
   * @todo, holder.size or timestamps.size ?
   */
  def size: Int = holders.size

  def exists(time: Long): Boolean = {
    /**
     * @NOTE:
     * Should only get index from timestamps which has the proper
     * position <-> time <-> item mapping
     */
    val idx = timestamps.indexOfOccurredTime(time)
    if (idx >= 0 && idx < holders.size) {
      true
    } else {
      this match {
        case x: SpotComputable =>
          /** re-get one by computing it */
          x.computeSpot(time)
          true
        case _ => false
      }
    }
  }


  protected def assignValue(tval: TVal) {
    // todo
  }

  /**
   * return a holder with flag != 0
   */
  protected def createItem(time: Long): Holder = 1

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
  def validate {
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

              val newHolders = new Array[Holder](insertSize)
              var i = 0
              while (i < insertSize) {
                val time = timestamps(begIdx1 + i)
                vars foreach {x => x.put(time, x.NullVal)}
                newHolders(i) = createItem(time)
                i += 1
              }
              holders.insertAll(begIdx1, newHolders)
              logger.fine(shortDescription + "(" + freq + ") Log check: cursor=" + checkingCursor + ", insertSize=" + insertSize + ", begIdx=" + begIdx1 + " => newSize=" + holders.size)
                            
            case TStampsLog.APPEND =>
              val begIdx = holders.size

              val appendSize = if (!cursorMoved) {
                tlogCurrSize - tsLogCheckedSize
              } else tlogCurrSize

              val newHolders = new Array[Holder](appendSize)
              var i = 0
              while (i < appendSize) {
                val time = timestamps(begIdx + i)
                vars foreach {x => x.put(time, x.NullVal)}
                newHolders(i) = createItem(time)
                i += 1
              }
              holders ++= newHolders
              logger.fine(shortDescription + "(" + freq + ") Log check: cursor=" + checkingCursor + ", appendSize=" + appendSize + ", begIdx=" + begIdx + " => newSize=" + holders.size)

            case x => assert(false, "Unknown log type: " + x)
          }
        }
                
        tsLogCheckedCursor = checkingCursor
        tsLogCheckedSize = tlogCurrSize
        checkingCursor = tlog.nextCursor(checkingCursor)
      }

      assert(timestamps.size == holders.size,
             "Timestamps size=" + timestamps.size + " vs items size=" + holders.size +
             ", checkedCursor=" + tsLogCheckedCursor +
             ", log=" + tlog)
    } catch {
      case ex => logger.log(Level.WARNING, "exception", ex)
    } finally {
      timestamps.readLock.unlock
    }

  }

  def clear(fromTime: Long): Unit = synchronized {
    try {
      timestamps.writeLock.lock
            
      val fromIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
      if (fromIdx < 0) {
        return
      }

      vars foreach {_.clear(fromIdx)}

      for (i <- timestamps.size - 1 to fromIdx) {
        timestamps.remove(i)
      }

      for (i <- holders.size - 1 to fromIdx) {
        holders.remove(i)
      }
    } finally {
      timestamps.writeLock.unlock
    }

    publish(TSerEvent.Clear(this, shortDescription, fromTime, Long.MaxValue))
  }

  def indexOfOccurredTime(time: Long): Int = {
    timestamps.indexOfOccurredTime(time)
  }

  def lastOccurredTime: Long = {
    timestamps.lastOccurredTime
  }

  override def toString = {
    val sb = new StringBuilder(20)
    val len = size
    sb.append(shortDescription).append("(").append(freq).append("): size=").append(len).append(", ")
    if (len > 0) {

      val first = timestamps(0)
      val last = timestamps(len - 1)
      val cal = Calendar.getInstance
      cal.setTimeInMillis(first)
      sb.append(cal.getTime)
      sb.append(" - ")
      cal.setTimeInMillis(last)
      sb.append(cal.getTime)
    }
    
    sb.append(", values=(\n")
    for (v <- vars) {
      sb.append(v.name).append(": ... ")
      var i = math.max(0, len - 6) // print last 6 values
      while (i < len) {
        sb.append(v(i)).append(", ")
        i += 1
      }
      sb.append("\n")
    }
    sb.append(")")
    
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

    final def put(time: Long, value: V): Boolean = {
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

    final def apply(time: Long): V = {
      val idx = timestamps.indexOfOccurredTime(time)
      values(idx)
    }

    final def update(time: Long, value: V) {
      val idx = timestamps.indexOfOccurredTime(time)
      values(idx) = value
    }

    // @Note, see https://lampsvn.epfl.ch/trac/scala/ticket/2599
    final override def apply(idx: Int): V = {
      super.apply(idx)
    }

    // @Note, see https://lampsvn.epfl.ch/trac/scala/ticket/2599
    override def update(idx: Int, value: V) {
      super.update(idx, value)
    }
 
  }

  protected class SparseTVar[V: Manifest](name: String, plot: Plot
  ) extends AbstractInnerTVar[V](name, plot) {

    val values = new TStampedMapBasedList[V](timestamps)

    def put(time: Long, value: V): Boolean = {
      val idx = timestamps.indexOfOccurredTime(time)
      if (idx >= 0) {
        values.add(time, value)
        true
      } else {
        assert(false, "Add timestamps first before add an element! " + ": " + "idx=" + idx + ", time=" + time)
        false
      }
    }

    def apply(time: Long): V = values(time)

    def update(time: Long, value: V) {
      values(time) = value
    }

    // @Note, see https://lampsvn.epfl.ch/trac/scala/ticket/2599
    override def apply(idx: Int): V = {
      super.apply(idx)
    }

    // @Note, see https://lampsvn.epfl.ch/trac/scala/ticket/2599
    override def update(idx: Int, value: V) {
      super.update(idx, value)
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

    def timestamps = DefaultTSer.this.timestamps

    /**
     * This method will never return null, return a nullValue at least.
     */
    def apply(idx: Int): V = {
      if (idx >= 0 && idx < values.size) {
        values(idx) match {
          case null => NullVal
          case value => value
        }
      } else NullVal
    }

    def update(idx: Int, value: V) {
      if (idx >= 0 && idx < values.size) {
        values(idx) = value
      } else {
        assert(false, "AbstractInnerVar.update(index, value): this index's value of Var not inited yet: " +
               "idx=" + idx + ", value size=" + values.size + ", timestamps size=" + timestamps.size)
      }
    }

    def getColor(idx: Int) = colors(idx)
    def setColor(idx: Int, color: Color) {
      colors(idx) = color
    }
  }
}




