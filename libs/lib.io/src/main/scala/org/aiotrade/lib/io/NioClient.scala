package org.aiotrade.lib.io

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import scala.actors.Actor
import scala.collection.immutable.Queue
import scala.collection.mutable.HashMap

object NioClient {
  case class SendData(data: Array[Byte], handler: RspHandler)
  val handle = new RspHandler

  // ----- simple test
  def main(args: Array[String]) {
    try {
      val client = new NioClient(InetAddress.getByName("localhost"), 9090)
      client.selectReactor.start
      
      client.selectReactor ! SendData("Hello World".getBytes, handle)
      //handler.waitForResponse
    } catch {case ex: Exception => ex.printStackTrace}
  }

}

class RspHandler extends Actor {
  //private var rsp: Array[Byte] = null;

  def handleResponse(rsp: Array[Byte]): Boolean = {
    //this.rsp = rsp
    System.out.println(new String(rsp))
    //this.notify
    true
  }

  def act = loop {
    react {
      case _ =>
    }
  }

//  def waitForResponse = synchronized {
//    while (rsp == null) {
//      try {
//        this.wait
//      } catch {case ex: InterruptedException =>}
//    }
//
//    System.out.println(new String(this.rsp))
//  }
}


/**
 * @parem hostAddress the host to connect to
 * @param port the port to connect to
 */
import NioClient._
@throws(classOf[IOException])
class NioClient(hostAddress: InetAddress, port: Int) {

  val selector = SelectorProvider.provider.openSelector

  val readWriteSelector = new ReadWriteSelector(selector)
  val selectReactor = new SelectReactor(readWriteSelector, handle)
  selectReactor.start

  readWriteSelector.addListener(selectReactor)
  readWriteSelector.start

  class SelectReactor(rwSelector: ReadWriteSelector, worker: Actor) extends Actor {
    // The buffer into which we'll read data when it's available
    private val readBuffer = ByteBuffer.allocate(8192)

    private val pendingData = new HashMap[SocketChannel, Queue[ByteBuffer]]

    // Maps a SocketChannel to a RspHandler
    private val rspHandlers = HashMap[SocketChannel, RspHandler]()

    def act = loop {
      react {
        case SendData(data, handler) =>
          // Start a new connection
          val channel = initiateConnection

          // Register the response handler
          rspHandlers += (channel -> handler)

          // And queue the data we want written
          val queue = pendingData.get(channel) match {
            case None => Queue(ByteBuffer.wrap(data))
            case Some(x) => x enqueue ByteBuffer.wrap(data)
          }
          pendingData += (channel -> queue)

          // Finally, wake up our selecting thread so it can make the required changes
          selector.wakeup

        case ReadKey(key) => read(key)
        case WriteKey(key) => write(key)

      }
    }

    @throws(classOf[IOException])
    private def initiateConnection: SocketChannel = {
      // open an channel and kick off connecting
      val socketChannel = SocketChannel.open
      socketChannel.connect(new InetSocketAddress(hostAddress, port))

      /**
       * @Note actor's loop is not compitable with non-blocking mode, i.e. cannot work with SelectionKey.OP_CONNECT
       */
      // Finish the connection. If the connection operation failed
      // this will raise an IOException.
      try {
        while (!socketChannel.finishConnect) {}
      } catch {case ex: IOException =>
          // Cancel the channel's registration with our selector
          System.out.println(ex)
          return null
      }

      // then we can set it non-blocking
      socketChannel.configureBlocking(false)

      // Register an interest in writing on this channel
      rwSelector.requestChange(Register(socketChannel, SelectionKey.OP_WRITE))

      socketChannel
    }

    @throws(classOf[IOException])
    private def read(key: SelectionKey) {
      val socketChannel = key.channel.asInstanceOf[SocketChannel]

      // Clear out our read buffer so it's ready for new data
      this.readBuffer.clear();

      // Attempt to read off the channel
      var numRead = -1
      try {
        numRead = socketChannel.read(this.readBuffer);
      } catch {case ex: IOException =>
          // The remote forcibly closed the connection, cancel
          // the selection key and close the channel.
          key.cancel
          socketChannel.close
          return;
      }

      if (numRead == -1) {
        // Remote entity shut the socket down cleanly. Do the
        // same from our end and cancel the channel.
        key.channel.close
        key.cancel
        return
      }

      // Handle the response
      handleResponse(socketChannel, readBuffer.array, numRead)
    }

    @throws(classOf[IOException])
    private def handleResponse(socketChannel: SocketChannel, data: Array[Byte], numRead: Int) {
      // Make a correctly sized copy of the data before handing it
      // to the client
      val rspData = new Array[Byte](numRead);
      System.arraycopy(data, 0, rspData, 0, numRead);

      // Look up the handler for this channel
      val handler = rspHandlers.get(socketChannel).getOrElse(return)

      // And pass the response to it
      if (handler.handleResponse(rspData)) {
        // The handler has seen enough, close the connection
        socketChannel.close
        socketChannel.keyFor(selector).cancel
      }
    }

    @throws(classOf[IOException])
    private def write(key: SelectionKey) {
      val socketChannel = key.channel.asInstanceOf[SocketChannel]

      var queue = pendingData.get(socketChannel).getOrElse(return)

      // Write until there's not more data ...
      var done = false
      while (!queue.isEmpty && !done) {
        val (head, tail) = queue.dequeue
        socketChannel.write(head)
        if (head.remaining > 0) {
          // ... or the socket's buffer fills up
          done = true
        } else {
          queue = tail
        }
      }
      pendingData(socketChannel) = queue

      if (queue.isEmpty) {
        // We wrote away all data, so we're no longer interested
        // in writing on this socket. Switch back to waiting for
        // data.
        key.interestOps(SelectionKey.OP_READ)
      }
    }

    @throws(classOf[IOException])
    private def finishConnection(key: SelectionKey) {
      val socketChannel = key.channel.asInstanceOf[SocketChannel]

      // Finish the connection. If the connection operation failed
      // this will raise an IOException.
      try {
        socketChannel.finishConnect
      } catch {case ex: IOException =>
          // Cancel the channel's registration with our selector
          System.out.println(ex);
          key.cancel
          return;
      }

      // Register an interest in writing on this channel
      key.interestOps(SelectionKey.OP_WRITE)
    }
  }
}


