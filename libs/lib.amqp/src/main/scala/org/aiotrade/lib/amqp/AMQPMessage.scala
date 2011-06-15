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
package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Envelope
import org.aiotrade.lib.util.reactors.Event

/**
 * @todo use null to replace body "RPC timeout"
 */
case object RpcTimeout extends RpcResponse("RPC timeout", null, null) {
  override def toString = "RPC timeout"
}

case class RpcRequest(args: Any*) extends Event

object RpcResponse {
  def apply(body: Any, props: AMQP.BasicProperties = null, envelope: Envelope = null) = new RpcResponse(body, props, envelope)
  def unapply(x: RpcResponse): Option[(Any, AMQP.BasicProperties)] = Some((x.body, x.props))
}

@serializable
@SerialVersionUID(-9115442645150361620L)
class RpcResponse(val body: Any, val props: AMQP.BasicProperties = new AMQP.BasicProperties.Builder().build, val envelope: Envelope = null) extends Event {
  override def toString = if (body == null) "null" else body.toString
}

/**
 * @param content A deserialized value received via AMQP.
 * @param props
 * @param envelope of AMQP
 *
 * Messages received from AMQP are wrapped in this case class. When you
 * register a listener, this is the case class that you will be matching on.
 */
@serializable
case class AMQPMessage(body: Any, props: AMQP.BasicProperties = new AMQP.BasicProperties.Builder().build, envelope: Envelope = null)
