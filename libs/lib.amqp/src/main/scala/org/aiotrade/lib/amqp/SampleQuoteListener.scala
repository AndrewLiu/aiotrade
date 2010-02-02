/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.amqp

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.ConnectionParameters
import com.rabbitmq.client.QueueingConsumer
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import scala.actors.Actor
import scala.util.Random

// The trade object that needs to be serialized
@serializable
case class Quote(ref: String, symbol: String, var value: Int)

object SampleQuoteListener {
  val params = new ConnectionParameters
  params.setUsername("guest")
  params.setPassword("guest")
  params.setVirtualHost("/")
  params.setRequestedHeartbeat(0)

  val factory = new ConnectionFactory(params)

  def main(args: Array[String]) {
    val amqp = new TopicAMQPDispatcher(factory, "localhost", 5672)

    testNoWhile

    //startListeners
    sendMessages
  }

  def testNoWhile {
    for (symbol <- Constants.symbols) {
      createConsumer(symbol, false)
    }
  }

  def testUseWhile {
    for (symbol <- Constants.symbols) {
      val consumer = new Runnable {
        def run {
          createConsumer(symbol, true)
        }
      }

      new Thread(consumer) start
    }
  }

  def sendMessages {
    // cretae msgsender
    val sender = new AMQPSender(factory, "localhost", 5672)
    sender.start

    // send messages
    for (i <- 0 until 10;
         symbol <- Constants.symbols
    ) {
      val value = Random.nextInt
      val quote = Quote("ABCD", symbol, value)
      sender ! AMQPMessage(quote, symbol)
    }

  }


  def createConsumer(symbol: String, useWhile: Boolean) {
    val conn = factory.newConnection("localhost", 5672)
    val channel = conn.createChannel
    val ticket = channel.accessRequest("/data")

    val exchange = Constants.exchange
    val queue = "mine_queue" // a private, autodelete queue
    channel.exchangeDeclare(ticket, Constants.exchange, "topic")
    channel.queueDeclare(ticket, queue)
    channel.queueBind(ticket, queue, exchange, symbol)
    val consumer = if (useWhile) new QueueingConsumer(channel) else new QuoteConsumer(channel, interesterRelay)
    channel.basicConsume(ticket, queue, consumer)

    while (useWhile) {
      val delivery = consumer.asInstanceOf[QueueingConsumer].nextDelivery
      val envelope = delivery.getEnvelope
      val body = delivery.getBody

      // deserialize
      val in = new ObjectInputStream(new ByteArrayInputStream(body))
      in.readObject match {
        case quote@Quote(ref, symbol, value) =>
          println(envelope.getRoutingKey + " received: " + symbol + " value=" + value)

          // send back to dispatcher for further relay to interested observers
          interesterRelay ! AMQPMessage(quote, symbol)
      }

      channel.basicAck(envelope.getDeliveryTag, false)
    }
  }

  def startListeners {
    interesterRelay.start

    import scala.actors.Actor._
    val quoteListener = actor {
      loop {
        react {
          case msg@AMQPMessage(contents: Quote, key: String) =>
            println("received quote: " + msg.message)
        }
      }
    }
    
    interesterRelay ! AddListener(quoteListener)
  }
}

class QuoteConsumer(channel: Channel, relay: Actor) extends AMQPConsumer[Quote](channel, relay)

case class AddListener(a: Actor)
object interesterRelay extends Actor {
  def act {
    loop(Nil)
    def loop(listeners: List[Actor]) {
      react {
        case AddListener(a) => loop(a :: listeners)
        case msg@AMQPMessage(quote, key: String) => listeners.foreach(_ ! msg); loop(listeners)
        case _ => loop(listeners)
      }
    }
  }
}

