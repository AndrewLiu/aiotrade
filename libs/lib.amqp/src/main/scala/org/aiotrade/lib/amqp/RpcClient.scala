package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.ShutdownSignalException
import java.io.EOFException
import java.io.IOException
import scala.collection.mutable
import scala.concurrent.SyncVar
import java.util.logging.Logger

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
class RpcClient($factory: ConnectionFactory, $reqExchange: String) extends AMQPDispatcher($factory, $reqExchange) {

  private val log = Logger.getLogger(getClass.getName)

  var replyQueue: String = _ // The name of our private reply queue

  /** Map from request correlation ID to continuation BlockingCell */
  private val continuationMap = mutable.Map[String, SyncVar[Any]]()
  /** Contains the most recently-used request correlation ID */
  private var correlationId = 0L
  /** Should hold strong ref for SyncVarSetterProcessor */
  private val processor = new SyncVarSetterProcessor

  @throws(classOf[IOException])
  def configure(channel: Channel): Option[Consumer] = {
    replyQueue = setupReplyQueue(channel)

    val consumer = new AMQPConsumer(channel, true) {
      override def handleShutdownSignal(consumerTag: String, signal: ShutdownSignalException) {
        continuationMap synchronized {
          for ((_, syncVar) <- continuationMap) {
            syncVar.set(signal.getMessage)
          }
        }
      }
    }

    // autoAck - true  if the server should consider messages acknowledged once delivered;
    //           false if the server should expect explicit acknowledgements
    // When consumer.isAutoAck == true, AMQPConsumer will call channel.basicAck(env.getDeliveryTag, false)
    channel.basicConsume(replyQueue, consumer.isAutoAck, consumer)
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
    val queueName = channel.queueDeclare("", false, true, true, null).getQueue
    log.fine("declared queue " + queueName)
    queueName
  }

  /**
   * Private API - ensures the RpcClient is correctly open.
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  protected def checkConsumer {
    if (consumer.isEmpty) throw new EOFException("Consumer of rpcClient is closed")
  }

  /**
   * Perform a simple byte-array-based RPC roundtrip.
   * @param req the rpc request message to send
   * @param routingKey the rpc routingKey to publish
   * @param props for request message, default null
   * @param timeout in milliseconds, default infinit (-1)
   * @return the response received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def rpcCall(req: Any, routingKey: String, props: AMQP.BasicProperties = new AMQP.BasicProperties, timeout: Long = -1): Any = {
    val syncVar = arpcCall(req, routingKey, props)

    val res = if (timeout == -1) {
      syncVar.get
    } else {
      syncVar.get(timeout) getOrElse RpcTimeout
    }

    res
  }

  /**
   * Perform a async simple byte-array-based RPC roundtrip.
   * @param req the rpc request message to send
   * @param routingKey the rpc routingKey to publish
   * @param props for request message, default null
   * @return a SyncVar that wrapps response received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def arpcCall(req: Any, routingKey: String, props: AMQP.BasicProperties = new AMQP.BasicProperties): SyncVar[Any] = {
    val syncVar = new SyncVar[Any]
    val replyId = continuationMap synchronized {
      correlationId += 1
      val replyIdx = correlationId.toString
      continuationMap.put(replyIdx, syncVar)
      replyIdx
    }

    try {
      checkConsumer
    } catch {
      case ex =>
        log.warning(ex.getMessage)
        syncVar.set(RpcResponse(ex.getMessage))
        return syncVar
    }

    props.setCorrelationId(replyId)
    props.setReplyTo(replyQueue)
    
    publish(exchange, routingKey, props, req)
    
    syncVar
  }

  class SyncVarSetterProcessor extends Processor {
    protected def process(msg: AMQPMessage) {
      msg match {
        case AMQPMessage(res: Any, props) =>
          val replyId = msg.props.getCorrelationId
          val syncVar = continuationMap synchronized {
            continuationMap.remove(replyId).get
          }
          syncVar.set(res)
        case x => log.warning("Wrong msg: " + x)
      }
    }
  }

}