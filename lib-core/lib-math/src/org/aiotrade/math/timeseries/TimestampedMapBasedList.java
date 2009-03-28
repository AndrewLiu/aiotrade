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
package org.aiotrade.math.timeseries;

import java.util.AbstractList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

/**
 * A package class that implements timestamped Map based List, used to store
 * sparse time series var.
 *
 * Below is a performance test result for various colletions:
 * http://www.artima.com/weblogs/viewpost.jsp?thread=122295
 *--------------------- ArrayList ---------------------
 *  size     add     get     set iteradd  insert  remove
 *    10     121     139     191     435    3952     446
 *   100      72     141     191     247    3934     296
 *  1000      98     141     194     839    2202     923
 * 10000     122     144     190    6880   14042    7333
 * --------------------- LinkedList ---------------------
 *  size     add     get     set iteradd  insert  remove
 *    10     182     164     198     658     366     262
 *   100     106     202     230     457     108     201
 *  1000     133    1289    1353     430     136     239
 * 10000     172   13648   13187     435     255     239
 * ----------------------- Vector -----------------------
 *  size     add     get     set iteradd  insert  remove
 *    10     129     145     187     290    3635     253
 *   100      72     144     190     263    3691     292
 *  1000      99     145     193     846    2162     927
 * 10000     108     145     186    6871   14730    7135
 * -------------------- Queue tests --------------------
 *  size    addFirst     addLast     rmFirst      rmLast
 *    10         199         163         251         253
 *   100          98          92         180         179
 *  1000          99          93         216         212
 * 10000         111         109         262         384
 * ------------- TreeSet -------------
 *  size       add  contains   iterate
 *    10       746       173        89
 *   100       501       264        68
 *  1000       714       410        69
 * 10000      1975       552        69
 * ------------- HashSet -------------
 *  size       add  contains   iterate
 *    10       308        91        94
 *   100       178        75        73
 *  1000       216       110        72
 * 10000       711       215       100
 * ---------- LinkedHashSet ----------
 *  size       add  contains   iterate
 *    10       350        65        83
 *   100       270        74        55
 *  1000       303       111        54
 * 10000      1615       256        58
 *
 * ---------- TreeMap ----------
 *  size     put     get iterate
 *    10     748     168     100
 *   100     506     264      76
 *  1000     771     450      78
 * 10000    2962     561      83
 * ---------- HashMap ----------
 *  size     put     get iterate
 *    10     281      76      93
 *   100     179      70      73
 *  1000     267     102      72
 * 10000    1305     265      97
 * ------- LinkedHashMap -------
 *  size     put     get iterate
 *    10     354     100      72
 *   100     273      89      50
 *  1000     385     222      56
 * 10000    2787     341      56
 * ------ IdentityHashMap ------
 *  size     put     get iterate
 *    10     290     144     101
 *   100     204     287     132
 *  1000     508     336      77
 * 10000     767     266      56
 * -------- WeakHashMap --------
 *  size     put     get iterate
 *    10     484     146     151
 *   100     292     126     117
 *  1000     411     136     152
 * 10000    2165     138     555
 * --------- Hashtable ---------
 *  size     put     get iterate
 *    10     264     113     113
 *   100     181     105      76
 *  1000     260     201      80
 * 10000    1245     134      77
 *
 * @author  Caoyuan Deng
 * @version 1.0, 11/22/2006
 * @since   1.0.4
 */
class TimestampedMapBasedList<E> extends AbstractList<E> implements List<E>, RandomAccess {
    
    private final Map<Long, E> timeMapElementData;
    private final Timestamps timestamps;
    
    TimestampedMapBasedList(Timestamps timestamps) {
        this.timestamps = timestamps;
        this.timeMapElementData = new HashMap<Long, E>();
    }
    
    public final int size() {
        return timestamps.size();
    }
    
    public final boolean isEmpty() {
        return timestamps.isEmpty();
    }
    
    public final boolean contains(Object o) {
        return timeMapElementData.containsValue(o);
    }
    
    public final boolean containsAll(Collection<?> c) {
        return timeMapElementData.values().containsAll(c);
    }
    
    public final Object[] toArray() {
        final int length = timestamps.size();
        return (Object[])toArray(new Object[length]);
    }
    
    public final <T> T[] toArray(T[] a) {
        final int length = timestamps.size();
        T[] array = a.length == length ? a : (T[])new Object[length];
        for (int i = 0; i < length; i++) {
            final Long time = timestamps.get(i);
            array[i] = time != null ? (T)timeMapElementData.get(time) : null;
        }
        
        return array;
    }
    
    public final boolean add(long time, E o) {
        if (o == null) {
            /** null value needs not to be put in map, this will spare the memory usage */
            return true;
        }
        
        final int idx = timestamps.indexOfOccurredTime(time);
        if (idx >= 0) {
            timeMapElementData.put(time, o);
            return true;
        } else {
            assert false: "Add timestamps first before add an element!";
            return false;
        }
    }
    
    public final E getByTime(long time) {
        return timeMapElementData.get(time);
    }
    
    public final E setByTime(long time, E o) {
        if (timestamps.contains(time)) {
            return timeMapElementData.put(time, o);
        } else {
            assert false : "Time out of bounds = " + time;
            return null;
        }
    }
    
    /**
     * @deprecated
     */
    public final boolean add(E o) {
        assert false :
            "add(E) is not supported by this collection! " +
            "use add(long time, E o)";
        return false;
    }
    
    /**
     * @deprecated
     */
    public final void add(int index, E element) {
        assert false :
            "add(int index, E element) is not supported by this collection! " +
            "use add(long time, E o)";
    }
    
    /**
     * @deprecated
     */
    public final boolean addAll(Collection<? extends E> c) {
        assert false :
            "addAll(Collection<? extends E> c) is not supported by this collection! " +
            "use add(long time, E o)";
        return false;
    }
    
    /**
     * @deprecated
     */
    public final boolean addAll(int index, Collection<? extends E> c) {
        assert false :
            "addAll(int index, Collection<? extends E> c) is not supported by this collection! " +
            "use add(long time, E o)";
        return false;
    }
    
    public final boolean remove(Object o) {
        return timeMapElementData.values().remove(o);
    }
    
    public final boolean removeAll(Collection<?> c) {
        return timeMapElementData.values().removeAll(c);
    }
    
    public final boolean retainAll(Collection<?> c) {
        return timeMapElementData.values().retainAll(c);
    }
    
    public final void clear() {
        timeMapElementData.clear();
    }
    
    public final boolean equals(Object o) {
        return timeMapElementData.equals(o);
    }
    
    public final int hashCode() {
        return timeMapElementData.hashCode();
    }
    
    public final E get(int index) {
        final Long time = timestamps.get(index);
        return time != null ? timeMapElementData.get(time) : null;
    }
    
    public final E set(int index, E o) {
        final Long time = timestamps.get(index);
        if (time != null) {
            return timeMapElementData.put(time, o);
        } else {
            assert false : "Index out of bounds! index = " + index;
            return null;
        }
    }
    
    public final E remove(int index) {
        final Long time = timestamps.get(index);
        if (time != null) {
            return timeMapElementData.remove(time);
        } else {
            return null;
        }
    }
    
    public final int indexOf(Object o) {
        for (Long time : timeMapElementData.keySet()) {
            if (timeMapElementData.get(time) == o) {
                return binarySearch(time, 0, timestamps.size() - 1);
            }
        }
        
        return -1;
    }
    
    public final int lastIndexOf(Object o) {
        int found = -1;
        for (Long time : timeMapElementData.keySet()) {
            if (timeMapElementData.get(time) == o) {
                found = binarySearch(time, 0, timestamps.size() - 1);
            }
        }
        
        return found;
    }
    
    private final int binarySearch(long time, int left, int right) {
        if (left == right) {
            return timestamps.get(left) == time ? left : -1;
        } else {
            final int middle = (int)((left + right) * 0.5);
            if (time < timestamps.get(middle)) {
                return middle == 0 ? -1 : binarySearch(time, left, middle - 1);
            } else {
                return binarySearch(time, middle, right);
            }
        }
    }
    
    
}

