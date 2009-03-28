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
package org.aiotrade.lib.math.timeseries;

import org.aiotrade.lib.math.timeseries.computable.SpotComputable;
import org.aiotrade.lib.math.timeseries.plottable.Plot;
import java.awt.Color;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aiotrade.lib.util.ReferenceOnly;

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
public class DefaultSer extends AbstractSer {

    private final static int INIT_CAPACITY = 200;
    /**
     * we implement occurred timestamps and items in density mode instead of spare
     * mode, to avoid getItem(time) return null even in case of timestamps has been
     * filled. DefaultItem is a lightweight virtual class, don't worry about the
     * memory occupied.
     *
     * Should only get index from timestamps which has the proper mapping of :
     * position <-> time <-> item
     */
    private final Timestamps timestamps = TimestampsFactory.createInstance(INIT_CAPACITY);
    private final List<SerItem> items = new ArrayList<SerItem>(INIT_CAPACITY);
    /**
     * Map contains vars. Each var element of array is a Var that contains a
     * sequence of values for one field of SerItem.
     */
    private final Map<Var<?>, String> varToName = new LinkedHashMap<Var<?>, String>();
    private String description = "";

    public DefaultSer() {
        /** do nothing */
    }

    public DefaultSer(final Frequency freq) {
        super(freq);
    }

    public final Timestamps timestamps() {
        return timestamps;
    }

    /**
     * used only by InnerVar's constructor and AbstractIndicator's functions
     */
    protected final void addVar(final Var<?> var) {
        varToName.put(var, var.getName());
    }

    /**
     * This should be the only interface to fetch item, what ever by time or by row.
     */
    synchronized private final SerItem internal_getItem(final long time) {
        /**
         * @NOTICE:
         * Should only get index from timestamps which has the proper
         * position <-> time <-> item mapping
         */
        final int idx = timestamps.indexOfOccurredTime(time);
        if (idx >= 0 && idx < timestamps.size()) {
            return items.get(idx);
        } else {
            return null;
        }
    }

    /**
     * Add a clear item and corresponding time in time order,
     * should process time position (add time to timestamps orderly).
     * Support inserting time/clearItem pair in random order
     *
     * @param time
     * @param clearItem
     */
    synchronized private final void internal_addClearItemAndNullVarValuesToList_And_Filltimestamps__InTimeOrder(final long itemTime, final SerItem clearItem) {
        final long lastOccurredTime = timestamps.lastOccurredTime();
        if (itemTime < lastOccurredTime) {
            /** should insert it to proper position to keep the order: */
            final int idx = timestamps.indexOfNearestOccurredTimeBehind(itemTime);
            assert idx >= 0 : "Since the itemTime < lastOccurredTime, the idx should be >= 0";
            /** time at idx > itemTime, insert it before this idx */
            internal_addTime_addClearItem_addNullVarValues(idx, itemTime, clearItem);
        } else if (itemTime > lastOccurredTime) {
            /** time > lastTime, just append it behind the last: */
            internal_addTime_addClearItem_addNullVarValues(itemTime, clearItem);
        } else {
            /** time == lastTime, keep same time and update item to clear. */
            assert false :
                    "As it's an adding action, we should not reach here! " +
                    "Check your code, you are probably from createItemOrClearIt(long), " +
                    "Does timestamps.indexOfOccurredTime(itemTime) = " + timestamps.indexOfOccurredTime(itemTime) +
                    " return -1 ?";
            /** to avoid concurrent conflict, just do nothing here. */
        }
    }

    private final void internal_clearItemAndVarValues(final int idx, final SerItem itemTobeClear) {
        for (Var<?> var : varToName.keySet()) {
            var.set(idx, null);
        }

        items.set(idx, itemTobeClear);
        itemTobeClear.clear();
    }

    private final void internal_addTime_addClearItem_addNullVarValues(final int idx, final long time, final SerItem clearItem) {
        /** should add timestamps first */
        timestamps.add(idx, time);

        for (Var<?> var : varToName.keySet()) {
            var.add(time, null);
        }

        /** as timestamps includes this time, we just always put in a none-null item  */
        items.add(idx, clearItem);
    }

    private final void internal_addTime_addClearItem_addNullVarValues(final long time, final SerItem clearItem) {
        /** should add timestamps first */
        timestamps.add(time);

        for (Var<?> var : varToName.keySet()) {
            var.add(time, null);
        }

        /** as timestamps includes this time, we just always put in a none-null item  */
        items.add(clearItem);
    }

    protected SerItem createItem(final long time) {
        return new DefaultItem(this, time);
    }

    public void setShortDescription(final String description) {
        this.description = description;
    }

    public String getShortDescription() {
        return description;
    }

    public final Set<Var<?>> varSet() {
        return varToName.keySet();
    }

    synchronized public void clear(final long fromTime) {
        final int fromIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime);
        if (fromIdx < 0) {
            return;
        }

        for (Var var : varToName.keySet()) {
            var.clear(fromIdx);
        }
        for (int i = timestamps.size() - 1; i >= fromIdx; i--) {
            timestamps.remove(i);
        }
        for (int i = items.size() - 1; i >= fromIdx; i--) {
            items.remove(i);
        }

        fireSerChangeEvent(new SerChangeEvent(
                this,
                SerChangeEvent.Type.Clear,
                getShortDescription(),
                fromTime,
                Long.MAX_VALUE));
    }

    public final List<SerItem> itemList() {
        return items;
    }

    public SerItem getItem(final long time) {
        SerItem item = internal_getItem(time);
        if (this instanceof SpotComputable) {
            if (item == null || (item != null && item.isClear())) {
                /** re-get one from calculator */
                item = ((SpotComputable) this).computeItem(time);
            }
        }

        return item;
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
    public final SerItem createItemOrClearIt(final long time) {
        SerItem item = internal_getItem(time);
        if (item == null) {
            /** item == null means timestamps.indexOfOccurredTime(time) is not in valid range */
            item = createItem(time);
            internal_addClearItemAndNullVarValuesToList_And_Filltimestamps__InTimeOrder(time, item);
        } else {
            item.clear();
        }

        return item;
    }

    public int size() {
        return timestamps.size();
    }

    public int indexOfOccurredTime(final long time) {
        return timestamps.indexOfOccurredTime(time);
    }

    public long lastOccurredTime() {
        return timestamps.lastOccurredTime();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(20);
        sb.append(this.getClass().getSimpleName()).append("(").append(getFreq());
        if (timestamps.size() > 0) {
            long start = timestamps.get(0);
            long end = timestamps.get(size() - 1);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(start);
            sb.append(", from ").append(cal.getTime());
            cal.setTimeInMillis(end);
            sb.append(" to ").append(cal.getTime()).append(")");
        }
        return sb.toString();
    }

    public final class DefaultVar<E> extends AbstractInnerVar<E> implements Var<E> {

        private final List<E> values = new ArrayList<E>(INIT_CAPACITY);

        public DefaultVar() {
            this("", Plot.None);
        }

        public DefaultVar(final String name) {
            this(name, Plot.None);
        }

        public DefaultVar(final String name, final Plot plot) {
            super(name, plot);
        }

        public final List<E> values() {
            return values;
        }

        public final boolean add(final long time, final E value) {
            final int idx = timestamps.indexOfOccurredTime(time);
            if (idx >= 0) {
                values.add(idx, value);
                return true;
            } else {
                assert false : "Add timestamps first before add an element!";
                return false;
            }
        }

        public final E getByTime(final long time) {
            final int idx = timestamps.indexOfOccurredTime(time);
            return values.get(idx);
        }

        public final E setByTime(final long time, final E value) {
            final int idx = timestamps.indexOfOccurredTime(time);
            return values.set(idx, value);
        }

        /**
         * All those instances of DefaultVar or extended class will be equals if
         * they have the same values, this prevent the duplicated manage of values.
         * @See AbstractIndicator.injectVarsToSer()
         */
        @Override
        public final boolean equals(final Object o) {
            if (o instanceof DefaultVar) {
                if (this.values == ((DefaultVar) o).values()) {
                    return true;
                }
            }

            return false;
        }
    }

    public final class SparseVar<E> extends AbstractInnerVar<E> implements Var<E> {

        private final TimestampedMapBasedList<E> values = new TimestampedMapBasedList<E>(timestamps);

        public SparseVar() {
            this("", Plot.None);
        }

        public SparseVar(final String name) {
            this(name, Plot.None);
        }

        public SparseVar(final String name, final Plot plot) {
            super(name, plot);
        }

        public final List<E> values() {
            return values;
        }

        public final boolean add(final long time, final E value) {
            final int idx = timestamps.indexOfOccurredTime(time);
            if (idx >= 0) {
                values.add(time, value);
                return true;
            } else {
                assert false : "Add timestamps first before add an element!";
                return false;
            }
        }

        public final E getByTime(final long time) {
            return values.getByTime(time);
        }

        public final E setByTime(final long time, final E value) {
            return values.setByTime(time, value);
        }

        /**
         * All those instances of SparseVar or extended class will be equals if
         * they have the same values, this prevent the duplicated manage of values.
         * @See AbstractIndicator.injectVarsToSer()
         */
        @Override
        public final boolean equals(final Object o) {
            if (o instanceof SparseVar) {
                if (this.values == ((SparseVar) o).values()) {
                    return true;
                }
            }

            return false;
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
    public abstract class AbstractInnerVar<E> extends AbstractVar<E> implements Var<E> {

        private final TimestampedMapBasedList<Color> colors = new TimestampedMapBasedList<Color>(timestamps);
        private final E nullValue = createNullValue();

        public AbstractInnerVar() {
            this("", Plot.None);
        }

        public AbstractInnerVar(final String name) {
            this(name, Plot.None);
        }

        public AbstractInnerVar(final String name, final Plot plot) {
            super(name, plot);
            addVar(this);
        }

        protected E createNullValue() {
            return (E) new Float(Float.NaN);
        }

        public final E nullValue() {
            return nullValue;
        }

        /**
         * This method will never return null, return a nullValue at least.
         */
        public final E get(final int idx) {
            if (idx >= 0 && idx < values().size()) {
                final E value = values().get(idx);
                if (value != null) {
                    return value;
                } else {
                    return nullValue;
                }
            } else {
                return nullValue;
            }
        }

        public final void set(final int idx, final E value) {
            if (idx >= 0 && idx < values().size()) {
                values().set(idx, value);
            } else {
                assert false : "DefaultVar.set(index, value): this index's value of Var not init yet! " +
                        idx + " size:" + values().size();
                return;
            }
        }

        /**
         * Clear values that >= fromIdx
         */
        public final void clear(final int fromIdx) {
            if (fromIdx < 0) {
                return;
            }
            for (int i = values().size() - 1; i >= fromIdx; i--) {
                values().remove(i);
            }
        }

        public final void setColor(final int idx, final Color color) {
            colors.set(idx, color);
        }

        public final Color getColor(final int idx) {
            return colors.get(idx);
        }

        public final int size() {
            return values().size();
        }
    }

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
    protected final void injectVarsIntoSer() {
        Field[] fields = this.getClass().getDeclaredFields();

        AccessibleObject.setAccessible(fields, true);

        for (Field field : fields) {
            Object value = null;

            try {
                value = field.get(this);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }

            if (value != null && value instanceof Var) {
                addVar((Var) value);
            }
        }
    }
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




