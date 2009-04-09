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
class TimestampedMapBasedList[E](timestamps:Timestamps) extends AbstractList[E] with List[E] with RandomAccess {
    
    private val timeMapElementData = new HashMap[Long, E]()
    
    def size :Int = timestamps.size

    override
    def isEmpty :Boolean = timestamps.isEmpty
    
    override
    def contains(o:Object) :Boolean = timeMapElementData.containsValue(o)
    
    override
    def containsAll(c:Collection[_]) :Boolean = timeMapElementData.values.containsAll(c)

    override
    def toArray :Array[Object] = {
        val length = timestamps.size()
        toArray(new Array[Object](length))
    }
    
    override
    def toArray[T](a:Array[T]) :Array[T] = {
        val length = timestamps.size()
        val array = if (a.length == length) a else new Array[T](size)
        for (i <- 0 until length) {
            val time = timestamps.get(i)
            array(i) = if (time != null) timeMapElementData.get(time).asInstanceOf[T] else null.asInstanceOf[T]
        }
        
        array
    }
    
    def add(time:Long, o:E) :Boolean = {
        if (o == null) {
            /** null value needs not to be put in map, this will spare the memory usage */
            return true;
        }
        
        val idx = timestamps.indexOfOccurredTime(time)
        if (idx >= 0) {
            timeMapElementData.put(time, o)
            true;
        } else {
            assert(false, "Add timestamps first before add an element!")
            false
        }
    }
    
    def getByTime(time:Long) :E = timeMapElementData.get(time)
    
    def setByTime(time:Long, o:E) :E = {
        if (timestamps.contains(time)) {
            timeMapElementData.put(time, o)
        } else {
            assert(false, "Time out of bounds = " + time)
            null.asInstanceOf[E]
        }
    }
    
    /**
     * @deprecated
     */
    override
    def add(o:E) :Boolean = {
        assert(false, "add(E) is not supported by this collection! " +
               ", please use add(long time, E o)")
        false
    }
    
    /**
     * @deprecated
     */
    override
    def add(index:Int, element:E) :Unit = {
        assert(false, "add(int index, E element) is not supported by this collection! " +
               ", please use add(long time, E o)")
    }
    
    /**
     * @deprecated
     */
    override
    def addAll(c:Collection[_ <: E]) :Boolean = {
        assert(false, "addAll(Collection<? extends E> c) is not supported by this collection! " +
               ", please use add(long time, E o)")
        false
    }
    
    /**
     * @deprecated
     */
    override
    def addAll(index:Int, c:Collection[_ <: E]) :Boolean = {
        assert(false, "addAll(int index, Collection<? extends E> c) is not supported by this collection! " +
               " please use add(long time, E o)")
        false
    }
    
    override
    def remove(o:Object) :Boolean = timeMapElementData.values.remove(o)
    
    override
    def removeAll(c:Collection[_]) :Boolean = timeMapElementData.values.removeAll(c)
    
    override
    def retainAll(c:Collection[_]) :Boolean = timeMapElementData.values.retainAll(c)
    
    override
    def clear :Unit = timeMapElementData.clear
    
    override
    def equals(o:Any) :Boolean = timeMapElementData.equals(o)
    
    override
    def hashCode :Int = timeMapElementData.hashCode
    
    def get(index:Int) :E = {
        val time = timestamps.get(index)
        if (time != null) timeMapElementData.get(time) else null.asInstanceOf[E]
    }
    
    override
    def set(index:Int, o:E) :E = {
        val time = timestamps.get(index)
        if (time != null) {
            timeMapElementData.put(time, o)
        } else {
            assert(false, "Index out of bounds! index = " + index)
            null.asInstanceOf[E]
        }
    }
    
    override
    def remove(index:Int) :E = {
        val time = timestamps.get(index)
        if (time != null) {
            timeMapElementData.remove(time)
        } else {
            null.asInstanceOf[E]
        }
    }
    
    override
    def indexOf(o:Object) :Int = {
        val itr = timeMapElementData.keySet.iterator
        while (itr.hasNext) {
            val time = itr.next
            if (timeMapElementData.get(time) == o) {
                return binarySearch(time, 0, timestamps.size() - 1)
            }
        }
        
        return -1
    }
    
    override
    def lastIndexOf(o:Object) :Int = {
        var found = -1
        val itr = timeMapElementData.keySet.iterator
        while (itr.hasNext) {
            val time = itr.next
            if (timeMapElementData.get(time) == o) {
                found = binarySearch(time, 0, timestamps.size() - 1)
            }
        }
        
        found
    }
    
    private def binarySearch(time:Long, left:Int, right:Int) :Int = {
        if (left == right) {
            if (timestamps.get(left) == time) left else -1
        } else {
            val middle = ((left + right) * 0.5).asInstanceOf[Int]
            if (time < timestamps.get(middle)) {
                if (middle == 0) -1 else binarySearch(time, left, middle - 1)
            } else {
                binarySearch(time, middle, right);
            }
        }
    }
    
    
}

