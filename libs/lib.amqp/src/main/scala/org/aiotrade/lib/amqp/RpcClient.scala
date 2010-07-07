package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.ShutdownSignalException
import java.io.EOFException
import java.io.IOException
import scala.actors.Actor
import scala.collection.mutable.HashMap
import scala.concurrent.SyncVar

case object RpcTimeOut

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
class RpcClient(factory: ConnectionFactory, host: String, port: Int, reqExchange: String, reqRoutingKey: String
) extends {
  var replyQueue: String = _ // The name of our private reply queue
} with AMQPDispatcher(factory, host, port, reqExchange) {

  /** Map from request correlation ID to continuation BlockingCell */
  val continuationMap = new HashMap[String, SyncVar[Any]]
  /** Contains the most recently-used request correlation ID */
  var correlationId = 0L

  @throws(classOf[IOException])
  override def start: this.type = {
    super.start
    new SyncProcessor
    this
  }

  @throws(classOf[IOException])
  override def configure(channel: Channel): Option[Consumer] = {
    replyQueue = setupReplyQueue(channel)
    val consumer = new AMQPConsumer(channel)
    channel.basicConsume(replyQueue, true, consumer)
    Some(consumer)
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
    if (consumer == None) {
      throw new EOFException("RpcClient is closed")
    }
  }

  /**
   * Perform a simple byte-array-based RPC roundtrip.
   * @param req the rpc request message to send
   * @param props for request message, default null
   * @param timeout in milliseconds, default infinit (-1)
   * @return the response received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def rpcCall(req: RpcRequest, $props: AMQP.BasicProperties = null, routingKey: String = reqRoutingKey, timeout: Long = -1): Any = {
    checkConsumer
    val props = if ($props == null) new AMQP.BasicProperties else $props

    val syncVar = new SyncVar[Any]
    continuationMap synchronized {
      correlationId += 1
      val replyId = correlationId.toString
      props.correlationId = replyId
      props.replyTo = replyQueue

      continuationMap.put(replyId, syncVar)
    }

    publish(reqExchange, routingKey, props, req)

    val res = if (timeout == -1) {
      syncVar.get
    } else {
      syncVar.get(timeout) getOrElse RpcTimeOut
    }

    res match {
      case sig: ShutdownSignalException =>
        val wrapper = new ShutdownSignalException(sig.isHardError,
                                                  sig.isInitiatedByApplication,
                                                  sig.getReason,
                                                  sig.getReference)
        wrapper.initCause(sig)
        throw wrapper
      case reply: Any => reply
    }
  }

  /**
   * Processor that will automatically added as listener of this AMQPDispatcher
   * and process AMQPMessage via process(msg)
   */
  abstract class Processor extends Actor {
    start
    RpcClient.this ! AMQPAddListener(this)

    protected def process(msg: AMQPMessage)

    def act = loop {
      react {
        case msg: AMQPMessage => process(msg)
        case AMQPStop => exit
      }
    }
  }

  class SyncProcessor extends Processor {

    protected def process(msg: AMQPMessage) {
      continuationMap synchronized  {
        val replyId = msg.props.correlationId
        val syncVar = continuationMap.get(replyId).get
        continuationMap.remove(replyId)
        syncVar.set(msg.content)
      }
    }
  }

}