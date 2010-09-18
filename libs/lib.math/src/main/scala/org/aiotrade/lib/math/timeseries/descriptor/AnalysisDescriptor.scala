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

import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.Action
import org.aiotrade.lib.math.PersistenceManager
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

  private val log = Logger.getLogger(this.getClass.getName)

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

  /**
   * init and return a server instance
   * @param args args to init server instance
   */
  def createdServerInstance: S = {
    assert(isServiceInstanceCreated, "This method should only be called after serviceInstance created!")
    serviceInstance().get
  }
    
  def serviceInstance(args: Any*): Option[S] = {
    if (_serviceInstance.isEmpty) {
      // @Note to pass a variable args to another function, should use type "_*" to extract it as a plain seq,
      // other wise, it will be treated as one arg:Seq[_], and the accepting function will compose it as
      // Seq(Seq(arg1, arg2, ...)) instead of Seq(arg1, arg2, ...)
      _serviceInstance = createServiceInstance(args: _*)
    }
    _serviceInstance.asInstanceOf[Option[S]]
  }
    
  def isServiceInstanceCreated: Boolean = {
    _serviceInstance.isDefined
  }

  def serviceClassName = _serviceClassName
  def serviceClassName_=(serviceClassName: String) = {
    this._serviceClassName = serviceClassName
  }

  def freq = _freq
  def freq_=(freq: TFreq) = {
    this._freq = freq
  }

  def active = _active
  def active_=(active: Boolean) = {
    this._active = active
  }
  
  def displayName: String
    
  def idEquals(serviceClassName: String, freq: TFreq): Boolean = {
    this.serviceClassName.equals(serviceClassName) && this.freq.equals(freq)
  }

  protected def createServiceInstance(args: Any*): Option[S]

  // --- helpers ---
  
  protected def lookupServiceTemplate[T <: AnyRef](tpe: Class[T], folderName: String): Option[T] = {
    val services = PersistenceManager().lookupAllRegisteredServices(tpe, folderName)
    services find {x =>
      val className = x.getClass.getName
      className == serviceClassName || (className + "$") == serviceClassName
    } match {
      case None =>
        try {
          log.warning("Cannot find registeredService of " + tpe + " in folder '" +
                      folderName + "': " + services.map(_.getClass.getName) +
                      ", try Class.forName call: serviceClassName=" + serviceClassName)

          val klass = Class.forName(serviceClassName)
          getScalaSingletonInstance(klass) match {
            case Some(x) if x.isInstanceOf[T] => Option(x.asInstanceOf[T])
            case _ => Option(klass.newInstance.asInstanceOf[T])
          }
        } catch {
          case ex: Exception => log.log(Level.SEVERE, "Failed to call Class.forName of class: " + serviceClassName, ex)
            None
        }
      case some => some
    }
  }

  protected def isScalaSingletonClass(klass: Class[_]) = {
    klass.getSimpleName.endsWith("$") && klass.getInterfaces.exists(_.getName == "scala.ScalaObject") &&
    klass.getDeclaredFields.exists(_.getName == "MODULE$")
  }

  protected def getScalaSingletonInstance(klass: Class[_]): Option[AnyRef] = {
    if (klass.getSimpleName.endsWith("$") && klass.getInterfaces.exists(_.getName == "scala.ScalaObject")) {
      klass.getDeclaredFields.find(_.getName == "MODULE$") match {
        case Some(x) => Option(x.get(klass))
        case None => None
      }
    } else None
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
