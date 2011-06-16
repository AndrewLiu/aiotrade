/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Envelope

/**
 * 
 * @author Caoyuan Deng
 */
package object amqp {

  case object AMQPConnected
  case object AMQPDisconnected

  case class AMQPAcknowledge(deliveryTag: Long)
  
  object AMQPExchange {
    
    /**
     * Each AMQP broker declares one instance of each supported exchange type on it's
     * own (for every virtual host). These exchanges are named after the their type
     * with a prefix of amq., e.g. amq.fanout. The empty exchange name is an alias
     * for amq.direct. For this default direct exchange (and only for that) the broker
     * also declares a binding for every queue in the system with the binding key
     * being identical to the queue name.
     *
     * This behaviour implies that any queue on the system can be written into by
     * publishing a message to the default direct exchange with it's routing-key
     * property being equal to the name of the queue.
     */
    val defaultDirect = "" // amp.direct

    sealed trait AMQPExchange
    case object Direct extends AMQPExchange {override def toString = "direct"}
    case object Topic  extends AMQPExchange {override def toString = "topic" }
    case object Fanout extends AMQPExchange {override def toString = "fanout"}
    case object Match  extends AMQPExchange {override def toString = "match" }
  }

  val DEFAULT_CONTENT_TYPE = ContentType.AVRO

  val LZMA = "lzma"
  val GZIP = "gzip"
  
  val TAG = "tag"
}
