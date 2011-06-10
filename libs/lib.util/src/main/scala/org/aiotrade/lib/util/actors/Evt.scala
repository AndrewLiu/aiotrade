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
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.avro.JsonDecoder
import org.aiotrade.lib.avro.JsonEncoder
import org.aiotrade.lib.avro.ReflectData
import org.aiotrade.lib.avro.ReflectDatumReader
import org.aiotrade.lib.avro.ReflectDatumWriter
import org.aiotrade.lib.util.ClassHelper
import org.apache.avro.Schema
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import scala.collection.mutable

/**
 * A sketch of business message protocals (APIs) design
 * 
 * 'Evt' is actually the evt definition, or the API definition
 * 'T' is the type of evt value
 * '(Int, T)' is the type of each evt message (EvtMessage)
 * 
 * Instead of using case class as evt message, the design here uses a plain Tuple
 * as the evt message, so:
 * 1. It's easier to keep off from possible serialization issue for lots concrete classes
 * 2. The meta data such as 'doc', 'tpeClass' are stored in evt definition for
 *    each type of evt. Only the message data is stored in each evt message
 * 3. The serialization size of evt message is smaller.
 * 4. We can pattern match it via named extract, or via regural Tuple match 
 * 
 * @param [T] the type of the evt body. 
 *        For list, although we support collection.Seq[_] type of T, but it's better 
 *        to use JVM type safed Array[_], since we here have to check all elements 
 *        of value to make sure the pattern match won't be cheated. 
 *        For varargs, use type safed Tuple instead of List.
 *        @see unapply
 * @param tag an unique id in int for this type of Evt
 * @param doc the document of this Evt
 * 
 * @author Caoyuan Deng
 */

case class Msg[T](tag: Int, value: T)

abstract class Evt[T](val tag: Int, val doc: String = "", schemaJson: String = null)(implicit m: Manifest[T]) {
  type ValType = T
  
  private val valueTypeParams = m.typeArguments map (_.erasure)
  val valueClass = m.erasure
  
  assert(!Evt.tagToEvt.contains(tag), "Tag: " + tag + " already existed!")
  Evt.tagToEvt(tag) = this
    
  /**
   * Avro schema: we've implement a reflect schema.
   * you can also override 'schemaJson' to ge custom json
   */
  lazy val schema: Schema = {
    if (schemaJson != null) {
      Schema.parse(schemaJson)
    } else {
      ReflectData.get.getSchema(valueClass)
    }
  }
  
  /**
   * Return the evt message that is to be passed to. the evt message is wrapped in
   * a tuple in form of (tag, evtValue)
   */
  def apply(evtVal: T): Msg[T] = Msg[T](tag, evtVal)

  /** 
   * @Note Since T is erasued after compiled, should check type of evt message 
   * via Manifest instead of v.isInstanceOf[T]
   * And, don't write as unapply(evtMsg: (Int, T)), which will confuse the compiler 
   * to generate wrong code for match {case .. case ..}
   */
  def unapply(evtMsg: Any): Option[T] = evtMsg match {
    case Msg(`tag`, value: T) if ClassHelper.isInstance(valueClass, value) =>
      // we will do 1-level type arguments check, and won't deep check t's type parameter anymore
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
    "Evt(tag=" + tag + ", tpe=" + valueClass + ", doc=\"" + doc + "\")"
  }
}

object Evt {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val tagToEvt = new mutable.HashMap[Int, Evt[_]]
  private val NullSchema = Schema.create(Schema.Type.NULL)
  
  def valueClassOf(tag: Int): Option[Class[_]] = tagToEvt.get(tag) map {_.valueClass}
  def schemaOf(tag: Int): Schema = tagToEvt.get(tag) map {_.schema} getOrElse NullSchema
  def tagToSchema = tagToEvt map {x => (x._1 -> schemaOf(x._1).toString)}
 
  def toAvro[T](msg: Msg[T]): Array[Byte] = {
    val tag = msg.tag
    val schema = schemaOf(tag)
    try {
      val bao = new ByteArrayOutputStream()
      val out = new DataOutputStream(bao)
      out.writeInt(msg.tag)
    
      val encoder = EncoderFactory.get.binaryEncoder(out, null)
      val writer = ReflectDatumWriter[T](schema)
      writer.write(msg.value, encoder)
      encoder.flush()
      bao.toByteArray
    } catch {
      case ex => log.log(Level.WARNING, ex.getMessage, ex); Array[Byte]()
    }
  }
  
  def toJson[T](msg: Msg[T]): Array[Byte] = {
    val tag = msg.tag
    val schema = schemaOf(tag)
    try {
      val bao = new ByteArrayOutputStream()
      val out = new DataOutputStream(bao)
      out.writeInt(tag)
    
      val encoder = JsonEncoder(schema, out)
      val writer = ReflectDatumWriter[T](schema)
      writer.write(msg.value, encoder)
      encoder.flush()
      bao.toByteArray
    } catch {
      case ex => log.log(Level.WARNING, ex.getMessage, ex); Array[Byte]()
    }
  }
  
  def fromAvro(bytes: Array[Byte]): Option[Any] = {
    val in = new DataInputStream(new ByteArrayInputStream(bytes))
    val tag = readTag(in)
    valueClassOf(tag) match {
      case Some(x) => 
        try {
          Option(fromValueAvro(x, schemaOf(tag), in))
        } catch {
          case ex => log.log(Level.WARNING, ex.getMessage, ex); None
        }
      case _ => None
    }
  }
  
  @throws(classOf[IOException])
  private def fromValueAvro[T](valueClass: Class[T], schema: Schema, in: InputStream): T = {
    val decoder = DecoderFactory.get.binaryDecoder(in, null)
    val reader = ReflectDatumReader[T](schema)
    reader.read(null.asInstanceOf[T], decoder)
  }
  
  @throws(classOf[IOException])
  def fromJson(bytes: Array[Byte]): Option[Any] = {
    val in = new DataInputStream(new ByteArrayInputStream(bytes))
    val tag = readTag(in)
    valueClassOf(tag) match {
      case Some(x) => 
        try {
          Option(fromValueJson(x, schemaOf(tag), in))
        } catch {
          case ex => log.log(Level.WARNING, ex.getMessage, ex); None
        }
      case _ => None
    }
  }
  
  @throws(classOf[IOException])
  private def fromValueJson[T](valueClass: Class[T], schema: Schema, in: InputStream): T = {
    val decoder = JsonDecoder(schema, in)
    val reader = ReflectDatumReader[T](schema)
    reader.read(null.asInstanceOf[T], decoder)
  }
  
  private def readTag(in: DataInputStream): Int = {
    try {
      in.readInt
    } catch {
      case ex => log.log(Level.WARNING, ex.getMessage, ex); Int.MinValue
    }
  }
  
  /**
   * A utility method to see the reflected schame of a class
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
      sb.append("\n\nValue Class:\n    ").append(evt.valueClass.getName)
      sb.append("\n\nParamters:  \n    ").append(evt.doc)
      sb.append("\n\nSchema:     \n    ").append(evt.schema.toString)
      sb.append("\n\n")
    }
    sb.append("\n================ End of APIs ==============")
    
    sb.toString
  }
  
  // -- simple test
  def main(args: Array[String]) {
    testMatch
    testObject
    testVmap
    
    println(prettyPrint(tagToEvt map (_._2)))
  }
  
  private def testMatch {
    object StrEvt extends Evt[String](-1)
    object IntEvt extends Evt[Int](-2)
    object ArrEvt extends Evt[Array[String]](-3)
    object LstEvt extends Evt[List[String]](-4)
    object MulEvt extends Evt[(Int, String, Double)](-5, "id, name, value", schemaJson = """{"type": "array", "items":["int", "double", "string"]}""")
    object EmpEvt extends Evt(-10) // T will be AnyRef
    object EmpEvt2 extends Evt[Unit](-11)
    
    println("\n==== apis: ")
    println(StrEvt)
    println(IntEvt)
    println(ArrEvt)
    println(LstEvt)
    println(MulEvt)
    
    val goodEvtMsgs = List(
      EmpEvt,
      EmpEvt2(),
      StrEvt("a"),
      StrEvt("b"),
      IntEvt(8),
      ArrEvt(Array("a", "b")),
      LstEvt(List("a", "b")),
      MulEvt(8, "a", 8.0),
      MulEvt(8, "a", 8)
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
      case EmpEvt => println("Matched emp evt"); true
      case Msg(EmpEvt2.tag, aval: EmpEvt2.ValType) => println("Matched emp evt2"); true
      case Msg(StrEvt.tag, aval: StrEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(IntEvt.tag, aval: IntEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(ArrEvt.tag, aval: ArrEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(LstEvt.tag, aval: LstEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(MulEvt.tag, aval: MulEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case _ => println("Unmatched: " + v); false
    }
    
    /** But we'd like a more concise approach: */
    def advancedMatch(v: Any) = v match {
      case EmpEvt => println("Matched emp evt"); true
      case EmpEvt2(_) => println("Matched emp evt2"); true
      case StrEvt("a")  => println("Matched with value equals: " + v + " => " + "a"); true
      case StrEvt(aval) => println("Matched: " + v + " => " + aval); true
      case IntEvt(aval) => println("Matched: " + v + " => " + aval); true
      case ArrEvt(aval) => println("Matched: " + v + " => " + aval); true
      case LstEvt(aval@List(a: String, b: String)) => println("Matched: " + v + " => " + aval); true
      case MulEvt(aint: Int, astr: String, adou: Double) => println("Matched: " + v + " => (" + aint + ", " + astr + ", " + adou + ")"); true
      case _ => println("Unmatched: " + v); false
    }
  }
  
  private def testObject {
    object TestDataEvt extends Evt[TestData](-100)

    printSchema(classOf[TestData])
    
    val data = TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f))
    val evt = TestDataEvt(data)

    val avroBytes = toAvro(evt)
    val avroDatum = fromAvro(avroBytes)
    println(avroDatum)
    
    val jsonBytes = toJson(evt)
    val jsonDatum = fromJson(jsonBytes)
    println(jsonDatum)    
  }
  
  private def testVmap {
    object TestVmapEvt extends Evt[collection.Map[String, Array[_]]](-101, schemaJson = """
      {"type":"map","values":{"type":"array","items":["long","double","string",{"type":"record","name":"TestData","namespace":"org.aiotrade.lib.util.actors.Evt$","fields":[{"name":"a","type":"string"},{"name":"b","type":"int"},{"name":"c","type":"double"},{"name":"d","type":{"type":"array","items":"float"}}]}]}}
    """)
    
    val vmap = new mutable.HashMap[String, Array[_]]
    vmap.put(".", Array(1L, 2L, 3L))
    vmap.put("a", Array(1.0, 2.0, 3.0))
    vmap.put("b", Array("a", "b", "c"))
    vmap.put("c", Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f))))
    
    val evt = TestVmapEvt(vmap)

    val avroBytes = toAvro(evt)
    val avroDatum = fromAvro(avroBytes).get.asInstanceOf[collection.Map[String, Array[_]]]
    println(avroDatum)
    avroDatum foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
    
    val jsonBytes = toJson(evt)
    val jsonDatum = fromJson(jsonBytes).get.asInstanceOf[collection.Map[String, Array[_]]]
    println(jsonDatum)    
    jsonDatum foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
  }
  
  private case class TestData(a: String, b: Int, c: Double, d: Array[Float]) {
    def this() = this(null, 0, 0.0, Array())
  }
}
