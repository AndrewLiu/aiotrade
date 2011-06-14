/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.avro

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import scala.collection.mutable

object MainTest {
  
  def main(args: Array[String]) {
    testArrayBuffer
    
    val t0 = System.currentTimeMillis
    testTuple
    testJavaVMap
    testScalaVMap
    testReflectClass
    testReflectArrayOfClass
    println("Finished in " + (System.currentTimeMillis - t0) + "ms")
  }
  
  def testArrayBuffer {
    // debug on it to see type of field array
    val alist = new ArrayList[Double]()
    val primitiveArray = alist.toArray
    println("The type of field array  should be double[]")
    
    val abuff = new collection.mutable.ArrayBuffer[Double]()
    val objectArray = abuff.toArray
    println("The type of field array is still Object[]")
    
    
    val tickers = new ArrayList[Ticker]()
    tickers += new Ticker
    val arr = tickers.toArray
    var i = -1
    while ({i += 1; i < arr.length}) {
      val ticker = arr(i)
      println(ticker)
    }
  }
  
  def testTuple {
    val schemaJson = """
    {"type": "record", "name": "Tuple", "fields": [
      {"name":"a", "type":"string"},
      {"name":"b", "type":"long"}
    ]}
    """
    val schema = Schema.parse(schemaJson)
    val record = new GenericData.Record(schema)
    record.put(0, "a")
    record.put(1, 1L)
    
    println("\n==== test tuple ====")
    val bytes= encode(record, schema, true)
    println("\n==== encoded ===")
    println(new String(bytes, "UTF-8"))
    
    val decoded = decode(bytes, schema, classOf[(String, Long)], true).get
    println("\n==== decoded ===")
    println(decoded)
  }
  
  def testJavaVMap {
    val schemaJson = """
    {"type": "map", "values": {"type": "array", "items": ["long", "double"]}}
    """
    
    
    val vmap = new java.util.HashMap[String, Array[_]]
    vmap.put(".", Array(1L, 2L, 3L))
    vmap.put("a", Array(1.0, 2.0, 3.0))

    testJsonMap(schemaJson, vmap)
    testAvroMap(schemaJson, vmap)
  }
  
  def testScalaVMap {
    val schemaJson = """
    {"type": "map", "values": {"type": "array", "items": ["long", "double", "string"]}}
    """
    
    val vmap = new mutable.HashMap[String, Array[_]]
    vmap.put(".", Array(1L, 2L, 3L))
    vmap.put("a", Array(1.0, 2.0, 3.0))
    vmap.put("b", Array("a", "b", "c"))

    testJsonMap(schemaJson, vmap)
    testAvroMap(schemaJson, vmap)
  }
  
  def testJsonMap[T](schemaDesc: String, vmap: T) {
    println("\n========= Json ============= ")
    //val schema = ReflectData.get.getSchema(vmap.getClass)
    val schema = Schema.parse(schemaDesc)
    println(schema.toString)
    
    // encode a map
    val bytes = encode(vmap, schema, true)
    val json = new String(bytes, "UTF-8")
    println(json)
    
    // decode to scala map
    val map = decode(bytes, schema, classOf[collection.Map[String, Array[_]]], true).get

    println("\ndecoded ==>")
    map foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
  }
  
  def testAvroMap[T <: AnyRef](schemaDesc: String, vmap: T) {
    println("\n========= Avro ============= ")
    //val schema = ReflectData.get.getSchema(vmap.getClass)
    val schema = Schema.parse(schemaDesc)
    println(schema.toString)
    
    // encode a map
    val bytes = encode(vmap, schema, false)
    
    // decode to scala map
    val map = decode(bytes, schema, classOf[collection.Map[String, Array[_]]], false).get
    
    println("\ndecoded ==>")
    map foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
  }
  
  def testReflectClass {
    val instance = new Ticker
    instance.flag = 0
    println("\n==== before ===")
    println(instance)

    val schema = ReflectData.get.getSchema(instance.getClass)
    println(schema.toString)
    
    // encode
    val bytes = encode(instance, schema, true)
    
    // decode
    val decoded = decode(bytes, schema, classOf[Ticker], true).get

    println("\n==== after ===")
    println(decoded)
  }
  
  def testReflectArrayOfClass {
    val instance = new Ticker
    instance.flag = 0
    println("\n==== before ===")
    println(instance)
    
    val instances = Array(instance)

    val schema = ReflectData.get.getSchema(instances.getClass)
    println(schema.toString)
    
    // encode
    val bytes = encode(instances, schema, true)
    println(new String(bytes, "UTF-8"))
    
    // decode
    val decoded = decode(bytes, schema, classOf[Array[Ticker]], true).get

    println("\n==== after ===")
    println(decoded)
  }
  
  
  private def encode[T](value: T, schema: Schema, forJson: Boolean): Array[Byte] = {
    var out: ByteArrayOutputStream = null
    try {
      out = new ByteArrayOutputStream()
    
      val encoder = 
        if (forJson) {
          JsonEncoder(schema, out)
        } else {
          EncoderFactory.get.binaryEncoder(out, null)
        }
      
      val writer = ReflectDatumWriter[T](schema)
      writer.write(value, encoder)
      encoder.flush()
      
      out.toByteArray
    } catch {
      case ex => println(ex.getMessage); Array[Byte]()
    } finally {
      if (out != null) try {out.close} catch {case _ =>}
    }
  }
  
  @throws(classOf[IOException])
  private def decode[T](bytes: Array[Byte], schema: Schema, valueType: Class[T], forJson: Boolean): Option[T] = {
    var in: InputStream = null
    try {
      in = new ByteArrayInputStream(bytes)
      
      val decoder = 
        if (forJson) {
          JsonDecoder(schema, in)
        } else {
          DecoderFactory.get.binaryDecoder(in, null)
        }
      
      val reader = ReflectDatumReader[T](schema)
      val value = reader.read(null.asInstanceOf[T], decoder)
      Some(value)
    } catch {
      case ex => println(ex.getMessage); None
    } finally {
      if (in != null) try {in.close} catch {case _ =>}
    }
  }
  
  class Ticker {
    private val data = Array(1.0, 2.0)
    
    @transient
    var flag: Byte = 10
    
    val open = 8.0
    private val high = 10.0f
    
    var close = 10.1
    private var volumn = 100
    
    override def toString = 
      "Ticker(data=" + data.mkString("[", ",", "]") + ", flag=" + flag + ", open=" + open + ", close=" + close + ", high=" + high +  ")"
  }
}