package org.aiotrade.lib.amqp

// message for adding observers
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.QueueingConsumer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import scala.actors.Actor

// The trade object that needs to be serialized
@serializable
case class Quote(ref: String, symbol: String, var value: Int)
case class QuoteMessage(message: Quote)

/**
 * The dispatcher that listens over the AMQP message endpoint.
 * It manages a list of subscribers to the trade message and also sends AMQP
 * messages coming in to the queue/exchange to the list of observers.
 */
object Constants {
  val exchange = "market.quote"
  val symbols = Array("600001", "600002", "600003")
}

import Constants._
class QuoteProducer(cf: ConnectionFactory, host: String, port: Int) {

  private val conn = cf.newConnection(host, port)
  private val channel = conn.createChannel
  private val ticket = channel.accessRequest("/data")

  // set up exchange and queue
  channel.exchangeDeclare(ticket, exchange, "topic")
}

/**
 * an actor that gets messages from upstream and publishes them to the AMQP exchange 
 */
class QuoteMessageSender(cf: ConnectionFactory, host: String, port: Int) extends Actor {

  private val conn = cf.newConnection(host, port)
  private val channel = conn.createChannel
  private val ticket = channel.accessRequest("/data")

  def act = {
    Actor.loop {
      react {
        case QuoteMessage(msg: Quote) => send(msg)
      }
    }
  }

  private def send(quote: Quote) {
    val bytes = new ByteArrayOutputStream
    val os = new ObjectOutputStream(bytes)
    os.writeObject(quote)
    os.close

    val body = bytes.toByteArray

    // publish to exchange, use symbol as routingKey
    val routingKey = quote.symbol
    channel.basicPublish(ticket, exchange, routingKey, null, body)
    println(quote + " sent: routingKey=" + routingKey + " size=" + body.length)
  }
}
