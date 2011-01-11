package org.aiotrade.lib.dbfdriver

import java.io.File
import java.util.Date

object TestWriter {

  def main(args: Array[String]) {
    val file = new File("testwrite.dbf")
    if (file.exists) file.delete
    
    val fields = Array (
      DBFField("ID",    'C',  8, 0),
      DBFField("Name",  'C', 10, 0),
      DBFField("T-Int", 'I', 5, 0),
      DBFField("TestF", 'F', 20, 6),
      DBFField("TestD", 'D',  8, 0),
      DBFField("T-DateTime", 'T',  17, 0),
      DBFField("Memo",  'C', 10, 0)
    )
    
    val writer = new DBFWriter(file)
    writer.setFields(fields)

    val records = Array(
      Array("1", "aaa", 500, 500.123, new Date, new Date,new String("中国".getBytes("GBK"),"8859_1")),
      Array("2", "bbb", 610, 600.234, new Date, new Date,new String("日本".getBytes("GBK"),"8859_1")),
      Array("3", "ccc", 320, 700.456, new Date, new Date,new String("缅甸".getBytes("GBK"),"8859_1"))
    )

    var i = 0
    while (i < records.length) {
      writer.addRecord(records(i))
      i += 1
    }
    writer.write
    writer.close
  }
}
