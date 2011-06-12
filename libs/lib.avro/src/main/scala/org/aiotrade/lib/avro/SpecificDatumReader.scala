package org.aiotrade.lib.avro

import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap;

import org.apache.avro.Schema;

object SpecificDatumReader {
  /** 
   * Tag interface that indicates that a class has a one-argument constructor
   * that accepts a Schema.
   * @see SpecificDatumReader#newInstance
   */
  trait SchemaConstructable

  
  private val NO_ARG = Array[Class[_]]()
  private val SCHEMA_ARG = Array[Class[_]](classOf[Schema])
  private val CTOR_CACHE = new ConcurrentHashMap[Class[_], Constructor[_]]()


  /** Create an instance of a class.  If the class implements {@link
   * SchemaConstructable}, call a constructor with a {@link
   * org.apache.avro.Schema} parameter, otherwise use a no-arg constructor. */
  protected[avro] def newInstance(c: Class[AnyRef], s: Schema): AnyRef = {
    val useSchema = classOf[SchemaConstructable].isAssignableFrom(c)
    try {
      var constructor = CTOR_CACHE.get(c).asInstanceOf[Constructor[AnyRef]]
      if (constructor == null) {
        val args = if (useSchema) SCHEMA_ARG else NO_ARG
        constructor = c.getDeclaredConstructor(args :_*)
        constructor.setAccessible(true)
        CTOR_CACHE.put(c, constructor)
      }
      val args = if (useSchema) Array[AnyRef](s) else Array[AnyRef]()
      constructor.newInstance(args :_*)
    } catch {
      case ex: Exception => throw new RuntimeException(ex); null
    }
  }
  
  /**
   * @Note Enum.valueOf(c.asInstanceOf[Class[_ <: Enum[_]]], name) doesn't work in Scala
   */
  def enumValueOf[T <: Enum[T]](c: Class[_], name: String): Enum[_] =
    Enum.valueOf(c.asInstanceOf[Class[T]], name).asInstanceOf[Enum[_]]

  def apply[T](writer: Schema, reader: Schema, data: SpecificData): SpecificDatumReader[T] = new SpecificDatumReader[T](writer, reader, data) 
  def apply[T](writer: Schema, reader: Schema): SpecificDatumReader[T] = new SpecificDatumReader[T](writer, reader, SpecificData.get)
  /** Construct where the writer's and reader's schemas are the same. */
  def apply[T](schema: Schema): SpecificDatumReader[T] = new SpecificDatumReader[T](schema, schema, SpecificData.get)
  def apply[T](c: Class[T]): SpecificDatumReader[T] = apply[T](SpecificData.get.getSchema(c))
  def apply[T](): SpecificDatumReader[T] = new SpecificDatumReader[T](null, null, SpecificData.get)
}

/** {@link org.apache.avro.io.DatumReader DatumReader} for generated Java classes. */
class SpecificDatumReader[T] protected (writer: Schema, reader: Schema, data: SpecificData) extends GenericDatumReader[T](writer, reader, data) {
  import SpecificDatumReader._
  
  override protected def newRecord(old: Any, schema: Schema): Any = {
    val c = SpecificData.get.getClass(schema)
    if (c == null) {
      super.newRecord(old, schema) // punt to generic
    } else {
      if (c.isInstance(old)) old else newInstance(c.asInstanceOf[Class[AnyRef]], schema)
    }
  }

  override protected def createEnum(symbol: String, schema: Schema): Any = {
    val c = SpecificData.get.getClass(schema)
    if (c == null) {
      super.createEnum(symbol, schema)
    } else {
      enumValueOf(c, symbol)
    }
  }

  override protected def createFixed(old: Any, schema: Schema): Any = {
    val c = SpecificData.get.getClass(schema)
    if (c == null){
      super.createFixed(old, schema)
    } else {
      if (c.isInstance(old)) old else newInstance(c.asInstanceOf[Class[AnyRef]], schema)
    }
  }

}