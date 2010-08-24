package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.collection.ArrayList

object Newses extends Table[News]{

  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)

  val author = "author"  VARCHAR(30) DEFAULT("''")
  val orgPublisher = "orgPublisher" VARCHAR(30) DEFAULT("''")
  val hotness = "hotness" FLOAT()
}

class News extends TVal with Flag {

  var generalInfo : GeneralInfo = _
  var author : String = ""
  var orgPublisher : String = ""
  var hotness : Float = _


  private var _newses: ArrayList[News] = ArrayList[News]()

  def newses = _newses

  def += [News](value: org.aiotrade.lib.info.model.News){
    println("value:" + value)
    println("value.generalInfo:" + value.generalInfo)
    println("value.generalInfo.publishTime:" + value.generalInfo.publishTime)
    assert(value.generalInfo.publishTime == this.time,
           value + " is appended to a different TVal with time=" + this.time)

    value +=: _newses
  }

  def ++= [News](values: ArrayList[org.aiotrade.lib.info.model.News]){
    values.foreach(v => assert(v.generalInfo.publishTime == this.time,
                               v + " is appended to a different TVal with time=" + this.time))

    values ++=: _newses
  }

  override def toString: String = {
    println("title:" + this.generalInfo.title)
    println("publishTime:" + this.generalInfo.publishTime)
    println("author:" + this.author)
    this.generalInfo.title + "|" + this.generalInfo.publishTime + "|" + this.author
  }
}
