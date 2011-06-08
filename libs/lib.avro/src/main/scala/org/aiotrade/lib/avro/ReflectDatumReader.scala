package org.aiotrade.lib.avro

import java.io.IOException
import java.lang.reflect.InvocationTargetException

import org.apache.avro.AvroRuntimeException
import org.apache.avro.Schema
import org.apache.avro.io.Decoder


object ReflectDatumReader {
  def apply[T](writer: Schema, reader: Schema, data: ReflectData) = new ReflectDatumReader[T](writer, reader, data)
  def apply[T](writer: Schema, reader: Schema) = new ReflectDatumReader[T](writer, reader, ReflectData.get)
  def apply[T](root: Schema) = new ReflectDatumReader[T](root, root, ReflectData.get)
  def apply[T](c: Class[T]) = apply[T](ReflectData.get.getSchema(c))
  def apply[T]() = new ReflectDatumReader[T](null, null, ReflectData.get)
}

/**
 * {@link org.apache.avro.io.DatumReader DatumReader} for existing classes via
 * Java reflection.
 */
class ReflectDatumReader[T] protected (writer: Schema, reader: Schema, data: ReflectData) extends SpecificDatumReader[T](writer, reader, data) {

  /** @TODO */
//  override protected def newArray(old: Any, size: Int, schema: Schema): Any = {
//    val collectionClass = ReflectData.getClassProp(schema, ReflectData.CLASS_PROP)
//    if (collectionClass != null) {
//      if (old.isInstanceOf[java.util.Collection[_]]) {
//        (old.asInstanceOf[java.util.Collection[_]]).clear
//        return old
//      }
//      if (collectionClass.isAssignableFrom(classOf[java.util.ArrayList[_]]))
//        return new java.util.ArrayList()
//      return SpecificDatumReader.newInstance(collectionClass.asInstanceOf[Class[AnyRef]], schema)
//    }
//    var elementClass = ReflectData.getClassProp(schema, ReflectData.ELEMENT_PROP)
//    if (elementClass == null) {
//      elementClass = ReflectData.get.getClass(schema.getElementType)
//    }
//    java.lang.reflect.Array.newInstance(elementClass, size)
//  }
//
//  override protected def addToArray(array: Any, pos: Long, e: Any) {
//    if (array.isInstanceOf[java.util.Collection[_]]) {
//      array.asInstanceOf[java.util.Collection[AnyRef]].add(e.asInstanceOf[AnyRef])
//    } else {
//      java.lang.reflect.Array.set(array, pos.toInt, e)
//    }
//  }

  override protected def peekArray(array: Any): Any = null

  @throws(classOf[IOException])
  override protected def readString(old: Any, s: Schema, in: Decoder): Any = {
    val value = readString(null, in).asInstanceOf[String]
    val c = ReflectData.getClassProp(s, ReflectData.CLASS_PROP)
    if (c != null)                                // Stringable annotated class
      try {                                       // use String-arg ctor
        return c.getConstructor(classOf[String]).newInstance(value)
      } catch {
        case ex: NoSuchMethodException => throw new AvroRuntimeException(ex)
        case ex: InstantiationException => throw new AvroRuntimeException(ex)
        case ex: IllegalAccessException => throw new AvroRuntimeException(ex)
        case ex: InvocationTargetException => throw new AvroRuntimeException(ex)
      }
    
    value
  }

  @throws(classOf[IOException])
  override protected def readString(old: Any, in: Decoder): Any = {
    super.readString(null, in).toString
  }

  override protected def createString(value: String): Any = value

  @throws(classOf[IOException])
  override protected def readBytes(old: Any, in: Decoder): Any = {
    val bytes = in.readBytes(null);
    val result = new Array[Byte](bytes.remaining)
    bytes.get(result)
    result
  }

  @throws(classOf[IOException])
  override protected def readInt(old: Any, expected: Schema, in: Decoder): Int = {
    val value = in.readInt()
    if (classOf[Short].getName == expected.getProp(ReflectData.CLASS_PROP))
      value.toShort
    else
      value
  }

}