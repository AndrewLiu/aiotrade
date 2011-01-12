
package org.aiotrade.lib.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Consumer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import org.aiotrade.lib.amqp.datatype.ContentType

object FilePublisher {

  // --- simple test
  def main(args: Array[String]) {
    val nSubscribers = 5

    val host = "localhost"
    val port = 5672

    val exchange = "market.internal"
    val routingKey = "source.file.cndbf"

    val factory = new ConnectionFactory
    factory.setHost(host)
    factory.setPort(port)
    factory.setUsername("guest")
    factory.setPassword("guest")
    factory.setVirtualHost("/")
    factory.setRequestedHeartbeat(0)

    val publisher = new FilePublisher(factory, exchange, routingKey, true)
    publisher.connect
    val files = List(new File("pom.xml"), new File("src/test/resources/testfile.txt"))

    publisher.sendFiles(files)
    System.exit(0)
  }
}

class FilePublisher(factory: ConnectionFactory, exchange: String, routingKey: String, durable: Boolean = false
) extends AMQPDispatcher(factory, exchange) {

  @throws(classOf[IOException])
  def configure(channel: Channel): Option[Consumer] = {
    channel.exchangeDeclare(exchange, "direct", durable)
    None
  }

  @throws(classOf[IOException])
  def sendFiles(files: List[File]) {
    files foreach (sendFile(_))
  }

  @throws(classOf[IOException])
  def sendFile(file: File, toName: Option[String] = None) {
    val is = new FileInputStream(file)
    val length = file.length.toInt
    val body = new Array[Byte](length)
    is.read(body)
    is.close

    sendFile(body, toName.getOrElse(file.getName))
  }

  def sendFile(body: Array[Byte], toName: String) {
    val headers: java.util.Map[String, AnyRef] = new java.util.HashMap
    headers.put("filename", toName)
    headers.put("length", body.length.asInstanceOf[AnyRef])
    val props = new BasicProperties
    props.setHeaders(headers)
    props.setContentType(ContentType.OCTET_STREAM.mimeType)
    if (durable) props.setDeliveryMode(2) // persistent
    publish(exchange, routingKey, props, body)
  }

}
