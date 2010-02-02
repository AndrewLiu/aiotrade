package org.aiotrade.lib.amqp

import com.rabbitmq.client.ConnectionFactory
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import scala.actors.Actor

case class AMQPMessage[T](message: T, routingKey: String)

class AMQPSender[T](cf: ConnectionFactory, host: String, port: Int) extends Actor {

  private val conn = cf.newConnection(host, port)
  private val channel = conn.createChannel
  private val ticket = channel.accessRequest("/data")

  def act {
    Actor.loop {
      react {
        case AMQPMessage(msg: T, routingKey: String) => send(msg, routingKey)
      }
    }
  }

  private def send(msg: T, routingKey: String) {
    val bytes = new ByteArrayOutputStream
    val os = new ObjectOutputStream(bytes)
    os.writeObject(msg)
    os.close

    val body = bytes.toByteArray

    // publish to exchange, use symbol as routingKey
    channel.basicPublish(ticket, Constants.exchange, routingKey, null, body)
    println(msg + " sent: routingKey=" + routingKey + " size=" + body.length)
  }
}