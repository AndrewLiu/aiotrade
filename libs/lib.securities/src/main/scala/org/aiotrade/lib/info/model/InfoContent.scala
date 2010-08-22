package org.aiotrade.lib.info.model

import scala.collection.mutable.Map

trait InfoContent{
  def publishTime: Long
  def weight: Float
  def link: String

  def exportToMap: Map[String, String]
  def exportToJavaMap: java.util.Map[String, String]
}
