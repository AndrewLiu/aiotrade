package org.aiotrade.lib.amqp

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.ConnectionParameters
import com.rabbitmq.client.Consumer

import scala.actors.Actor


object FileConsumer {

  // --- simple test
  def main(args: Array[String]) {
    val host = "localhost"

    val params = new ConnectionParameters
    params.setUsername("guest")
    params.setPassword("guest")
    params.setVirtualHost("/")
    params.setRequestedHeartbeat(0)

    val outputDirPath = System.getProperty("user.home") + File.separator + "storage"

    val factory = new ConnectionFactory(params)

    for (i <- 0 until 5) {
      val queuei = FileProducer.queueName(i)
      val consumer = new FileConsumer(FileProducer.factory,
                                      host,
                                      FileProducer.port,
                                      FileProducer.exchange,
                                      queuei,
                                      FileProducer.routingKey,
                                      outputDirPath)
      
      new consumer.DefaultProcessor
      consumer.start
    }
  }

}

class FileConsumer(cf: ConnectionFactory, host: String, port: Int, exchange: String, queue: String, routingKey: String, outputDirPath: String
) extends AMQPDispatcher(cf, host, port, exchange) {
  val outputDir = new File(outputDirPath)
  if (!outputDir.exists) {
    outputDir.mkdirs
  } else {
    assert(outputDir.isDirectory, "outputDir should be director: " + outputDir)
  }
  
  @throws(classOf[IOException])
  override def configure(channel: Channel): Option[Consumer] = {
    channel.exchangeDeclare(exchange, "direct", true)
    channel.queueDeclare(queue, true)
    channel.queueBind(queue, exchange, routingKey)
    
    val consumer = new AMQPConsumer(channel)
    channel.basicConsume(queue, consumer)
    Some(consumer)
  }

  abstract class Processor extends Actor {
    start
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
        var fileName = headers.get("filename").toString + "_" + System.currentTimeMillis
        var outputFile = new File(outputDir, fileName)
        var i = 1
        while (outputFile.exists) {
          fileName = fileName + "_" + i
          outputFile = new File(outputDir, fileName)
          i += 1
        }
        
        val out = new FileOutputStream(outputFile)
        out.write(content)
        out.close
      } catch {
        case e => e.printStackTrace
      }
    }
  }

}
