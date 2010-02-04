package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.io.IOException
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
 * @param message A deserialized value received via AMQP.
 * @param routingKey
 *
 * Messages received from AMQP are wrapped in this case class. When you
 * register a listener, this is the case class that you will be matching on.
 */
case class AMQPMessage[T](message: T, props: AMQP.BasicProperties)

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
  private val log = Logger.getLogger(this.getClass.getName)

  private lazy val reconnectTimer = new Timer("AMQPReconnectTimer")
  private var as: List[Actor] = Nil

  var conn: Connection = _
  var channel: Channel = _

  connect

  private def connect {
    conn = cf.newConnection(host, port)
    channel = conn.createChannel
    configure(channel)
  }

  /**
   * Override this to configure the Channel and Consumer.
   */
  def configure(channel: Channel)

  def act {
    Actor.loop {
      react {
        case msg: AMQPMessage[_] =>
          as foreach (_ ! msg)
        case AMQPAddListener(a) =>
          as ::= a
        case AMQPReconnect(delay) => reconnect(delay)
        case AMQPStop => disconnect; exit
      }
    }
  }

  protected def disconnect = {
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




