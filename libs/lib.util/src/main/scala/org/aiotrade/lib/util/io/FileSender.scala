package org.aiotrade.lib.util.io

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import Encoding._

/**
 * @todo file length as Long
 */
object FileSender {
  // host and port of receiver
  private val port = 4711
  private val host = "localhost"

  def main(files: Array[String]) {
    val files = Array("/Users/dcaoyuan/file.text", "/Users/dcaoyuan/file.text")
    val sender = new FileSender(host, port)
    sender.send(files)
  }

}

class FileSender(host: String, port: Int) {

  def send(files: Array[String]) {
    try {
      val socket = new Socket(host, port)
      val os = socket.getOutputStream
      val nFiles = files.length

      writeInt(os, nFiles)
      for (fileName <- files) {
        writeString(os, fileName)
        writeFile(os, new File(fileName))
      }
    } catch {case ex: Exception => ex.printStackTrace}
  }

  @throws(classOf[IOException])
  private def writeInt(os: OutputStream, i: Int) {
    val bytes = encodeInt(i)
    os.write(bytes)
  }

  @throws(classOf[IOException])
  private def writeString(os: OutputStream, s: String) {
    val bytes = s.getBytes
    val len = bytes.length
    writeInt(os, len)
    os.write(bytes)
    os.flush
  }

  @throws(classOf[FileNotFoundException])
  @throws(classOf[IOException])
  private def writeFile(os: OutputStream, file: File) {
    writeInt(os, file.length.toInt)

    val bytes = new Array[Byte](1024)
    val is = new FileInputStream(file)
    var numRead = 0
    while ({numRead = is.read(bytes); numRead > 0}) {
      os.write(bytes, 0, numRead)
    }
    os.flush
  }

}
