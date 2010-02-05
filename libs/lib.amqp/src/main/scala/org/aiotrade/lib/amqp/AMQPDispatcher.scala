package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.DefaultConsumer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Logger
import java.util.logging.Level
import scala.actors.Actor

/**
 * Option 1:
 * create one queue per consumer with several bindings, one for each stock.
 * Prices in this case will be sent with a topic routing key.
 *
 * Option 2:
 * Another option is to create one queue per stock. each consumer will be
 * subscribed to several queues. Messages will be sent with a direct routing key.
 *
 * Best Practice:
 * Option 1: should work fine, except there is no need to use a topic exchange.
 * Just use a direct exchange, one queue per user and for each of the stock
 * symbols a user is interested in create a binding between the user's queue
 * and the direct exchange.
 *
 * Option 2: each quote would only go to one consumer, which is probably not
 * what you want. In an AMQP system, to get the same message delivered to N
 * consumers you need (at least) N queues. Exchanges *copy* messages to queues,
 * whereas queues *round-robin* message delivery to consumers.
 */

/**
 * @param content A deserialized value received via AMQP.
 * @param props
 *
 * Messages received from AMQP are wrapped in this case class. When you
 * register a listener, this is the case class that you will be matching on.
 */
case class AMQPMessage(content: Any, props: AMQP.BasicProperties)

case class AMQPPublish(content: Any, routingKey: String, props: AMQP.BasicProperties)

/**
 * @param a The actor to add as a Listener to this Dispatcher.
 */
case class AMQPAddListener(a: Actor)

/**
 * Reconnect to the AMQP Server after a delay of {@code delay} milliseconds.
 */
case class AMQPReconnect(delay: Long)

case object AMQPStop

/**
 * The dispatcher that listens over the AMQP message endpoint.
 * It manages a list of subscribers to the trade message and also sends AMQP
 * messages coming in to the queue/exchange to the list of observers.
 */
abstract class AMQPDispatcher(cf: ConnectionFactory, host: String, port: Int, val exchange: String) extends Actor {
  case class State(conn: Connection, channel: Channel, queue: String, consumer: Consumer)

  private val log = Logger.getLogger(this.getClass.getName)

  private lazy val reconnectTimer = new Timer("AMQPReconnectTimer")
  private var as: List[Actor] = Nil

  private var state = connect

  private def conn = state.conn
  def channel = state.channel
  def queue = state.queue
  def consumer = state.consumer

  private def connect: State = {
    val conn = cf.newConnection(host, port)
    val channel = conn.createChannel
    val (queue, consumer) = configure(channel)
    State(conn, channel, queue, consumer)
  }

  /**
   * Registers queue and consumer.
   * @throws IOException if an error is encountered
   * @return the newly created and registered (queue, consumer)
   */
  @throws(classOf[IOException])
  protected def configure(channel: Channel): (String, Consumer)

  def act {
    Actor.loop {
      react {
        case msg: AMQPMessage =>
          as foreach (_ ! msg)
        case AMQPAddListener(a) =>
          as ::= a
        case AMQPPublish(content, routingKey, props) => publish(content, routingKey, props)
        case AMQPReconnect(delay) => reconnect(delay)
        case AMQPStop => disconnect; exit
      }
    }
  }

  protected def publish(content: Any, routingKey: String, props: AMQP.BasicProperties) {
    val bytes = new ByteArrayOutputStream
    val store = new ObjectOutputStream(bytes)
    store.writeObject(content)
    store.close

    val body = bytes.toByteArray

    channel.basicPublish(exchange, routingKey, props, body)
    //println(content + " sent: routingKey=" + routingKey + " size=" + body.length)
  }

  protected def disconnect = {
    if (consumer != null) {
      channel.basicCancel(consumer.asInstanceOf[DefaultConsumer].getConsumerTag)
    }
    try {
      channel.close
    } catch {
      case e: IOException => log.log(Level.INFO, "Could not close AMQP channel %s:%s [%s]", Array(host, port, this))
      case _ => ()
    }
    try {
      conn.close
      log.log(Level.FINEST, "Disconnected AMQP connection at %s:%s [%s]", Array(host, port, this))
    } catch {
      case e: IOException => log.log(Level.WARNING, "Could not close AMQP connection %s:%s [%s]", Array(host, port, this))
      case _ => ()
    }
  }

  protected def reconnect(delay: Long) = {
    disconnect
    try {
      connect
      log.log(Level.FINEST, "Successfully reconnected to AMQP Server %s:%s [%s]", Array(host, port, this))
    } catch {
      case e: Exception =>
        val waitInMillis = delay * 2
        val self = this
        log.log(Level.FINEST, "Trying to reconnect to AMQP server in %n milliseconds [%s]", Array(waitInMillis, this))
        reconnectTimer.schedule(new TimerTask {
            def run {
              self ! AMQPReconnect(waitInMillis)
            }
          }, delay)
    }
  }

}




