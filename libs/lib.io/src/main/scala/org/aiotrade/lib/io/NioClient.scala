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
  case class ProcessData(data: Array[Byte], postAction: (Boolean) => Unit)

  // ----- simple test
  def main(args: Array[String]) {
    try {
      val client = new NioClient(InetAddress.getByName("localhost"), 9090)
      client.selectReactor.start
      
      val handler = new RspHandler
      handler.start
      client.selectReactor ! SendData("Hello World".getBytes, handler)
    } catch {case ex: Exception => ex.printStackTrace}
  }

}

import NioClient._
class RspHandler extends Actor {

  def handleResponse(rsp: Array[Byte]): Boolean = {
    println(new String(rsp))
    true
  }

  def act = loop {
    react {
      case ProcessData(data, postAction) =>
        val finished = handleResponse(data)
        postAction(finished)
    }
  }
}


/**
 * @parem hostAddress the host to connect to
 * @param port the port to connect to
 */
import NioClient._
@throws(classOf[IOException])
class NioClient(hostAddress: InetAddress, port: Int) {

  val selector = SelectorProvider.provider.openSelector

  val selectorActor = new SelectorActor(selector)
  val selectReactor = new SelectReactor(selectorActor)
  selectReactor.start

  selectorActor.addListener(selectReactor)
  selectorActor.start

  class SelectReactor(rwSelector: SelectorActor) extends Actor {
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

        case ConnectKey(key) =>
          // Register an interest in writing on this channel
          key.interestOps(SelectionKey.OP_WRITE)
          
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
          ex.printStackTrace
          return null
      }

      // then we can set it non-blocking
      socketChannel.configureBlocking(false)

      // Register an interest in writing on this channel
      //rwSelector.requestChange(Register(socketChannel, SelectionKey.OP_CONNECT))
      rwSelector.requestChange(Register(socketChannel, SelectionKey.OP_WRITE))

      socketChannel
    }

    @throws(classOf[IOException])
    private def read(key: SelectionKey) {
      val socketChannel = key.channel.asInstanceOf[SocketChannel]

      // Clear out our read buffer so it's ready for new data
      readBuffer.clear

      // Attempt to read off the channel
      var numRead = -1
      try {
        numRead = socketChannel.read(readBuffer)
      } catch {case ex: IOException =>
          // The remote forcibly closed the connection, cancel
          // the selection key and close the channel.
          key.cancel
          socketChannel.close
          return
      }

      if (numRead == -1) {
        // Remote entity shut the socket down cleanly. Do the
        // same from our end and cancel the channel.
        key.channel.close
        key.cancel
        return
      }

      if (numRead > 0) {
        // Handle the response
        handleResponse(socketChannel, readBuffer.array, numRead)
      }
    }

    @throws(classOf[IOException])
    private def handleResponse(socketChannel: SocketChannel, data: Array[Byte], numRead: Int) {
      // Make a correctly sized copy of the data before handing it to the client
      val rspData = new Array[Byte](numRead)
      System.arraycopy(data, 0, rspData, 0, numRead)

      // Look up the handler for this channel
      val handler = rspHandlers.get(socketChannel).getOrElse(return)

      // And pass the response to it
      handler ! ProcessData(rspData, finished =>
        // The handler has seen enough?, if true, close the connection
        if (finished) {
          socketChannel.close
          socketChannel.keyFor(selector).cancel
        }
      )
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
  }
}


