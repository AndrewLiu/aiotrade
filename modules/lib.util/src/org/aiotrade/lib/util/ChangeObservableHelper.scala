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
package org.aiotrade.lib.util

import java.util.ArrayList
import java.util.Collection
import java.util.HashMap
import java.util.List
import java.util.Map

/**
 * Do not implement observerMap list using WeakReference, otherwise, we can not
 * add anonymous observer by:
 * addObserver(new ChangeObserver<Subject> {
 *      public void update(Subject) {
 *      }
 * });
 * 
 * 
 * @author  Caoyuan Deng
 * @version 1.0, November 24, 2006, 5:09 PM
 * @since 1.0.4
 */
class ChangeObservableHelper {
  
  @transient
  val observerMapOwner = new HashMap[ChangeObserver[_], Object]
    
  def ChangeObservableHelper :Unit = {}
    
  def getObservers :Collection[ChangeObserver[_]] = {
    observerMapOwner.keySet
  }
    
  def addObserver(owner:Object, observer:ChangeObserver[_]) :Unit = synchronized {
    observerMapOwner.put(observer, owner)
  }
    
  def  removeObserver(observer:ChangeObserver[_]) :Unit = synchronized {
    if (observer == null) {
      return
    }
        
    if (observerMapOwner.keySet.contains(observer)) {
      observerMapOwner.remove(observer)
    }
  }
    
  def removeObserversOf(owner:Object) :Unit = {
    val toBeRemoved = new ArrayList[ChangeObserver[_]]
    val itr = observerMapOwner.keySet.iterator
    while (itr.hasNext) {
      val observer = itr.next
      if (observerMapOwner.get(observer) == owner) {
        toBeRemoved.add(observer)
      }
    }
        
    synchronized {
      observerMapOwner.keySet.removeAll(toBeRemoved)
    }
  }
    
  def notifyObserversChanged[T <: ChangeObserver[Any]](subject:ChangeObservable, observerType:Class[T]) :Unit = {
    val itr = observerMapOwner.keySet.iterator
    while (itr.hasNext) {
      val observer = itr.next
      if (observerType.isInstance(observer)) {
        observer.update(subject)
      }
    }
  }
    
  def getObservers[T <: ChangeObserver[Any]](observerType:Class[T]) :List[T] = {
    val result = new ArrayList[T]
    val itr = observerMapOwner.keySet.iterator
    while (itr.hasNext) {
      val observer = itr.next
      result.add(observer.asInstanceOf[T])
      
    }
    result
  }
    
  /**
   * Returns the total number of obervers.
   */
  def getObserverCount :Int = {
    observerMapOwner.size
  }
    
  private def getObserverCount(observerType:Class[_]) :Int = {
    var count = 0
    val itr = observerMapOwner.keySet.iterator
    while (itr.hasNext) {
      val observer = itr.next
      if (observerType.isInstance(observer)) {
        count += 1
      }
    }
    count
  }
    
  /**
   * @param observer the observer to be added
   */
  protected def add[T <: ChangeObserver[Any]](owner:Object, observer:T) :Unit = synchronized {
    assert(observer != null, "Do not add a null observer!")
    observerMapOwner.put(observer, owner)
  }
    
  /**
   * @param observer the observer to be removed
   */
  protected def  remove[T <: ChangeObserver[Any]](observer:T) :Unit = synchronized {
    if (observer == null) {
      return
    }
        
    if (observerMapOwner.keySet().contains(observer)) {
      observerMapOwner.remove(observer)
    }
  }
    
  override def toString :String = {
    val s = new StringBuilder("ChangeObserverList: ")
    s.append(observerMapOwner.size).append(" observers: ")
    val itr = observerMapOwner.keySet.iterator
    while (itr.hasNext) {
      val observer = itr.next
      s.append(" type ").append(observer.getClass.getName)
      s.append(" observer ").append(observer)
    }
        
    s.toString
  }
}
