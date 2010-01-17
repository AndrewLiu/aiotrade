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
package org.aiotrade.lib.securities.dataserver

import java.util.Calendar
import org.aiotrade.lib.math.timeseries.datasource.{DataContract,DataServer}
import org.aiotrade.lib.securities.Sec
import org.aiotrade.lib.util.serialization.JavaDocument
import org.aiotrade.lib.util.serialization.BeansDocument
import org.w3c.dom.Element

/**
 * most fields' default value should be OK.
 *
 * @author Caoyuan Deng
 */
abstract class SecDataContract[S <: DataServer[_]] extends DataContract[S] {
  var reqId = 0
  var secType: Sec.Type = Sec.Type.Stock
  var primaryExchange = "SUPERSOES"
  var exchange = "SMART"
  var currency = "USD"
    
  active = true
  urlString = ""
  refreshable = false
  refreshInterval = 60 // seconds
  inputStream = None
    
  private val cal = Calendar.getInstance
  endDate = cal.getTime
  cal.set(1970, Calendar.JANUARY, 1)
  beginDate = cal.getTime

  override def writeToBean(doc: BeansDocument): Element = {
    val bean = super.writeToBean(doc)
        
    doc.valuePropertyOfBean(bean, "secType", secType)
    doc.valuePropertyOfBean(bean, "primaryExchange", primaryExchange)
    doc.valuePropertyOfBean(bean, "exchange", exchange)
    doc.valuePropertyOfBean(bean, "currency", currency)
        
    bean
  }
    
  override def writeToJava(id: String): String = {
    ""
    super.writeToJava(id) +
    JavaDocument.set(id, "setSecType", classOf[Sec.Type].getName + "." + secType) +
    JavaDocument.set(id, "setPrimaryExchange", "" + primaryExchange) +
    JavaDocument.set(id, "setExchange", "" + exchange) +
    JavaDocument.set(id, "setCurrency", "" + currency)
  }
    
}

