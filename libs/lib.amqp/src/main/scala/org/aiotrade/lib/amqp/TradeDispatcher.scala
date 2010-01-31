package org.aiotrade.lib.amqp

// message for adding observers
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import scala.actors.Actor

case class AddListener(a: Actor)

// The trade object that needs to be serialized
@serializable
case class Trade(ref: String, security: String, var value: Int)

case class TradeMessage(message: Trade)

// The dispatcher that listens over the AMQP message endpoint
class TradeDispatcher(cf: ConnectionFactory, host: String, port: Int) extends Actor {

  val conn = cf.newConnection(host, port)
  val channel = conn.createChannel()
  val ticket = channel.accessRequest("/data")

  // set up exchange and queue
  channel.exchangeDeclare(ticket, "mult", "direct")
  channel.queueDeclare(ticket, "mult_queue")
  channel.queueBind(ticket, "mult_queue", "mult", "routeroute")

  // register consumer
  channel.basicConsume(ticket, "mult_queue", false, new TradeValueCalculator(channel, this))

  def act = loop(Nil)

  def loop(as: List[Actor]) {
    react {
      case AddListener(a) => loop(a :: as)
      case msg@TradeMessage(t) => as.foreach(_ ! msg); loop(as)
      case _ => loop(as)
    }
  }
}

class TradeMessageGenerator(cf: ConnectionFactory, host: String,
                            port: Int, exchange: String, routingKey: String) extends Actor {

  val conn = cf.newConnection(host, port)
  val channel = conn.createChannel()
  val ticket = channel.accessRequest("/data")

  def send(msg: Trade) {

    val bytes = new ByteArrayOutputStream
    val store = new ObjectOutputStream(bytes)
    store.writeObject(msg)
    store.close

    // publish to exchange
    channel.basicPublish(ticket, exchange, routingKey, null, bytes.toByteArray)
  }

  def act = loop

  def loop {
    react {
      case TradeMessage(msg: Trade) => send(msg); loop
    }
  }
}


class TradeValueCalculator(channel: Channel, a: Actor) extends DefaultConsumer(channel) {

  override def handleDelivery(tag: String, env: Envelope,
                              props: AMQP.BasicProperties, body: Array[Byte]) {

    val routingKey = env.getRoutingKey
    val contentType = props.contentType
    val deliveryTag = env.getDeliveryTag
    val in = new ObjectInputStream(new ByteArrayInputStream(body))

    // deserialize
    var t = in.readObject.asInstanceOf[Trade]

    // invoke business processing logic
    //t.value = computeTradeValue(...)

    // send back to dispatcher for further relay to
    // interested observers
    a ! TradeMessage(t)

    channel.basicAck(deliveryTag, false)
  }
}


