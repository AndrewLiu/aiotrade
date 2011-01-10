package org.aiotrade.lib.amqp

import java.io.File
import java.io.FileOutputStream
import java.util.logging.Logger
import com.rabbitmq.client.ConnectionFactory

object FileSubscriber {

  // --- simple test
  def main(args: Array[String]) {
    val host = "localhost"
    val port = 5672

    val queue = "filequeue"
    val exchange = "market.internal"
    val routingKey = "source.file.cndbf"

    val outputDirPath = System.getProperty("user.home") + File.separator + "storage"

    val factory = new ConnectionFactory
    factory.setHost(host)
    factory.setPort(port)
    factory.setUsername("guest")
    factory.setPassword("guest")
    factory.setVirtualHost("/")
    factory.setRequestedHeartbeat(0)

    for (i <- 0 until 5) {
      val queuei = queue + i
      val subscriber = new FileSubscriber(factory,
                                          exchange,
                                          outputDirPath)
      
      new subscriber.SafeProcessor
      subscriber.connect
      subscriber.consumeQueue(queuei, true, false, false, true)
      subscriber.subscribeTopic(routingKey)
    }
  }

}

class FileSubscriber(factory: ConnectionFactory, exchange: String, outputDirPath: String, isAutoAck: Boolean = true
) extends AMQPSubscriber(factory, exchange) {
  val outputDir = new File(outputDirPath)
  if (!outputDir.exists) {
    outputDir.mkdirs
  } else {
    assert(outputDir.isDirectory, "outputDir should be director: " + outputDir)
  }
  
  class DefaultProcessor extends Processor {
    protected def process(msg: AMQPMessage) {
      val headers = msg.props.getHeaders
      val body = msg.body.asInstanceOf[Array[Byte]]

      try {
        var fileName = headers.get("filename").toString
        var outputFile = new File(outputDir, fileName)
        var i = 1
        while (outputFile.exists) {
          fileName = fileName + "_" + i
          outputFile = new File(outputDir, fileName)
          i += 1
        }
        
        val out = new FileOutputStream(outputFile)
        out.write(body)
        out.close
      } catch {
        case e => e.printStackTrace
      }
    }
  }

  /**
   * Firstly save the file with a temporary file name.
   * When finish receiving all the data, then rename to the regular file in the same folder.
   */
  class SafeProcessor extends Processor {
    private val log = Logger.getLogger(this.getClass.getName)
    
    protected def process(msg: AMQPMessage) {
      val headers = msg.props.getHeaders
      val body = msg.body.asInstanceOf[Array[Byte]]

      try {
        var fileName = headers.get("filename").toString
        var outputFile = new File(outputDir, "." + fileName + ".tmp")
        var i = 1
        while (outputFile.exists) {
          fileName = fileName + "_" + i
          outputFile = new File(outputDir, "." + fileName + ".tmp")
          i += 1
        }
        
        val out = new FileOutputStream(outputFile)
        out.write(body)
        out.close

        outputFile.renameTo(new File(outputDir, fileName))
        log.info("Received " + fileName)
      } catch {
        case e => e.printStackTrace
      }
    }
  }

}
