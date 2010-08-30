package org.aiotrade.lib.info.model

import scala.collection.mutable.Map

trait InfoContent{
  def publishTime: Long

  /**
   * relative weight of the InfoContent
   */
  //def weight: Float

  /**
   * link is the unique identifier of each piece of InfoContent
   */
  def link: String

  def exportToMap: Map[String, String]
  def exportToJavaMap: java.util.Map[String, String]
}
