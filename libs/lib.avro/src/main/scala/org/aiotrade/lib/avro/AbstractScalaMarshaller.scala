/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.avro

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract Marshaller implementation containing shared implementations.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
abstract class AbstractScalaMarshaller extends Marshaller {

   val DEFAULT_BUF_SIZE = 512;

   /**
    * This is a convenience method for converting an object into a {@link org.infinispan.io.ByteBuffer} which takes
    * an estimated size as parameter. A {@link org.infinispan.io.ByteBuffer} allows direct access to the byte
    * array with minimal array copying
    *
    * @param o object to marshall
    * @param estimatedSize an estimate of how large the resulting byte array may be
    * @return a ByteBuffer
    * @throws Exception
    */
   protected def objectToBuffer(o : AnyRef, estimatedSize : Int) : ByteBuffer

   
   override def objectToBuffer(obj : AnyRef) : ByteBuffer = {
      return objectToBuffer(obj, DEFAULT_BUF_SIZE);
   }

   override def objectToByteBuffer(o : AnyRef) : Array[Byte] = {
      return objectToByteBuffer(o, DEFAULT_BUF_SIZE);
   }

   override def objectToByteBuffer(obj : AnyRef, estimatedSize : Int) : Array[Byte] = {
      var b = objectToBuffer(obj, estimatedSize)
      var bytes = new Array[Byte](b.getLength())
      System.arraycopy(b.getBuf(), b.getOffset(), bytes, 0, b.getLength());
      return bytes;
   }

   override def objectFromByteBuffer( buf : Array[Byte]) : AnyRef = {
      return objectFromByteBuffer(buf, 0, buf.length);
   }

   /**
    * This method implements {@link StreamingMarshaller#objectFromInputStream(java.io.InputStream)}, but its
    * implementation has been moved here rather that keeping under a class that implements StreamingMarshaller
    * in order to avoid code duplication.
    */
   def objectFromInputStream( inputStream : InputStream) : AnyRef = {
      val len = inputStream.available();
      var bytes = new ExposedByteArrayOutputStream(len);
      var buf = new Array[Byte](Math.min(len, 1024));
      var bytesRead : Int = inputStream.read(buf, 0, buf.length);
      while (bytesRead != -1) {
        bytes.write(buf, 0, bytesRead);
        bytesRead = inputStream.read(buf, 0, buf.length)
      }
      return objectFromByteBuffer(bytes.getRawBuffer(), 0, bytes.size());
   }

}
