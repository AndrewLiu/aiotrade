/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.avro

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Properties
import org.apache.avro.io.BinaryDecoder
import org.apache.avro.io.BinaryEncoder
import java.util.logging.Logger;

object MarshallerTest {

  val log = Logger.getLogger(this.getClass.getSimpleName)
  
  def main(args : Array[String]) {
//    testJavaCache
    testAvro
  }
  
  def testJavaCache {
    val test = new  ApacheAvroMarshallerTest()
    test.testBooleanArrayMarshalling
    test.testBooleanMarshalling
    test.testBytesMarshalling
    test.testDoubleArrayMarshalling
    test.testDoubleMarshalling
    test.testFloatArrayMarshalling
    test.testFloatMarshalling
    test.testIntArrayMarshalling
    test.testIntegerMarshalling
    test.testListMarshalling
    test.testLongArrayMarshalling
    test.testLongMarshalling
    test.testMapMarshalling
    test.testNullMarshalling
    test.testSetMarshalling
    test.testStringArrayMarshalling
    test.testStringMarshalling
    test.testListMapMarshalling
  }
  
  def testAvro() {
    val listIntDemo = List(1,2,3);
    val listStringDemo = List("a", "b", "hello world");
    val listMapDemo = List(Map(2->"q", 3->"b"))
    val mapListDemo = Map("2"->"hellowr", "3"->1)
    val setDemo = Set(1,2, "3")
    val stringArrayDemo = Array("a", "b", "c")
    val doubleArrayDemo = Array[Double](2.0,2.1)
    val floatArrayDemo = Array[Float](2.3f,2.4f)
    val intArrayDemo = Array[Integer](1,2)
    val longArrayDemo = Array[Long](3L,4L)
    val booleanDemo = Array[Boolean](true,false)
    
    var buffer : Array[Byte] = null
    
    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(listIntDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer))
  
    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(listStringDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer))

    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(listMapDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer))

    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(mapListDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer))

    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(setDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer))

    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(stringArrayDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer).asInstanceOf[Array[String]].mkString(","))

    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(doubleArrayDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer).asInstanceOf[Array[AnyRef]].mkString(","))

    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(floatArrayDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer).asInstanceOf[Array[AnyRef]].mkString(","))

    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(intArrayDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer).asInstanceOf[Array[AnyRef]].mkString(","))

    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(longArrayDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer).asInstanceOf[Array[AnyRef]].mkString(","))
    
    buffer = ScalaApacheAvroMarshaller().objectToByteBuffer(booleanDemo)
    println(ScalaApacheAvroMarshaller().objectFromByteBuffer(buffer).asInstanceOf[Array[AnyRef]].mkString(","))
  }
}
