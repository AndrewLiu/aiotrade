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
package org.aiotrade.lib.math.timeseries.datasource

import java.awt.Image
import java.util.TimeZone
import org.aiotrade.lib.math.timeseries.Ser

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
trait DataServer[C <: DataContract[_]] extends Ordered[DataServer[C]] {
    
    def displayName:String
    
    def defaultDateFormatString :String
    
    /**
     * 
     * 
     * 
     * @param contract DataContract which contains all the type, market info for this source
     * @param ser the Ser that will be filled by this server
     */
    def subscribe(contract:C, ser:Ser) :Unit
    
    /**
     * first ser is the master one,
     * second one (if available) is that who concerns first one, etc.
     * Example: tickering ser also will compose today's quoteSer
     * 
     * 
     * 
     * @param contract DataContract which contains all the type, market info for this source
     * @param ser the Ser that will be filled by this server
     * @param chairSers
     */
    def subscribe(contract:C, ser:Ser, chainSers:Seq[Ser])
    
    def unSubscribe(contract:C)
    
    def isContractSubsrcribed(contract:C) :Boolean
    
    def startLoadServer :Unit
    
    def startUpdateServer(updateInterval:Int) :Unit
    
    def stopUpdateServer :Unit
    
    def inLoading: Boolean
    def inUpdating :boolean
    
    def createNewInstance :Option[DataServer[_]]
    
    /**
     * @return a long type source id, the format will be only 1 none-zero bit, 
     *         the position of this bit is the source serial number 
     */
    def sourceId :Long
    
    /**
     * @return a byte(from -128 to 127) type serial number, only 0 to 63 is valid.
     */
    def sourceSerialNumber :Byte
    
    def icon :Option[Image]

    def sourceTimeZone :TimeZone
}


