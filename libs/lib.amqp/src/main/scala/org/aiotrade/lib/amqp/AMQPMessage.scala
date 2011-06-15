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
