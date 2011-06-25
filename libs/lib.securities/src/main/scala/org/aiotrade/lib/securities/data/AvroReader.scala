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
package org.aiotrade.lib.securities.data

import java.io.File
import java.io.IOException
import org.aiotrade.lib.securities.model.Secs
import org.apache.avro.Schema
import org.apache.avro.file.DataFileReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.reflect.ReflectDatumReader
import org.apache.avro.util.Utf8
import ru.circumflex.orm.Table
import scala.collection.mutable

/**
 * 
 * A helper method to print avro file's records
 * 
 * @author Caoyuan Deng
 */
object AvroReader {
  var record: GenericRecord = null // repeatly used 

  def main(args: Array[String]) {
    try {
      testRead(Secs)
      
      System.exit(0)
    } catch {
      case ex => ex.printStackTrace; System.exit(-1)
    }
  }
  
  private def testRead(table: Table[_]) {
    val tableName = table.relationName
    val fileName = SyncUtil.exportDataDirPath + "/" + tableName + ".avro"
    read(fileName) {
      case null => println("null")
      case row =>  println(row.mkString(" \t| "))
    }
  } 
  
  @throws(classOf[IOException])
  def read(fileName: String)(action: Array[Any] => Unit) {
    val reader = new DataFileReader[AnyRef](new File(fileName), new ReflectDatumReader[AnyRef]())
    val schemaFields = readSchemaFields(reader)
    val columnNames = schemaFields.keys.toArray
    
    println(columnNames.mkString(" \t| "))
    while (reader.hasNext) {
      val row = readRow(reader, columnNames)
      action(row)
    }
  }

  private def readSchemaFields(reader: DataFileReader[AnyRef]) = {
    val schemaFields = mutable.HashMap[String, Schema.Field]()

    val schema = reader.getSchema
    schema.getType match {
      case Schema.Type.RECORD =>
        val fields = schema.getFields.iterator
        while (fields.hasNext) {
          val field = fields.next
          schemaFields += ((field.name, field))
        }
    }
    schemaFields
  }

  def readRow(reader: DataFileReader[AnyRef], columnNames: Array[String]): Array[Any] = {
    if (reader.hasNext) {
      val row = new Array[Any](columnNames.length)
      try {
        record = reader.next(record).asInstanceOf[GenericRecord]
        var i = -1
        while ({i += 1; i < columnNames.length}) {
          val colName = columnNames(i)
          row(i) = record.get(colName) match {
            case x: Utf8 => x.toString
            case x => x
          }
        }
      } catch {
        case e: IOException => throw e
      }
      row
    } else {
      null
    }
  }
}
