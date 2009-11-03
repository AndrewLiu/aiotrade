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
import org.aiotrade.lib.util.collection.ArrayList
import scala.collection.mutable.HashMap

class ChangeObservableHelper {
  
  @transient
  val observerToOwner = new HashMap[ChangeObserver[Any], AnyRef]
    
  def getObservers: Collection[ChangeObserver[Any]] = {
    observerToOwner.keySet
  }
    
  def addObserver(owner: Object, observer: ChangeObserver[Any]): Unit = synchronized {
    observerToOwner += (observer -> owner)
  }
    
  def  removeObserver(observer: ChangeObserver[Any]): Unit = synchronized {
    if (observer == null) {
      return
    }
        
    if (observerToOwner.keySet.contains(observer)) {
      observerToOwner -= observer
    }
  }
    
  def removeObserversOf(owner: Object): Unit = {
    val toBeRemoved = new ArrayList[ChangeObserver[Any]]
    for (observer <- observerToOwner.keysIterator if observerToOwner.get(observer) == owner) {
      toBeRemoved += observer
    }
        
    synchronized {
      observerToOwner --= toBeRemoved
    }
  }
    
  def notifyObserversChanged[T <: ChangeObserver[Any]](subject: Any, observerType: Class[T]): Unit = {
    for (observer <- observerToOwner.keysIterator if observerType.isInstance(observer)) {
      observer.asInstanceOf[ChangeObserver[Any]].update(subject)
    }
  }
    
  def getObservers[T <: ChangeObserver[Any]: Manifest](observerType: Class[T]): ArrayList[T] = {
    val result = new ArrayList[T]
    for (observer <- observerToOwner.keysIterator) {
      result += (observer.asInstanceOf[T])
    }
    result
  }
    
  /**
   * Returns the total number of obervers.
   */
  def getObserverCount: Int = {
    observerToOwner.size
  }
    
  private def getObserverCount(observerType: Class[_]): Int = {
    var count = 0
    for (observer <- observerToOwner.keysIterator if observerType.isInstance(observer)) {
      count += 1
    }
    count
  }
    
  /**
   * @param observer the observer to be added
   */
  protected def add[T <: ChangeObserver[Any]](owner: AnyRef, observer: T): Unit = synchronized {
    assert(observer != null, "Do not add a null observer!")
    observerToOwner.put(observer, owner)
  }
    
  /**
   * @param observer the observer to be removed
   */
  protected def remove[T <: ChangeObserver[Any]](observer: T): Unit = synchronized {
    if (observer == null) {
      return
    }
        
    if (observerToOwner.keySet.contains(observer)) {
      observerToOwner.remove(observer)
    }
  }
    
  override def toString = {
    val sb = new StringBuilder("ChangeObserverList: ")
    sb.append(observerToOwner.size).append(" observers: ")
    for (observer <- observerToOwner.keysIterator) {
      sb.append(" type ").append(observer.getClass.getName)
      sb.append(" observer ").append(observer)
    }
        
    sb.toString
  }
}
