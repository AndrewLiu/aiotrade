package org.aiotrade.lib.util.io

import java.io.File
import java.net.Socket
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import org.aiotrade.lib.util.io.SelectActor.Read
import scala.actors.Actor
import Encoding._

object FileReceiver {
  private val port = 4711

  // ----- simple test
  def main(args: Array[String]) {
    val receiver = new FileReceiver
    receiver.start
  }
}

import FileReceiver._
class FileReceiver extends Actor {
  selectActor.start
  
  object selectActor extends SelectActor(SelectionKey.OP_READ)

  val serverChannel = ServerSocketChannel.open
  serverChannel.socket.bind(new InetSocketAddress(port))
  serverChannel.configureBlocking(true)

  def stop {
    serverChannel.close
    super.exit
  }

  /**
   * loops, emitting newly connected sockets.
   */
  def act = loopWhile(serverChannel.isOpen) {
    val clientChannel = serverChannel.accept
    if (clientChannel != null) {
      clientChannel.configureBlocking(false)
      selectActor.addListener(new ClientConnection(clientChannel))
    }
  }
}

class ClientConnection(val channel: SocketChannel) extends Actor with ChannelListener {
  private var _isOpen = true
  private val requestParser = new RequestParser
  private val readBuf = ByteBuffer.allocate(8192)

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
      case Read(sender) =>
        try {
          val finished = readMoreInput
          if (!finished) {
            // add back this connection to the read selector.
            sender.addListener(this)
          } else {
            close
          }
        } catch {case ex: IOException => ex.printStackTrace}
    }
  }

  /**
   * Reads more input, if any, and passes it to the http request parser.
   *
   * @return True if all data has been sent; false if not.
   */
  def readMoreInput: Boolean = {
    if (!_isOpen) throw new IOException("Cannot read a closed connection!")
    readBuf.clear
    var numRead = -1
    try {
      numRead = channel.read(readBuf)
    } catch {case ioe: IOException => close; throw ioe}

    if (numRead == -1) {
      close
      println("Remote entity shut down socket (cleanly).")
      throw new IOException("Remote entity shut down socket (cleanly).")
    }

    val bytes = readBuf.array
    var i = 0
    while (i < numRead) {
      requestParser.consume(bytes(i))
      i += 1
    }

    requestParser.parsed
  }

  /**
   * convert 4 bytes to int
   */
  private def toInt(bytes: Array[Byte]): Int = {
    var ret = 0
    var i = 0
    while (i < 4) {
      var b = bytes(i) & 0xFF
      ret += b << (i * 8)
      i += 1
    }
    ret
  }

  private def littleEndianToLong(bytes: Array[Byte]): Int = (
    (bytes(0) & 0xFF) +
    (bytes(1) & 0xFF) << 8  +
    (bytes(2) & 0xFF) << 16 +
    (bytes(3) & 0xFF) << 24
  )

  /**
   * A continuation-based HTTP request parser.
   *
   * The parser consumes one character at a time,
   * which means that the parsing process can be suspended at any time.
   *
   * At the moment, this does not support Keep-Alive HTTP connections.
   */
  private class RequestParser {

    /**
     * Indicates the current position of the parser.
     */
    trait State
    case class NumFiles(buf: Array[Byte], idx: Int, len: Int) extends State
    case class FileMeta(name: Option[String], buf: Array[Byte], idx: Int, len: Int) extends State
    case class FileData(out: OutputStream, idx: Long, len: Long) extends State
    case object End extends State
    val NoneName = Some("")

    private var state: State = NumFiles(new Array[Byte](4), 0, 4)

    /* Components of the request. */
    private var numFiles = 0
    private var cntFiles = 0

    /**
     * Has a complete request been parsed?
     */
    def parsed = state == End

    /**
     * Update the state of the parser with the next character.
     */
    def consume(b: Byte) {
      state = state match {
        case NumFiles(buf, i, len) if i < len - 1 => buf(i) = b; NumFiles(buf, i + 1, len)
        case NumFiles(buf, i, _) =>
          buf(i) = b
          numFiles = decodeInt(buf)
          //println("number of files: " + numFiles)
          FileMeta(None, new Array[Byte](4), 0, 4)

          // read filename length
        case FileMeta(None, buf, i, len) if i < len - 1 => buf(i) = b; FileMeta(None, buf, i + 1, len)
        case FileMeta(None, buf, i, _) =>
          buf(i) = b
          val len = decodeInt(buf)
          //println("file name length: " + len)
          FileMeta(NoneName, new Array[Byte](len), 0, len)

          // read filename string
        case FileMeta(NoneName, buf, i, len) if i < len - 1 => buf(i) = b; FileMeta(NoneName, buf, i + 1, len)
        case FileMeta(NoneName, buf, i, _) =>
          buf(i) = b
          val name = new String(buf) + System.currentTimeMillis
          //println("file name: " + name)
          // expect file length in Long
          FileMeta(Some(name), new Array[Byte](8), 0, 8)

          // read file length
        case FileMeta(Some(name), buf, i, len) if i < len - 1 => buf(i) = b; FileMeta(Some(name), buf, i + 1, len)
        case FileMeta(Some(name), buf, i, _) =>
          buf(i) = b
          val len = decodeLong(buf)
          println("file length: " + len)
          val file = new File(name)
          val out = new FileOutputStream(file)
          FileData(out, 0, len)

          // read file content
        case FileData(out, i, len) if i < len - 1 => out.write(b); FileData(out, i + 1, len)
        case FileData(out, _, _) =>
          out.write(b)
          out.close
          cntFiles += 1
          // all files received? if true end, else begin a new file
          if (cntFiles == numFiles) End else FileMeta(None, new Array[Byte](4), 0, 4)

        case _ => throw new Exception(state + "/" + b)
      }
    }
  }
}