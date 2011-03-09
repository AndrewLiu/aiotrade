package org.aiotrade.lib.amqp

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import java.util.logging.Logger
import net.lag.configgy.Config
import com.rabbitmq.client.Envelope
import java.io.IOException

/**
 * A simple service to relay all messages from master topic exchange to slave RabbitMQ broker instance.
 * 
 * @author guibin
 */

object AMQPRelay {

  val log = Logger.getLogger(getClass.getName)

  var consumer: RelayConsumer = _
  var publisher: RelayPublisher = _
  var isInitialized = false

  def init(config: Config) {

    val masterHost = config.getString("amqp.relay.master.host", "localhost")
    val masterPort = config.getInt("amqp.relay.master.port", 5672)
    val masterUsername = config.getString("amqp.relay.master.username", "guest")
    val masterPassword = config.getString("amqp.relay.master.password", "guest")
    val masterExchange = config.getString("amqp.relay.master.exchange", "market.internal")
    val masterQueueKey = config.getString("amqp.relay.master.queuekey", "source.file.cndbf")
    val masterPrefetchCount = config.getInt("amqp.relay.master.prefetchCount", 0)

    val slaveHost = config.getString("amqp.relay.slave.host", "localhost")
    val slavePort = config.getInt("amqp.relay.slave.port", 5673)
    val slaveUsername = config.getString("amqp.relay.slave.username", "guest")
    val slavePassword = config.getString("amqp.relay.slave.password", "guest")
    val slaveExchange = config.getString("amqp.relay.slave.exchange", "market.internal")
    val slaveQueueKey = config.getString("amqp.relay.slave.queuekey", "source.file.cndbf")
    val slavePrefetchCount = config.getInt("amqp.relay.slave.prefetchCount", 0)

    val masterFactory = new ConnectionFactory
    masterFactory.setHost(masterHost)
    masterFactory.setPort(masterPort)
    masterFactory.setUsername("guest")
    masterFactory.setPassword("guest")
    masterFactory.setVirtualHost("/")
    masterFactory.setRequestedHeartbeat(0)

    val slaveFactory = new ConnectionFactory
    slaveFactory.setHost(slaveHost)
    slaveFactory.setPort(slavePort)
    slaveFactory.setUsername("guest")
    slaveFactory.setPassword("guest")
    slaveFactory.setVirtualHost("/")
    slaveFactory.setRequestedHeartbeat(0)

    consumer = new RelayConsumer(masterFactory, masterExchange, masterQueueKey, masterPrefetchCount)
    publisher = new RelayPublisher(slaveFactory, slaveExchange, slaveQueueKey, slavePrefetchCount)

    //Be sure the safty exiting.
    Runtime.getRuntime.addShutdownHook(new Thread(){
        override def run {
          log.info("Received shutdown signal, AMQPRelay are shutting down...")
          AMQPRelay.stop
        }
      })

    isInitialized = true
  }

  def start = {
    if(!isInitialized) {
      consumer.connect
      publisher.connect
    } else {
      log.severe("AMQPRelay is not correctly initialized. Please invoke init(config) firstly.")
      System.exit(1)
    }
    
  }

  def stop = {
    consumer.stop
    publisher.stop
    isInitialized = false
    log.info("AMQPRelay shut down successfully.")
  }
  
}

import AMQPRelay._
class RelayConsumer(factory: ConnectionFactory, exchange: String, queueKey: String, prefetchCount: Int = 0) extends AMQPDispatcher(factory, exchange) {

  override def configure(channel: Channel): Option[Consumer] = {
    
    channel.basicQos(prefetchCount)
    channel.exchangeDeclare(exchange, "direct")
    channel.queueDeclare(queueKey, true, false, false, null)
    channel.queueBind(queueKey, exchange, queueKey)

    log.info("Declared master exchange: " + exchange + ", master queueKey: " + queueKey)

    //We want isAutoAck = false, after relaying the message, it will be acknowledged explicitly.
    val consumer =  new AMQPConsumer(channel, false) {
      override def handleAck(isAutoAck: Boolean, channel: Channel, envelope: Envelope) {}
    }
    
    channel.basicConsume(queueKey, consumer.isAutoAck, consumer)
    
    Some(consumer)
  }

  class RelayProcessor extends Processor {
    protected def process(msg: AMQPMessage) {
      log.fine("Received delivery tag: " + msg.envelope.getDeliveryTag)
      //Publish the received msg to RelayPublisher
      publish(msg)
    }
  }

  reactions += {
    case AMQPAcknowledge(deliveryTag) =>
      channel.foreach{ch => ch.basicAck(deliveryTag, false)}
  }
  listenTo(AMQPRelay.publisher)
  
}


class RelayPublisher(factory: ConnectionFactory, exchange: String, queueKey: String, prefetchCount: Int = 0) extends AMQPDispatcher(factory, exchange) {

  private val log = Logger.getLogger(getClass.getName)

  @throws(classOf[IOException])
  def configure(channel: Channel): Option[Consumer] = {

    channel.basicQos(prefetchCount)
    channel.exchangeDeclare(exchange, "direct")
    channel.queueDeclare(queueKey, true, false, false, null)
    channel.queueBind(queueKey, exchange, queueKey)

    log.info("Declared slave exchange: " + exchange + ", master queueKey: " + queueKey)
    
    None
  }

  reactions += {
    case msg: AMQPMessage =>
      //Publish the msg to the slave AMQP
      publish(exchange, queueKey, msg.props, msg.body)
      //Then acknowledge the delivery to master AMQP
      publish(AMQPAcknowledge(msg.envelope.getDeliveryTag))
  }
  listenTo(AMQPRelay.consumer)
  
}
