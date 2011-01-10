package org.aiotrade.lib.dbfdriver

import java.io.File
import java.util.Date

object TestWriter {

  def main(args: Array[String]) {
    val fields = Array (
      DBFField("ID",    'C',  8, 0),
      DBFField("Name",  'C', 32, 0),
      DBFField("TestN", 'N', 20, 0),
      DBFField("TestF", 'F', 20, 6),
      DBFField("TestD", 'D',  8, 0)
    )
    
    val writer = new DBFWriter(new File("testwrite.dbf"))
    writer.setFields(fields)

    val records = Array(
      Array("1", "aaa", 500d, 500.123, new Date),
      Array("2", "bbb", 600d, 600.234, new Date),
      Array("3", "ccc", 700d, 700.456, new Date)
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
