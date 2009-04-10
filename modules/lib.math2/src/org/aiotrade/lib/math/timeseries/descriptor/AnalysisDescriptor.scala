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
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.util.serialization.BeansDocument;
import org.aiotrade.lib.util.serialization.DeserializationConstructor;
import org.aiotrade.lib.util.serialization.JavaDocument;
import org.aiotrade.lib.util.swing.action.WithActions;
import org.aiotrade.lib.util.swing.action.WithActionsHelper;
import org.w3c.dom.Element;

/**
 * Descriptor is something like NetBeans' DataObject
 *
 * <S> Service class type
 *
 * @author Caoyuan Deng
 */
abstract class AnalysisDescriptor[S](var serviceClassName:String, var freq:Frequency, var active:Boolean) extends WithActions {
    private val withActionsHelper = new WithActionsHelper(this)
    
    var containerContents :AnalysisContents = _
    
    private var _serviceInstance :Option[S] = None
    
    def this() {
        this(null, Frequency.DAILY, false)
    }
            
    def set(serviceClassName:String, freq:Frequency) :Unit = {
        this.serviceClassName = serviceClassName
        this.freq = freq.clone
    }
            
    def createdServerInstance(args:Object*) :Option[S] =  {
        assert(_serviceInstance != None, "This method should only be called after serviceInstance created!")
        serviceInstance()
    }
    
    def serviceInstance(args:Object*) :Option[S] = {
        if (_serviceInstance == None) {
            _serviceInstance = createServiceInstance(args)
        }
        _serviceInstance
    }
    
    protected def isServiceInstanceCreated :Boolean = {
        _serviceInstance != None
    }
    
    protected def createServiceInstance(args:Object* ) :Option[S]
    
    def getDisplayName:String
    
    def idEquals(serviceClassName:String, freq:Frequency) :Boolean = {
        this.serviceClassName.equals(serviceClassName) && this.freq.equals(freq)
    }
    
    def addAction(action:Action) :Action = {
        withActionsHelper.addAction(action);
    }
    
    def lookupAction[T <: Action](tpe:Class[T]) :T = {
        withActionsHelper.lookupAction(tpe)
    }
    
    def createDefaultActions :Array[Action] = {
        Array[Action]()
    }
    
    def writeToBean(doc:BeansDocument) :Element = {
        val bean = doc.createBean(this)
        
        doc.valuePropertyOfBean(bean, "active", active)
        doc.valuePropertyOfBean(bean, "serviceClassName", serviceClassName)
        doc.innerPropertyOfBean(bean, "freq", freq.writeToBean(doc))
        
        bean
    }
    
    def writeToJava(id:String) :String = {
        freq.writeToJava("freq") +
        JavaDocument.create(id, this.getClass,
                            "" + serviceClassName +
                            "freq" +
                            active)
    }
    
}
