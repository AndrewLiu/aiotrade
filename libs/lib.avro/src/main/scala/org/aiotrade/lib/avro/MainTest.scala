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

import java.io.ByteArrayOutputStream
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import scala.collection.mutable

object MainTest {
  
  def main(args: Array[String]) {
    val t0 = System.currentTimeMillis
    testJavaVMap
    testScalaVMap
    testReflectClass
    println("Finished in " + (System.currentTimeMillis - t0) + "ms")
  }
  
  def testJavaVMap {
    val schemaDesc = """
    {"type": "map", "values": {"type": "array", "items": ["long", "double"]}}
    """
    
    
    val vmap = new java.util.HashMap[String, Array[_]]
    vmap.put(".", Array(1L, 2L, 3L))
    vmap.put("a", Array(1.0, 2.0, 3.0))

    testJsonMap(schemaDesc, vmap)
    testAvroMap(schemaDesc, vmap)
  }
  
  def testScalaVMap {
    val schemaDesc = """
    {"type": "map", "values": {"type": "array", "items": ["long", "double", "string"]}}
    """
    
    
    val vmap = new mutable.HashMap[String, Array[_]]
    vmap.put(".", Array(1L, 2L, 3L))
    vmap.put("a", Array(1.0, 2.0, 3.0))
    vmap.put("b", Array("a", "b", "c"))

    testJsonMap(schemaDesc, vmap)
    testAvroMap(schemaDesc, vmap)
  }
  
  def testJsonMap[T](schemaDesc: String, vmap: T) {
    println("\n========= Json ============= ")
    //val schema = ReflectData.get.getSchema(vmap.getClass)
    val schema = org.apache.avro.Schema.parse(schemaDesc)
    println(schema.toString)
    
    // encode a map
    val bao = new ByteArrayOutputStream()
    val encoder = JsonEncoder(schema, bao)
    val writer = ReflectDatumWriter[T](schema)
    writer.write(vmap, encoder)
    encoder.flush()
    val json = new String(bao.toByteArray, "UTF-8")
    println(json)
    
    // decode to scala map
    val decoder = JsonDecoder(schema, json)
    val reader = AvroDatumReader[collection.Map[String, Array[_]]](schema)
    val map = reader.read(null, decoder)

    println("\ndecoded ==>")
    map foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
  }
  
  def testAvroMap[T <: AnyRef](schemaDesc: String, vmap: T) {
    println("\n========= Avro ============= ")
    //val schema = ReflectData.get.getSchema(vmap.getClass)
    val schema = org.apache.avro.Schema.parse(schemaDesc)
    println(schema.toString)
    
    // encode a map
    val bao = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get.binaryEncoder(bao, null)
    val writer = ReflectDatumWriter[T](schema)
    writer.write(vmap, encoder)
    encoder.flush()
    val bytes= bao.toByteArray
    
    // decode to scala map
    val decoder = DecoderFactory.get.binaryDecoder(bytes, null)
    val reader = ReflectDatumReader[collection.Map[String, Array[_]]](schema)
    val map = reader.read(null, decoder)
    
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
    val bao = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get.binaryEncoder(bao, null)
    val writer = ReflectDatumWriter[Ticker](schema)
    writer.write(instance, encoder)
    encoder.flush()
    val bytes= bao.toByteArray
    
    // decode
    val decoder = DecoderFactory.get.binaryDecoder(bytes, null)
    val reader = ReflectDatumReader[Ticker](schema)
    val decoded = reader.read(null, decoder)

    println("\n==== after ===")
    println(decoded)
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