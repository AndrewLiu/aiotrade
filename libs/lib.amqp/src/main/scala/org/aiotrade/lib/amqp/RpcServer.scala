//   The contents of this file are subject to the Mozilla Public License
//   Version 1.1 (the "License"); you may not use this file except in
//   compliance with the License. You may obtain a copy of the License at
//   http://www.mozilla.org/MPL/
//
//   Software distributed under the License is distributed on an "AS IS"
//   basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//   License for the specific language governing rights and limitations
//   under the License.
//
//   The Original Code is RabbitMQ.
//
//   The Initial Developers of the Original Code are LShift Ltd,
//   Cohesive Financial Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created before 22-Nov-2008 00:00:00 GMT by LShift Ltd,
//   Cohesive Financial Technologies LLC, or Rabbit Technologies Ltd
//   are Copyright (C) 2007-2008 LShift Ltd, Cohesive Financial
//   Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created by LShift Ltd are Copyright (C) 2007-2009 LShift
//   Ltd. Portions created by Cohesive Financial Technologies LLC are
//   Copyright (C) 2007-2009 Cohesive Financial Technologies
//   LLC. Portions created by Rabbit Technologies Ltd are Copyright
//   (C) 2007-2009 Rabbit Technologies Ltd.
//
//   All Rights Reserved.
//
//   Contributor(s): ______________________________________.
//

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
    val queue = if ($queue == null || $queue == "") {
      channel.queueDeclare.getQueue
    } else $queue
    
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

