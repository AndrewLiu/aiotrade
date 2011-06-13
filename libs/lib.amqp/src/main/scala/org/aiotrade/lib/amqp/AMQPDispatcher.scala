package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.ShutdownListener
import com.rabbitmq.client.ShutdownSignalException
import java.io.IOException
import java.io.InvalidClassException
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Logger
import java.util.logging.Level
/**
 * @Note If we use plain sync Publisher/Reactor instead of actor based async model, it will because:
 * 1. It seems that when actor model is mixed with a hard coded thread (AMQPConnection has a MainLoop thread),
 *    the scheduler of actor may deley delivery message, that causes unacceptable latency for amqp messages
 * 2. Unlick indicator, tser etc, we do not need async, parallel scale for amcp clients
 */
import org.aiotrade.lib.util.actors.Msg
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.actors.Reactor
import org.aiotrade.lib.util.reactors.Event
import org.aiotrade.lib.amqp.datatype.ContentType

/*_ rabbitmqctl common usages:
 sudo rabbitmq-server -n rabbit@localhost &
 sudo rabbitmqctl -n rabbit@localhost stop

 sudo rabbitmqctl -n rabbit@localhost stop_app
 sudo rabbitmqctl -n rabbit@localhost reset
 sudo rabbitmqctl -n rabbit@localhost start_app
 sudo rabbitmqctl -n rabbit@localhost list_queues name messages messages_uncommitted messages_unacknowledged

 If encountered troubes when start the server up, since the tables in the mnesia
 database backing rabbitmq are locked (Don't know why this is the case). you can
 get this running again brute force styleee by deleting the database:

 sudo rm -rf /opt/local/var/lib/rabbitmq/mnesia/
 */

/*_
 * Option 1:
 * create one queue per consumer with several bindings, one for each stock.
 * Prices in this case will be sent with a topic routing key.
 *
 * Option 2:
 * Another option is to create one queue per stock. each consumer will be
 * subscribed to several queues. Messages will be sent with a direct routing key.
 *
 * Best Practice:
 * Option 1: should work fine, except there is no need to use a topic exchange.
 * Just use a direct exchange, one queue per user and for each of the stock
 * symbols a user is interested in create a binding between the user's queue
 * and the direct exchange.
 *
 * Option 2: each quote would only go to one consumer, which is probably not
 * what you want. In an AMQP system, to get the same message delivered to N
 * consumers you need (at least) N queues. Exchanges *copy* messages to queues,
 * whereas queues *round-robin* message delivery to consumers.
 */

/**
 * Encapsulates an arbitrary message - simple "bean" holder structure.
 */
case class Delivery(body: Array[Byte], properties: AMQP.BasicProperties, envelope: Envelope) extends Event

case class AMQPAcknowledge(deliveryTag: Long) extends Event

case object AMQPConnected
case object AMQPDisconnected

object AMQPExchange {
  /**
   * Each AMQP broker declares one instance of each supported exchange type on it's
   * own (for every virtual host). These exchanges are named after the their type
   * with a prefix of amq., e.g. amq.fanout. The empty exchange name is an alias
   * for amq.direct. For this default direct exchange (and only for that) the broker
   * also declares a binding for every queue in the system with the binding key
   * being identical to the queue name.
   *
   * This behaviour implies that any queue on the system can be written into by
   * publishing a message to the default direct exchange with it's routing-key
   * property being equal to the name of the queue.
   */
  val defaultDirect = "" // amp.direct
}


object AMQPDispatcher {
  val DEFAULT_CONTENT_TYPE = ContentType.JAVA_SERIALIZED_OBJECT

  private val defaultReconnectDelay = 3000
}

/**
 * The dispatcher that listens over the AMQP message endpoint.
 * It manages a list of subscribers to the trade message and also sends AMQP
 * messages coming in to the queue/exchange to the list of observers.
 */
abstract class AMQPDispatcher(factory: ConnectionFactory, val exchange: String) extends Publisher {
  private val log = Logger.getLogger(getClass.getName)

  case class State(connection: Option[Connection], channel: Option[Channel], consumer: Option[Consumer])
  private var state = State(None, None, None)

  private lazy val timer = new Timer("AMQPReconnectTimer")

  private var reconnectDelay: Long = AMQPDispatcher.defaultReconnectDelay

  /**
   * Connect only when start, so we can control it to connect at a appropriate time,
   * for instance, all processors are ready. Otherwise, the messages may have been
   * consumered before processors ready.
   */
  def connect: this.type = {
    try {
      doConnect
    } catch {
      case _ => // don't log ex here, we hope ShutdownListener will give us the cause
    }

    this
  }

  def connection = state.connection
  def channel = state.channel
  def consumer = state.consumer

  @throws(classOf[IOException])
  private def doConnect {
    log.info("Begin to connect " + factory.getHost + ":" + factory.getPort + "...")

    (try {
        val conn = factory.newConnection
        // @Note: Should listen to connection instead of channel on ShutdownSignalException,
        // @see com.rabbitmq.client.impl.AMQPConnection.MainLoop
        conn.addShutdownListener(new ShutdownListener {
            def shutdownCompleted(cause: ShutdownSignalException) {
              publish(AMQPDisconnected)
              reconnect(cause)
            }
          })

        Left(conn)
      } catch {
        case ex => Right(ex)
      }
    ) match {
      case Left(conn) =>
        // we won't catch exceptions thrown during the following procedure, since we need them to fire ShutdownSignalException
        
        val channel = conn.createChannel
        val consumer = configure(channel)

        state = State(Option(conn), Option(channel), consumer)

        log.info("Successfully connected at: " + conn.getHost + ":" + conn.getPort)
        reconnectDelay = AMQPDispatcher.defaultReconnectDelay
        publish(AMQPConnected)

      case Right(ex) =>
        publish(AMQPDisconnected)
        // @Note **only** when there is none created connection, we'll try to reconnect here,
        // let shutdown listener to handle all other reconnetion needs
        reconnect(ex)
    }
  }

  private def reconnect(cause: Throwable) {
    log.warning("Will try to reconnect in " + reconnectDelay + " ms, the cause is:")
    log.log(Level.WARNING, cause.getMessage, cause)

    disconnect
    
    timer.schedule(new TimerTask {
        def run {
          try {
            reconnectDelay *= 2
            doConnect
          } catch {
            case _ => // don't log ex here, we hope ShutdownListener will give us the cause
          }
        }
      }, reconnectDelay)
  }

  private def disconnect {
    channel foreach {ch =>
      try {
        consumer foreach {case x: DefaultConsumer => ch.basicCancel(x.getConsumerTag)}
        ch.close
      } catch {
        case _ =>
      }
    }

    connection foreach {conn =>
      if (conn.isOpen) {
        try {
          conn.close
          log.log(Level.FINEST, "Disconnected AMQP connection at %s:%s [%s]", Array(factory.getHost, factory.getPort, this))
        } catch {
          case _ =>
        }
      }
    }
  }

  def isConnected = connection.isDefined && connection.get.isOpen

  /**
   * Registers queue and consumer.
   * @throws IOException if an error is encountered
   * @return the newly created and registered (queue, consumer)
   */
  @throws(classOf[IOException])
  protected def configure(channel: Channel): Option[Consumer]

  @throws(classOf[IOException])
  def publish(exchange: String, routingKey: String, props: AMQP.BasicProperties, content: Any) {
    channel foreach {ch =>
      import ContentType._

      val props1 = if (props == null) new AMQP.BasicProperties else props

      val contentType = props1.getContentType match {
        case null | "" => AMQPDispatcher.DEFAULT_CONTENT_TYPE 
        case x => ContentType(x)
      }

      val body = contentType.mimeType match {
        case JSON.mimeType => 
          content match {
            case msg: Msg[_] => 
              val headers = props1.getHeaders match {
                case null => new java.util.HashMap[String, AnyRef]
                case x => x
              }
              headers.put("tag", msg.tag.asInstanceOf[AnyRef])
              props1.setHeaders(headers)
          }
          Serializer.encodeJson(content)
        case AVRO.mimeType => 
          content match {
            case msg: Msg[_] => 
              val headers = props1.getHeaders match {
                case null => new java.util.HashMap[String, AnyRef]
                case x => x
              }
              headers.put("tag", msg.tag.asInstanceOf[AnyRef])
              props1.setHeaders(headers)
          }
          Serializer.encodeAvro(content)  

        case JAVA_SERIALIZED_OBJECT.mimeType => Serializer.encodeJava(content)
        case OCTET_STREAM.mimeType => content.asInstanceOf[Array[Byte]]
        case _ => content.asInstanceOf[Array[Byte]]
      }

      val contentEncoding = props1.getContentEncoding match {
        case null | "" => props1.setContentEncoding("gzip"); "gzip"
        case x => x
      }
    
      val body1 = contentEncoding match {
        case "gzip" => Serializer.gzip(body)
        case "lzma" => Serializer.lzma(body)
        case _ => body
      }

      log.fine(content + " sent: routingKey=" + routingKey + " size=" + body.length)
      ch.basicPublish(exchange, routingKey, props1, body1)
    }
  }

  protected def deleteQueue(queue: String) {
    channel foreach {ch =>
      try {
        // Check if the queue existed, if existed, will return a declareOk object, otherwise will throw IOException
        val declareOk = ch.queueDeclarePassive(queue)
        try {
          // the exception thrown here will destroy the connection too, so use it carefully
          ch.queueDelete(queue)
          log.info("Deleted queue: " + queue)
        } catch {
          case ex => log.log(Level.SEVERE, ex.getMessage, ex)
        }
      } catch {
        case ex: IOException => // queue doesn't exist
        case ex => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  class AMQPConsumer(channel: Channel, val isAutoAck: Boolean) extends DefaultConsumer(channel) {
    private val log = Logger.getLogger(this.getClass.getName)

    // When this is non-null the queue is in shutdown mode and nextDelivery should
    // throw a shutdown signal exception.
    @volatile private var _shutdown: ShutdownSignalException = _

    override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException ) {
      _shutdown = sig
    }

    def handleAck(_isAutoAck: Boolean, _channel: Channel, _envelope: Envelope) {
      if (!_isAutoAck) {
        try {
          // Params:
          //   deliveryTag - the tag from the received AMQP.Basic.GetOk or AMQP.Basic.Deliver
          //   multiple - true  to acknowledge all messages up to and including the supplied delivery tag;
          //              false to acknowledge just the supplied delivery tag.
          _channel.basicAck(_envelope.getDeliveryTag, false)
        } catch {
          case ex => log.log(Level.WARNING, ex.getMessage, ex)
        }
      }
    }

    @throws(classOf[IOException])
    override def handleDelivery(tag: String, envelope: Envelope, props: AMQP.BasicProperties, body: Array[Byte]) {
      // If needs ack, do it right now to avoid the queue on server is blocked.
      // @Note: when autoAck is set false, messages will be blocked until an ack to broker,
      // so should ack it. (Although prefetch may deliver more than one message to consumer)
      handleAck(isAutoAck, channel, envelope)

      log.fine("Got amqp message: " + (body.length / 1024.0) + "k" )

      val body1 = props.getContentEncoding match {
        case "gzip" => Serializer.ungzip(body)
        case "lzma" => Serializer.unlzma(body)
        case _ => body
      }

      import ContentType._
      val contentType = props.getContentType match {
        case null | "" =>  AMQPDispatcher.DEFAULT_CONTENT_TYPE
        case x => ContentType(x)
      }

      val headers = props.getHeaders match {
        case null => java.util.Collections.emptyMap[String, AnyRef]
        case x => x
      }
      
      try {
        val content = contentType.mimeType match {
          case JSON.mimeType => headers.get("tag") match {
              case tag: java.lang.Integer => Serializer.decodeJson(body1, tag.intValue)
              case _ => null
            }
          case AVRO.mimeType => headers.get("tag") match {
              case tag: java.lang.Integer => Serializer.decodeAvro(body1, tag.intValue)
              case _ => null
            }
            
          case JAVA_SERIALIZED_OBJECT.mimeType => Serializer.decodeJava(body1)
          case OCTET_STREAM.mimeType => body1
          case _ => body1
        }

        // send back to interested observers for further relay
        publish(AMQPMessage(content, props, envelope))
        log.fine("Published amqp message: " + content)
        log.fine(processors.map(_.getState.toString).mkString("(", ",", ")"))
      } catch {
        // should catch it when old version classes were sent by old version of clients.
        case ex: InvalidClassException => log.log(Level.WARNING, ex.getMessage, ex)
        case ex => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  /**
   * Hold strong refs of processors to avoid them to be GCed
   */
  var processors: List[Processor] = Nil
  
  /**
   * Processor that will automatically added as listener of this AMQPDispatcher
   * and process AMQPMessage via process(msg)
   */
  abstract class Processor extends Reactor {
    processors ::= this
    
    reactions += {
      case msg: AMQPMessage => process(msg)
    }
    listenTo(AMQPDispatcher.this)

    protected def process(msg: AMQPMessage)
  }

}
