package org.aiotrade.lib.avro

import java.io.IOException

import org.apache.avro.Schema
import org.apache.avro.io.Encoder

object ReflectDatumWriter {
  def apply[T](root: Schema, data: ReflectData) = new ReflectDatumWriter[T](root, data)
  def apply[T](root: Schema) = new ReflectDatumWriter[T](root, ReflectData.get)
  def apply[T](data: ReflectData) = new ReflectDatumWriter[T](null, data)
  def apply[T]() = new ReflectDatumWriter[T](null, ReflectData.get)
  
  def apply[T](c: Class[T], data: ReflectData) = new ReflectDatumWriter[T](data.getSchema(c), data)
//  def apply[T](c: Class[T]) = apply[T](c, ReflectData.get)
}

/**
 * {@link org.apache.avro.io.DatumWriter DatumWriter} for existing classes
 * via Java reflection.
 */
class ReflectDatumWriter[T] protected (root: Schema, reflectData: ReflectData) extends SpecificDatumWriter[T](root, reflectData) {
  @throws(classOf[IOException])
  override protected def writeString(schema: Schema, datum: Any, out: Encoder) {
    val datum1 = if (schema.getProp(ReflectData.CLASS_PROP) != null) // Stringable annotated
      datum.toString                                       // call toString()
    else datum
    writeString(datum, out)
  }

  @throws(classOf[IOException])
  override protected def writeBytes(datum: Any, out: Encoder) {
    datum match {
      case x: scala.Array[Byte] => out.writeBytes(x)
      case _ => super.writeBytes(datum, out)
    }
  }

  @throws(classOf[IOException])
  override protected def write(schema: Schema, datum: Any, out: Encoder) {
    val datum1 = datum match {
      case x: Short => x.toInt
      case x: java.lang.Short => x.intValue
      case _ => datum
    }
    try {
      super.write(schema, datum, out);
    } catch {
      case ex: NullPointerException =>           // improve error message
        val result = new NullPointerException("in " + schema.getFullName + " " + ex.getMessage)
        result.initCause(if (ex.getCause == null) ex else ex.getCause)
        throw result
    }
  }

}
