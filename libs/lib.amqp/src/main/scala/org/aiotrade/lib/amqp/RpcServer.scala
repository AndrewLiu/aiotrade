package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import java.io.IOException
import scala.actors.Actor


/**
 * Class which manages a request queue for a simple RPC-style service.
 * The class is agnostic about the format of RPC arguments / return values.
 * @param Channel we are communicating on
 * @param Queue to receive requests from
 */
class RpcServer(factory: ConnectionFactory, host: String, port: Int, exchange: String, reqQueue: String
) extends AMQPDispatcher(factory, host, port, exchange) {

  /**
   * Creates an RpcServer listening on a temporary exclusive
   * autodelete queue.
   */
  @throws(classOf[IOException])
  def this(factory: ConnectionFactory, host: String, port: Int) = {
    this(factory, host, port, "", null)
  }

  @throws(classOf[IOException])
  override def configure(channel: Channel): Option[Consumer] = {
    if (exchange != AMQPExchange.defaultDirect) channel.exchangeDeclare(exchange, "direct")
    val queue = reqQueue match {
      case null | "" => channel.queueDeclare.getQueue
      case _ => channel.queueDeclare(reqQueue); reqQueue
    }
    // use routingKey identical to queue name
    val routingKey = queue

    channel.queueBind(queue, exchange, routingKey)

    val consumer = new AMQPConsumer(channel)
    channel.basicConsume(queue, consumer)
    Some(consumer)
  }

  /**
   * Processor that will automatically added as listener of this AMQPDispatcher
   * and process AMQPMessage and reply to client via process(msg).
   */
  abstract class Processor extends Actor {
    start
    RpcServer.this ! AMQPAddListener(this)

    /**
     *
     * @return AMQPMessage that will be send back to caller
     */
    protected def process(msg: AMQPMessage): AMQPMessage

    def act = loop {
      react {
        case msg: AMQPMessage =>
          val reqProps = msg.props
          if (reqProps.correlationId != null && reqProps.replyTo != null) {
            val (replyContent, replyProps) = process(msg) match {
              case AMQPMessage(replyContentx, null) => (replyContentx, new AMQP.BasicProperties)
              case AMQPMessage(replyContentx, replyPropsx) => (replyContentx, replyPropsx)
            }
              
            replyProps.correlationId = reqProps.correlationId
            publish("", reqProps.replyTo, replyProps, replyContent)
          }
        case AMQPStop => exit
      }
    }
    
  }
}

