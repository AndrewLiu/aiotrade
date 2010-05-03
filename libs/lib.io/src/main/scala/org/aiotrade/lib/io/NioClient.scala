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

  // ----- simple test
  def main(args: Array[String]) {
    try {
      val client = new NioClient(InetAddress.getByName("localhost"), 9090)
      client.start
      val handler = new RspHandler
      client ! SendData("Hello World".getBytes, handler)
      //handler.waitForResponse
    } catch {case ex: Exception => ex.printStackTrace}
  }

}

class RspHandler {
  //private var rsp: Array[Byte] = null;

  def handleResponse(rsp: Array[Byte]): Boolean = {
    //this.rsp = rsp
    System.out.println(new String(rsp))
    //this.notify
    true
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
 *   // The host:port combination to connect to
 private InetAddress hostAddress;
 private int port;

 */
import NioClient._
@throws(classOf[IOException])
class NioClient(hostAddress: InetAddress, port: Int) extends Actor {
  changingActor.start
  
  // The selector we'll be monitoring
  private val selector = SelectorProvider.provider.openSelector


  // A list of PendingChange instances
  //private List pendingChanges = new LinkedList();


  object changingActor extends Actor {
    // The buffer into which we'll read data when it's available
    private val readBuffer = ByteBuffer.allocate(8192)

    private val pendingData = new HashMap[SocketChannel, Queue[ByteBuffer]]

    // Maps a SocketChannel to a RspHandler
    private val rspHandlers = HashMap[SocketChannel, RspHandler]()

    def act = loop {
      react {
        case SendData(data, handler) =>
          // Start a new connection
          val socket = initiateConnection

          // Register the response handler
          rspHandlers += (socket -> handler);

          // And queue the data we want written
          val queue = pendingData.get(socket) match {
            case None => Queue(ByteBuffer.wrap(data))
            case Some(x) => x enqueue ByteBuffer.wrap(data)
          }
          pendingData += (socket -> queue)

          // Finally, wake up our selecting thread so it can make the required changes
          selector.wakeup

        case ChangeOps(socket, ops) =>
          val key = socket.keyFor(selector)
          key.interestOps(ops)

        case Register(socket, ops) =>
          socket.register(selector, ops)

        case ConnectKey(key) => finishConnection(key)
        case ReadKey(key) => read(key)
        case WriteKey(key) => write(key)

      }
    }

    @throws(classOf[IOException])
    private def initiateConnection: SocketChannel = {
      // Create a non-blocking socket channel
      val socketChannel = SocketChannel.open
      socketChannel.configureBlocking(false);

      // Kick off connection establishment
      socketChannel.connect(new InetSocketAddress(hostAddress, port))

      // Queue a channel registration since the caller is not the
      // selecting thread. As part of the registration we'll register
      // an interest in connection events. These are raised when a channel
      // is ready to complete connection establishment.
      this ! Register(socketChannel, SelectionKey.OP_CONNECT)
//      synchronized(this.pendingChanges) {
//        this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
//      }

      return socketChannel;
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
      key.interestOps(SelectionKey.OP_WRITE);
    }


  }

//  public void send(byte[] data, RspHandler handler) {
//    // Start a new connection
//    SocketChannel socket = this.initiateConnection();
//
//    // Register the response handler
//    this.rspHandlers.put(socket, handler);
//
//    // And queue the data we want written
//    synchronized (this.pendingData) {
//      List queue = (List) this.pendingData.get(socket);
//      if (queue == null) {
//        queue = new ArrayList();
//        this.pendingData.put(socket, queue);
//      }
//      queue.add(ByteBuffer.wrap(data));
//    }
//
//    // Finally, wake up our selecting thread so it can make the required changes
//    this.selector.wakeup();
//  }

  def act = loop {
    while (true) {
      try {
        // Process any pending changes
//        synchronized (this.pendingChanges) {
//          Iterator changes = this.pendingChanges.iterator();
//          while (changes.hasNext()) {
//            ChangeRequest change = (ChangeRequest) changes.next();
//            switch (change.type) {
//              case ChangeRequest.CHANGEOPS:
//                SelectionKey key = change.socket.keyFor(this.selector);
//                key.interestOps(change.ops);
//                break;
//              case ChangeRequest.REGISTER:
//                change.socket.register(this.selector, change.ops);
//                break;
//            }
//          }
//          this.pendingChanges.clear();
//        }

        // Wait for an event one of the registered channels
        this.selector.select();

        // Iterate over the set of keys for which events are available
        val selectedKeys = this.selector.selectedKeys.iterator
        while (selectedKeys.hasNext) {
          val key = selectedKeys.next.asInstanceOf[SelectionKey]
          selectedKeys.remove

          if (key.isValid) {
            // Check what event is available and deal with it
            if (key.isConnectable) {
              changingActor ! ConnectKey(key)
            } else if (key.isReadable) {
              changingActor ! ReadKey(key)
            } else if (key.isWritable) {
              changingActor ! WriteKey(key)
            }
          }

        }
      } catch {case ex: Exception => ex.printStackTrace}
    }
  }



}


