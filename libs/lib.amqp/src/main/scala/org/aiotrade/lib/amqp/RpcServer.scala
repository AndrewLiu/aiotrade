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
class RpcServer(cf: ConnectionFactory, host: String, port: Int, exchange: String, $queue: String
) extends AMQPDispatcher(cf, host, port, exchange) {

  /**
   * Creates an RpcServer listening on a temporary exclusive
   * autodelete queue.
   */
  @throws(classOf[IOException])
  def this(cf: ConnectionFactory, host: String, port: Int, exchange: String) = {
    this(cf, host, port, exchange, null)
  }

  @throws(classOf[IOException])
  override def configure(channel: Channel): Consumer = {
    val queue = $queue match {
      case null | "" => channel.queueDeclare.getQueue
      case _ =>$queue
    }
    
    val consumer = new AMQPConsumer(channel)
    channel.basicConsume(queue, consumer)
    consumer
  }

  abstract class Processor extends Actor {
    RpcServer.this ! AMQPAddListener(this)

    /**
     *
     * @return content that will be send back to caller
     */
    protected def process(msg: AMQPMessage): Any

    def act {
      Actor.loop {
        react {
          case msg: AMQPMessage =>
            val requestProps = msg.props
            if (requestProps.correlationId != null && requestProps.replyTo != null) {
              val replyProps = new AMQP.BasicProperties
              val replyContent = process(msg)
              replyProps.correlationId = requestProps.correlationId
              publish("", replyContent, requestProps.replyTo, replyProps)
            }
          case AMQPStop => exit
        }
      }
    }
  }
}

