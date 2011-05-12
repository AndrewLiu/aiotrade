/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.lib.avro

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.util.Utf8;
import org.apache.avro.AvroRuntimeException;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import scala.collection.mutable.HashSet;
import scala.collection.Seq;
import scala.collection.Map;
import scala.collection.Set;
import scala.collection.Iterable;
import scala.collection.JavaConversions._;


/**
 * This is a portable serialization marshaller based on Apache Avro. It supports basic type and collection marshalling.
 * Basic types include UTF-8 String, int long, float, double, boolean and null, and the collections supported include
 * arrays, list, map and set composed of basic types.
 *
 * Primitive types short and byte are not supported per se. Instead, pass integers which will be encoded efficiently
 * using variable-length (http://lucene.apache.org/java/2_4_0/fileformats.html#VInt) zig zag
 * (http://code.google.com/apis/protocolbuffers/docs/encoding.html#types) coding.
 *
 * Primitive arrays not supported except byte arrays. Instead, use their object counter partners, i.e. Integer...etc.
 *
 * For more detailed information, go to: http://community.jboss.org/docs/DOC-15774
 *
 * @author Galder Zamarreño
 * @since 5.0
 */


object ScalaApacheAvroMarshaller {
  val marshaller = new ScalaApacheAvroMarshaller()
  
  def apply() = marshaller
  
  
}

class ScalaApacheAvroMarshaller extends AbstractScalaMarshaller {

   private val STRING_SCHEMA = Schema.create(Schema.Type.STRING);
   private val INT_SCHEMA = Schema.create(Schema.Type.INT);
   private val LONG_SCHEMA = Schema.create(Schema.Type.LONG);
   private val FLOAT_SCHEMA = Schema.create(Schema.Type.FLOAT);
   private val DOUBLE_SCHEMA = Schema.create(Schema.Type.DOUBLE);
   private val  BOOLEAN_SCHEMA = Schema.create(Schema.Type.BOOLEAN);
   private val  BYTES_SCHEMA = Schema.create(Schema.Type.BYTES);
   private val  NULL_SCHEMA = Schema.create(Schema.Type.NULL);
   private val  STRING_ARRAY_SCHEMA = Schema.createArray(STRING_SCHEMA);
   private val  INT_ARRAY_SCHEMA = Schema.createArray(INT_SCHEMA);
   private val  LONG_ARRAY_SCHEMA = Schema.createArray(LONG_SCHEMA);
   private val  FLOAT_ARRAY_SCHEMA = Schema.createArray(FLOAT_SCHEMA);
   private val  DOUBLE_ARRAY_SCHEMA = Schema.createArray(DOUBLE_SCHEMA);
   private val  BOOLEAN_ARRAY_SCHEMA = Schema.createArray(BOOLEAN_SCHEMA);



                                                
   private val  STRING_TYPE = new StringMarshallableType(0);
   private val  INT_TYPE = new MarshallableType(INT_SCHEMA, 1);
   private val  LONG_TYPE = new MarshallableType(LONG_SCHEMA, 2);
   private val  FLOAT_TYPE = new MarshallableType(FLOAT_SCHEMA, 3);
   private val  DOUBLE_TYPE = new MarshallableType(DOUBLE_SCHEMA, 4);
   private val  BOOLEAN_TYPE = new MarshallableType(BOOLEAN_SCHEMA, 5);
   private val  BYTES_TYPE = new BytesMarshallableType(6);
   private val  NULL_TYPE = new MarshallableType(NULL_SCHEMA, 7);
   private val  STRING_ARRAY_TYPE = new StringArrayMarshallableType(8);
   private val  INT_ARRAY_TYPE = new ArrayMarshallableType[Integer](INT_ARRAY_SCHEMA, classOf[Integer], 9);
   private val  LONG_ARRAY_TYPE = new ArrayMarshallableType[Long](LONG_ARRAY_SCHEMA, classOf[Long], 10);
   private val  DOUBLE_ARRAY_TYPE = new ArrayMarshallableType[Double](DOUBLE_ARRAY_SCHEMA, classOf[Double], 11);
   private val  FLOAT_ARRAY_TYPE = new ArrayMarshallableType[Float](FLOAT_ARRAY_SCHEMA, classOf[Float], 12);
   private val  BOOLEAN_ARRAY_TYPE = new ArrayMarshallableType[Boolean](BOOLEAN_ARRAY_SCHEMA, classOf[Boolean], 13);   
   private val  listType = new ListMarshallableType(14, this);
   private val  mapType = new MapMarshallableType(15, this);
   private val  setType = new SetMarshallableType(16, this);

   private def getType($type : Int) : MarshallableType = {
      $type match {
         case 0 => STRING_TYPE;
         case 1 =>  INT_TYPE;
         case 2 =>  LONG_TYPE;
         case 3 =>  FLOAT_TYPE;
         case 4 =>  DOUBLE_TYPE;
         case 5 =>  BOOLEAN_TYPE;
         case 6 =>  BYTES_TYPE;
         case 7 =>  NULL_TYPE;
         case 8 =>  STRING_ARRAY_TYPE;
         case 9 =>  INT_ARRAY_TYPE;
         case 10 =>  LONG_ARRAY_TYPE;
         case 11 =>  DOUBLE_ARRAY_TYPE;
         case 12 =>  FLOAT_ARRAY_TYPE;
         case 13 =>  BOOLEAN_ARRAY_TYPE;
         case 14 =>  listType;
         case 15 =>  mapType;
         case 16 =>  setType;
         case _ => throw new AvroRuntimeException("Unknown type " + $type);
      }
   }

   override protected def objectToBuffer(o : AnyRef, estimatedSize : Int) : ByteBuffer = {
      var baos = new ExposedByteArrayOutputStream(estimatedSize);
      var encoder = new BinaryEncoder(baos);
      objectToBuffer(o, encoder);
      return new ByteBuffer(baos.getRawBuffer(), 0, baos.size());
   }

   private def objectToBuffer(o : AnyRef, encoder : Encoder) {
      if (o == null) {
         NULL_TYPE.write(o, encoder);
      } else {
         val clazz = o.getClass();
         var $type : MarshallableType = (clazz,o) match {
           case (a,b) if a.equals(classOf[String]) => STRING_TYPE
           case (a,b) if a.equals(classOf[Array[Byte]]) => BYTES_TYPE
           case (a,b) if a.equals(classOf[Boolean]) => BOOLEAN_TYPE  
           case (a,b) if a.equals(classOf[Integer]) => INT_TYPE  
           case (a,b) if a.equals(classOf[Long]) => LONG_TYPE  
           case (a,b) if a.equals(classOf[Float]) => FLOAT_TYPE  
           case (a,b) if a.equals(classOf[Double]) => DOUBLE_TYPE  
           case (a,b) if a.equals(classOf[Array[String]]) => STRING_ARRAY_TYPE  
           case (a,b) if a.equals(classOf[Array[Integer]]) => INT_ARRAY_TYPE
           case (a,b) if a.equals(classOf[Array[Long]]) => LONG_ARRAY_TYPE
           case (a,b) if a.equals(classOf[Array[Float]]) => FLOAT_ARRAY_TYPE
           case (a,b) if a.equals(classOf[Array[Double]]) => DOUBLE_ARRAY_TYPE
           case (a,b) if a.equals(classOf[Array[Boolean]]) => BOOLEAN_ARRAY_TYPE
           case (a,b) if b.isInstanceOf[List[_]] => listType
           case (a,b) if b.isInstanceOf[Map[_,_]] => mapType
           case (a,b) if b.isInstanceOf[Set[_]] => setType
           case _ => listType             
         }
         $type.write(o, encoder);
      }
   }

   override def objectFromByteBuffer(buf : Array[Byte], offset : Int, length : Int) : AnyRef = {
      val factory = new DecoderFactory(); // TODO: Could this be cached?
      val is = new ByteArrayInputStream(buf, offset, length);
      val decoder = factory.createBinaryDecoder(is, null);
      return objectFromByteBuffer(decoder);
   }

   private def objectFromByteBuffer(decoder : Decoder) : AnyRef = {
      val $type = decoder.readInt();
      return getType($type).read(decoder);
   }

   override def isMarshallable(o : AnyRef) : Boolean = {
      val clazz = o.getClass();
      return (clazz.equals(classOf[String]) || clazz.equals(classOf[Array[Byte]])
            || clazz.equals(classOf[Boolean]) || clazz.equals(classOf[Integer])
            || clazz.equals(classOf[Long]) || clazz.equals(classOf[Float])
            || clazz.equals(classOf[Double]) || clazz.equals(classOf[Array[String]])
            || clazz.equals(classOf[Array[Integer]]) || clazz.equals(classOf[Array[Long]])
            || clazz.equals(classOf[Array[Float]]) || clazz.equals(classOf[Array[Double]])
            || clazz.equals(classOf[Array[Boolean]]) || o.isInstanceOf[List[_]]
            || o.isInstanceOf[Map[_,_]] || o.isInstanceOf[Set[_]]);
   }

   class MarshallableType($schema : Schema , $id : Int) {
      var schema : Schema = _
      var id : Int = 0
      
      this.schema = $schema
      this.id = $id


      def read(decoder : Decoder) : AnyRef = {
         return new GenericDatumReader(schema).read(null, decoder);
      }

      def write(o : AnyRef, encoder : Encoder) {
         var writer = new GenericDatumWriter(schema); // TODO: Could this be cached? Maybe, but ctor is very cheap
         encoder.writeInt(id);
         write(writer, o, encoder);
      }

    def write(writer : GenericDatumWriter[_], o : AnyRef, encoder : Encoder) {
      writer.asInstanceOf[GenericDatumWriter[AnyRef]].write(o, encoder);
      }

   }

   class StringMarshallableType($id : Int) extends MarshallableType(STRING_SCHEMA, $id) {

      override def read(decoder :Decoder)  : AnyRef = {
         return new GenericDatumReader(schema).read(null, decoder).toString();
      }

      override def write(writer : GenericDatumWriter[_], o : AnyRef, encoder : Encoder )  {
        writer.asInstanceOf[GenericDatumWriter[Utf8]].write(new Utf8(o.asInstanceOf[String]), encoder);
      }
   }

   class BytesMarshallableType($id : Int ) extends MarshallableType(BYTES_SCHEMA, $id) {

      override def read(decoder : Decoder ) : AnyRef = {
        var byteBuffer = (new GenericDatumReader(schema).read(null, decoder)).asInstanceOf[java.nio.ByteBuffer]
        var bytes = new Array[Byte](byteBuffer.limit()) // TODO: Limit or capacity ? Limit works
         byteBuffer.get(bytes);
         return bytes;
      }


      override def write(writer : GenericDatumWriter[_], o : AnyRef, encoder : Encoder)  {
        writer.asInstanceOf[GenericDatumWriter[java.nio.ByteBuffer]].write(java.nio.ByteBuffer.wrap(o.asInstanceOf[Array[Byte]]), encoder);
      }
   }

   class StringArrayMarshallableType($id : Int) extends MarshallableType(STRING_ARRAY_SCHEMA, $id) {

      override def read(decoder : Decoder) : AnyRef = {
        var utf8s = (new GenericDatumReader(schema).read(null, decoder)).asInstanceOf[GenericData.Array[Utf8]]
        var strings = new scala.collection.mutable.ListBuffer[String]()
         for (utf8 <- utf8s)
           strings + (utf8.toString)
         val result = strings.toList.toArray
         result
      }

      override def write(writer : GenericDatumWriter[_], o : AnyRef, encoder : Encoder) {
         var strings = o.asInstanceOf[Array[String]];
         var array = new GenericData.Array[Utf8](strings.length, schema);
         for (str <- strings)
            array.add(new Utf8(str));
          writer.asInstanceOf[GenericDatumWriter[GenericData.Array[Utf8]]].write(array, encoder);
      }
   }

   class ArrayMarshallableType[T]($schema : Schema, $type : Class[T] , $id : Int) extends MarshallableType($schema, $id) {
      var _type : Class[T] = _

      this._type = $type

      override def read(decoder : Decoder) : AnyRef = {
        
        var avroData = (new GenericDatumReader(schema).read(null, decoder)).asInstanceOf[GenericData.Array[T]].toArray;
        val result = avroData.toArray
        result

      }


      override def write(writer : GenericDatumWriter[_], o : AnyRef, encoder : Encoder)  {
         var array = o.asInstanceOf[Array[T]];
         var avroArray = new GenericData.Array[T](array.length, schema);
         for (t <- array)
             avroArray.add(t);
           writer.asInstanceOf[GenericDatumWriter[GenericData.Array[T]]].write(avroArray, encoder);
      }
   }

   abstract class CollectionMarshallableType($id : Int, $marshaller : ScalaApacheAvroMarshaller) extends MarshallableType(null, $id) {
      var  marshaller : ScalaApacheAvroMarshaller = _
      
      this.marshaller = $marshaller
    
      override def read(decoder :Decoder) : AnyRef = {
         var size = decoder.readArrayStart();
         var collection = createCollection(size.asInstanceOf[Int]);
         readResult(collection, size.asInstanceOf[Int],decoder)
      }
      
      def readResult(result : Iterable[_], size : Int, decoder :Decoder) : Iterable[_] = {
        null
      } 

      override def write(o : AnyRef, encoder : Encoder)  {
         var collection = o.asInstanceOf[Iterable[AnyRef]];
         encoder.writeInt(id);
         encoder.setItemCount(collection.size);
         for (element <- collection)
            marshaller.objectToBuffer(element, encoder);
      }

      def createCollection(size : Int) : Iterable[_] = {
         return null;
      }
   }

   class ListMarshallableType($id : Int, $marshaller : ScalaApacheAvroMarshaller) extends CollectionMarshallableType($id,$marshaller) {
      

      override def readResult(result : Iterable[_], size : Int, decoder :Decoder) : Iterable[_] = {
         var data : AnyRef = null
         for ( k <- 0 until size) 
             result.asInstanceOf[scala.collection.mutable.ListBuffer[AnyRef]] + marshaller.objectFromByteBuffer(decoder)   
           result.toList
      }
      
      override def createCollection(size : Int) : Iterable[_] = {
             scala.collection.mutable.ListBuffer[AnyRef]()
      }

   }

   class MapMarshallableType($id : Int, $marshaller : ScalaApacheAvroMarshaller) extends CollectionMarshallableType($id,$marshaller) {
   
      override def read(decoder : Decoder) : AnyRef = {
         var size = decoder.readArrayStart();
         var map = new HashMap[AnyRef, AnyRef]();
         for (i <- 0 until size.asInstanceOf[Int])
            map(marshaller.objectFromByteBuffer(decoder)) =marshaller.objectFromByteBuffer(decoder);
          val result =  scala.collection.JavaConverters.asScalaMapConverter(map).asScala;
          result
      }

      override def write(o : AnyRef, encoder : Encoder)  {
         var map = o.asInstanceOf[Map[AnyRef, AnyRef]];
         encoder.writeInt(id);
         encoder.setItemCount(map.size());
         for ((key,value) <- map) {
            marshaller.objectToBuffer(key, encoder);
            marshaller.objectToBuffer(value, encoder);
         }

      }
   }

   class SetMarshallableType($id : Int, $marshaller : ScalaApacheAvroMarshaller) extends CollectionMarshallableType($id, $marshaller) {

      override def readResult(result : Iterable[_], size : Int, decoder :Decoder) : Iterable[_] = {
         
         for ( k <- 0 until size.asInstanceOf[Int])
           result.asInstanceOf[HashSet[AnyRef]].add(marshaller.objectFromByteBuffer(decoder))
         result
        
      }
      
      override def createCollection(size : Int) : Iterable[_] = {
         new HashSet[AnyRef]()
      }      
   }  

}
