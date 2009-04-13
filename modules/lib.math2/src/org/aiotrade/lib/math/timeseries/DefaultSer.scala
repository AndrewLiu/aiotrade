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

import java.awt.Color;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.aiotrade.lib.math.timeseries.computable.SpotComputable
import org.aiotrade.lib.math.timeseries.plottable.Plot
import scala.collection.Set
import scala.collection.mutable.{ArrayBuffer,LinkedHashMap}


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
class DefaultSer(freq:Frequency) extends AbstractSer(freq) {

    private val INIT_CAPACITY = 200
    /**
     * we implement occurred timestamps and items in density mode instead of spare
     * mode, to avoid getItem(time) return null even in case of timestamps has been
     * filled. DefaultItem is a lightweight virtual class, don't worry about the
     * memory occupied.
     *
     * Should only get index from timestamps which has the proper mapping of :
     * position <-> time <-> item
     */
    val timestamps = TimestampsFactory.createInstance(INIT_CAPACITY)
    private val items = new ArrayList[SerItem](INIT_CAPACITY)
    /**
     * Map contains vars. Each var element of array is a Var that contains a
     * sequence of values for one field of SerItem.
     */
    private val varToName = new LinkedHashMap[Var[Any], String]
    private var description = ""

    def this() = {
        /** do nothing */
        this(Frequency.DAILY)
    }

    /**
     * used only by InnerVar's constructor and AbstractIndicator's functions
     */
    protected def addVar(v:Var[Any]) :Unit = {
        varToName.put(v, v.name)
    }

    /**
     * This should be the only interface to fetch item, what ever by time or by row.
     */
    private def internal_getItem(time:Long) :SerItem = synchronized {
        /**
         * @NOTICE:
         * Should only get index from timestamps which has the proper
         * position <-> time <-> item mapping
         */
        val idx = timestamps.indexOfOccurredTime(time)
        if (idx >= 0 && idx < timestamps.size) {
            items.get(idx)
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
    private def internal_addClearItemAndNullVarValuesToList_And_Filltimestamps__InTimeOrder(itemTime:Long, clearItem:SerItem) :Unit = synchronized {
        val lastOccurredTime = timestamps.lastOccurredTime
        if (itemTime < lastOccurredTime) {
            val idx = timestamps.indexOfNearestOccurredTimeBehind(itemTime);
            assert(idx >= 0,  "Since the itemTime < lastOccurredTime, the idx should be >= 0")
            /** time at idx > itemTime, insert it before this idx */
            internal_addTime_addClearItem_addNullVarValues(idx, itemTime, clearItem)
        } else if (itemTime > lastOccurredTime) {
            /** time > lastTime, just append it behind the last: */
            internal_addTime_addClearItem_addNullVarValues(itemTime, clearItem);
        } else {
            /** time == lastTime, keep same time and update item to clear. */
            assert(false,
                   "As it's an adding action, we should not reach here! " +
                   "Check your code, you are probably from createItemOrClearIt(long), " +
                   "Does timestamps.indexOfOccurredTime(itemTime) = " + timestamps.indexOfOccurredTime(itemTime) +
                   " return -1 ?")
            /** to avoid concurrent conflict, just do nothing here. */
        }
    }

    private def internal_clearItemAndVarValues(idx:Int, itemTobeClear:SerItem) :Unit = {

        for (v <- varToName.keySet) {
            v(idx) = null
        }

        items.set(idx, itemTobeClear)
        itemTobeClear.clear
    }

    private def internal_addTime_addClearItem_addNullVarValues(idx:Int, time:Long, clearItem:SerItem) :Unit = {
        /** should add timestamps first */
        timestamps.insert(idx, time)

        for (v <- varToName.keySet) {
            v.add(time, null)
        }

        /** as timestamps includes this time, we just always put in a none-null item  */
        items.add(idx, clearItem)
    }

    private def internal_addTime_addClearItem_addNullVarValues(time:Long, clearItem:SerItem) :Unit = {
        /** should add timestamps first */
        timestamps + time

        for (v <- varToName.keySet) {
            v.add(time, null)
        }

        /** as timestamps includes this time, we just always put in a none-null item  */
        items.add(clearItem)
    }

    protected def createItem(time:Long) :SerItem = {
        new DefaultItem(this, time)
    }

    def shortDescription_=(description:String) :Unit = {
        this.description = description
    }

    def shortDescription :String = description

    def varSet :Set[Var[Any]] = varToName.keySet

    def clear(fromTime:Long) :Unit = synchronized {
        val fromIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
        if (fromIdx < 0) {
            return
        }

        for (v <- varToName.keySet) {
            v.clear(fromIdx)
        }
        for (i <- timestamps.size - 1 to fromIdx) {
            timestamps.remove(i)
        }
        for (i <- items.size - 1 to fromIdx) {
            items.remove(i)
        }

        fireSerChangeEvent(new SerChangeEvent(this,
                                              SerChangeEvent.Type.Clear,
                                              shortDescription,
                                              fromTime,
                                              Long.MaxValue))
    }

    def itemList :List[SerItem] = items

    def getItem(time:Long) :SerItem = {
        var item = internal_getItem(time)
        this match {
            case x:SpotComputable =>
                if (item == null || (item != null && item.isClear)) {
                    /** re-get one from calculator */
                    item = x.computeItem(time)
                }
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
    def createItemOrClearIt(time:Long) :SerItem = {
        var item = internal_getItem(time)
        if (item == null) {
            /** item == null means timestamps.indexOfOccurredTime(time) is not in valid range */
            item = createItem(time)
            internal_addClearItemAndNullVarValuesToList_And_Filltimestamps__InTimeOrder(time, item)
        } else {
            item.clear
        }

        item
    }

    def size :Int = timestamps.size

    def indexOfOccurredTime(time:Long) :Int = timestamps.indexOfOccurredTime(time)

    def lastOccurredTime :Long = timestamps.lastOccurredTime

    override
    def toString :String = {
        val sb = new StringBuilder(20)
        sb.append(this.getClass.getSimpleName).append("(").append(freq)
        if (timestamps.size > 0) {
            val start = timestamps(0)
            val end = timestamps(size - 1)
            val cal = Calendar.getInstance
            cal.setTimeInMillis(start)
            sb.append(", from ").append(cal.getTime)
            cal.setTimeInMillis(end)
            sb.append(" to ").append(cal.getTime).append(")")
        }
        sb.toString
    }

    class DefaultVar[E](name:String, plot:Plot) extends AbstractInnerVar[E](name, plot) {

        val values = new ArrayBuffer[E]

        def this() = {
            this("", Plot.None)
        }

        def this(name:String) = {
            this(name, Plot.None)
        }

        def add(time:Long, value:E) :Boolean = {
            val idx = timestamps.indexOfOccurredTime(time)
            if (idx >= 0) {
                values.insert(idx, value)
                true
            } else {
                assert(false, "Add timestamps first before add an element!")
                false
            }
        }

        def getByTime(time:long) :E = {
            val idx = timestamps.indexOfOccurredTime(time)
            values(idx)
        }

        def setByTime(time:Long, value:E) :E = {
            val idx = timestamps.indexOfOccurredTime(time)
            values(idx) = value
            value
        }

        /**
         * All those instances of DefaultVar or extended class will be equals if
         * they have the same values, this prevent the duplicated manage of values.
         * @See AbstractIndicator.injectVarsToSer()
         */
        override
        def equals(o:Any) :Boolean = {
            o match {
                case x:DefaultVar[_] =>
                    if (this.values == x.values) {
                        return true
                    } else false
                case _ => false
            }
        }
    }

    class SparseVar[E](name:String, plot:Plot) extends AbstractInnerVar[E](name, plot) {

        val values = new TimestampedMapBasedList[E](timestamps)

        def this() = {
            this("", Plot.None);
        }

        def this(name:String) = {
            this(name, Plot.None)
        }

        def add(time:Long, value:E) :Boolean = {
            val idx = timestamps.indexOfOccurredTime(time);
            if (idx >= 0) {
                values.add(time, value)
                true
            } else {
                assert(false, "Add timestamps first before add an element!")
                false
            }
        }

        def getByTime(time:Long) :E = values.getByTime(time)

        def setByTime(time:Long, value:E) :E = values.setByTime(time, value)

        /**
         * All those instances of SparseVar or extended class will be equals if
         * they have the same values, this prevent the duplicated manage of values.
         * @See AbstractIndicator.injectVarsToSer()
         */
        override
        def equals(o:Any) :Boolean = {
            o match {
                case x:SparseVar[_] =>
                    if (this.values == x.values) {
                        true
                    } else false
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
    abstract class AbstractInnerVar[E](name:String, plot:Plot) extends AbstractVar[E](name, plot) {

        private val colors = new TimestampedMapBasedList[Color](timestamps)
        val nullValue = Float.NaN.asInstanceOf[E]

        addVar(this.asInstanceOf[Var[Any]])

        def this() = {
            this("", Plot.None)
        }

        def this(name:String) = {
            this(name, Plot.None)
        }

        /**
         * This method will never return null, return a nullValue at least.
         */
        override
        def apply(idx:Int) :E = {
            if (idx >= 0 && idx < values.size) {
                val value = values(idx)
                if (value != null) {
                    value
                } else {
                    nullValue
                }
            } else {
                nullValue
            }
        }

        override
        def update(idx:Int, value:E) :Unit = {
            if (idx >= 0 && idx < values.size) {
                values(idx) = value
            } else {
                assert(false, "DefaultVar.set(index, value): this index's value of Var not init yet! " +
                       idx + " size:" + values.size)
            }
        }

        /**
         * Clear values that >= fromIdx
         */
        def clear(fromIdx:Int) :Unit = {
            if (fromIdx < 0) {
                return;
            }
            for (i <- values.size - 1 to fromIdx) {
                values.remove(i)
            }
        }

        def setColor(idx:Int, color:Color) :Unit = {
            colors(idx) = color
        }

        def getColor(idx:Int) :Color = colors.apply(idx)

        def size :Int = values.size
    }

    /*-
     /**
      * @deprecated
      * This method inject declared Var(s) of current instance into vars, sub-
      * class should also call it in the constructor (except no-arg constructor)
      * after all Var(s) have got the proper value(s) to return a useful
      * instance.
      *
      * We define it as a final to keep this contract.
      */
     @ReferenceOnly
     @Deprecated
     protected def injectVarsIntoSer :Unit = {
     val fields = this.getClass.getDeclaredFields

     AccessibleObject.setAccessible(fields, true)

     for (field <- fields) {
     var value:Any = null

     try {
     value = field.get(this)
     } catch  {
     case ex:IllegalArgumentException => ex.printStackTrace
     case ex:IllegalAccessException => ex.printStackTrace
     }

     if (value != null && value.isInstanceOf[Var[_]]) {
     addVar(value.asInstanceOf[Var[_]])
     }
     }
     }
     */
    /*-
     abstract public class BaseHibernateEntityDao<T> extends HibernateDaoSupport {
     private Class<T> entityClass;
     public BaseHibernateEntityDao() {
     entityClass =(Class<T>) ((ParameterizedType) getClass()
     .getGenericSuperclass()).getActualTypeArguments()[0];
     }
     public T get(Serializable id) {
     T o = (T) getHibernateTemplate().get(entityClass, id);
     }
     }
     */
}




