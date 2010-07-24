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
import com.rabbitmq.client.QueueingConsumer
import com.rabbitmq.client.ShutdownSignalException
import java.io.IOException
import scala.annotation.tailrec


/**
 * Class which manages a request queue for a simple RPC-style service.
 * The class is agnostic about the format of RPC arguments / return values.
 * @param Channel we are communicating on
 * @param Queue to receive requests from
 */
class RpcServerOrig(val channel: Channel, $queue: String) {
  /** Boolean controlling the exit from the mainloop. */
  protected var mainloopRunning = true

  val queue = if ($queue == null || $queue == "") {
    channel.queueDeclare.getQueue
  } else $queue

  /** Consumer attached to our request queue */
  protected var consumer: QueueingConsumer = setupConsumer

  /**
   * Creates an RpcServer listening on a temporary exclusive
   * autodelete queue.
   */
  @throws(classOf[IOException])
  def this(channel: Channel) = this(channel, null)


  /**
   * Public API - cancels the consumer, thus deleting the queue, if
   * it was a temporary queue, and marks the RpcServer as closed.
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  def close {
    if (consumer != null) {
      channel.basicCancel(consumer.getConsumerTag)
      consumer = null
    }
    terminateMainloop
  }

  /**
   * Registers a consumer on the reply queue.
   * @throws IOException if an error is encountered
   * @return the newly created and registered consumer
   */
  @throws(classOf[IOException])
  protected def setupConsumer: QueueingConsumer = {
    val consumerx = new QueueingConsumer(channel)
    channel.basicConsume(queue, consumerx)
    consumerx
  }

  /**
   * Public API - main server loop. Call this to begin processing
   * requests. Request processing will continue until the Channel
   * (or its underlying Connection) is shut down, or until
   * terminateMainloop) is called.
   *
   * Note that if the mainloop is blocked waiting for a request, the
   * termination flag is not checked until a request is received, so
   * a good time to call terminateMainloop() is during a request
   * handler.
   *
   * @return the exception that signalled the Channel shutdown, or null for orderly shutdown
   */
  @throws(classOf[IOException])
  def mainloop: ShutdownSignalException = {
    @tailrec
    def loop: ShutdownSignalException = {
      if (!mainloopRunning) return null
      var request: QueueingConsumer.Delivery = null
      try {
        request = consumer.nextDelivery
      } catch {case ex: InterruptedException => loop}
      processRequest(request)
      channel.basicAck(request.getEnvelope.getDeliveryTag, false)
      loop
    }
    
    try {
      loop
    } catch {case ex: ShutdownSignalException => return ex}
  }

  /**
   * Call this method to terminate the mainloop.
   *
   * Note that if the mainloop is blocked waiting for a request, the
   * termination flag is not checked until a request is received, so
   * a good time to call terminateMainloop() is during a request
   * handler.
   */
  def terminateMainloop {
    mainloopRunning = false
  }

  /**
   * Private API - Process a single request. Called from mainloop().
   */
  @throws(classOf[IOException])
  def processRequest(request: QueueingConsumer.Delivery) {
    val requestProps = request.getProperties
    if (requestProps.getCorrelationId != null && requestProps.getReplyTo != null) {
      val replyProps = new AMQP.BasicProperties
      val replyBody = handleCall(request, replyProps)
      replyProps.setCorrelationId(requestProps.getCorrelationId)
      channel.basicPublish("", requestProps.getReplyTo,  replyProps, replyBody)
    } else {
      handleCast(request)
    }
  }

  /**
   * Lowest-level response method. Calls
   * handleCall(AMQP.BasicProperties,byte[],AMQP.BasicProperties).
   */
  def handleCall(request: QueueingConsumer.Delivery, replyProps: AMQP.BasicProperties): Array[Byte] = {
    handleCall(request.getProperties, request.getBody, replyProps)
  }

  /**
   * Mid-level response method. Calls
   * handleCall(byte[],AMQP.BasicProperties).
   */
  def handleCall(requestProps: AMQP.BasicProperties, requestBody: Array[Byte], replyProps: AMQP.BasicProperties): Array[Byte] = {
    handleCall(requestBody, replyProps)
  }

  /**
   * High-level response method. Returns an empty response by
   * default - override this (or other handleCall and handleCast
   * methods) in subclasses.
   */
  def handleCall(requestBody: Array[Byte], replyProps: AMQP.BasicProperties): Array[Byte] = {
    Array[Byte](0)
  }

  /**
   * Lowest-level handler method. Calls
   * handleCast(AMQP.BasicProperties,byte[]).
   */
  def handleCast(request: QueueingConsumer.Delivery) {
    handleCast(request.getProperties, request.getBody)
  }

  /**
   * Mid-level handler method. Calls
   * handleCast(byte[]).
   */
  def handleCast(requestProps: AMQP.BasicProperties, requestBody: Array[Byte]) {
    handleCast(requestBody)
  }

  /**
   * High-level handler method. Does nothing by default - override
   * this (or other handleCast and handleCast methods) in
   * subclasses.
   */
  def handleCast(requestBody: Array[Byte]) {
    // Does nothing.
  }

}

