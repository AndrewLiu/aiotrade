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

import java.awt.Image
import java.util.Calendar
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities.PersistenceManager

/**
 * most fields' default value should be OK.
 *
 * @author Caoyuan Deng
 */
object QuoteContract {
  val folderName = "QuoteServers"
}

class QuoteContract extends SecDataContract[QuoteServer] {
  import QuoteContract._

  serviceClassName = "org.aiotrade.platform.modules.dataserver.yahoo.YahooQuoteServer"
  active = true
  /** default freq */
  freq = TFreq.DAILY
  dateFormatPattern = "yyyy-MM-dd"
  urlString = ""
  refreshable = false
  refreshInterval = 60 // seconds
  inputStream = None

  private val cal = Calendar.getInstance
  endDate = cal.getTime
  cal.set(1970, Calendar.JANUARY, 1)
  beginDate = cal.getTime

  def icon: Option[Image] =  {
    val server = if (isServiceInstanceCreated) createdServerInstance() else lookupServiceTemplate

    server match {
      case None => None
      case Some(x) => x.icon
    }
  }

  def supportedFreqs: Array[TFreq] = {
    val server = if (isServiceInstanceCreated) createdServerInstance() else lookupServiceTemplate

    server match {
      case None => Array()
      case Some(x) => x.supportedFreqs
    }
  }

  def isFreqSupported(freq: TFreq): Boolean = {
    //        /** check if is my default freq, if true at least support default freq */
    //        if (freq.equals(getFreq())) {
    //            return true;
    //        }

    val server = if (isServiceInstanceCreated) createdServerInstance() else lookupServiceTemplate
    server match {
      case None => false
      case Some(x) => x.isFreqSupported(freq)
    }
  }

  override def displayName = "Quote Data Contract"

  /**
   * @param none args are needed.
   */
  override def createServiceInstance(args: Any*) :Option[QuoteServer] = {
    lookupServiceTemplate match {
      case None => None
      case Some(x) => x.createNewInstance.asInstanceOf[Option[QuoteServer]]
    }
  }

  def lookupServiceTemplate: Option[QuoteServer] =  {
    val services = PersistenceManager().lookupAllRegisteredServices(classOf[QuoteServer], folderName)
    services.find{x => x.getClass.getName.equals(serviceClassName)} match {
      case None =>
        try {
          Some(Class.forName(serviceClassName).newInstance.asInstanceOf[QuoteServer])
        } catch {case ex: Exception => ex.printStackTrace; None}
      case some => some
    }
  }

}
