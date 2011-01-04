package org.aiotrade.lib.amqp

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import java.util.logging.Level
import java.util.logging.Logger

/**
 *
 * @author Caoyuan Deng
 */
class AMQPSubscriber(factory: ConnectionFactory, exchange: String, isAutoAck: Boolean = true) extends AMQPDispatcher(factory, exchange) {
  private val log = Logger.getLogger(this.getClass.getName)

  private var _defaultQueue: Option[String] = None
  private var _consumingQueues = Set[String]()
  private var _subscribedTopics = Set[String]()

  reactions += {
    case AMQPConnected =>
      // When reconnecting, should resubscribing existed queues and topics:
      log.info("(Re)subscribing : " + _consumingQueues + ", " + _subscribedTopics)
      val default = _defaultQueue
      _consumingQueues foreach {consumeQueue(_)}
      _defaultQueue = default
      _subscribedTopics foreach {subscribeTopic(_)}
  }

  def defaultQueue = synchronized {_defaultQueue}
  def consumingQueues = synchronized {_consumingQueues}
  def subscribedTopics = synchronized {_subscribedTopics}

  override protected def configure(channel: Channel): Option[Consumer] = {
    Some(new AMQPConsumer(channel, isAutoAck))
  }

  /**
   * Consumer queue. If this queue does not exist yet, also delcare it here.
   */
  def consumeQueue(queue: String, durable: Boolean = false, exclusive: Boolean = false, autoDelete: Boolean = true,
                   isDefault: Boolean = false
  ): Unit = synchronized {
    assert(isConnected, "Should connect before delareQueue.")

    if (isDefault) {
      _defaultQueue = Some(queue)
    } else {
      _defaultQueue = Some(_defaultQueue.getOrElse(queue))
    }

    _consumingQueues += queue
    for (ch <- channel; cs <- consumer) {
      ch.exchangeDeclare(exchange, "direct")

      // We need a non-exclusive queue, so when reconnected, this queue can be used by the new created connection.
      // string queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
      try {
        ch.queueDeclare(queue, durable, exclusive, autoDelete, null)
      } catch {
        case ex => log.log(Level.SEVERE, ex.getMessage, ex)
      }
      
      ch.basicConsume(queue, cs.asInstanceOf[AMQPConsumer].isAutoAck, cs)
      log.info("Declared and will consumer queue: " + queue + ", consumingQueues=" + _consumingQueues + ", defaultQueue=" + _defaultQueue)
    }
  }

  def subscribeTopic(topic: String, queue: String = null): Unit = synchronized {
    assert(isConnected, "Should connect before subscribeTopic.")
    assert(_consumingQueues.nonEmpty, "At least one queue should be declared before subscribe topic.")
    
    _subscribedTopics += topic
    for (ch <- channel) {
      val q = Option(queue).getOrElse(_defaultQueue.getOrElse(null))
      if (q != null) {
        ch.queueBind(q, exchange, topic)
        log.info("Subscribed topic: " + topic + "-->" + q)
      }
    }
  }

  def unsubscribeTopic(topic: String, queue: String = null): Unit = synchronized {
    if (!isConnected) return
    
    _subscribedTopics -= topic
    for (ch <- channel) {
      val q = Option(queue).getOrElse(_defaultQueue.getOrElse(null))
      if (q != null) {
        ch.queueUnbind(q, exchange, topic)
      }
    }
  }

}



