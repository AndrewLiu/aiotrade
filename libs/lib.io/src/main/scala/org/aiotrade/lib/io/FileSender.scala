package org.aiotrade.lib.io

import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import org.aiotrade.lib.io.SelectActor.Connect
import org.aiotrade.lib.io.SelectActor.Write
import scala.actors.Actor
import scala.collection.immutable.Queue
import Encoding._

object FileSender {
  private val port = 4711
  private val host = "localhost"

  val writeSelector = new SelectActor(SelectionKey.OP_WRITE)
  writeSelector.start

  // ----- simple test
  def main(files: Array[String]) {
    val files = Array("/Users/dcaoyuan/file.text", "/Users/dcaoyuan/file.text")
    val sender = new FileSender(host, port)
    sender.send(files)
    sender.send(files)
  }
}

import FileSender._
/**
 * @param host host of receiver
 * @param port port of receiver
 */
class FileSender(host: String, port: Int) {

  // open an channel and kick off connecting
  val socketChannel = SocketChannel.open
  socketChannel.connect(new InetSocketAddress(host, port))

  /**
   * @Note actor's loop is not compitable with non-blocking mode, i.e. cannot work with SelectionKey.OP_CONNECT
   */
  while (!socketChannel.finishConnect) {}
  // then we can set it non-blocking
  socketChannel.configureBlocking(false)

  // connected, create listener and listen to writeSelector
  val conn = new ClientConnection(socketChannel)
  writeSelector.addListener(conn)
  
  def send(files: Array[String]) {
    try {
      sendInt(conn, files.length)
      for (fileName <- files) {
        sendString(conn, fileName)
        sendFile(conn, fileName)
      }
    } catch {case ex: Exception => ex.printStackTrace}
  }

  private def sendInt(conn: ClientConnection, i: Int) {
    val bytes = encodeInt(i)
    conn.send(ByteBuffer.wrap(bytes))
  }

  private def sendLong(conn: ClientConnection, i: Long) {
    val bytes = encodeLong(i)
    conn.send(ByteBuffer.wrap(bytes))
  }

  private def sendString(conn: ClientConnection, s: String) {
    val bytes = s.getBytes
    val len = bytes.length
    sendInt(conn, len)
    conn.send(ByteBuffer.wrap(bytes))
  }

  private def sendFile(conn: ClientConnection, file: String) {
    val fileChannel = (new RandomAccessFile(file, "r")).getChannel
    val size = fileChannel.size
    val byteBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size)
    sendLong(conn, size)
    conn.send(byteBuf)
  }

  class ClientConnection(val channel: SocketChannel) extends Actor with ChannelListener {
    private var _isOpen = true

    start
    println("clientConnection started")

    /**
     * Is this connection currentnly open?
     */
    def isOpen = _isOpen && channel.isOpen && channel.isConnected

    /**
     * Closes the connection.
     */
    def close {
      _isOpen = false
      channel.close
      super.exit
    }

    def act = loop {
      react {
        case Connect(sender) =>
          // should call finishConnect to get connected
          while (!channel.finishConnect) {}
          writeSelector.addListener(this)
        case Write(sender) =>
          println(" ** Sending !")
          try {
            val finished = flushWriteBuffers
            sender.addListener(this)
//          if (finished) {
//            close
//          } else {
//            // add back this connection to the write selector.
//            sender.addListener(this)
//          }
          } catch {case ex: IOException => ex.printStackTrace}
        case x => println(x)
      }
    }

    /**
     * A list of buffers that need to be written.
     */
    private val writeMutex = new Object
    private var writeBufs = Queue[ByteBuffer]()

    /**
     * Queues a reply to send to this connection.
     *
     * <code>flushWriteBuffers</code> must be called until it returns true to
     * guarantee the reply has been sent.
     */
    def send(byteBuf: ByteBuffer) {
      writeBufs = writeBufs enqueue byteBuf
    }


    /**
     * Sends as much data in its buffer queue as it can without blocking.
     *
     * @return True if all data has been sent; false if not.
     */
    def flushWriteBuffers: Boolean = {
      if (!_isOpen) {
        System.err.println("ERROR: Attempt to write to closed channel.")
        return true
      }

      if (!writeBufs.isEmpty) {
        val (head, tail) = writeBufs.dequeue
        if (head.remaining > 0) {
          try {
            channel.write(head)
          } catch {case ioe: IOException => close; throw ioe}
        }
        if (head.remaining == 0) {
          writeBufs = tail
        }
      }

      writeBufs isEmpty
    }

  }
}


