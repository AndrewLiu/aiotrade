/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.sector.model

import ru.circumflex.orm.Table
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.info.model.InfoContent
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

object BullVSBears extends Table[BullVSBear]{
  val time = "time" BIGINT
  val ratio = "ratio" FLOAT()
  var summary  = "summary" VARCHAR(3000)
  INDEX(getClass.getSimpleName + "_time_idx", time.name)
}

class BullVSBear extends TVal with Flag with InfoContent{
//    var time : Long = _
    var ratio : Float = _
    var summary : String = ""

  def publishTime: Long = this.time
  def weight: Float = 0F
  def link: String = ""

  def exportToMap: Map[String, String] = {
    val map = Map[String, String]()
    map += ("PREDICT_TIME" -> time.toString)
    map += ("OPTIMISM" -> ratio.toString)
    map += ("PESSIMISM" -> (100 - ratio).toString)
    map += ("ANALYSIS" -> summary)

    map
  }

  def exportToJavaMap: java.util.Map[String, String] = exportToMap
}
