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
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.ShutdownSignalException
import com.rabbitmq.utility.BlockingCell
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.ObjectOutputStream
import org.aiotrade.lib.amqp.impl.MethodArgumentReader
import org.aiotrade.lib.amqp.impl.MethodArgumentWriter



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
class RpcClientOrig(channel: Channel, exchange: String, routingKey: String) {
  /** Map from request correlation ID to continuation BlockingCell */
  val continuationMap: java.util.Map[String, BlockingCell[Object]] = new java.util.HashMap[String, BlockingCell[Object]]
  /** Contains the most recently-used request correlation ID */
  var correlationId = 0

  /** The name of our private reply queue */
  val replyQueue = setupReplyQueue
  /** Consumer attached to our reply queue */
  var consumer: Consumer = setupConsumer

  /**
   * Private API - ensures the RpcClient is correctly open.
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  def checkConsumer {
    if (consumer == null) {
      throw new EOFException("RpcClient is closed")
    }
  }

  /**
   * Public API - cancels the consumer, thus deleting the temporary queue, and marks the RpcClient as closed.
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  def close {
    if (consumer != null) {
      channel.basicCancel(consumer.asInstanceOf[DefaultConsumer].getConsumerTag)
      consumer = null
    }
  }

  /**
   * Creates a server-named exclusive autodelete queue to use for
   * receiving replies to RPC requests.
   * @throws IOException if an error is encountered
   * @return the name of the reply queue
   */
  @throws(classOf[IOException])
  private def setupReplyQueue: String = {
    channel.queueDeclare.getQueue
  }

  /**
   * Registers a consumer on the reply queue.
   * @throws IOException if an error is encountered
   * @return the newly created and registered consumer
   */
  @throws(classOf[IOException])
  private def setupConsumer: DefaultConsumer = {
    val consumerx = new DefaultConsumer(channel) {
      override def handleShutdownSignal(consumerTag: String, signal: ShutdownSignalException ) {
        continuationMap synchronized {
          val itr = continuationMap.entrySet.iterator
          while (itr.hasNext) {
            itr.next.getValue.set(signal)
          }
          consumer = null
        }
      }

      @throws(classOf[IOException])
      override def handleDelivery(consumerTag: String, env: Envelope, prop: AMQP.BasicProperties, body: Array[Byte]) {
        continuationMap synchronized  {
          val replyId = prop.getCorrelationId
          val blocker = continuationMap.get(replyId)
          continuationMap.remove(replyId)
          blocker.set(body)
        }
      }
    }
    channel.basicConsume(replyQueue, true, consumerx)
    
    consumerx
  }

  @throws(classOf[IOException])
  def publish(props: AMQP.BasicProperties, message: Array[Byte]) {
    channel.basicPublish(exchange, routingKey, props, message)
  }

  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def primitiveCall(props: AMQP.BasicProperties, message: Any): Any = {
    // serialize
    val bytes = new ByteArrayOutputStream
    val store = new ObjectOutputStream(bytes)
    store.writeObject(message)
    store.close

    val body = bytes.toByteArray
    primitiveCall(props, body)
  }

  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def primitiveCall($props: AMQP.BasicProperties, message: Array[Byte]): Array[Byte] = {
    checkConsumer
    val props = if ($props == null) {
      new AMQP.BasicProperties(null, null, null, null,
                               null, null,
                               null, null, null, null,
                               null, null, null, null)
    } else $props
    val k = new BlockingCell[Object]
    continuationMap synchronized {
      correlationId += 1
      val replyId = correlationId.toString
      props.setCorrelationId(replyId)
      props.setReplyTo(replyQueue)
      
      continuationMap.put(replyId, k)
    }
    publish(props, message)
    k.uninterruptibleGet match {
      case sig: ShutdownSignalException =>
        val wrapper = new ShutdownSignalException(sig.isHardError,
                                                  sig.isInitiatedByApplication,
                                                  sig.getReason,
                                                  sig.getReference)
        wrapper.initCause(sig)
        throw wrapper
      case reply: Array[Byte] => reply
    }
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
  def primitiveCall(message: Array[Byte]): Array[Byte] = {
    primitiveCall(null, message)
  }

  /**
   * Perform a simple string-based RPC roundtrip.
   * @param message the string request message to send
   * @return the string response received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def stringCall(message: String): String = {
    new String(primitiveCall(message.getBytes))
  }

  /**
   * Perform an AMQP wire-protocol-table based RPC roundtrip <br><br>
   *
   * There are some restrictions on the values appearing in the table: <br>
   * they must be of type {@link String}, {@link com.rabbitmq.client.impl.LongString}, {@link Integer}, {@link java.math.BigDecimal}, {@link Date},
   * or (recursively) a {@link Map} of the enclosing type.
   *
   * @param message the table to send
   * @return the table received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def mapCall(message: Map[String, _]): Map[String, _] = {
    val buffer = new ByteArrayOutputStream
    val writer = new MethodArgumentWriter(new DataOutputStream(buffer))
    writer.writeTable(message)
    writer.flush
    val reply = primitiveCall(buffer.toByteArray)
    val reader = new MethodArgumentReader(new DataInputStream(new ByteArrayInputStream(reply)))
    reader.readTable
  }

  /**
   * Perform an AMQP wire-protocol-table based RPC roundtrip, first
   * constructing the table from an array of alternating keys (in
   * even-numbered elements, starting at zero) and values (in
   * odd-numbered elements, starting at one) <br>
   * Restrictions on value arguments apply as in {@link RpcClient#mapCall(Map)}.
   *
   * @param keyValuePairs alternating {key, value, key, value, ...} data to send
   * @return the table received
   * @throws ShutdownSignalException if the connection dies during our wait
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  @throws(classOf[ShutdownSignalException])
  def mapCall(keyValuePairs: Array[_]): Map[String, _] = {
    var message = Map[String, Any]()
    var i = 0
    while (i < keyValuePairs.length) {
      message += (keyValuePairs(i).asInstanceOf[String] -> keyValuePairs(i + 1))
      i += 1
    }
    mapCall(message)
  }

}

