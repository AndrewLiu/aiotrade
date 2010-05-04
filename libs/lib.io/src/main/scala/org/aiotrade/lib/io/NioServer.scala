package org.aiotrade.lib.io

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.spi.SelectorProvider
import scala.actors.Actor


abstract class KeyEvent(key: SelectionKey)
case class AcceptKey (key: SelectionKey) extends KeyEvent(key)
case class ReadKey   (key: SelectionKey) extends KeyEvent(key)
case class WriteKey  (key: SelectionKey) extends KeyEvent(key)
case class ConnectKey(key: SelectionKey) extends KeyEvent(key)

object NioServer {
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
        case ProcessData(reactor, channel, key, data) =>
          // Return to sender
          reactor ! SendData(channel, data, this)
      }
    }
  }

}

/**
 * @parem hostAddress the host to connect to
 * @param port the port to connect to
 */
import NioServer._
class NioServer(hostAddress: InetAddress, port: Int) extends Actor {

  // Create a new non-blocking server socket channel
  val serverChannel = ServerSocketChannel.open
  serverChannel.configureBlocking(true)

  // Bind the server socket to the specified address and port
  serverChannel.socket.bind(new InetSocketAddress(hostAddress, port))

  // Create a new selector
  //val acceptSelector = SelectorProvider.provider.openSelector
  // Register the server socket channel, indicating an interest in
  // accepting new connections
  //serverChannel.register(acceptSelector, SelectionKey.OP_ACCEPT)

  val selector = SelectorProvider.provider.openSelector
  
  val selectorActor = new SelectDispatcher(selector)
  val selectReactor = new SelectReactor(selectorActor)
  selectReactor.start
  
  selectorActor.addListener(selectReactor)
  selectorActor.start

  def act = loop {
    val clientChannel = serverChannel.accept
    if (clientChannel != null) {
      clientChannel.configureBlocking(false)
      println("new connection accepted")

      selectReactor ! SetResponseHandler(clientChannel, worker)
      // Register the new SocketChannel with our Selector, indicating
      // we'd like to be notified when there's data waiting to be read
      //
      // @Note it seems this call doesn't work, the selector.select and this register should be in same thread
      //clientChannel.register(clientSelector, SelectionKey.OP_READ)
      selectorActor.requestChange(InterestInOps(clientChannel, SelectionKey.OP_READ))
    }
  }
}
