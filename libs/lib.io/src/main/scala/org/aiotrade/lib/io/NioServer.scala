package org.aiotrade.lib.io

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import scala.actors.Actor
import scala.collection.immutable.Queue
import scala.collection.mutable.HashMap


abstract class KeyEvent(key: SelectionKey)
case class AcceptKey (key: SelectionKey) extends KeyEvent(key)
case class ReadKey   (key: SelectionKey) extends KeyEvent(key)
case class WriteKey  (key: SelectionKey) extends KeyEvent(key)
case class ConnectKey(key: SelectionKey) extends KeyEvent(key)

object NioServer {
  case class ServerDataEvent(sender: Actor, socket: SocketChannel, data: Array[Byte], count: Int)
  case class SendData(socket: SocketChannel, data: Array[Byte])

  val worker = new EchoWorker

  def main(args: Array[String]) {
    try {
      worker.start
      new NioServer(null, 9090) start
    } catch {case ex: IOException => ex.printStackTrace}
  }

  class EchoWorker extends Actor {
    def act = loop {
      react {
        case ServerDataEvent(actor, channel, data, count) =>
          val dataCopy = new Array[Byte](count)
          System.arraycopy(data, 0, dataCopy, 0, count)
          // Return to sender
          actor ! SendData(channel, dataCopy)
      }
    }
  }

}

import NioServer._
class NioServer(hostAddress: InetAddress, port: Int) extends Actor {
  // Create a new selector
  val acceptSelector = SelectorProvider.provider.openSelector

  // Create a new non-blocking server socket channel
  val serverChannel = ServerSocketChannel.open
  serverChannel.configureBlocking(true)

  // Bind the server socket to the specified address and port
  serverChannel.socket.bind(new InetSocketAddress(hostAddress, port))

  // Register the server socket channel, indicating an interest in
  // accepting new connections
  //serverChannel.register(acceptSelector, SelectionKey.OP_ACCEPT)

  val clientSelector = SelectorProvider.provider.openSelector
  
  val readWriteSelector = new ReadWriteSelector(clientSelector)
  val selectReactor = new SelectReactor(readWriteSelector, worker)
  selectReactor.start
  
  readWriteSelector.addListener(selectReactor)
  readWriteSelector.start

  def act = loop {
    val clientChannel = serverChannel.accept
    if (clientChannel != null) {
      clientChannel.configureBlocking(false)
      println("new connection accepted")
      // Register the new SocketChannel with our Selector, indicating
      // we'd like to be notified when there's data waiting to be read
      //
      // @Note it seems this call doesn't work, the selector.select and this register should be in same thread
      //clientChannel.register(clientSelector, SelectionKey.OP_READ)
      readWriteSelector.requestChange(Register(clientChannel, SelectionKey.OP_READ))
    }
  }

  class SelectReactor(rwSelector: ReadWriteSelector, worker: Actor) extends Actor {
    // The buffer into which we'll read data when it's available
    private val readBuffer = ByteBuffer.allocate(8192)

    private val pendingData = new HashMap[SocketChannel, Queue[ByteBuffer]]

    def act = loop {
      react {
        case SendData(socket, data) =>
          // Indicate we want the interest ops set changed
          rwSelector.requestChange(ChangeOps(socket, SelectionKey.OP_WRITE))

          // And queue the data we want written
          val queue = pendingData.get(socket) match {
            case None => Queue(ByteBuffer.wrap(data))
            case Some(x) => x enqueue ByteBuffer.wrap(data)
          }
          pendingData += (socket -> queue)

        case ReadKey(key) => read(key)
        case WriteKey(key) => write(key)
      }
    }

    @throws(classOf[IOException])
    private def read(key: SelectionKey) {
      println("new read selected")
      val socketChannel = key.channel.asInstanceOf[SocketChannel]

      // Clear out our read buffer so it's ready for new data
      this.readBuffer.clear

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

      // Hand the data off to our worker thread
      worker ! ServerDataEvent(this, socketChannel, readBuffer.array, numRead)
    }

    @throws(classOf[IOException])
    private def write(key: SelectionKey) {
      println("new write selected")
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

class ReadWriteSelector(selector: Selector) extends Actor {

  private var listeners = List[Actor]()
  private var pendingChanges = List[ChangeRequest]()

  def addListener(listener: Actor) {
    listeners synchronized {listeners ::= listener}
  }

  def requestChange(change: ChangeRequest) {
    pendingChanges synchronized {pendingChanges ::= change}
    
    // wake up selecting thread so it can make the required changes.
    // which will interrupt selector.select blocking, so the loop can re-begin
    selector.wakeup
  }
  
  def act = loop {
    try {
      pendingChanges synchronized {
        pendingChanges foreach {
          case ChangeOps(channel, ops) =>
            val key = channel.keyFor(selector)
            key.interestOps(ops)

          case Register(channel, ops) =>
            channel.register(selector, ops)
        }

        pendingChanges = Nil
      }
      
      selector.select

      // Iterate over the set of keys for which events are available
      val selectedKeys = selector.selectedKeys.iterator
      while (selectedKeys.hasNext) {
        val key = selectedKeys.next
        selectedKeys.remove

        if (key.isValid) {
          // Check what event is available and deal with it
          if (key.isConnectable) {
            if (finishConnection(key)) {
              listeners foreach {_ ! ConnectKey(key)}
            }
          } else if (key.isReadable) {
            listeners foreach {_ ! ReadKey(key)}
          } else if (key.isWritable) {
            listeners foreach {_ ! WriteKey(key)}
          }
        }
      }
    } catch {case ex: Exception => ex.printStackTrace}
  }

  /**
   * When a connectable key selected, finishConnect call should be in same thread,
   * so we finishConnection here
   */
  @throws(classOf[IOException])
  private def finishConnection(key: SelectionKey): Boolean = {
    val socketChannel = key.channel.asInstanceOf[SocketChannel]

    // Finish the connection. If the connection operation failed
    // this will raise an IOException.
    try {
      socketChannel.finishConnect
    } catch {case ex: IOException =>
        // Cancel the channel's registration with our selector
        ex.printStackTrace
        key.cancel
        false
    }

    true
  }

}



abstract class ChangeRequest(channel: SocketChannel, ops: Int)
case class Register (channel: SocketChannel, ops: Int) extends ChangeRequest(channel, ops)
case class ChangeOps(channel: SocketChannel, ops: Int) extends ChangeRequest(channel, ops)

