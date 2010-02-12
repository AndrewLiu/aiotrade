/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.json

import java.io.InputStream
import java.io.InputStreamReader
import scala.collection.mutable.ArrayBuffer

class JsonInputStreamReader(in: InputStream, charsetName: String)  extends InputStreamReader(in, charsetName) {
  private val rin = new InputStreamReader(in)

  private val ret = JsonBuilder.readJson(rin) match {
    case map: Json.Object if map.size == 1 =>
      map.iterator.next match {
        case (name, fields) => readObject(name, fields)
        case _ => null
      }
    case xs: List[_] =>
      val ret = new ArrayBuffer[Any]
      var is = xs
      while (!is.isEmpty) {
        val obj = is.head match {
          case map: Json.Object if map.size == 1 =>
            map.iterator.next match {
              case (name, fields) => readObject(name, fields)
              case _ => null
            }
        }
        ret += obj
        is = is.tail
      }
      ret.toArray
    case x =>
      println(x)
      x
  }

  private def readObject(name: String, fields: Map[String, _]): Any = {
    try {
      Class.forName(name).newInstance match {
        case x: JsonSerializable => x.readJson(fields); x
        case _ => null
      }
    } catch {case ex: Exception => null}
  }

  def readObject: Any = ret
}
