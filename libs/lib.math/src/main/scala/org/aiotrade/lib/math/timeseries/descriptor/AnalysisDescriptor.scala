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
package org.aiotrade.lib.math.timeseries.descriptor

import javax.swing.Action
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.util.serialization.BeansDocument
//import org.aiotrade.lib.util.serialization.DeserializationConstructor
import org.aiotrade.lib.util.serialization.JavaDocument
import org.aiotrade.lib.util.swing.action.WithActions
import org.aiotrade.lib.util.swing.action.WithActionsHelper
import org.w3c.dom.Element

/**
 * Descriptor is something like NetBeans' DataObject
 *
 * [S] Service class type
 *
 * @author Caoyuan Deng
 */
abstract class AnalysisDescriptor[+S](private var _serviceClassName: String,
                                      private var _freq: TFreq,
                                      private var _active: Boolean) extends WithActions {

  private val withActionsHelper = new WithActionsHelper(this)

  var containerContents: AnalysisContents = _

  /** @Note: covariant type S can not occur in contravariant position in type S of parameter of setter */
  private var _serviceInstance: Option[_] = None
    
  def this() {
    this(null, TFreq.DAILY, false)
  }
            
  def set(serviceClassName: String, freq: TFreq): Unit = {
    this.serviceClassName = serviceClassName
    this.freq = freq.clone
  }

  protected def createServiceInstance(args: Any*): Option[S]

  /**
   * init and return a server instance
   * @param args args to init server instance
   */
  def createdServerInstance(args: Any*): Option[S] =  {
    assert(_serviceInstance != None, "This method should only be called after serviceInstance created!")
    // * @Note to pass a variable args to another function, should use type "_*" to extract it as a plain seq,
    // other wise, it will be treated as one arg:Seq[_], and the accepting function will compose it as
    // Seq(Seq(arg1, arg2, ...)) instead of Seq(arg1, arg2, ...)
    serviceInstance(args: _*)
  }
    
  def serviceInstance(args: Any*): Option[S] = {
    if (_serviceInstance == None) {
      _serviceInstance = createServiceInstance(args: _*)
    }
    _serviceInstance.asInstanceOf[Option[S]]
  }
    
  protected def isServiceInstanceCreated: Boolean = {
    _serviceInstance != None
  }

  def serviceClassName_=(serviceClassName: String) = this._serviceClassName = serviceClassName
  def serviceClassName = _serviceClassName

  def freq_=(freq: TFreq) = this._freq = freq
  def freq = _freq

  def active_=(active: Boolean) = this._active = active
  def active = _active

  def displayName: String
    
  def idEquals(serviceClassName: String, freq: TFreq): Boolean = {
    this.serviceClassName.equals(serviceClassName) && this.freq.equals(freq)
  }
    
  def addAction(action: Action): Action = {
    withActionsHelper.addAction(action)
  }
    
  def lookupAction[T <: Action](tpe: Class[T]): Option[T] = {
    withActionsHelper.lookupAction(tpe)
  }
    
  def createDefaultActions: Array[Action] = {
    Array[Action]()
  }
    
  def writeToBean(doc:BeansDocument): Element = {
    val bean = doc.createBean(this)
        
    doc.valuePropertyOfBean(bean, "active", active)
    doc.valuePropertyOfBean(bean, "serviceClassName", serviceClassName)
    doc.innerPropertyOfBean(bean, "freq", freq.writeToBean(doc))
        
    bean
  }
    
  def writeToJava(id:String): String = {
    freq.writeToJava("freq") +
    JavaDocument.create(id, this.getClass,
                        "" + serviceClassName +
                        "freq" +
                        active)
  }
    
}
