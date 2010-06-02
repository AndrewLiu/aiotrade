package org.aiotrade.lib.amqp

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Consumer

import scala.actors.Actor


object FileConsumer {
  val hostname = "localhost"
  val port = 5672

  val queue = "request.quote"
  val exchange = "market.file"
  val routingKey = "faster.server.dbffile"

}

class FileConsumer(cf: ConnectionFactory, host: String, port: Int, exchange: String, $queue: String, outputDirName: String
) extends AMQPDispatcher(cf, host, port, exchange) {
  val outputDir = new File(outputDirName)
  if (!outputDir.exists) {
    outputDir.mkdirs
  }

  @throws(classOf[IOException])
  override def configure(channel: Channel): Consumer = {
    channel.exchangeDeclare(exchange, "direct")
    val queue = $queue match {
      case null | "" => channel.queueDeclare.getQueue
      case _ => channel.queueDeclare($queue); $queue
    }

    val consumer = new AMQPConsumer(channel)
    channel.basicConsume(queue, consumer)
    consumer
  }

  abstract class Processor extends Actor {
    FileConsumer.this ! AMQPAddListener(this)

    protected def process(msg: AMQPMessage)

    def act = loop {
      react {
        case msg: AMQPMessage => process(msg)
        case AMQPStop => exit
      }
    }
  }

  class DefaultProcessor extends Processor {
    override protected def process(msg: AMQPMessage) {
      val headers = msg.props.headers
      val content = msg.content.asInstanceOf[Array[Byte]]

      try {
        val fileName = headers.get("filename").toString + "_" + System.currentTimeMillis
        val file = new File(fileName)
        val saveToFile = new File(outputDir, fileName)
        val out = new FileOutputStream(saveToFile)
        out.write(content)
        out.close
      } catch {
        case e => e.printStackTrace
      }
    }
  }

}
