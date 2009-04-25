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
package org.aiotrade.lib.math.timeseries.datasource;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisDescriptor;
import org.aiotrade.lib.util.serialization.BeansDocument;
import org.aiotrade.lib.util.serialization.JavaDocument;
import org.w3c.dom.Element;

/**
 * Securities' data source request contract. It know how to find and invoke
 * server by call createBindInstance().
 *
 * We simplely inherit AnalysisDescriptor, we may think the bindClass provides
 * service for descriptor.
 *
 * most fields' default value should be OK.
 *
 * @author Caoyuan Deng
 */
abstract class DataContract[S <: DataServer[_]] extends AnalysisDescriptor[S] {

    var symbol :String = _ // symbol in source
    var category :String = _
    var shortName :String = _
    var longName :String = _
    var dateFormatPattern :String = _
    var urlString :String = ""
    var refreshable :Boolean = false
    var refreshInterval :Int = 5 // seconds
    var inputStream :Option[InputStream] = None

    private val cal = Calendar.getInstance
    cal.set(1990, Calendar.JANUARY, 1)
    var beginDate :Date = cal.getTime
    var endDate :Date = cal.getTime
    
    override
    def toString :String = displayName

    override
    def writeToBean(doc:BeansDocument) :Element = {
        val bean = super.writeToBean(doc)

        doc.valuePropertyOfBean(bean, "symbol", symbol)
        doc.valuePropertyOfBean(bean, "dateFormatPattern", dateFormatPattern)

        val begDateBean = doc.createBean(beginDate)
        doc.innerPropertyOfBean(bean, "begDate", begDateBean)
        doc.valueConstructorArgOfBean(begDateBean, 0, beginDate.getTime)

        val endDateBean = doc.createBean(endDate)
        doc.innerPropertyOfBean(bean, "endDate", endDateBean)
        doc.valueConstructorArgOfBean(endDateBean, 0, endDate.getTime)

        doc.valuePropertyOfBean(bean, "urlString", urlString)
        doc.valuePropertyOfBean(bean, "refreshable", refreshable)
        doc.valuePropertyOfBean(bean, "refreshInterval", refreshInterval)

        bean
    }

    override
    def writeToJava(id:String) :String = {
        super.writeToJava(id) +
        JavaDocument.set(id, "setSymbol", "" + symbol) +
        JavaDocument.set(id, "setDateFormatPattern", "" + dateFormatPattern) +
        JavaDocument.create("begDate", classOf[Date], beginDate.getTime.asInstanceOf[AnyRef]) +
        JavaDocument.set(id, "setBegDate", "begDate") +
        JavaDocument.create("endDate", classOf[Date], endDate.getTime.asInstanceOf[AnyRef]) +
        JavaDocument.set(id, "setEndDate", "endDate") +
        JavaDocument.set(id, "setUrlString", urlString) +
        JavaDocument.set(id, "setRefreshable", refreshable) +
        JavaDocument.set(id, "setRefreshInterval", refreshInterval)
    }
}

