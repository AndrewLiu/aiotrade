package org.aiotrade.lib.dbfdriver

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Calendar

object DBFReader {
  val END_OF_DATA = 0x1A

  @throws(classOf[IOException])
  def apply(is: InputStream) = is match {
    case x: FileInputStream =>
      val fileChannel = x.getChannel
      try {
        new DBFReader(Left(fileChannel))
      } catch {
        case ex => tryCloseFileChannel(fileChannel); throw ex
      }

    case _ => new DBFReader(Right(is))
  }

  @throws(classOf[IOException])
  def apply(file: File) = {
    val fileChannel = (new FileInputStream(file)).getChannel
    try {
      new DBFReader(Left(new FileInputStream(file) getChannel))
    } catch {
      case ex => tryCloseFileChannel(fileChannel); throw ex
    }
  }

  @throws(classOf[IOException])
  def apply(fileName: String) = {
    val fileChannel = (new RandomAccessFile(fileName, "r")).getChannel
    try {
      new DBFReader(Left(fileChannel))
    } catch {
      case ex => tryCloseFileChannel(fileChannel); throw ex
    }
  }

  private def tryCloseFileChannel(fileChannel: FileChannel) {
    if (fileChannel != null) {
      try {
        fileChannel.close
      } catch {
        case ex =>
      }
    }
  }
}

import DBFReader._
@throws(classOf[IOException])
class DBFReader private (input: Either[FileChannel, InputStream]) {
  var charsetName = "8859_1"
  var isClosed = false

  private var in: ByteBuffer = _
  var header: DBFHeader = _

  load

  /** Can be reloaded */
  def load {
    input match {
      case Left(x)  => load(x)
      case Right(x) => load(x)
    }
  }

  private def load(fileChannel: FileChannel) {
    if (isClosed) throw new IOException("This reader has closed")

    in = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size)
    header = DBFHeader.read(in)
  }

  private def load(is: InputStream) {
    if (isClosed) throw new IOException("This reader has closed")
    
    val out = new ByteArrayOutputStream
    val buf = new Array[Byte](4096)
    val bis = new BufferedInputStream(is)
    var len = -1
    while ({len = bis.read(buf); len != -1}) {
      out.write(buf, 0, len)
    }
    val bytes = out.toByteArray
    
    in = ByteBuffer.wrap(bytes)
    header = DBFHeader.read(in)
  }


  // it might be required to leap to the start of records at times
  val dataStartIndex = header.headerLength - (32 + (32 * header.fields.length)) - 1
  if (dataStartIndex > 0) {
    in.position(in.position + dataStartIndex)
    //is.skip(dataStartIndex)
  }

  override def toString = {
    val sb = new StringBuilder
    sb.append(header.year).append("/").append(header.month).append("/").append(header.day).append("\n")
    sb.append("Total records: ").append(header.numberOfRecords)
    sb.append("\nHeader length: ").append(header.headerLength)

    header.fields map (_.name) mkString("/n")

    sb.toString
  }

  /**
   * Returns the number of records in the DBF.
   */
  def recordCount = header.numberOfRecords

  /**
   * Returns the asked Field. In case of an invalid index,
   * it returns a ArrayIndexOutofboundsException.
   *
   * @param index. Index of the field. Index of the first field is zero.
   */
  @throws(classOf[IOException])
  def getField(idx: Int): DBFField = {
    if (isClosed) {
      throw new IOException("Source is not open")
    }
    header.fields(idx)
  }

  /**
   * Returns the number of field in the DBF.
   */
  @throws(classOf[IOException])
  def fieldCount: Int = {
    if (isClosed) {
      throw new IOException("Source is not open")
    }
    if (header.fields != null) {
      header.fields.length
    } else -1
  }

  def hasNext: Boolean = in.position < in.capacity

  /**
   * Reads the returns the next row in the DBF stream.
   * @returns The next row as an Object array. Types of the elements
   * these arrays follow the convention mentioned in the class description.
   */
  @throws(classOf[IOException])
  def nextRecord: Array[Any] = {
    if (isClosed) {
      throw new IOException("Source is not open")
    }

    val values = new Array[Any](header.fields.length)
    try {
      in.get //Skip the Record deleted flag
      
//      var isDeleted = false
//      do {
//        if (isDeleted) {
//          try{
//            in.position(in.position + header.recordLength - 1)
//          }catch{case ex: Exception => return null}
//          //is.skip(header.recordLength - 1)
//        }
//
//        if(in.position >= in.capacity)
//          return null
//
//        val b = in.get
//        if (b == END_OF_DATA) {
//          return null
//        }
//
//        isDeleted = (b == '*')
//      } while (isDeleted)

      var i = 0
      while (i < header.fields.length) {
        val obj = header.fields(i).dataType match {
          case 'C' =>
            try{
              val bytes = new Array[Byte](header.fields(i).length)
              in.get(bytes)
              new String(bytes, charsetName)
            } catch{
              case ex:Exception => new String("".getBytes, charsetName)
            }
          case 'D' =>
            try {
              val y = new Array[Byte](4)
              in.get(y)
              val m = new Array[Byte](2)
              in.get(m)
              val d = new Array[Byte](2)
              in.get(d)
              val h = new Array[Byte](2)
              in.get(h)
              val min = new Array[Byte](2)
              in.get(min)
              val sec = new Array[Byte](2)
              in.get(sec)
              val milsec = new Array[Byte](3)
              in.get(milsec)
              val cal = Calendar.getInstance
              cal.set(Calendar.YEAR, (new String(y)).toInt)
              cal.set(Calendar.MONTH, (new String(m)).toInt - 1)
              cal.set(Calendar.DAY_OF_MONTH, (new String(d)).toInt)
              cal.set(Calendar.HOUR_OF_DAY, new String(h).toInt)
              cal.set(Calendar.MINUTE, new String(min).toInt)
              cal.set(Calendar.SECOND, new String(sec).toInt)
              cal.set(Calendar.MILLISECOND, new String(milsec).toInt)
              cal.getTime
            } catch {
              case ex: Exception => null // this field may be empty or may have improper value set
            } 

          case 'F' =>
            try {
              var bytes = new Array[Byte](header.fields(i).length)
              in.get(bytes)
              bytes = Utils.trimLeftSpaces(bytes)
            
              if (bytes.length > 0 && !Utils.contains(bytes, '?')) {
                (new String(bytes)).toFloat
              } else null
            } catch {
              case ex: NumberFormatException => 0.0F // throw new IOException("Failed to parse Float: " + ex.getMessage)
              case ex: Exception => 0.0F
            }

          case 'N' =>
            try {
              var bytes = new Array[Byte](header.fields(i).length)
              in.get(bytes)
              bytes = Utils.trimLeftSpaces(bytes)
            
              if (bytes.length > 0 && !Utils.contains(bytes, '?')) {
                (new String(bytes)).toDouble
              } else null
            } catch {
              case ex: NumberFormatException => 0.0 // throw new IOException("Failed to parse Number: " + ex.getMessage)
              case ex: Exception => 0.0
            }

          case 'L' =>
            in.get match {
              case 'Y' | 'y' | 'T' | 't' => true
              case _ => false
            }
            
          case 'M' => "null" // TODO Later
          case _ => "null"
        }
        
        values(i) = obj
        i += 1
      }
    } catch {
      case ex: EOFException => return null
      case ex: IOException => throw new IOException(ex.getMessage)
    }

    values
  }

  def close {
    isClosed = true
    try {
      input match {
        case Left(x) => x.close
        case Right(x) => x.close
      }
    } catch {
      case ex: IOException => throw ex
    }
  }

}
