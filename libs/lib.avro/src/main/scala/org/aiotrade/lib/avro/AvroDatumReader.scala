package org.aiotrade.lib.avro

import java.io.IOException

import org.aiotrade.lib.collection.ArrayList
import org.apache.avro.AvroRuntimeException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericArray
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericFixed
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.IndexedRecord
import org.apache.avro.io.DatumReader
import org.apache.avro.io.Decoder
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.ResolvingDecoder
import org.apache.avro.util.Utf8
import org.apache.avro.util.WeakIdentityHashMap
import scala.collection.mutable

object AvroDatumReader {
  private val RESOLVER_CACHE =
    new ThreadLocal[java.util.Map[Schema,java.util.Map[Schema,ResolvingDecoder]]]() {
      override protected def initialValue() = new WeakIdentityHashMap[Schema, java.util.Map[Schema,ResolvingDecoder]]()
    }

  /** Skip an instance of a schema. */
  @throws(classOf[IOException])
  def skip(schema: Schema, in: Decoder) {
    import Schema.Type._
    schema.getType match {
      case RECORD =>
        val fields = schema.getFields.iterator
        while (fields.hasNext) {
          skip(fields.next.schema, in)
        }
      case ENUM =>
        in.readInt
      case ARRAY =>
        val elementType = schema.getElementType
        var l = in.skipArray
        while (l > 0) {
          var i = 0L
          while (i < l) {
            skip(elementType, in)
            i += 1
          }
          l = in.skipArray
        }
      case MAP =>
        val value = schema.getValueType
        var l = in.skipMap
        while (l > 0) {
          var i = 0L
          while (i < l) {
            in.skipString
            skip(value, in)
            i += 1
          }
          l = in.skipMap
        }
      case UNION =>
        skip(schema.getTypes.get(in.readIndex.asInstanceOf[Int]), in)
      case FIXED =>
        in.skipFixed(schema.getFixedSize)
      case STRING =>
        in.skipString
      case BYTES =>
        in.skipBytes
      case INT =>     in.readInt
      case LONG =>    in.readLong
      case FLOAT =>   in.readFloat
      case DOUBLE =>  in.readDouble
      case BOOLEAN => in.readBoolean
      case NULL =>
      case _ => throw new RuntimeException("Unknown type: "+schema)
    }
  }

  def apply[R]() = new AvroDatumReader[R](null, null)
  /** Construct where the writer's and reader's schemas are the same. */
  def apply[R](schema: Schema) = new AvroDatumReader[R](schema, schema)
  def apply[R](reader: Schema, writer: Schema) = new AvroDatumReader[R](reader, writer)
}

/**
 * @param actual  writer's schema
 * @param expected  reader's schema
 */
import AvroDatumReader._
class AvroDatumReader[R](private var actual: Schema, private var expected: Schema) extends DatumReader[R] {

  override def setSchema(writer: Schema) {
    this.actual = writer
    if (expected == null) {
      expected = actual
    }
    threadResolver.set(null)
  }

  /** Set the reader's schema. */
  @throws(classOf[IOException])
  def setExpected(reader: Schema) {
    this.expected = reader
    threadResolver.set(null)
  }

  private final val threadResolver = new ThreadLocal[ResolvingDecoder]()

  @throws(classOf[IOException])
  private def getResolver(actual: Schema, expected: Schema): ResolvingDecoder = {
    threadResolver.get match {
      case null =>
        val cache = RESOLVER_CACHE.get.get(actual) match {
          case null =>
            val cache = new WeakIdentityHashMap[Schema, ResolvingDecoder]()
            RESOLVER_CACHE.get().put(actual, cache)
            cache
          case x => x
        }
        val resolver = cache.get(expected) match {
          case null =>
            val resolver = DecoderFactory.get.resolvingDecoder(Schema.applyAliases(actual, expected), expected, null)
            cache.put(expected, resolver)
            resolver
          case x => x
        }
        threadResolver.set(resolver)
        resolver
      case resolver => resolver
    }
  }

  @throws(classOf[IOException])
  def read(reuse: R, in: Decoder): R = {
    val resolver = getResolver(actual, expected)
    resolver.configure(in)
    val result = read(reuse, expected, resolver).asInstanceOf[R]
    resolver.drain
    result
  }

  /** Called to read data.*/
  @throws(classOf[IOException])
  protected def read(old: Any, expected: Schema, in: ResolvingDecoder): Any = {
    import Schema.Type._
    expected.getType match {
      case RECORD =>  readRecord(old.asInstanceOf[AnyRef], expected, in)
      case ENUM =>    readEnum(expected, in)
      case ARRAY =>   readArray(old.asInstanceOf[AnyRef], expected, in)
      case MAP =>     readMap(old.asInstanceOf[AnyRef], expected, in)
      case UNION =>   read(old, expected.getTypes.get(in.readIndex), in)
      case FIXED =>   readFixed(old.asInstanceOf[AnyRef], expected, in)
      case STRING =>  readString(old.asInstanceOf[AnyRef], expected, in)
      case BYTES =>   readBytes(old.asInstanceOf[AnyRef], in)
      case INT =>     readInt(old, expected, in)
      case LONG =>    in.readLong
      case FLOAT =>   in.readFloat
      case DOUBLE =>  in.readDouble
      case BOOLEAN => in.readBoolean
      case NULL =>    in.readNull; null
      case _ => throw new AvroRuntimeException("Unknown type: " + expected)
    }
  }

  /** Called to read a record instance. May be overridden for alternate record
   * representations.*/
  @throws(classOf[IOException])
  protected def readRecord(old: AnyRef, expected: Schema, in: ResolvingDecoder): AnyRef = {
    val record = newRecord(old, expected)

    for (field <- in.readFieldOrder) {
      val pos = field.pos
      val name = field.name
      val oldDatum = if (old != null) getField(record, name, pos) else null
      setField(record, name, pos, read(oldDatum, field.schema, in))
    }

    record
  }

  /** Called by the default implementation of {@link #readRecord} to set a
   * record fields value to a record instance.  The default implementation is
   * for {@link IndexedRecord}.*/
  protected def setField(record: AnyRef, name: String, position: Int, value: Any) {
    record.asInstanceOf[IndexedRecord].put(position, value)
  }

  /** Called by the default implementation of {@link #readRecord} to retrieve a
   * record field value from a reused instance.  The default implementation is
   * for {@link IndexedRecord}.*/
  protected def getField(record: AnyRef, name: String, position: Int): AnyRef = {
    record.asInstanceOf[IndexedRecord].get(position)
  }

  /** Called by the default implementation of {@link #readRecord} to remove a
   * record field value from a reused instance.  The default implementation is
   * for {@link GenericRecord}.*/
  protected def removeField(record: AnyRef, field: String, position: Int) {
    record.asInstanceOf[GenericRecord].put(position, null)
  }

  /** Called to read an enum value. May be overridden for alternate enum
   * representations.  By default, returns a GenericEnumSymbol. */
  @throws(classOf[IOException])
  protected def readEnum(expected: Schema, in: Decoder): AnyRef = {
    createEnum(expected.getEnumSymbols.get(in.readEnum), expected)
  }

  /** Called to create an enum value. May be overridden for alternate enum
   * representations.  By default, returns a GenericEnumSymbol. */
  protected def createEnum(symbol: String, schema: Schema): AnyRef = {
    new GenericData.EnumSymbol(schema, symbol)
  }

  /** Called to read an array instance.  May be overridden for alternate array
   * representations.*/
  @throws(classOf[IOException])
  protected def readArray(old: AnyRef, expected: Schema, in: ResolvingDecoder): AnyRef = {
    val expectedType = expected.getElementType
    var l = in.readArrayStart
    var base = 0L
    val result = if (l > 0) {
      val array = newArray(old, l.toInt, expected)
      do {
        var i = 0L
        while (i < l) {
          addToArray(array, base + i, read(peekArray(array), expectedType, in))
          i += 1
        }
        base += l
      } while ({l = in.arrayNext; l > 0})
      array
    } else {
      newArray(old, 0, expected)
    }
    result.toArray
  }

  /** Called by the default implementation of {@link #readArray} to retrieve a
   * value from a reused instance.  The default implementation is for {@link
   * GenericArray}.*/
  protected def peekArray(array: AnyRef): AnyRef = {
    array match {
      case x: GenericArray[AnyRef] => x.peek
      case _ => null
    }
  }

  /** Called by the default implementation of {@link #readArray} to add a
   * value.  The default implementation is for {@link Collection}.*/
  protected def addToArray(array: AnyRef, pos: Long, e: Any) {
    array.asInstanceOf[ArrayList[Any]] += e
  }

  /** Called to read a map instance.  May be overridden for alternate map
   * representations.*/
  @throws(classOf[IOException])
  protected def readMap(old: AnyRef, expected: Schema, in: ResolvingDecoder): AnyRef = {
    val eValue = expected.getValueType
    var l = in.readMapStart
    val map = newMap(old, l.toInt)
    if (l > 0) {
      do {
        var i = 0
        while (i < l) {
          addToMap(map, readString(null, in), read(null, eValue, in))
          i += 1
        }
      } while ({l = in.mapNext; l > 0})
    }
    map
  }

  /** Called by the default implementation of {@link #readMap} to add a
   * key/value pair.  The default implementation is for {@link Map}.*/
  protected def addToMap(map: AnyRef, key: Any, value: Any) {
    map.asInstanceOf[mutable.Map[Any, Any]] += (key -> value)
  }

  /** Called to read a fixed value. May be overridden for alternate fixed
   * representations.  By default, returns {@link GenericFixed}. */
  @throws(classOf[IOException])
  protected def readFixed(old: AnyRef, expected: Schema, in: Decoder): AnyRef = {
    val fixed = createFixed(old, expected).asInstanceOf[GenericFixed]
    in.readFixed(fixed.bytes, 0, expected.getFixedSize)
    fixed
  }

  /** Called to create an fixed value. May be overridden for alternate fixed
   * representations.  By default, returns {@link GenericFixed}. */
  protected def createFixed(old: AnyRef, schema: Schema): AnyRef = {
    old match {
      case x: GenericFixed if x.bytes.length == schema.getFixedSize => old
      case _ => new GenericData.Fixed(schema)
    }
  }

  /** Called to create an fixed value. May be overridden for alternate fixed
   * representations.  By default, returns {@link GenericFixed}. */
  protected def createFixed(old: AnyRef, bytes: Array[Byte], schema: Schema): AnyRef = {
    val fixed = createFixed(old, schema).asInstanceOf[GenericFixed]
    System.arraycopy(bytes, 0, fixed.bytes(), 0, schema.getFixedSize)
    fixed
  }
  /**
   * Called to create new record instances. Subclasses may override to use a
   * different record implementation. The returned instance must conform to the
   * schema provided. If the old object contains fields not present in the
   * schema, they should either be removed from the old object, or it should
   * create a new instance that conforms to the schema. By default, this returns
   * a {@link GenericData.Record}.
   */
  protected def newRecord(old: AnyRef, schema: Schema): AnyRef = {
    old match {
      case x: IndexedRecord if x.getSchema == schema => x
      case _ => new GenericData.Record(schema)
    }
  }

  /** Called to create new array instances.  Subclasses may override to use a
   * different array implementation.  By default, this returns a {@link
   * Array}.*/
  protected def newArray(old: AnyRef, size: Int, schema: Schema): ArrayList[_] = {
    import Schema.Type._
    schema.getElementType.getType match {
      case RECORD | ARRAY | MAP | UNION  | FIXED | STRING | BYTES | NULL => new ArrayList[AnyRef](size)
      case INT =>     new ArrayList[Int](size)
      case ENUM =>    new ArrayList[Int](size)
      case LONG =>    new ArrayList[Long](size)
      case FLOAT =>   new ArrayList[Float](size)
      case DOUBLE =>  new ArrayList[Double](size)
      case BOOLEAN => new ArrayList[Boolean](size)
      case _ => throw new AvroRuntimeException("Unknown type: " + expected)
    }
  }

  /** Called to create new array instances.  Subclasses may override to use a
   * different map implementation.  By default, this returns a {@link
   * HashMap}.*/
  protected def newMap(old: AnyRef, size: Int): AnyRef = {
    old match {
      case x: mutable.HashMap[_, _] => x.clear; old
      case _ => new mutable.HashMap[AnyRef, AnyRef]
    }
  }

  /** Called to read strings.  Subclasses may override to use a different
   * string representation.  By default, this calls {@link
   * #readString(AnyRef,Decoder)}.*/
  @throws(classOf[IOException])
  protected def readString(old: AnyRef, expected: Schema, in: Decoder): String = {
    readString(old, in)
  }
  /** Called to read strings.  Subclasses may override to use a different
   * string representation.  By default, this calls {@link
   * Decoder#readString(Utf8)}.*/
  @throws(classOf[IOException])
  protected def readString(old: AnyRef, in: Decoder): String = {
    in.readString(if (old.isInstanceOf[Utf8]) old.asInstanceOf[Utf8] else null).toString
  }

  /** Called to create a string from a default value.  Subclasses may override
   * to use a different string representation.  By default, this calls {@link
   * Utf8#Utf8(String)}.*/
  protected def createString(value: String): AnyRef = {
    new Utf8(value)
  }

  /** Called to read byte arrays.  Subclasses may override to use a different
   * byte array representation.  By default, this calls {@link
   * Decoder#readBytes(ByteBuffer)}.*/
  @throws(classOf[IOException])
  protected def readBytes(old: AnyRef, in: Decoder): java.nio.ByteBuffer = {
    old match {
      case null => in.readBytes(null)
      case x: java.nio.ByteBuffer => in.readBytes(x)
      case x: Array[Byte] => in.readBytes(java.nio.ByteBuffer.wrap(x))
    }
  }

  /** Called to read integers.  Subclasses may override to use a different
   * integer representation.  By default, this calls {@link
   * Decoder#readInt()}.*/
  @throws(classOf[IOException])
  protected def readInt(old: Any, expected: Schema, in: Decoder): Int = {
    in.readInt
  }

  /** Called to create byte arrays from default values.  Subclasses may
   * override to use a different byte array representation.  By default, this
   * calls {@link ByteBuffer#wrap(byte[])}.*/
  protected def createBytes(value: Array[Byte]): AnyRef = { 
    java.nio.ByteBuffer.wrap(value)
  }

}

