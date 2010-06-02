
package org.aiotrade.lib.amqp

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.HashMap
import java.util.Map

import com.rabbitmq.client.Consumer
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.AMQP.BasicProperties

object FileProducer {
  val hostname = "localhost"
  val port = 5672

  val queue = "request.quote"
  val exchange = "market.file"
  val routingKey = "faster.server.dbffile"

  def main(args: Array[String]) {

  }
}

class FileProducer(cf: ConnectionFactory, host: String, port: Int, exchange: String, $queue: String
) extends AMQPDispatcher(cf, host, port, exchange) {

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

  @throws(classOf[IOException])
  def sendFiles(files: List[File]) {
    for (file <- files) {
      val is = new FileInputStream(file)
      val length = file.length.toInt
      val body = new Array[Byte](length)
      is.read(body)
      is.close

      val headers: java.util.Map[String, AnyRef] = new HashMap
      headers.put("filename", file.getName)
      headers.put("length",  length.asInstanceOf[AnyRef])
      val props = new BasicProperties
      props.headers = headers
      props.contentType = "application/octet-stream"
      props.deliveryMode = 2 // persistent
      publish(body, FileProducer.routingKey, props)
    }
  }
}
