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
 * 
 * @author Caoyuan Deng
 */ 
import java.lang.ref.WeakReference
import org.aiotrade.lib.util.collection.ArrayList

trait Observable {

  private var changed: Boolean = _
  private val observerRefs = new ArrayList[WeakReference[Observer[_]]]

  def addObserver(observer: Observer[_]): Unit = synchronized {
    assert(observer != null, "Can't add a null observer!")
    val contained = observerRefs exists {ref => ref.get == observer}
    if (!contained) {
      observerRefs += new WeakReference[Observer[_]](observer)
    }
  }

  def deleteObserver(observer: Observer[_]): Unit = synchronized {
    observerRefs.find{ref => ref.get == observer} match {
      case Some(x) => observerRefs remove observerRefs.indexOf(x)
      case None =>
    }
  }

  /**
   * for use of sub-class
   */
  def notifyObservers: Unit = {
    notifyObservers(this)
  }

  /**
   * for use of wrap class
   */
  def notifyObservers(subject: Observable): Unit = synchronized {
    if (changed) {
      /** must clone the observers in case deleteObserver is called */
      val clone = new Array[WeakReference[Observer[_]]](observerRefs.size)
      observerRefs.copyToArray(clone, 0)
      clearChanged
      clone foreach {_.get.update(subject)}
    }
  }

  def deleteObservers: Unit = synchronized {
    observerRefs.clear
  }

  protected def setChanged: Unit = synchronized {
    changed = true
  }

  protected def clearChanged: Unit = synchronized {
    changed = false
  }

  def hasChanged: Boolean = synchronized {
    changed
  }

  def countObservers: Int = synchronized {
    observerRefs.size
  }

  // helper:
  def printObservers: Unit = synchronized {
    println("Observer of " + this + " :")
    for (observerRef <- observerRefs) {
      System.out.println(observerRef.get)
    }
  }
}

