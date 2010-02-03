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

class AMQPConsumer[T](channel: Channel, observer: Actor) extends DefaultConsumer(channel) {
  override def handleDelivery(tag: String, env: Envelope, props: AMQP.BasicProperties, body: Array[Byte]) {
    val contentType = props.contentType

    // deserialize
    val in = new ObjectInputStream(new ByteArrayInputStream(body))
    in.readObject match {
      case content: T =>
        // send back for further relay to interested observers
        observer ! AMQPMessage(content, props)
    }

    channel.basicAck(env.getDeliveryTag, false)
  }
}