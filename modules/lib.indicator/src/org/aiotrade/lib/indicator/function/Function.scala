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
package org.aiotrade.lib.indicator.function

import org.aiotrade.lib.math.timeseries.Ser

/**
 *
 * @author Caoyuan Deng
 */
object FunctionID {
    def apply[T <: Function](tpe:Class[T], baseSer:Ser, args:Any*) = new FunctionID(tpe, baseSer, args:_*)
}

class FunctionID[T <: Function](val tpe:Class[T], val baseSer:Ser, val args:Any*) {
    override
    def equals(o:Any) :Boolean = o match {
        case x:FunctionID[_] if this.tpe.getName.equals(x.tpe.getName) && this.baseSer.equals(x.baseSer) && this.args.size == x.args.size =>
            val itr1 = this.args.elements
            val itr2 = x.args.elements
            while (itr1.hasNext) {
                if (!itr1.next.equals(itr2.next)) {
                    return false
                }
            }
            true
        case _ => false
    }

    override
    def hashCode :int = {
        var h = 17
        h = 37 * h + this.getClass.getName.hashCode
        h = 37 * h + baseSer.hashCode
        val itr = args.elements
        while (itr.hasNext) {
            val more :Int = itr.next match {
                case x:Short   => x
                case x:Char    => x
                case x:Byte    => x
                case x:Boolean => if (x) 0 else 1
                case x:Long    => (x ^ (x >>> 32)).toInt
                case x:Float   => java.lang.Float.floatToIntBits(x)
                case x:Double  => val x1 = java.lang.Double.doubleToLongBits(x); (x1 ^ (x1 >>> 32)).toInt
                case x:AnyRef  => x.hashCode
            }
            h = 37 * h + more
        }
        h

    }
}
    
trait Function {
    
    /**
     * set the function's arguments. 
     * @param baseSer, the ser that this function is based, ie. used to compute
     */
    def set(baseSer:Ser, args:Any*) :Unit

    def id :FunctionID[_]

    def idEquals(baseSer:Ser, args:Any*) :Boolean

    /**
     * This method will compute from computedIdx <b>to</b> idx.
     *
     * and AbstractIndicator.compute(final long begTime) will compute <b>from</b>
     * begTime to last item
     * 
     * @param sessionId, the sessionId usally is controlled by outside caller, 
     *        such as an indicator
     * @param idx, the idx to be computed to
     */
    def computeTo(sessionId:Long, idx:Int) :Unit
}


