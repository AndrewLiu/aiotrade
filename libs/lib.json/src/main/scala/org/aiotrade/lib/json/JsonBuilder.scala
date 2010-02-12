package org.aiotrade.lib.json


import java.io.Reader
import java.math.BigDecimal
import java.math.BigInteger
import org.aiotrade.lib.io.RestReader
import scala.collection.mutable.ListBuffer

object JsonBuilder {  
  def readJson(json: String): Any = {
    val parser = new JsonParser(new RestReader(json))
    new JsonBuilder(parser).getVal
  }

  def readJson(json: Reader): Any = {
    val parser = new JsonParser(new RestReader(json))
    new JsonBuilder(parser).getVal
  }
}

import Json.Event._
class JsonBuilder(parser: JsonParser) {

  if (parser.lastEvent == 0) parser.nextEvent

  def getVal = parser.lastEvent match {
    case STRING       => getString
    case LONG         => getLong
    case NUMBER       => getNumber
    case BIGNUMBER    => getBigNumber
    case BOOLEAN      => getBoolean
    case NULL         => getNull
    case OBJECT_START => getObject
    case OBJECT_END   => () // or ERROR?
    case ARRAY_END    => () // or ERROR?
    case ARRAY_START  => getArray
    case EOF          => () // or ERROR?
    case _            => () // or ERROR?
  }

  def getObject: Map[String, _] = {
    var elems = Map[String, Any]()
    while (parser.nextEvent != OBJECT_END) {
      val key = getString
      parser.nextEvent
      elems += (key -> getVal)
    }
    elems
  }

  def getArray: List[_] = {
    val elems = new ListBuffer[Any]
    while (parser.nextEvent != ARRAY_END) {
      elems += getVal
    }
    elems.toList
  }
    
  def getString = parser.getString

  def getLong = parser.getLong

  def getNumber = {
    val str = parser.getNumberChars.toString
    val num = str.toDouble

    if (java.lang.Double.isInfinite(num)) new BigDecimal(str) else num
  }

  def getBigNumber: Any = {
    val chars = parser.getNumberChars.toCharArray
    var isBigDec = false
    var i = 0
    while (i < chars.length) {
      chars(i) match {
        case ',' | 'e' | 'E' => return new BigDecimal(chars)
        case _ => // go on
      }
      i += 1
    }

    new BigInteger(chars.toString)
  }

  def getBoolean: Boolean = parser.getBoolean
    
  def getNull = parser.getNull
}
