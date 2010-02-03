package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

abstract class AMQPSender[T](cf: ConnectionFactory, host: String, port: Int, exchange: String){

  val (conn, channel, ticket) = connect

  private def connect: (Connection, Channel, Int) = {
    val conn = cf.newConnection(host, port)
    val channel = conn.createChannel
    val ticket = configure(channel)
    (conn, channel, ticket)
  }

  def configure(channel: Channel): Int

  def send(msg: T, routingKey: String, props: AMQP.BasicProperties) {
    val bytes = new ByteArrayOutputStream
    val store = new ObjectOutputStream(bytes)
    store.writeObject(msg)
    store.close

    val body = bytes.toByteArray

    channel.basicPublish(ticket, exchange, routingKey, props, body)
    //println(msg + " sent: routingKey=" + routingKey + " size=" + body.length)
  }
}