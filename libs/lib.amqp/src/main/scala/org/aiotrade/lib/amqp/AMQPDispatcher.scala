package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.util.Timer
import java.util.TimerTask
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
abstract class AMQPDispatcher(cf: ConnectionFactory, host: String, port: Int) extends Actor {

  private val reconnectTimer = new Timer("AMQPReconnectTimer")
  private var as: List[Actor] = Nil

  var (conn, channel) = connect

  private def connect: (Connection, Channel) = {
    val conn = cf.newConnection(host, port)
    val channel = conn.createChannel
    configure(channel)
    (conn, channel)
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
        case AMQPReconnect(delay: Long) =>
          try {
            val (connx, channelx) = connect
            conn = connx
            channel = channelx
            println("AMQPDispatcher: Successfully reconnected to AMQP Server")
          } catch {
            // Attempts to reconnect again using geometric back-off.
            case e: Exception => {
                val amqp = this
                println("AMQPDispatcher: Will attempt reconnect again in " + (delay * 2) + "ms.")
                reconnectTimer.schedule(new TimerTask {
                    def run {
                      amqp ! AMQPReconnect(delay * 2)
                    }}, delay)
              }
          }
        case AMQPStop => exit
      }
    }
  }

}




