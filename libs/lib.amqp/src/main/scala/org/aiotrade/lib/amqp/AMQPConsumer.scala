/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import scala.actors.Actor

class AMQPConsumer(channel: Channel, observer: Actor) extends DefaultConsumer(channel) {
  override def handleDelivery(tag: String, env: Envelope, props: AMQP.BasicProperties, body: Array[Byte]) {
    val contentType = props.contentType

    // deserialize
    val in = new ObjectInputStream(new ByteArrayInputStream(body))
    in.readObject match {
      case content =>
        // send back to interested observers for further relay
        observer ! AMQPMessage(content, props)
    }

    // if noAck is set false, messages will be blocked until an ack to broker, 
    // so it's better always ack it. (Although prefetch may deliver more than
    // one message to consumer)
    channel.basicAck(env.getDeliveryTag, false)
  }
}