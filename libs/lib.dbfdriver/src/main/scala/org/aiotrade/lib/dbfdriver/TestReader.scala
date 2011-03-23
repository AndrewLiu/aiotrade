package org.aiotrade.lib.dbfdriver
import java.util.Date
import java.text.SimpleDateFormat
object TestReader {
  val willPrintRecord = true

  val warmTimes = 5
  val times = if (willPrintRecord) 1 else 30
  val filename = "testwrite.dbf"
  
  def main(args: Array[String]) {
    test1
    println("==============")
    test2
  }

  def test1 {
    var t0 = System.currentTimeMillis
    var i = 0
    while (i < times) {
      if (i == warmTimes) t0 = System.currentTimeMillis // warm using the head warmTimes reading

      val reader = DBFReader(filename)
      reader.charsetName_=("GBK")

      readRecords(reader)
      reader.close

      i += 1
    }

    val countTimes = if (times > warmTimes) (times - warmTimes) else times
    println("Averagy time: " + (System.currentTimeMillis - t0) / countTimes + " ms")
  }

  def test2 {
    var t0 = System.currentTimeMillis
    val reader = DBFReader(filename)
      reader.charsetName_=("GBK")
      
    var i = 0
    while (i < times) {
      if (i == warmTimes) t0 = System.currentTimeMillis // warm using head warmTimes reading

      reader.load
      readRecords(reader)

      i += 1
    }
    reader.close

    val countTimes = if (times > warmTimes) (times - warmTimes) else times
    println("Averagy time: " + (System.currentTimeMillis - t0) / countTimes + " ms")
  }

  def readRecords(reader: DBFReader) {
    if (willPrintRecord) {
      reader.header.fields foreach {x => print(x.name + " | ")}
      println
    }

    var i = 0
    val l = reader.recordCount
    while (i < l) {
      val recordObjs = reader.nextRecord
      if (willPrintRecord) {
        recordObjs foreach {x => x match {
            case x: String => print( x + " | ")
            case x: Date => print(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(x) + " | ")
            case av => print(av + " | ")
          }}
        println
      } else {
        recordObjs foreach {x =>}
      }
      i += 1
    }
    
    println("Total Count: " + i)
  }

}
