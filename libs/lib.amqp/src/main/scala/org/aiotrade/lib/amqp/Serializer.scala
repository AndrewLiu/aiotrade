/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.amqp

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.aiotrade.lib.json.JsonInputStreamReader
import org.aiotrade.lib.json.JsonOutputStreamWriter
import org.aiotrade.lib.json.JsonSerializable

object Serializer {
  /**
   * lzma properties with:
   * encoder.SetEndMarkerMode(true)     // must set true
   * encoder.SetDictionarySize(1 << 20) // 1048576
   */
  val lzmaProps = Array[Byte](93, 0, 0, 16, 0)
}

import Serializer._
trait Serializer {
  def encodeJava(content: Any): Array[Byte] = {
    val out = new ByteArrayOutputStream
    val oout = new ObjectOutputStream(out)
    oout.writeObject(content)
    oout.close

    out.toByteArray
  }

  def decodeJava(body: Array[Byte]): Any = {
    val in = new ObjectInputStream(new ByteArrayInputStream(body))
    in.readObject
  }

  def encodeJson(content: Any): Array[Byte] = {
    val out = new ByteArrayOutputStream
    val jout = new JsonOutputStreamWriter(out, "utf-8")

    content match {
      case x: JsonSerializable => 
        jout.write(x)
      case xs: Array[JsonSerializable] =>
        jout.write(xs)
      case xs: List[JsonSerializable] =>
        jout.write(xs)
      case _ => //todo
    }
    jout.close

    out.toByteArray
  }

  def decodeJson(body: Array[Byte]): Any = {
    val jin = new JsonInputStreamReader(new ByteArrayInputStream(body), "utf-8")
    jin.readObject
  }

  @throws(classOf[IOException])
  def gzip(input: Array[Byte]): Array[Byte] = {
    val out = new ByteArrayOutputStream
    val bout = new BufferedOutputStream(new GZIPOutputStream(out))
    bout.write(input)
    bout.close

    val body = out.toByteArray

    out.close
    body
  }

  @throws(classOf[IOException])
  def ungzip(input: Array[Byte]): Array[Byte] = {
    val in = new ByteArrayInputStream(input)
    val bin = new BufferedInputStream(new GZIPInputStream(in))
    val out = new ByteArrayOutputStream

    val buf = new Array[Byte](1024)
    var len = -1
    while ({len = bin.read(buf); len > 0}) {
      out.write(buf, 0, len)
    }

    val body = out.toByteArray

    in.close
    bin.close
    out.close
    body
  }

  @throws(classOf[IOException])
  def lzma(input: Array[Byte]): Array[Byte] = {
    val in = new ByteArrayInputStream(input)
    val out = new ByteArrayOutputStream

    val encoder = new SevenZip.Compression.LZMA.Encoder
    encoder.SetEndMarkerMode(true)     // must set true
    encoder.SetDictionarySize(1 << 20) // 1048576

    encoder.Code(in, out, -1, -1, null)

    val body = out.toByteArray

    in.close
    out.close
    body
  }

  @throws(classOf[IOException])
  def unlzma(input: Array[Byte]): Array[Byte] = {
    val in = new ByteArrayInputStream(input)
    val out = new ByteArrayOutputStream

    val decoder = new SevenZip.Compression.LZMA.Decoder
    decoder.SetDecoderProperties(lzmaProps)
    decoder.Code(in, out, -1)

    val body = out.toByteArray

    in.close
    out.close
    body
  }


  
}
