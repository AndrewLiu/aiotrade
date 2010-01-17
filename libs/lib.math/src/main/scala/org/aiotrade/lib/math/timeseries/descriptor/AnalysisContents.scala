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
package org.aiotrade.lib.math.timeseries.descriptor;

import javax.swing.Action;
import org.aiotrade.lib.math.PersistenceManager;
import org.aiotrade.lib.math.timeseries.TFreq;
import org.aiotrade.lib.math.timeseries.datasource.SerProvider
import org.aiotrade.lib.util.serialization.BeansDocument;
import org.aiotrade.lib.util.swing.action.WithActions;
import org.aiotrade.lib.util.swing.action.WithActionsHelper;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.w3c.dom.Element
import scala.collection.mutable.ArrayBuffer
/**
 *
 * @author Caoyuan Deng
 */
class AnalysisContents(var uniSymbol: String) extends WithActions {
  private val withActionsHelper = new WithActionsHelper(this)

  /** Ser could be loaded lazily */
  var serProvider: SerProvider[_] = _
    
  /** use List to store descriptor, so they can be ordered by index */
  private var descriptorBuf = ArrayBuffer[AnalysisDescriptor[_]]()
    
  def descriptors: List[AnalysisDescriptor[_]] = descriptorBuf.toList
    
  def addDescriptor(descriptor: AnalysisDescriptor[_]) {
    if (!descriptorBuf.contains(descriptor)) {
      descriptorBuf += descriptor
      descriptor.containerContents = this
    }
  }
    
  def removeDescriptor(descriptor: AnalysisDescriptor[_]) {
    descriptorBuf.remove(descriptorBuf.indexOf(descriptor))
  }
    
  def removeDescriptor(idx: Int) {
    descriptorBuf.remove(idx)
  }
    
  def indexOf(descriptor: AnalysisDescriptor[_]): Int = {
    descriptorBuf.indexOf(descriptor)
  }
    
  def lastIndexOf[T <: AnalysisDescriptor[Any]](clz: Class[T]): Int = {
    var lastOne: T = null.asInstanceOf[T]
    for (descriptor <- descriptorBuf if clz.isInstance(descriptor)) {
      lastOne = descriptor.asInstanceOf[T]
    }
        
    if (lastOne != null) descriptorBuf.indexOf(lastOne) else -1
  }
    
  def clearDescriptors[T <: AnalysisDescriptor[_]](clz: Class[T]) {
    /**
     * try to avoid java.util.ConcurrentModificationException by add those to
     * toBeRemoved, then call descriptorList.removeAll(toBeRemoved)
     */
    var toBeRemoved = List[Int]()
    var i = 0
    for (descriptor <- descriptorBuf) {
      if (clz.isInstance(descriptor)) {
        toBeRemoved ::= i
      }
      i += 1
    }
        
    for (i <- toBeRemoved) {
      descriptorBuf.remove(i)
    }
  }
    
  /**
   *
   * @param clazz the Class being looking up
   * @return found collection of AnalysisDescriptor instances.
   *         If found none, return an empty collection other than null
   */
  def lookupDescriptors[T <: AnalysisDescriptor[_]](clz: Class[T]): Seq[T] = {
    for (descriptor <- descriptorBuf if clz.isInstance(descriptor)) yield descriptor.asInstanceOf[T]
  }
    
  /**
   * Lookup the descriptorList of clazz (Indicator/Drawing/Source etc) with the same time frequency
   */
  def lookupDescriptors[T <: AnalysisDescriptor[_]](clz: Class[T], freq: TFreq): Seq[T] = {
    for (descriptor <- descriptorBuf if clz.isInstance(descriptor) && descriptor.freq == freq)
      yield descriptor.asInstanceOf[T]
  }
    
  def lookupDescriptor[T <: AnalysisDescriptor[_]](clz: Class[T], serviceClassName: String, freq: TFreq): Option[T] = {
    lookupDescriptors(clz) find (_.idEquals(serviceClassName, freq))
  }
    
  def lookupActiveDescriptor[T <: AnalysisDescriptor[_]](clz: Class[T]): Option[T] = {
    lookupDescriptors(clz) find (_.active)
  }
    
  def createDescriptor[T <: AnalysisDescriptor[_]](clz: Class[T], serviceClassName: String, freq: TFreq): Option[T] = {
    try {
      val descriptor = clz.newInstance
      descriptor.set(serviceClassName, freq)
      addDescriptor(descriptor)
            
      Some(descriptor.asInstanceOf[T])
    } catch {
      case ex: IllegalAccessException => ex.printStackTrace; None
      case ex: InstantiationException => ex.printStackTrace; None
    }
  }
            
  def addAction(action: Action): Action = {
    withActionsHelper.addAction(action)
  }
    
  def lookupAction[T <: Action](tpe: Class[T]): Option[T] = {
    withActionsHelper.lookupAction(tpe)
  }
    
  def createDefaultActions: Array[Action] = {
    Array(new ContentsSaveAction)
  }
    
  def writeToBean(doc: BeansDocument): Element = {
    val bean = doc.createBean(this)
        
    val list = doc.listPropertyOfBean(bean, "descriptors")
    for (descriptor <- descriptorBuf) {
      doc.innerElementOfList(list, descriptor.writeToBean(doc))
    }
        
    bean
  }
    
  private class ContentsSaveAction extends SaveAction {
        
    def execute: Unit = {
      PersistenceManager().saveContents(AnalysisContents.this)
    }
        
  }
    
}

