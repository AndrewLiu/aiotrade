package org.aiotrade.lib.util.json


import java.io.Reader
import java.math.BigDecimal
import java.math.BigInteger
import org.aiotrade.lib.util.io.RestReader

import scala.collection.mutable.{ListBuffer, Map, HashMap}

object JsonBuilder {  
  def readJson(json: String) = {
    val parser = new JsonParser(new RestReader(json))
    new JsonBuilder(parser).getVal
  }

  def readJson(json: Reader) = {
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
    case ARRAY_START  => getArray
    case ARRAY_END    => () // or ERROR?
    case EOF          => () // or ERROR?
    case _            => () // or ERROR?
  }

  def getObject: Map[_, _] = {
    val elems = new HashMap[Any, Any]
    while (parser.nextEvent != OBJECT_END) {
      val key = getString

      parser.nextEvent
      val value = getVal

      elems(key) = value
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
    val numStr = parser.getNumberChars.toString
    val num = numStr.toDouble

    if (java.lang.Double.isInfinite(num)) new BigDecimal(numStr)
    else num
  }

  def getBigNumber: Any = {
    val numChars = parser.getNumberChars.toCharArray
    var isBigDec = false
    var i = 0
    while (i < numChars.length) {
      numChars(i) match {
        case ',' | 'e' | 'E' => return new BigDecimal(numChars)
        case _ => // go on
      }
      i +=1 
    }

    new BigInteger(numChars.toString)
  }

  def getBoolean: Boolean = parser.getBoolean
    
  def getNull = parser.getNull
}
