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
import org.aiotrade.lib.avro.ReflectData
import org.aiotrade.lib.avro.ReflectDatumWriter
import org.aiotrade.lib.util.actors.Evt
import org.aiotrade.lib.util.actors.Msg
import org.apache.avro.io.EncoderFactory

object Serializer {
  /**
   * lzma properties with:
   * encoder.SetEndMarkerMode(true)     // must set true
   * encoder.SetDictionarySize(1 << 20) // 1048576
   */
  val lzmaProps = Array[Byte](93, 0, 0, 16, 0)
  
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

  def encodeAvro(content: Any): Array[Byte] = {
    content match {
      case msg: Msg[_] => Evt.toAvro(msg)
      case _ =>
        // best trying
        val schema = ReflectData.get.getSchema(content.asInstanceOf[AnyRef].getClass)
        val bao = new ByteArrayOutputStream()
        val encoder = EncoderFactory.get.binaryEncoder(bao, null)
        val writer = ReflectDatumWriter[Any](schema)
        writer.write(content, encoder)
        encoder.flush()
        val body = bao.toByteArray
        bao.close
        body
    }
  }

  def decodeAvro(body: Array[Byte], tag: Int = Evt.NO_TAG): Any = {
    Evt.fromAvro(body, tag) match {
      case Some(x) => x
      case None => null
    }
  }
  
  def encodeJson(content: Any): Array[Byte] = {
    content match {
      case msg: Msg[_] => Evt.toJson(msg)
      case _ => Array[Byte]()
    }
  }

  def decodeJson(body: Array[Byte], tag: Int = Evt.NO_TAG): Any = {
    Evt.fromJson(body, tag) match {
      case Some(x) => x
      case None => null
    }
  }

  @throws(classOf[IOException])
  def gzip(input: Array[Byte]): Array[Byte] = {
    val out = new ByteArrayOutputStream()
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
    val out = new ByteArrayOutputStream()

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
