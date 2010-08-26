package org.aiotrade.lib.info.model

import scala.collection.mutable.Map

trait InfoContent{
  def publishTime: Long
<<<<<<< HEAD
=======

  /**
   * relative weight of the InfoContent
   */
  def weight: Float

  /**
   * link is the unique identifier of each piece of InfoContent
   */
>>>>>>> 5e429caad46449d294fb3dda967b358c5e74b9fc
  def link: String

  def exportToMap: Map[String, String]
  def exportToJavaMap: java.util.Map[String, String]
}
