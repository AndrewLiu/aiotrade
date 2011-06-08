package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

object RpcServer {
  private val log = Logger.getLogger(this.getClass.getName)

  def declareServer(factory: ConnectionFactory, exchange: String, requestQueues: Seq[String]) {
    try {
      val conn = factory.newConnection
      val channel = conn.createChannel
      
      if (exchange != AMQPExchange.defaultDirect) channel.exchangeDeclare(exchange, "direct")

      declareQueue(channel, exchange, requestQueues)

      conn.close
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }

  /**
   * @Note queue should always be declared with same props in object RpcServer and in class RpcServer
   */
  private def declareQueue(channel: Channel, exchange: String, requestQueues: Seq[String]) {
    for (requestQueue <- requestQueues) {
      // durable = false, exclusive = false, autoDelete = true
      channel.queueDeclare(requestQueue, false, false, true, null)

      // use routingKey identical to queue name
      val routingKey = requestQueue
      channel.queueBind(requestQueue, exchange, routingKey)
    }
  }
}

/**
 * Class which manages a request queue for a simple RPC-style service.
 * The class is agnostic about the format of RPC arguments / return values.
 * @param Channel we are communicating on
 * @param Queue to receive requests from
 */
class RpcServer($factory: ConnectionFactory, $exchange: String, val requestQueue: String
) extends AMQPDispatcher($factory, $exchange) {
  assert(requestQueue != null && requestQueue != "", "We need explicitly named requestQueue")

  /**
   * Creates an RpcServer listening on a temporary exclusive
   * autodelete queue.
   */
  @throws(classOf[IOException])
  def this(factory: ConnectionFactory) = this(factory, "", null)

  @throws(classOf[IOException])
  override def configure(channel: Channel): Option[Consumer] = {
    // Set prefetchCount to 1, so the requestQueue can be shared and balanced by lots of rpc servers on 1 message each time behavior
    channel.basicQos(1)

    if (exchange != AMQPExchange.defaultDirect) channel.exchangeDeclare(exchange, "direct")

    RpcServer.declareQueue(channel, exchange, List(requestQueue))

    val consumer = new AMQPConsumer(channel, false)
    channel.basicConsume(requestQueue, consumer.isAutoAck, consumer)
    Some(consumer)
  }

  /**
   * Processor that will automatically added as listener of this AMQPDispatcher
   * and process AMQPMessage and reply to client via process(msg).
   */
  abstract class Handler extends Processor {

    /**
     * @return AMQPMessage that will be send back to caller
     */
    protected def process(msg: AMQPMessage) {
      msg match {
        case AMQPMessage(req: Any, reqProps)  =>
          if (reqProps.getCorrelationId != null && reqProps.getReplyTo != null) {
            val reply = handle(req)
            // If replyPropreties is set to replyContent then we use it, otherwise create a new one
            val replyProps = if (reply.props != null) reply.props else new AMQP.BasicProperties
            
            replyProps.setCorrelationId(reqProps.getCorrelationId)
            publish(exchange, reqProps.getReplyTo, replyProps, reply.body)
          }
      }
    }
    
    /**
     * @return AMQPMessage that will be send back to caller
     */
    protected def handle(req: Any): AMQPMessage
  }
}