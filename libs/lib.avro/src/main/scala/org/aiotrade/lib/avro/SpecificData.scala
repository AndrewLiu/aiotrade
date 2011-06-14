package org.aiotrade.lib.avro

import java.lang.reflect.ParameterizedType

import org.apache.avro.Schema
import org.apache.avro.Protocol
import org.apache.avro.AvroRuntimeException
import org.apache.avro.AvroTypeException
import org.apache.avro.Schema.Type
import org.apache.avro.generic.GenericData

object SpecificData {
  private val INSTANCE = new SpecificData

  private val NO_CLASS = (new Object {}).getClass
  private val NULL_SCHEMA = Schema.create(Schema.Type.NULL)

  /** Return the singleton instance. */
  def get(): SpecificData = INSTANCE
}

/** Utilities for generated Java classes and interfaces. */
class SpecificData protected () extends GenericData {
  import ClassHelper._
  import SpecificData._
  import Schema.Type._

  private val classCache = new java.util.concurrent.ConcurrentHashMap[String, Class[_]]()
  private val schemaCache = new java.util.WeakHashMap[java.lang.reflect.Type, Schema]()

  override protected def isEnum(datum: Object): Boolean = {
    datum.isInstanceOf[Enum[_]] || super.isEnum(datum)
  }


  /** Return the class that implements a schema, or null if none exists. */
  def getClass(schema: Schema): Class[_] = {
    schema.getType match {
      case FIXED | RECORD | ENUM =>
        val name = schema.getFullName
        val c = classCache.get(name) match {
          case null =>
            try {
              Class.forName(getClassName(schema))
            } catch {
              case e: ClassNotFoundException => NO_CLASS
            }
          case x => x
        }
        classCache.put(name, c)
        
        if (c == NO_CLASS) null else c
      case ARRAY => classOf[java.util.List[_]]
      case MAP => classOf[java.util.Map[_, _]]
      case UNION =>
        val types = schema.getTypes     // elide unions with null
        if ((types.size == 2) && types.contains(NULL_SCHEMA))
          getClass(types.get(if (types.get(0).equals(NULL_SCHEMA)) 1 else 0))
        else
          classOf[Object]
      case STRING =>  classOf[java.lang.CharSequence]
      case BYTES =>   classOf[java.nio.ByteBuffer]
      case INT =>     java.lang.Integer.TYPE
      case LONG =>    java.lang.Long.TYPE
      case FLOAT =>   java.lang.Float.TYPE
      case DOUBLE =>  java.lang.Double.TYPE
      case BOOLEAN => java.lang.Boolean.TYPE
      case NULL =>    java.lang.Void.TYPE
      case _ => throw new AvroRuntimeException("Unknown type: " + schema)
    }
  }

  /** Returns the Java class name indicated by a schema's name and namespace. */
  def getClassName(schema: Schema): String = {
    val namespace = schema.getNamespace
    val name = schema.getName
    if (namespace == null) {
      name
    } else {
      val dot = if (namespace.endsWith("$")) "" else "."
      namespace + dot + name
    }
  }

  /** Find the schema for a Java type. */
  def getSchema(tpe: java.lang.reflect.Type): Schema = {
    schemaCache.get(tpe) match {
      case null =>
        val schema = createSchema(tpe, new java.util.LinkedHashMap[String, Schema]())
        schemaCache.put(tpe, schema)
        schema
      case x => x
    }
  }

  /** Create the schema for a Java type. */
  protected def createSchema(tpe: java.lang.reflect.Type, names: java.util.Map[String, Schema]): Schema = {
    tpe match {
      case c: Class[_] if CharSequenceClass.isAssignableFrom(c) => Schema.create(Type.STRING)
      case ByteBufferClass => Schema.create(Type.BYTES)
      case VoidType    | JVoidClass => Schema.create(Type.NULL)
      case IntegerType | IntClass     | JIntegerClass => Schema.create(Type.INT)
      case LongType    | LongClass    | JLongClass    => Schema.create(Type.LONG)
      case FloatType   | FloatClass   | JFloatClass   => Schema.create(Type.FLOAT)
      case DoubleType  | DoubleClass  | JDoubleClass  => Schema.create(Type.DOUBLE)
      case BooleanType | BooleanClass | JBooleanClass => Schema.create(Type.BOOLEAN)
/*       case c: Class[_] if ClassHelper.isTupleClass(c) =>
        val params = c.getTypeParameters
 */      case ptype: ParameterizedType =>
        val raw = ptype.getRawType.asInstanceOf[Class[_]]
        val params = ptype.getActualTypeArguments

        if (JCollectionClass.isAssignableFrom(raw) || SeqClass.isAssignableFrom(raw)) { 
          // array
          if (params.length != 1) throw new AvroTypeException("No array type specified.")
          Schema.createArray(createSchema(params(0), names))

        } else if (JMapClass.isAssignableFrom(raw) || MapClass.isAssignableFrom(raw)) {   
          // map
          val key = params(0)
          val value = params(1)
          tpe match {
            case c: Class[_] if CharSequenceClass.isAssignableFrom(c) =>
              Schema.createMap(createSchema(value, names))
            case _ => throw new AvroTypeException("Map key class not CharSequence: " + key)
          }
          
        } else {
          createSchema(raw, names)
        }
      case c: Class[_] =>
        val fullName = c.getName
        val schema = names.get(fullName) match {
          case null =>
            try {
              c.getDeclaredField("SCHEMA$").get(null).asInstanceOf[Schema]
            } catch {
              case e: NoSuchFieldException => throw new AvroRuntimeException(e)
              case e: IllegalAccessException => throw new AvroRuntimeException(e)
            }
          case schema => schema
        }
        names.put(fullName, schema)
        schema
      case _ => throw new AvroTypeException("Unknown type: " + tpe)
    }
  }

  /** Return the protocol for a Java interface. */
  def getProtocol(iface: Class[_]): Protocol = {
    try {
      iface.getDeclaredField("PROTOCOL").get(null).asInstanceOf[Protocol]
    } catch {
      case e: NoSuchFieldException=> throw new AvroRuntimeException(e);
      case e: IllegalAccessException => throw new AvroRuntimeException(e);
    }
  }

  override def compare(o1: Object, o2: Object, s: Schema): Int = {
    s.getType match {
      case ENUM if !(o1.isInstanceOf[String]) =>               // not generic
        (o1.asInstanceOf[Enum[_]]).ordinal - (o2.asInstanceOf[Enum[_]]).ordinal
      case _ => super.compare(o1, o2, s)
    }
  }
}