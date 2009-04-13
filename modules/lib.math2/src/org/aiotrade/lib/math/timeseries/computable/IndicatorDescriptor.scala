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
package org.aiotrade.lib.math.computable

import javax.swing.Action
import org.aiotrade.lib.math.PersistenceManager
import org.aiotrade.lib.math.timeseries.Frequency
import org.aiotrade.lib.math.timeseries.computable.ComputableHelper
import org.aiotrade.lib.math.timeseries.computable.Indicator
import org.aiotrade.lib.math.timeseries.computable.Opt
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisDescriptor
import org.aiotrade.lib.math.timeseries.Ser
import org.aiotrade.lib.util.serialization.BeansDocument
import org.w3c.dom.Element
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Caoyuan Deng
 */
class IndicatorDescriptor(serviceClassName:String, freq:Frequency, optsX:ArrayBuffer[Opt], active:Boolean) extends AnalysisDescriptor[Indicator](serviceClassName, freq, active) {

    private var _opts:ArrayBuffer[Opt] = optsX

    def this() {
        this(null, Frequency.DAILY, new ArrayBuffer[Opt], false)

    }

    override
    def set(serviceClassName:String, freq:Frequency) :Unit = {
        super.set(serviceClassName, freq)

        setOptsToDefault
    }

    def opts :ArrayBuffer[Opt]= _opts
    def opts_=(opts:ArrayBuffer[Opt]) :Unit = {
        /**
         * @NOTICE:
         * always create a new copy of inOpts to seperate the opts of this
         * and that transfered in (we don't know who transfer it in, so, be more
         * carefule is always good)
         */
        val mySize = _opts.size
        if (_opts != null) {
            for (i <- 0 until opts.size) {
                val newOpt = _opts(i).clone
                if (i < mySize) {
                    _opts(i) = newOpt
                } else {
                    _opts += newOpt
                }
            }
        } else {
            _opts.clear
        }
    }

    override
    def displayName :String = {
        val indicator = if (isServiceInstanceCreated) createdServerInstance() else lookupServiceTemplate
        val displayStr = indicator match {
            case None => serviceClassName
            case Some(x) => x.shortDescription
        }
        
        ComputableHelper.displayName(displayStr, opts)
    }

    /**
     * @NOTICE
     * Here we get a new indicator instance by searching DefaultFileSystem(on NetBeans).
     * This is because that this instance may from other modules (i.e. SolarisIndicator),
     * it may not be seen from this module. Actually we should not set dependency on
     * those added-on modules.
     * @param baseSer for indicator
     */
    override
    protected def createServiceInstance(args:Any*) :Option[Indicator] = {
        val baseSer = args(0).asInstanceOf[Ser]

        lookupServiceTemplate match {
            case None => None
            case Some(x) => 
                val instance = x.createNewInstance(baseSer)
                
                if (opts.size == 0) {
                    /** this means this indicatorDescritor's opts may not be set yet, so set a default one now */
                    opts = instance.opts
                } else {
                    /** should set opts here, because it's from those stored in xml */
                    instance.opts = opts
                }
                Some(instance)
        }
    }

    def setOptsToDefault :Unit = {
        val defaultOpts = PersistenceManager.getDefault.defaultContents.lookupDescriptor(
            classOf[IndicatorDescriptor], serviceClassName, freq) match {
            case None => lookupServiceTemplate match {
                    case None => None
                    case Some(template) => Some(template.opts)
                }
            case Some(defaultDescriptor) => Some(defaultDescriptor.opts)
        }

        defaultOpts match {
            case Some(x) => opts = x
            case None =>
        }
    }

    def lookupServiceTemplate :Option[Indicator] = {
        val indicators = PersistenceManager.getDefault.lookupAllRegisteredServices(classOf[Indicator], folderName)
        indicators.find{x => x.getClass.getName.equals(serviceClassName)}
    }

    def folderName = "Indicators"

    override
    def createDefaultActions :Array[Action] = {
        IndicatorDescriptorActionFactory.getDefault.createActions(this)
    }

    override
    def writeToBean(doc:BeansDocument) :Element = {
        val bean = super.writeToBean(doc)

        val list = doc.listPropertyOfBean(bean, "opts")
        for (opt <- opts) {
            doc.innerElementOfList(list, opt.writeToBean(doc))
        }

        bean
    }
}

