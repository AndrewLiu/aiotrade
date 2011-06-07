/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aiotrade.lib.avro

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.GenericArrayType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Protocol
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.reflect.Nullable
import org.apache.avro.reflect.Union
import org.apache.avro.reflect.Stringable
import org.apache.avro.specific.FixedSize;

import com.thoughtworks.paranamer.CachingParanamer

object ReflectData {
  private val INSTANCE = new ReflectData()
  /** Return the singleton instance. */
  def get(): ReflectData =  INSTANCE

  /** {@link ReflectData} implementation that permits null field values.  The
   * schema generated for each field is a union of its declared tpe and
   * null. */
  object AllowNull extends ReflectData {
    override protected def createFieldSchema(field: Field, names: java.util.Map[String, Schema]): Schema = {
      val schema = super.createFieldSchema(field, names)
      makeNullable(schema)
    }
  }

  private val FIELD_CACHE = new java.util.concurrent.ConcurrentHashMap[Class[_], java.util.Map[String,Field]]()

  /** Return the named field of the provided class.  Implementation caches
   * values, since this is used at runtime to get and set fields. */
  protected def getField(c: Class[_], name: String): Field = {
    val fields = FIELD_CACHE.get(c) match {
      case null =>
        val fields = new java.util.concurrent.ConcurrentHashMap[String, Field]()
        FIELD_CACHE.put(c, fields)
        fields
      case fields => fields
    }
    
    fields.get(name) match {
      case null =>
        val field = findField(c, name)
        fields.put(name, field)
        field
      case field => field
    }
  }

  private def findField(c$: Class[_], name: String): Field = {
    var c = c$
    do {
      try {
        val f = c.getDeclaredField(name)
        f.setAccessible(true)
        return f
      } catch {
        case ex: NoSuchFieldException =>
      }
      c = c.getSuperclass
    } while (c != null)
    throw new AvroRuntimeException("No field named " + name + " in: " + c)
  }

  protected val CLASS_PROP = "java-class"
  protected val ELEMENT_PROP = "java-element-class"

  protected def getClassProp(schema: Schema, prop: String): Class[_] = {
    schema.getProp(prop) match {
      case null => null
      case name =>
        try {
          Class.forName(name)
        } catch {
          case ex: ClassNotFoundException => throw new AvroRuntimeException(ex)
        }
    }
  }

  private val BYTES_CLASS = Array[Byte]().getClass

  private val THROWABLE_MESSAGE = makeNullable(Schema.create(Schema.Type.STRING))

  /** Create and return a union of the null schema and the provided schema. */
  def makeNullable(schema: Schema): Schema = {
    Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), schema))
  }
}

/** Utilities to use existing Java classes and interfaces via reflection. */
import org.aiotrade.lib.util.ClassHelper._
import ReflectData._
class ReflectData protected () extends org.apache.avro.reflect.ReflectData {
  import Schema.Type._

  override
  protected def isRecord(datum: Object): Boolean = {
    if (datum == null) return false
    getSchema(datum.getClass).getType == Schema.Type.RECORD
  }

  override
  protected def isArray(datum: Object): Boolean = {
    datum.isInstanceOf[Collection[_]] || datum.getClass.isArray
  }

  override
  protected def isBytes(datum: Object): Boolean = {
    if (datum == null) return false
    val c = datum.getClass
    c.isArray && c.getComponentType == java.lang.Byte.TYPE
  }

  override
  protected def getRecordSchema(record: Object): Schema = {
    getSchema(record.getClass)
  }

  override
  def validate(schema: Schema, datum: Object): Boolean = {
    schema.getType match {
      case RECORD =>
        if (datum == null) return false
        val c = datum.getClass
        val fields = schema.getFields.iterator
        while (fields.hasNext) {
          val f = fields.next
          try {
            if (!validate(f.schema, ReflectData.getField(c, f.name).get(datum))) return false
          } catch {
            case ex: IllegalAccessException => throw new AvroRuntimeException(ex)
          }
        }

        true
      case ARRAY =>
        datum match {
          case xs: collection.Seq[_] => 
            val itr = xs.iterator
            while (itr.hasNext) {
              val element = itr.next.asInstanceOf[AnyRef]
              if (!validate(schema.getElementType, element)) return false
            }

            true
          case xs: java.util.Collection[_] => // collection
            val itr = xs.iterator
            while (itr.hasNext) {
              val element = itr.next.asInstanceOf[AnyRef]
              if (!validate(schema.getElementType, element)) return false
            }

            true
          case _ if datum.getClass.isArray => // array
            val length = java.lang.reflect.Array.getLength(datum)
            var  i = 0
            while (i < length) {
              if (!validate(schema.getElementType, java.lang.reflect.Array.get(datum, i))) return false
              i += 1
            }
            
            true
          case _ => false
        }
      case _ => super.validate(schema, datum)
    }
  }


  override def getClass(schema: Schema): Class[_] = {
    schema.getType match {
      case ARRAY =>
        val collectionClass = getClassProp(schema, CLASS_PROP)
        if (collectionClass != null) return collectionClass
        java.lang.reflect.Array.newInstance(getClass(schema.getElementType), 0).getClass
      case STRING => StringClass
      case BYTES =>  BYTES_CLASS
      case INT if JShortClass.getName.equals(schema.getProp(CLASS_PROP)) =>
        ShortType
      case _ => super.getClass(schema)
    }
  }

  override protected def createSchema(tpe: Type, names: java.util.Map[String, Schema]): Schema = {
    tpe match {
      case atype: GenericArrayType => // generic array
        atype.getGenericComponentType match {
          case ByteType => // byte array
            Schema.create(Schema.Type.BYTES)
          case component =>
            val result = Schema.createArray(createSchema(component, names))
            setElement(result, component)
            result
        }
      case ptype: ParameterizedType =>
        val raw = ptype.getRawType.asInstanceOf[Class[_]]
        val params = ptype.getActualTypeArguments
        if (JMapClass.isAssignableFrom(raw)) {
          // Map
          val key = params(0)
          val value = params(1)
          if (key != StringClass) throw new AvroTypeException("Map key class not String: " + key)
          Schema.createMap(createSchema(value, names))
        } else if (JCollectionClass.isAssignableFrom(raw)) {
          // Collection
          if (params.length != 1) throw new AvroTypeException("No array tpe specified.")
          val schema = Schema.createArray(createSchema(params(0), names))
          schema.addProp(CLASS_PROP, raw.getName)
          schema
        } else {
          super.createSchema(tpe, names)
        }
      case JShortClass | ShortType =>
        val result = Schema.create(Schema.Type.INT)
        result.addProp(CLASS_PROP, JShortClass.getName)
        result
      case c: Class[_] => // Class
        if (c.isPrimitive || NumberClass.isAssignableFrom(c) || c == JVoidClass || c == JBooleanClass) {
          // primitive
          return super.createSchema(tpe, names)
        }

        if (c.isArray) {
          // array
          val component = c.getComponentType
          if (component == ByteType) {
            // byte array
            return Schema.create(Schema.Type.BYTES)
          }
          val result = Schema.createArray(createSchema(component, names))
          setElement(result, component)
          return result
        }

        if (CharSequenceClass.isAssignableFrom(c)) {
          // String
          return Schema.create(Schema.Type.STRING)
        }

        val fullName = c.getName
        var schema = names.get(fullName)
        if (schema == null) {
          val name = c.getSimpleName
          val space = if (c.getEnclosingClass != null) {
            // nested class
            c.getEnclosingClass.getName + "$"
          } else {
            //if (c.getPackage == null) "" else c.getPackage.getName
            Option(c.getPackage) map (_.getName) getOrElse ""
          }
          val union = c.getAnnotation(classOf[Union])
          if (union != null) {
            // union annotated
            return getAnnotatedUnion(union, names)
          } else if (c.isAnnotationPresent(classOf[Stringable])) {
            // Stringable
            val result = Schema.create(Schema.Type.STRING)
            result.addProp(CLASS_PROP, c.getName)
            return result
          } else if (c.isEnum) {
            // Enum
            val symbols = new ArrayList[String]()
            val constants = c.getEnumConstants.asInstanceOf[Array[Enum[_]]]
            constants foreach {x => symbols.add(x.name)}
            schema = Schema.createEnum(name, null /* doc */, space, symbols)
          } else if (classOf[GenericFixed].isAssignableFrom(c)) {
            // fixed
            val size = c.getAnnotation(classOf[FixedSize]).value
            schema = Schema.createFixed(name, null /* doc */, space, size)
          } else if (classOf[IndexedRecord].isAssignableFrom(c)) {
            // specific
            return super.createSchema(tpe, names)
          } else {
            // record
            val error = classOf[Throwable].isAssignableFrom(c)
            schema = Schema.createRecord(name, null /* doc */, space, error)
            names.put(c.getName, schema)
            val fields = Arrays.asList(
              getPersistentFields(c) map {field =>
                val fieldSchema = createFieldSchema(field, names)
                new Schema.Field(field.getName, fieldSchema, null /* doc */, null)
              } toArray: _*
            )
            if (error) { // add Throwable message
              fields.add(new Schema.Field("detailMessage", THROWABLE_MESSAGE, null, null))
            }
            schema.setFields(fields)
          }
          names.put(fullName, schema)
        }
        schema
      case _ => super.createSchema(tpe, names)
    }
  }

  // if array element tpe is a class with a union annotation, note it
  // this is required because we cannot set a property on the union itself
  private def setElement(schema: Schema, element: Type) {
    element match {
      case c: Class[_] =>
        val union = c.getAnnotation(classOf[Union])
        if (union != null) {
          // element is annotated union
          schema.addProp(ELEMENT_PROP, c.getName)
        }
      case _ =>
    }
  }

  // construct a schema from a union annotation
  private def getAnnotatedUnion(union: Union, names: java.util.Map[String,Schema]): Schema = {
    val branches = Arrays.asList(union.value map (branch => createSchema(branch, names)): _*)
    Schema.createUnion(branches)
  }

  // Return of this class and its superclasses to serialize.
  // Not cached, since this is only used to create schemas, which are cached.
  private def getPersistentFields(recordClass: Class[_]) = {
    var names = collection.mutable.LinkedHashMap[String, Field]()
    var c = recordClass
    var break = false
    do {
      if (Option(c.getPackage) exists (_.getName.startsWith("java."))) {
        break = true // skip java built-in classes
      } else {
        for (field <- c.getDeclaredFields if (field.getModifiers & (Modifier.TRANSIENT|Modifier.STATIC)) == 0) {
          val name = field.getName
          if (names.contains(name)) throw new AvroTypeException(c+" contains two fields named: "+field)
          names += (name -> field)
        }
        c = c.getSuperclass
      }
    } while (c != null && !break)
    names.values
  }

  /** Create a schema for a field. */
  override protected def createFieldSchema(field: Field, names: java.util.Map[String, Schema]): Schema = {
    val schema = createSchema(field.getGenericType(), names)
    if (field.isAnnotationPresent(classOf[Nullable])) { // nullable
      makeNullable(schema)
    } else {
      schema
    }
  }

  /** Return the protocol for a Java interface.
   * <p>Note that this requires that <a
   * href="http://paranamer.codehaus.org/">Paranamer</a> is run over compiled
   * interface declarations, since Java 6 reflection does not provide access to
   * method parameter names.  See Avro's build.xml for an example. */
  override def getProtocol(iface: Class[_]): Protocol = {
    val protocol = new Protocol(iface.getSimpleName, Option(iface.getPackage) map (_.getName) getOrElse "")
    val names = new java.util.LinkedHashMap[String, Schema]()
    val messages = protocol.getMessages
    for (method <- iface.getMethods if (method.getModifiers & Modifier.STATIC) == 0) {
      val name = method.getName
      if (messages.containsKey(name)) throw new AvroTypeException("Two methods with same name: "+name)
      messages.put(name, getMessage(method, protocol, names))
    }

    // reverse types, since they were defined in reference order
    val types = new ArrayList[Schema](names.values)
    Collections.reverse(types)
    protocol.setTypes(types)

    protocol
  }

  private val paranamer = new CachingParanamer()

  private def getMessage(method: Method, protocol: Protocol, names: java.util.Map[String,Schema]): Protocol#Message = {
    val fields = new ArrayList[Schema.Field]()
    val paramNames = paranamer.lookupParameterNames(method)
    val paramTypes = method.getGenericParameterTypes
    val annotations = method.getParameterAnnotations
    var i = 0
    while (i < paramTypes.length) {
      var paramSchema = getSchema(paramTypes(i), names)
      var j = 0
      while (j < annotations(i).length) {
        annotations(i)(j) match {
          case x: Union => paramSchema = getAnnotatedUnion(x, names)
          case x: Nullable => paramSchema = makeNullable(paramSchema)
          case _ =>
        }
        j += 1
      }
      val paramName = if (paramNames.length == paramTypes.length) paramNames(i) else paramSchema.getName + i
      fields.add(new Schema.Field(paramName, paramSchema, null /* doc */, null))
      i += 1
    }
    val request = Schema.createRecord(fields)

    var response = method.getAnnotation(classOf[Union]) match {
      case null => getSchema(method.getGenericReturnType, names)
      case union => getAnnotatedUnion(union, names)
    }
    if (method.isAnnotationPresent(classOf[Nullable])) { // nullable
      response = makeNullable(response)
    }

    val errs = new ArrayList[Schema]()
    errs.add(Protocol.SYSTEM_ERROR) // every method can throw
    for (err <- method.getGenericExceptionTypes if err != classOf[AvroRemoteException]) {
      errs.add(getSchema(err, names))
    }
    val errors = Schema.createUnion(errs)

    protocol.createMessage(method.getName, null /* doc */, request, response, errors)
  }

  private def getSchema(tpe: Type, names: java.util.Map[String,Schema]): Schema = {
    try {
      createSchema(tpe, names)
    } catch {
      case ex: AvroTypeException=> throw new AvroTypeException("Error getting schema for "+tpe+": " +ex.getMessage(), ex) // friendly exception
    }
  }

  override def compare(o1: Object, o2: Object, s: Schema): Int = {
    throw new UnsupportedOperationException
  }

}
