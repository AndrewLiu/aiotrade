package org.aiotrade.lib.amqp

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.util.Timer
import java.util.TimerTask
import scala.actors.Actor

object Constants {
  val exchange = "market.quote"
  val symbols = Array("600001", "600002", "600003")
}

/**
 * Reconnect to the AMQP Server after a delay of {@code delay} milliseconds.
 */
case class AMQPReconnect(delay: Long)

/**
 * The dispatcher that listens over the AMQP message endpoint.
 * It manages a list of subscribers to the trade message and also sends AMQP
 * messages coming in to the queue/exchange to the list of observers.
 */
abstract class AMQPDispatcher(cf: ConnectionFactory, host: String, port: Int) extends Actor {

  connect

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

  private val reconnectTimer = new Timer("AMQPReconnectTimer")

  def act {
    Actor.loop {
      react {
        case AMQPReconnect(delay: Long) =>
          try {
            val (conn, channel) = connect
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
      }
    }
  }

}

import Constants._
class TopicAMQPDispatcher(factory: ConnectionFactory, host: String, port: Int) extends AMQPDispatcher(factory, host, port) {
  val tpe = "topic"
  override def configure(channel: Channel) {
    val ticket = channel.accessRequest("/data")
    channel.exchangeDeclare(ticket, exchange, tpe)
  }
}

class DirectAMQPDispatcher(factory: ConnectionFactory, host: String, port: Int) extends AMQPDispatcher(factory, host, port) {
  val tpe = "direct"
  override def configure(channel: Channel) {
    val ticket = channel.accessRequest("/data")
    channel.exchangeDeclare(ticket, exchange, tpe)
    channel.queueDeclare(ticket, "mult_queue")
    channel.queueBind(ticket, "mult_queue", exchange, "routeroute")
  }
}

