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
package org.aiotrade.lib.indicator

import org.aiotrade.lib.math.timeseries.SerItem
import org.aiotrade.lib.math.timeseries.Ser
import org.aiotrade.lib.math.timeseries.computable.SpotComputable

/**
 * Abstract Profile Indicator
 * The indicator's factor is time - mulitiple values (time, and values in z plane),
 * The series usually contains only one SerItem instance of requried time.
 * 
 * 
 * 
 * 
 * 
 * @author Caoyuan Deng
 */
abstract class SpotIndicator(baseSer: Ser) extends AbstractIndicator(baseSer) with SpotComputable {
    
  var spotTime = -Long.MaxValue
    
  def this() {
    this(null)
  }
    
  def computeItem(time: Long): SerItem = {
        
    /** get masterIndex before preCalc(), which may clear this data */
    val baseIdx = _baseSer.indexOfOccurredTime(time)
        
    preComputeFrom(time)
        
    val newItem = computeSpot(time, baseIdx)
        
    spotTime = time
        
    postComputeFrom
        
    newItem;
  }
    
  protected def computeCont(begIdx: Int, itemSize: Int): Unit = {
    var i = begIdx
    while (i < itemSize) {
      val time = _baseSer.timestamps(i)
      if (time == spotTime) {
        computeSpot(time, i)
      }
      i += 1
    }
  }
    
  protected def computeSpot(time: Long, baseIdx: Int): SerItem
}

