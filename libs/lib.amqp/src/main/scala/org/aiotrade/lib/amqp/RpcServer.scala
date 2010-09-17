package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import java.io.IOException

case class RpcRequest(args: Any*)
case class RpcResponse(req: RpcRequest, result: Any)

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
    if (exchange != AMQPExchange.defaultDirect) channel.exchangeDeclare(exchange, "direct")

    // durable = false, exclusive = false, autoDelete = true
    channel.queueDeclare(requestQueue, false, false, true, null)

    // use routingKey identical to queue name
    val routingKey = requestQueue
    channel.queueBind(requestQueue, exchange, routingKey)

    val consumer = new AMQPConsumer(channel)
    channel.basicConsume(requestQueue, consumer)
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
        case AMQPMessage(req: RpcRequest, reqProps) =>
          if (reqProps.getCorrelationId != null && reqProps.getReplyTo != null) {
            val replyContent = handle(req)
            val replyProps = new AMQP.BasicProperties
            
            replyProps.setCorrelationId(reqProps.getCorrelationId)
            publish("", reqProps.getReplyTo, replyProps, replyContent)
          }
      }
    }
    
    /**
     * @return AMQPMessage that will be send back to caller
     */
    protected def handle(req: RpcRequest): RpcResponse
  }
}