package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.collection.ArrayList

object Filings extends Table[Filing]{
  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)
  
  val publisher = "publisher" VARCHAR(30) DEFAULT("''")
  val format = "format" TINYINT
  val size = "size" BIGINT
}

class Filing extends TVal with Flag {
  var generalInfo : GeneralInfo = _

  var publisher : String = ""
  var format : Int = _ 
  var size : Long = _

  private var _filings: ArrayList[Filing] = ArrayList[Filing]()

  def filings = _filings

  def += [Filings](value: org.aiotrade.lib.info.model.Filing){
    assert(value.generalInfo.publishTime == this.time,
           value + " is appended to a different TVal with time=" + this.time)

    value +=: _filings
  }

  def ++= [Filings](values: ArrayList[org.aiotrade.lib.info.model.Filing]){
    values.foreach(v => assert(v.generalInfo.publishTime == this.time,
                               v + " is appended to a different TVal with time=" + this.time))

    values ++=: _filings
  }


  object Format {
    val pdf = 1
    val txt = 2
    val word = 3
    val others = 99
  }
}


