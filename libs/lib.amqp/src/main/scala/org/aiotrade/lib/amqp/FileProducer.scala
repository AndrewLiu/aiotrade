
package org.aiotrade.lib.amqp

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.HashMap
import java.util.Map

import com.rabbitmq.client.ConnectionParameters
import com.rabbitmq.client.Consumer
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.AMQP.BasicProperties

object FileProducer {
  private val queue = "filequeue"
  val exchange = "market.file"
  val routingKey = "faster.server.dbffile"
  val port = 5672

  private val params = new ConnectionParameters
  params.setUsername("guest")
  params.setPassword("guest")
  params.setVirtualHost("/")
  params.setRequestedHeartbeat(0)

  val factory = new ConnectionFactory(params)

  def queueName(i: Int) = queue + i

  // --- simple test
  def main(args: Array[String]) {
    val host = "localhost"
    val nConsumers = 5

    val producer = new FileProducer(factory, host, port, exchange, queue, routingKey, nConsumers)
    producer.start
    val files = List(new File("pom.xml"), new File("src/test/resources/testfile.txt"))

    producer.sendFiles(files)
    System.exit(0)
  }
}

class FileProducer(cf: ConnectionFactory, host: String, port: Int, exchange: String, queue: String, routingKey: String, nConsumers: Int
) extends AMQPDispatcher(cf, host, port, exchange) {

  @throws(classOf[IOException])
  override def configure(channel: Channel): Option[Consumer] = {
    channel.exchangeDeclare(exchange, "direct", true)
    // produce to n queues:
    for (i <- 0 until nConsumers) {
      val queuei = FileProducer.queueName(i)
      channel.queueDeclare(queuei, true)
      channel.queueBind(queuei, exchange, routingKey)
    }
    
    None
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
      headers.put("length", length.asInstanceOf[AnyRef])
      val props = new BasicProperties
      props.headers = headers
      props.contentType = "application/octet-stream"
      props.deliveryMode = 2 // persistent
      publish(body, routingKey, props)
    }
  }
}
