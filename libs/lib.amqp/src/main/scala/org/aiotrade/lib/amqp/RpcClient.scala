package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.ShutdownSignalException
import java.io.EOFException
import java.io.IOException
import scala.actors.Actor

/**
 * Convenience class which manages a temporary reply queue for simple RPC-style communication.
 * The class is agnostic about the format of RPC arguments / return values.
 * It simply provides a mechanism for sending a message to an exchange with a given routing key,
 * and waiting for a response on a reply queue.
 *
 * @param channel the channel to use for communication
 * @param exchange the exchange to connect to
 * @param routingKey the routing key
 * @throws IOException if an error is encountered
 * @see #setupReplyQueue
 */
@throws(classOf[IOException])
class RpcClient(cf: ConnectionFactory, host: String, port: Int, exchange: String, routingKey: String
) extends {
  var replyQueue: String = _ // The name of our private reply queue
} with AMQPDispatcher(cf, host, port, exchange) {
  /** Contains the most recently-used request correlation ID */
  var correlationId = 0

  @throws(classOf[IOException])
  override def configure(channel: Channel): Consumer = {
    replyQueue = setupReplyQueue(channel)
    channel.queueBind(replyQueue, exchange, routingKey)
    
    val consumer = new AMQPConsumer(channel)
    channel.basicConsume(replyQueue, true, consumer)
    consumer
  }

  /**
   * Creates a server-named exclusive autodelete queue to use for
   * receiving replies to RPC requests.
   * @throws IOException if an error is encountered
   * @return the name of the reply queue
   */
  @throws(classOf[IOException])
  private def setupReplyQueue(channel: Channel): String = {
    channel.queueDeclare("", false, false, true, true, null).getQueue
  }

  /**
   * Private API - ensures the RpcClient is correctly open.
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  protected def checkConsumer {
    if (consumer == null) {
      throw new EOFException("RpcClient is closed")
    }
  }

  /* @throws(classOf[IOException])
   def publish(props: AMQP.BasicProperties, message: Array[Byte]) {
   channel.basicPublish(exchange, routingKey, props, message)
   } */

  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def rpcCall($props: AMQP.BasicProperties, content: Any): Any = {
    checkConsumer
    val props = if ($props == null) {
      new AMQP.BasicProperties
    } else $props

    correlationId += 1
    val replyId = correlationId.toString
    props.correlationId = replyId
    props.replyTo = replyQueue

    publish(content, routingKey: String, props: AMQP.BasicProperties)
  }

  /**
   * Perform a simple byte-array-based RPC roundtrip.
   * @param message the byte array request message to send
   * @return the byte array response received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def rpcCall(content: Any): Any = {
    rpcCall(null, content)
  }


  abstract class Processor extends Actor {
    RpcClient.this ! AMQPAddListener(this)

    protected def process(msg: AMQPMessage)

    def act = loop {
      react {
        case msg: AMQPMessage => process(msg)
        case AMQPStop => exit
      }
    }
  }
}

