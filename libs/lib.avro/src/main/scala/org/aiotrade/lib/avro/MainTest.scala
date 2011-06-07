/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.avro

import java.io.ByteArrayOutputStream
import org.apache.avro.io.EncoderFactory
import org.apache.avro.reflect.ReflectDatumWriter
import scala.collection.mutable

object MainTest {
  
  def main(args: Array[String]) {
    testJavaVMap
    testScalaVMap
  }
  
  def testJavaVMap {
    val schemaDesc = """
    {"type": "map", "values": {"type": "array", "items": ["long", "double"]}}
    """
    
    
    val r = new java.util.HashMap[String, Array[_]]
    r.put(".", Array(1L, 2L, 3L))
    r.put("a", Array(1.0, 2.0, 3.0))

    //val schema = ReflectData.get).getSchema(r.getClass)
    val schema = org.apache.avro.Schema.parse(schemaDesc)
    println(schema.toString)
    val bao = new ByteArrayOutputStream()
    val encoder = JsonEncoder(schema, bao)
    val writer = new ReflectDatumWriter[java.util.HashMap[String, Array[_]]](schema)
    writer.write(r, encoder)
    encoder.flush()
    val json = new String(bao.toByteArray, "UTF-8")
    println(json)
    
    val itr = r.entrySet.iterator
    while (itr.hasNext) {
      val entry = itr.next
      //val schema = ReflectData.get).getSchema(entry.getValue.getClass)
      val schema = ReflectData.get.getSchema(classOf[Array[_]])
      println(schema.toString)
      val bao = new ByteArrayOutputStream()
      val encoder = EncoderFactory.get.jsonEncoder(schema, bao)
      val writer = new ReflectDatumWriter[Array[_]](schema)
      writer.write(entry.getValue, encoder)
      encoder.flush()
      val json = new String(bao.toByteArray, "UTF-8")
      println(json)
    }
  }
  
  def testScalaVMap {
    val schemaDesc = """
    {"type": "map", "values": {"type": "array", "items": ["long", "double"]}}
    """
    
    
    val r = new mutable.HashMap[String, Array[_]]
    r.put(".", Array(1L, 2L, 3L))
    r.put("a", Array(1.0, 2.0, 3.0))

    //val schema = ReflectData.get).getSchema(r.getClass)
    val schema = org.apache.avro.Schema.parse(schemaDesc)
    println(schema.toString)
    val bao = new ByteArrayOutputStream()
    val encoder = JsonEncoder(schema, bao)
    val writer = AvroDatumWriter[collection.Map[String, Array[_]]](schema)
    writer.write(r, encoder)
    encoder.flush()
    val json = new String(bao.toByteArray, "UTF-8")
    println(json)
    
    val itr = r.iterator
    while (itr.hasNext) {
      val entry = itr.next
      //val schema = ReflectData.get).getSchema(entry.getValue.getClass)
      val schema = ReflectData.get.getSchema(classOf[Array[_]])
      println(schema.toString)
      val bao = new ByteArrayOutputStream()
      val encoder = EncoderFactory.get.jsonEncoder(schema, bao)
      val writer = new ReflectDatumWriter[Array[_]](schema)
      writer.write(entry._2, encoder)
      encoder.flush()
      val json = new String(bao.toByteArray, "UTF-8")
      println(json)
    }
  }
}
