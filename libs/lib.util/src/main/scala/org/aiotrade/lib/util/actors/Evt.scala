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
package org.aiotrade.lib.util.actors

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.avro.JsonDecoder
import org.aiotrade.lib.avro.JsonEncoder
import org.aiotrade.lib.avro.ReflectData
import org.aiotrade.lib.avro.ReflectDatumReader
import org.aiotrade.lib.avro.ReflectDatumWriter
import org.aiotrade.lib.util.ClassHelper
import org.apache.avro.Schema
import org.apache.avro.io.BinaryData
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import scala.collection.mutable

/**
 * A sketch of business message protocals (APIs) design
 * 
 * 'Evt' is actually the evt definition, or the API definition
 * 'T' is the type of evt value
 * 'Msg[T](Int, T)' is the type of each evt message
 * 
 * Instead of using case class as each evt , the design here uses combination of 
 * object Evt and case class Msg, so:
 * 1. It's easier to keep off from possible serialization issue for lots concrete classes
 * 2. The meta data such as 'doc', 'tpeClass' are stored in evt definition for
 *    each type of evt. Only the message data is stored in each evt message
 * 3. The serialization size of evt message is smaller.
 * 4. We can pattern match it via named extract, or via regural Tuple match 
 * 
 * @param [T] the type of the evt value. 
 *        For list, although we support collection.Seq[_] type of T, but it's better 
 *        to use JVM type safed Array[_], since we here have to check all elements 
 *        of value to make sure the pattern match won't be cheated. 
 *        For varargs, use type safed Tuple instead of List.
 *        @see unapply
 * @param tag an unique id in int for this type of Evt
 * @param doc the document of this Evt
 * @param schemaJson as the custom schema 
 * 
 * @author Caoyuan Deng
 */

case class Msg[T](tag: Int, value: T)

/**
 * We don't encourage to use 'object anApi extends Evt[T](..)' to define an Evt, instead,
 * use 'val anApi = Evt[T](..)' to define new api. Since object is something lazy val, 
 * which should be explicitly referred to invoke initializing code, that is, it may
 * not be regirtered in Evt.tagToEvt yet when you call its static 'apply', 'unapply' 
 * methods.
 */
final class Evt[T] private (val tag: Int, val doc: String = "", schemaJson: String)(implicit m: Manifest[T]) {
  type ValType = T
  type MsgType = Msg[T]
  
  assert(!Evt.tagToEvt.contains(tag), "Tag: " + tag + " already existed!")
  Evt.tagToEvt(tag) = this
    
  private val valueTypeParams = m.typeArguments map (_.erasure)
  val valueType: Class[T] = m.erasure.asInstanceOf[Class[T]]
  
  /**
   * Avro schema of evt value: we've implemented a reflect schema.
   * you can also override 'schemaJson' to get custom json
   */
  lazy val valueSchema: Schema = {
    if (schemaJson != null) {
      Schema.parse(schemaJson)
    } else {
      ReflectData.get.getSchema(valueType)
    }
  }
  
  /**
   * Return the evt message that is to be passed to. the evt message is wrapped in
   * a tuple in form of (tag, evtValue)
   */
  def apply(msgVal: T): Msg[T] = Msg[T](tag, msgVal)

  /** 
   * @Note Since T is erasued after compiled, should check type of evt message 
   * via Manifest instead of v.isInstanceOf[T]
   * 1. Don't write to unapply(evtMsg: Msg[T]), which will confuse the compiler 
   *    to generate wrong code for match {case .. case ..}
   * 2. Don't write to unapply(evtMsg: Msg[_]), which will cause sth like:
   *    msg match {case => StrEvt("") => ... } doesn't work
   */
  def unapply(evtMsg: Any): Option[T] = evtMsg match {
    case Msg(`tag`, value: T) if ClassHelper.isInstance(valueType, value) =>
      // we will do 1-level type arguments check, and won't deep check it's type parameter anymore
      value match {
        case x: collection.Seq[_] =>
          val t = valueTypeParams.head
          val vs = x.iterator
          while (vs.hasNext) {
            if (!ClassHelper.isInstance(t, vs.next)) 
              return None
          }
          Some(value)
        case x: Product if ClassHelper.isTuple(x) =>
          val vs = x.productIterator
          val ts = valueTypeParams.iterator
          while (vs.hasNext) {
            if (!ClassHelper.isInstance(ts.next, vs.next)) 
              return None
          }
          Some(value)
        case _ => Some(value)
      }
    case _ => None
  }
  
  override def toString = {
    "Evt(tag=" + tag + ", tpe=" + valueType + ", doc=\"" + doc + "\")"
  }
}

object Evt {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val tagToEvt = new mutable.HashMap[Int, Evt[_]]
  private val NullSchema = Schema.create(Schema.Type.NULL)
  
  private val AVRO = 0
  private val JSON = 1
  val NO_TAG = Int.MinValue
  
  def exists(tag: Int): Boolean = tagToEvt.get(tag).isDefined
  def evtOf(tag: Int): Option[Evt[_]] = tagToEvt.get(tag)
  def typeOf(tag: Int): Option[Class[_]] = tagToEvt.get(tag) map (_.valueType)
  def schemaOf(tag: Int): Option[Schema] = tagToEvt.get(tag) map (_.valueSchema)
  def tagToSchema = tagToEvt map {x => (x._1 -> schemaOf(x._1))}
 
  def toAvro[T](value: T, tag: Int): Array[Byte] = schemaOf(tag) match {
    case Some(schema) => encode(value, schema, AVRO)
    case None => Array[Byte]()
  }
  
  def toJson[T](value: T, tag: Int): Array[Byte] = schemaOf(tag) match {
    case Some(schema) => encode(value, schema, JSON)
    case None => Array[Byte]()
  }
  
  private def encode[T](value: T, schema: Schema, contentType: Int): Array[Byte] = {
    var out: ByteArrayOutputStream = null
    try {
      out = new ByteArrayOutputStream()
    
      val encoder = contentType match {
        case JSON => JsonEncoder(schema, out)
        case AVRO => EncoderFactory.get.binaryEncoder(out, null)
      }
      
      val writer = ReflectDatumWriter[T](schema)
      writer.write(value, encoder)
      encoder.flush()
      
      out.toByteArray
    } catch {
      case ex => log.log(Level.WARNING, ex.getMessage, ex); Array[Byte]()
    } finally {
      if (out != null) try {out.close} catch {case _ =>}
    }
  }
  
  def fromAvro(bytes: Array[Byte], tag: Int): Option[_] = evtOf(tag) match {
    case Some(evt) => decode(bytes, evt.valueSchema, evt.valueType, AVRO)
    case None => None
  }

  def fromJson(bytes: Array[Byte], tag: Int): Option[_] = evtOf(tag) match {
    case Some(evt) => decode(bytes, evt.valueSchema, evt.valueType, JSON)
    case None => None
  }
  
  @throws(classOf[IOException])
  private def decode[T](bytes: Array[Byte], schema: Schema, valueType: Class[T], contentType: Int): Option[T] = {
    var in: InputStream = null
    try {
      in = new ByteArrayInputStream(bytes)
      
      val decoder = contentType match {
        case JSON => JsonDecoder(schema, in)
        case AVRO => DecoderFactory.get.binaryDecoder(in, null)
      }
      
      val reader = ReflectDatumReader[T](schema)
      reader.read(null.asInstanceOf[T], decoder) match {
        case null => 
          import ClassHelper._
          valueType match {
            case UnitClass | JVoidClass  => Some(().asInstanceOf[T])
            case NullClass => Some(null.asInstanceOf[T])
            case _ => None
          }
        case value => Some(value)
      }
    } catch {
      case ex => log.log(Level.WARNING, ex.getMessage, ex); None
    } finally {
      if (in != null) try {in.close} catch {case _ =>}
    }
  }
  
  @throws(classOf[IOException])
  private def writeTag(tag: Int, out: OutputStream) {
    val buf = new Array[Byte](5) // max bytes is 5
    val len = BinaryData.encodeInt(tag, buf, 0)
    out.write(buf, 0, len)
  }
  
  /** @see org.apache.avro.io.DirectBinaryDecoder#readInt */
  @throws(classOf[IOException])
  private def readTag(in: InputStream): Int = {
    var n = 0
    var b = 0
    var shift = 0
    do {
      b = in.read()
      if (b >= 0) {
        n |= (b & 0x7F) << shift
        if ((b & 0x80) == 0) {
          return (n >>> 1) ^ -(n & 1) // back to two's-complement
        }
      } else {
        throw new EOFException()
      }
      shift += 7
    } while (shift < 32)
    
    throw new IOException("Invalid int encoding")
  }
  
  /**
   * A utility method to see the reflected schema of a class
   */
  def printSchema(x: Class[_]) {
    val schema = ReflectData.get.getSchema(x)
    println(schema)
  }
  
  def prettyPrint(evts: collection.Iterable[Evt[_]]): String = {
    val sb = new StringBuffer
    
    sb.append("\n================ APIs ==============")
    evts foreach {evt =>
      sb.append("\n==============================")
      sb.append("\n\nName:       \n    ").append(evt.getClass.getName)
      sb.append("\n\nValue Class:\n    ").append(evt.valueType.getName)
      sb.append("\n\nParamters:  \n    ").append(evt.doc)
      sb.append("\n\nSchema:     \n    ").append(evt.valueSchema.toString)
      sb.append("\n\n")
    }
    sb.append("\n================ End of APIs ==============")
    
    sb.toString
  }
  
  def apply[T: Manifest](tag: Int, doc: String = "", schemaJson: String = null) = new Evt[T](tag, doc, schemaJson)
  
  // -- simple test
  def main(args: Array[String]) {
    testMatch
    testObject
    testPrimitives
    testVmap
    
    println(prettyPrint(tagToEvt map (_._2)))
  }
  
  private def testMatch {
    import TestAPIs._

    println("\n==== apis: ")
    println(StringEvt)
    println(IntEvt)
    println(ArrayEvt)
    println(ListEvt)
    println(TupleEvt)
    
    val goodEvtMsgs = List(
      BadEmpEvt,
      EmpEvt(),
      StringEvt("a"),
      StringEvt("b"),
      IntEvt(8),
      ArrayEvt(Array("a", "b")),
      ListEvt(List("a", "b")),
      TupleEvt(8, "a", 8.0, Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f)))),
      TupleEvt(8, "a", 8, Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f))))
    )

    val badEvtMsgs = List(
      Msg(-1, 8),
      Msg(-2, "a"),
      Msg(-3, Array(8, "a")),
      Msg(-3, Array(8, 8)),
      Msg(-4, List(1, "a")),
      Msg(-5, (8, "a")),
      Msg(-5, (8, 8, 8))
    )
    
    println("\n==== good evt messages: ")
    goodEvtMsgs map println
    println("\n==== bad evt messages: ")
    badEvtMsgs  map println
    
    println("\n==== regular matched: ")
    assert(!(goodEvtMsgs map regularMatch).contains(false), "Test failed")
    println("\n==== regular unmatched: ")
    //assert(!(badEvtMsgs  map regularMatch).contains(true),  "Test failed")
    
    println("\n==== advanced matched: ")
    assert(!(goodEvtMsgs map advancedMatch).contains(false), "Test failed")
    println("\n==== advanced unmatched: ")
    assert(!(badEvtMsgs  map advancedMatch).contains(true),  "Test failed") 
    
    /** 
     * @TODO bad match on ValType, need more research
     * The regular match on those evts look like: 
     */
    def regularMatch(v: Any) = v match {
      case BadEmpEvt => println("Matched emp evt"); true
      case Msg(EmpEvt.tag, aval: EmpEvt.ValType) => println("Matched emp evt2"); true
      case Msg(StringEvt.tag, aval: StringEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(IntEvt.tag, aval: IntEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(ArrayEvt.tag, aval: ArrayEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(ListEvt.tag, aval: ListEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(TupleEvt.tag, aval: TupleEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case _ => println("Unmatched: " + v); false
    }
    
    /** But we'd like a more concise approach: */
    def advancedMatch(v: Any) = v match {
      case EmpEvt(_) => println("Matched emp evt2"); true
      case StringEvt("a")  => println("Matched with value equals: " + v + " => " + "a"); true
      case StringEvt(aval) => println("Matched: " + v + " => " + aval); true
      case IntEvt(aval) => println("Matched: " + v + " => " + aval); true
      case ArrayEvt(aval) => println("Matched: " + v + " => " + aval); true
      case ListEvt(aval@List(a: String, b: String)) => println("Matched: " + v + " => " + aval); true
      case TupleEvt(aint: Int, astr: String, adou: Double, xs: Array[TestData]) => println("Matched: " + v + " => (" + aint + ", " + astr + ", " + adou + ")"); true
      case BadEmpEvt => println("Matched emp evt"); true
      case _ => println("Unmatched: " + v); false
    }
  }
  
  private def testObject {
    import TestAPIs._
    
    printSchema(classOf[TestData])
    
    val data = TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f))
    val msg = TestDataEvt(data)
    msg match {
      case TestDataEvt(data1) => println("matched: " + data1)
      case _ => error("Failed to match")
    }
    testMsg(TestDataEvt(data))
  }
  
  private def testPrimitives {
    import TestAPIs._
    
    testMsg(EmpEvt())
    testMsg(IntEvt(1))
    testMsg(LongEvt(1L))
    testMsg(FloatEvt(1.0f))
    testMsg(DoubleEvt(1.0))
    testMsg(BooleanEvt(true))
    testMsg(StringEvt("abc"))
    testMsg(TupleEvt(1, "a", 100000L, Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f)))))
  }
  
  private def testMsg[T](msg: Msg[T]) = msg match {
    case Msg(tag, value) =>
      println(schemaOf(tag))

      val jsonBytes = toJson(value, tag)
      println(new String(jsonBytes, "UTF-8"))
      val jsonDatum = fromJson(jsonBytes, tag).get.asInstanceOf[T]
      println(jsonDatum)    

      val avroBytes = toAvro(value, tag)
      val avroDatum = fromAvro(avroBytes, tag).get.asInstanceOf[T]
      println(avroDatum)
  }

  private def testVmap {
    import TestAPIs._
    
    val vmap = new mutable.HashMap[String, Array[_]]
    vmap.put(".", Array(1L, 2L, 3L))
    vmap.put("a", Array(1.0, 2.0, 3.0))
    vmap.put("b", Array("a", "b", "c"))
    vmap.put("c", Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f))))
    
    val msg = TestVmapEvt(vmap)

    val avroBytes = toAvro(msg.value, msg.tag)
    val avroDatum = fromAvro(avroBytes, msg.tag).get.asInstanceOf[collection.Map[String, Array[_]]]
    println(avroDatum)
    avroDatum foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
    
    val jsonBytes = toJson(msg.value, msg.tag)
    val jsonDatum = fromJson(jsonBytes, msg.tag).get.asInstanceOf[collection.Map[String, Array[_]]]
    println(jsonDatum)    
    jsonDatum foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
  }
  
}

private[actors] object TestAPIs {
  
  val EmpEvt = Evt[Unit](-1)
  val IntEvt = Evt[Int](-2)
  val LongEvt = Evt[Long](-3)
  val FloatEvt = Evt[Float](-4)
  val DoubleEvt = Evt[Double](-5)
  val BooleanEvt = Evt[Boolean](-6)
  val StringEvt = Evt[String](-7)

  val ListEvt = Evt[List[String]](-10)
  val ArrayEvt = Evt[Array[String]](-11)
  val TupleEvt = Evt[(Int, String, Double, Array[TestData])](-12, "id, name, value")

  val BadEmpEvt = Evt(-13) // T will be AnyRef
  
  val TestDataEvt =  Evt[TestData](-100)
  val TestVmapEvt = Evt[collection.Map[String, Array[_]]](-101, schemaJson = """
    {"type":"map","values":{"type":"array","items":["long","double","string",
     {"type":"record","name":"TestData","namespace":"org.aiotrade.lib.util.actors.TestAPIs$",
       "fields":[
         {"name":"x1","type":"string"},
         {"name":"x2","type":"int"},
         {"name":"x3","type":"double"},
         {"name":"x4","type":{"type":"array","items":"float"}}
       ]}
     ]}}
  """)

  case class TestData(x1: String, x2: Int, x3: Double, x4: Array[Float]) {
    def this() = this(null, 0, 0.0, Array())
    override def toString = "TestData(" + x1 + "," + x2 + "," + x3 + "," + x4.mkString("[", ",", "]")
  }

}
