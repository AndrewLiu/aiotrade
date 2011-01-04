package org.aiotrade.lib.amqp

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Subscriber that will declare and consumer a durable=false, exclusive=false, autoDelete=true queue.
 * 
 * @author Caoyuan Deng
 */
class AMQPSubscriber(factory: ConnectionFactory, exchange: String) extends AMQPDispatcher(factory, exchange) {
  private val log = Logger.getLogger(this.getClass.getName)

  private var defaultQueue: Option[String] = None
  private var declaredQueues = Set[String]()
  private var subscribedTopics = Set[String]()

  reactions += {
    case AMQPConnected =>
      // when reconnecting, should resubscribing existed queues and topics:
      log.info("Resubscribing : " + declaredQueues + ", " + subscribedTopics)
      val default = defaultQueue
      declaredQueues foreach {declareQueue(_)}
      defaultQueue = default
      subscribedTopics foreach {subscribeTopic(_)}
  }

  override protected def configure(channel: Channel): Option[Consumer] = {
    // A consumer that set autoAck == true
    val consumer = new AMQPConsumer(channel, true)
    Some(consumer)
  }

  @deprecated("Do not delete old queue, so we can reuse it when reconnected.")
  private def deleteOldQueue {
    for (ch <- channel; q <- declaredQueues) {
      try {
        // check if the queue existed, if existed, will return a declareOk object, otherwise will throw IOException
        val declareOk = ch.queueDeclarePassive(q)
        try {
          // the exception thrown will destroy the connection too, so use it carefully
          ch.queueDelete(q)
          log.info("consumer deleted queue: " + q)
        } catch {
          case e => log.log(Level.WARNING, e.getMessage, e)
        }
      } catch {
        case e => // queue doesn't exist
      }
    }
  }

  def declareQueue(queue: String, isDefault: Boolean = false): Unit = synchronized {
    if (isDefault) {
      defaultQueue = Some(queue)
    } else {
      if (defaultQueue.isEmpty) defaultQueue = Some(queue)
    }

    declaredQueues += queue
    for (ch <- channel; cs <- consumer) {
      ch.exchangeDeclare(exchange, "direct")

      // we need a non-exclusive queue, so when reconnected, this queue can be used by the new created connection.
      // string queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
      ch.queueDeclare(queue, false, false, true, null)
      ch.basicConsume(queue, cs.asInstanceOf[AMQPConsumer].isAutoAck, cs)
      log.info("Declared and consumer queue: " + queue)
    }
  }

  def subscribeTopic(topic: String, queue: String = null): Unit = synchronized {
    assert(!declaredQueues.isEmpty, "At least one queue should be declared before subscribe topic.")
    subscribedTopics += topic
    for (ch <- channel) {
      val q = if (queue == null) defaultQueue.getOrElse(null) else queue
      if (q != null) {
        ch.queueBind(q, exchange, topic)
      }
    }
  }

  def unsubscribeTopic(topic: String, queue: String = null): Unit = synchronized {
    subscribedTopics -= topic
    for (ch <- channel) {
      val q = if (queue == null) defaultQueue.getOrElse(null) else queue
      if (q != null) {
        ch.queueUnbind(q, exchange, topic)
      }
    }
  }
}



